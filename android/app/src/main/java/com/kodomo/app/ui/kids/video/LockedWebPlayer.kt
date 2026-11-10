package com.kodomo.app.ui.kids.video

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import java.io.ByteArrayInputStream

/**
 * How far a [LockedWebPlayer] may wander.
 *
 * - [Pinned]: the child's mode. Only the one approved page (same host and path) can ever be shown; every link, banner,
 *   pop-up, client-side route change or redirect elsewhere is cancelled and the page is restored.
 * - [SameSite]: the parent's picker mode. Browsing is allowed inside the given hosts only (e.g. telewebion.ir/kids).
 */
sealed interface WebLock {
    data class Pinned(val url: String) : WebLock
    /** [anyHost]: the parent's own sign-in and browsing may leave the site (e.g. a separate login domain). */
    data class SameSite(val startUrl: String, val hosts: List<String>, val anyHost: Boolean = false) : WebLock
}

object WebLockRules {
    /** Advertising / tracking networks used on Iranian sites. Requests to them are answered with an empty response. */
    val adHosts = listOf(
        "yektanet.com", "yektanet.ir", "tapsell.ir", "tapsell.com", "adivery.com", "mediaad.org", "mediad.ir", "sabavision.com",
        "anetwork.ir", "adtrace.io", "kaprila.com", "deema.agency", "hezardastan.net", "zarpop.com",
        "doubleclick.net", "googlesyndication.com", "googleadservices.com", "google-analytics.com", "googletagmanager.com",
        "adservice.google.com", "amazon-adsystem.com", "taboola.com", "outbrain.com", "popads.net", "propellerads.com",
        "najva.com", "pushe.co", "metrix.ir", "webengage.com", "hotjar.com", "clarity.ms", "facebook.net", "chabok.io",
    )

    /** Hosts whose pages and players telewebion legitimately needs. */
    val telewebionHosts = listOf("telewebion.ir", "telewebion.com", "twcdn.ir", "telewebion.net")

    fun hostMatches(host: String, list: List<String>) = list.any { host == it || host.endsWith(".$it") }

    fun isAd(host: String) = hostMatches(host, adHosts)

    /** The «same page» test for [WebLock.Pinned]: host family and path must match; query/fragment may differ. */
    fun samePage(allowed: String, candidate: String): Boolean {
        val a = Uri.parse(allowed)
        val b = Uri.parse(candidate)
        val hostA = a.host.orEmpty().removePrefix("www.")
        val hostB = b.host.orEmpty().removePrefix("www.")
        val pa = a.path.orEmpty().trimEnd('/')
        val pb = b.path.orEmpty().trimEnd('/')
        // the approved page and its own sub-pages (e.g. a book's reader at /book/123/read), nothing else
        return hostA == hostB && (pa == pb || (pa.isNotEmpty() && pb.startsWith("$pa/")))
    }

    fun allowsMainFrame(lock: WebLock, url: String): Boolean {
        val uri = Uri.parse(url)
        if (uri.scheme != "https" && uri.scheme != "http") return false
        return when (lock) {
            is WebLock.Pinned -> samePage(lock.url, url)
            is WebLock.SameSite -> lock.anyHost || hostMatches(uri.host.orEmpty().removePrefix("www."), lock.hosts) || hostMatches(uri.host.orEmpty(), lock.hosts)
        }
    }

    /**
     * Injected after every load: cancels link clicks and `window.open`, freezes client-side routing (pushState) in
     * pinned mode and hides site chrome and ad slots, so the child sees only the player.
     */
    fun lockScript(pinned: Boolean, frames: List<String> = telewebionHosts, free: Boolean = false): String = """
        (function(){
          if (window.__kodomoLock) return; window.__kodomoLock = true;
          var pinned = $pinned, free = $free;
          // «Get our app» / cookie / notification pop-ups that cover the page (e.g. Taaghche) are closed.
          var promo = /(اپلیکیشن|اپلیکیشن|نصب|دانلود\s*اپ|برنامه‌ی|نسخه‌ی اندروید|cookie|کوکی|اعلان|notification|install the app|open in app)/i;
          function clearPromos(){
            document.querySelectorAll('div,section,aside,dialog').forEach(function(el){
              var cs = getComputedStyle(el);
              if ((cs.position === 'fixed' || cs.position === 'sticky') && parseInt(cs.zIndex || '0') >= 10) {
                var r = el.getBoundingClientRect();
                var covers = r.width > innerWidth * 0.6 && r.height > 40;
                if (covers && promo.test((el.innerText || '').slice(0, 400)) && !el.querySelector('video')) el.style.setProperty('display', 'none', 'important');
              }
            });
            [document.documentElement, document.body].forEach(function(n){ if (n && getComputedStyle(n).overflow === 'hidden' && !document.querySelector('.kd-player')) n.style.setProperty('overflow', 'auto', 'important'); });
          }
          clearPromos(); var pc = 0; var pt = setInterval(function(){ clearPromos(); if (++pc > 20) clearInterval(pt); }, 1500);
          if (free) return;
          window.open = function(){ return null; };
          document.addEventListener('click', function(e){
            var a = e.target && e.target.closest ? e.target.closest('a,[href],[onclick*="location"]') : null;
            if (!a) return;
            var href = a.getAttribute('href') || '';
            var internal = !pinned && href && (href.charAt(0) === '/' || href.indexOf(location.host) >= 0) && a.target !== '_blank';
            if (!internal) { e.preventDefault(); e.stopImmediatePropagation(); }
          }, true);
          if (pinned) {
            var path = location.pathname;
            var guard = function(orig){ return function(s,t,u){ try { if (u) { var p = new URL(u, location.href); if (p.pathname !== path) return; } } catch(x) { return; } return orig.apply(this, arguments); }; };
            history.pushState = guard(history.pushState);
            history.replaceState = guard(history.replaceState);
          }
          var css = pinned
            ? 'header,footer,nav,aside,[class*="navbar" i],[class*="sidebar" i],[class*="banner" i],[class*="advert" i],[id*="advert" i],[class*="yektanet" i],[id*="yektanet" i],[class*="tapsell" i],[class*="download-app" i],[class*="app-promo" i],[class*="comment" i],[class*="share" i],[class*="social" i],iframe${frames.joinToString("") { ":not([src*=\"" + it.substringBefore('.') + "\"])" }},a[target="_blank"]{display:none!important;}'
            : '[class*="advert" i],[id*="advert" i],[class*="yektanet" i],[id*="yektanet" i],[class*="tapsell" i]{display:none!important;}';
          var st = document.createElement('style'); st.textContent = css;
          (document.head || document.documentElement).appendChild(st);
          document.addEventListener('contextmenu', function(e){ e.preventDefault(); }, true);
        })();
    """.trimIndent()

    /**
     * Child mode: the video always plays full screen. The first time the child taps the page (or the site's own play
     * button starts a video) the largest video goes into the browser's own fullscreen, which Kodomo shows sideways
     * over everything (onShowCustomView). The picture fills the screen edge to edge, without black bars. Nothing on
     * the page is hidden, so pages without a video (sign-in, subscription) still work.
     */
    const val AUTO_FULLSCREEN_JS = """
(function(){
  if (window.__kdAutoFull) return; window.__kdAutoFull = true;
  var st = document.createElement('style');
  st.textContent = 'video:fullscreen,video:-webkit-full-screen,:fullscreen video,:-webkit-full-screen video{object-fit:cover!important;width:100%!important;height:100%!important;background:#000!important}';
  (document.head || document.documentElement).appendChild(st);
  function area(e){ var r = e.getBoundingClientRect(); return r.width * r.height; }
  function biggest(){ var best = null, a = 0; document.querySelectorAll('video').forEach(function(v){ var x = area(v); if (x > a) { a = x; best = v; } }); return best; }
  function isFull(){ return !!(document.fullscreenElement || document.webkitFullscreenElement); }
  function goFull(v){
    if (!v || isFull()) return;
    try { var f = v.requestFullscreen || v.webkitRequestFullscreen || v.webkitEnterFullscreen; if (f) { var p = f.call(v); if (p && p.catch) p.catch(function(){}); } } catch(e) {}
  }
  document.addEventListener('play', function(e){ if (e.target && e.target.tagName === 'VIDEO') goFull(e.target); }, true);
  document.addEventListener('touchend', function(){
    var v = biggest(); if (!v) return;
    if (v.paused) { try { var p = v.play(); if (p && p.catch) p.catch(function(){}); } catch(e) {} }
    goFull(v);
  }, true);
})();
"""

    private val emptyBody = ByteArray(0)
    fun empty() = WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(emptyBody))
}

/**
 * A WebView locked to [lock]. Used for Telewebion pages (live «پویا» channel and approved cartoons), which only play
 * inside their own page. [onPageChanged] reports the current URL and title (used by the parent picker).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LockedWebPlayer(
    lock: WebLock,
    modifier: Modifier = Modifier,
    onPageChanged: (url: String, title: String) -> Unit = { _, _ -> },
    onWebView: (WebView) -> Unit = {},
    visible: Boolean = true,
    /** Child mode: the video jumps to full screen (sideways, edge to edge) as soon as it plays. */
    focusPlayer: Boolean = true,
) {
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var webRef by remember { mutableStateOf<WebView?>(null) }
    var fullscreenCallback by remember { mutableStateOf<android.webkit.WebChromeClient.CustomViewCallback?>(null) }
    val pinned = lock is WebLock.Pinned
    val home = when (lock) {
        is WebLock.Pinned -> lock.url
        is WebLock.SameSite -> lock.startUrl
    }
    // Frames (players) may only come from the locked site itself.
    val frameHosts = remember(lock) {
        when (lock) {
            is WebLock.Pinned -> (ContentProvider.forUrl(lock.url)?.hosts ?: WebLockRules.telewebionHosts) + Uri.parse(lock.url).host.orEmpty().removePrefix("www.")
            is WebLock.SameSite -> lock.hosts
        }
    }

    val free = lock is WebLock.SameSite && lock.anyHost
    val script = remember(pinned, frameHosts, free) { WebLockRules.lockScript(pinned, frameHosts, free) }

    Box(modifier.background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    settings.setSupportMultipleWindows(false)
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.setGeolocationEnabled(false)
                    settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    // Sign-in of Filimo / Namava / Filmnet uses cookies across their own domains.
                    val webSelf = this
                    android.webkit.CookieManager.getInstance().let { it.setAcceptCookie(true); it.setAcceptThirdPartyCookies(webSelf, true) }
                    // Some sites refuse WebViews; present a normal mobile Chrome user agent.
                    settings.userAgentString = settings.userAgentString.replace("; wv", "").replace("Version/4.0 ", "")
                    isLongClickable = false
                    setOnLongClickListener { true }
                    setDownloadListener { _, _, _, _, _ -> }
                    webViewClient = object : WebViewClient() {
                        private var bounces = 0
                        private var bounceWindowStart = 0L

                        /** Back to the approved page; a site that keeps redirecting away is stopped instead of looping. */
                        private fun restore(view: WebView) {
                            view.stopLoading()
                            val now = System.currentTimeMillis()
                            if (now - bounceWindowStart > 10_000) { bounceWindowStart = now; bounces = 0 }
                            if (++bounces > 4) {
                                view.loadDataWithBaseURL(null, "<html><body style='background:#000;color:#fff;font-family:sans-serif;text-align:center;padding-top:40%'>این صفحه در دسترس نیست 🙈</body></html>", "text/html", "utf-8", null)
                            } else {
                                view.loadUrl(home)
                            }
                        }

                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val url = request.url.toString()
                            if (request.isForMainFrame) return !WebLockRules.allowsMainFrame(lock, url)
                            // Sub-frames: only the site's own player frames.
                            return !free && !WebLockRules.hostMatches(request.url.host.orEmpty(), frameHosts)
                        }

                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                            val host = request.url.host.orEmpty()
                            if (WebLockRules.isAd(host)) return WebLockRules.empty()
                            // A foreign HTML document (an ad iframe) is never loaded.
                            val accept = request.requestHeaders["Accept"].orEmpty()
                            if (!request.isForMainFrame && accept.startsWith("text/html") &&
                                !free && !WebLockRules.hostMatches(host, frameHosts)
                            ) return WebLockRules.empty()
                            return null
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            loadError = null
                            if (url.startsWith("http") && !WebLockRules.allowsMainFrame(lock, url)) restore(view)
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            if (!url.startsWith("http")) return
                            view.evaluateJavascript(script, null)
                            if (pinned && focusPlayer) view.evaluateJavascript(WebLockRules.AUTO_FULLSCREEN_JS, null)
                            onPageChanged(url, view.title.orEmpty())
                        }

                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                            if (request.isForMainFrame) loadError = "${error.errorCode}: ${error.description} \n${request.url.host}"
                        }

                        override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                            if (request.isForMainFrame && response.statusCode >= 400) loadError = "HTTP ${response.statusCode} \n${request.url.host}"
                        }

                        override fun onReceivedSslError(view: WebView, handler: android.webkit.SslErrorHandler, error: android.net.http.SslError) {
                            handler.cancel()
                            loadError = "SSL ${error.primaryError} \n${android.net.Uri.parse(error.url).host}"
                        }

                        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                            // Catches client-side (SPA) route changes that never reach shouldOverrideUrlLoading.
                            if (!url.startsWith("http")) return
                            if (!WebLockRules.allowsMainFrame(lock, url)) {
                                restore(view)
                                return
                            }
                            view.evaluateJavascript(script, null)
                            onPageChanged(url, view.title.orEmpty())
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                            fullscreenView = view
                            fullscreenCallback = callback
                        }

                        override fun onHideCustomView() {
                            fullscreenView = null
                            fullscreenCallback = null
                        }

                        override fun onReceivedTitle(view: WebView, title: String?) {
                            onPageChanged(view.url.orEmpty(), title.orEmpty())
                        }

                        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?) = false
                    }
                    onWebView(this)
                    webRef = this
                    loadUrl(home)
                }
            },
            update = { web ->
                // Hidden (e.g. while the parent reads the kodoumo review): pause it so no video surface draws over the UI.
                val target = if (visible) View.VISIBLE else View.GONE
                if (web.visibility != target) {
                    web.visibility = target
                    if (visible) web.onResume() else web.onPause()
                }
            },
        )
        loadError?.let { err ->
        Box(Modifier.fillMaxSize().background(Color(0xFF101826)), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                androidx.compose.material3.Text("صفحه باز نشد 😕", color = Color.White, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                androidx.compose.material3.Text("اینترنت یا فیلترشکن را بررسی کنید. اگر تکرار شد، این متن را برای پشتیبانی بفرستید:", color = Color.White.copy(alpha = 0.8f), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                androidx.compose.material3.Text(err, color = Color(0xFFFFD54F), style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
                androidx.compose.material3.Button(onClick = { loadError = null; webRef?.loadUrl(home) }) { androidx.compose.material3.Text("تلاش دوباره") }
            }
        }
        }
    }
    // The site's own «fullscreen» button: whole screen, turned sideways, with Kodomo's back button.
    fullscreenView?.let { view ->
        val exit = { runCatching { fullscreenCallback?.onCustomViewHidden() }; fullscreenView = null; fullscreenCallback = null }
        androidx.compose.ui.window.Dialog(onDismissRequest = exit, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
            Landscape()
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        FrameLayout(context).apply {
                            setBackgroundColor(android.graphics.Color.BLACK)
                            (view.parent as? ViewGroup)?.removeView(view)
                            addView(view, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                        }
                    },
                )
                FullscreenBackButton(exit, Modifier.align(Alignment.TopStart))
            }
        }
    }
    DisposableEffect(Unit) { onDispose { fullscreenView = null } }
}

/** Turns the screen sideways while composed (fullscreen video), back to normal afterwards. */
@Composable
fun Landscape() {
    val activity = androidx.compose.ui.platform.LocalContext.current.findActivity()
    DisposableEffect(activity) {
        val before = activity?.requestedOrientation
        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose { activity?.requestedOrientation = before ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    Immersive()
}

/** Hides the status and navigation bars (and uses the camera cut-out area) so the video fills the glass edge to edge. */
@Composable
fun Immersive() {
    val view = androidx.compose.ui.platform.LocalView.current
    val window = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        ?: view.context.findActivity()?.window
    DisposableEffect(window) {
        val w = window ?: return@DisposableEffect onDispose {}
        val controller = androidx.core.view.WindowCompat.getInsetsController(w, w.decorView)
        val cutoutBefore = if (android.os.Build.VERSION.SDK_INT >= 28) w.attributes.layoutInDisplayCutoutMode else 0
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            w.attributes = w.attributes.apply { layoutInDisplayCutoutMode = android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        }
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w, false)
        controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
        onDispose {
            controller.show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            if (android.os.Build.VERSION.SDK_INT >= 28) w.attributes = w.attributes.apply { layoutInDisplayCutoutMode = cutoutBefore }
        }
    }
}

@Composable
fun FullscreenBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier.padding(16.dp).background(Color.Black.copy(alpha = 0.55f), androidx.compose.foundation.shape.CircleShape),
    ) {
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "خروج از تمام صفحه", tint = Color.White)
    }
}

private fun android.content.Context.findActivity(): android.app.Activity? {
    var c: android.content.Context? = this
    while (c is android.content.ContextWrapper) { if (c is android.app.Activity) return c; c = c.baseContext }
    return null
}
