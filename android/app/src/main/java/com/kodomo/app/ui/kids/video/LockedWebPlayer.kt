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
    data class SameSite(val startUrl: String, val hosts: List<String>) : WebLock
}

object WebLockRules {
    /** Advertising / tracking networks used on Iranian sites. Requests to them are answered with an empty response. */
    val adHosts = listOf(
        "yektanet.com", "yektanet.ir", "tapsell.ir", "tapsell.com", "adivery.com", "mediaad.org", "mediad.ir", "sabavision.com",
        "sabaidea.com", "anetwork.ir", "adtrace.io", "kaprila.com", "deema.agency", "hezardastan.net", "zarpop.com",
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
        return hostA == hostB && a.path.orEmpty().trimEnd('/') == b.path.orEmpty().trimEnd('/')
    }

    fun allowsMainFrame(lock: WebLock, url: String): Boolean {
        val uri = Uri.parse(url)
        if (uri.scheme != "https" && uri.scheme != "http") return false
        return when (lock) {
            is WebLock.Pinned -> samePage(lock.url, url)
            is WebLock.SameSite -> hostMatches(uri.host.orEmpty().removePrefix("www."), lock.hosts) || hostMatches(uri.host.orEmpty(), lock.hosts)
        }
    }

    /**
     * Injected after every load: cancels link clicks and `window.open`, freezes client-side routing (pushState) in
     * pinned mode and hides site chrome and ad slots, so the child sees only the player.
     */
    fun lockScript(pinned: Boolean): String = """
        (function(){
          if (window.__kodomoLock) return; window.__kodomoLock = true;
          window.open = function(){ return null; };
          var pinned = $pinned;
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
            ? 'header,footer,nav,aside,[class*="navbar" i],[class*="sidebar" i],[class*="banner" i],[class*="advert" i],[id*="advert" i],[class*="yektanet" i],[id*="yektanet" i],[class*="tapsell" i],[class*="download-app" i],[class*="app-promo" i],[class*="comment" i],[class*="share" i],[class*="social" i],iframe:not([src*="telewebion"]),a[target="_blank"]{display:none!important;}'
            : '[class*="advert" i],[id*="advert" i],[class*="yektanet" i],[id*="yektanet" i],[class*="tapsell" i]{display:none!important;}';
          var st = document.createElement('style'); st.textContent = css;
          (document.head || document.documentElement).appendChild(st);
          document.addEventListener('contextmenu', function(e){ e.preventDefault(); }, true);
        })();
    """.trimIndent()

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
) {
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    val pinned = lock is WebLock.Pinned
    val home = when (lock) {
        is WebLock.Pinned -> lock.url
        is WebLock.SameSite -> lock.startUrl
    }
    val script = remember(pinned) { WebLockRules.lockScript(pinned) }
    // Frames (players) may only come from the locked site itself.
    val frameHosts = remember(lock) {
        when (lock) {
            is WebLock.Pinned -> WebLockRules.telewebionHosts + Uri.parse(lock.url).host.orEmpty().removePrefix("www.")
            is WebLock.SameSite -> lock.hosts
        }
    }

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
                            return !WebLockRules.hostMatches(request.url.host.orEmpty(), frameHosts)
                        }

                        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                            val host = request.url.host.orEmpty()
                            if (WebLockRules.isAd(host)) return WebLockRules.empty()
                            // A foreign HTML document (an ad iframe) is never loaded.
                            val accept = request.requestHeaders["Accept"].orEmpty()
                            if (!request.isForMainFrame && accept.startsWith("text/html") &&
                                !WebLockRules.hostMatches(host, frameHosts)
                            ) return WebLockRules.empty()
                            return null
                        }

                        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                            if (url.startsWith("http") && !WebLockRules.allowsMainFrame(lock, url)) restore(view)
                        }

                        override fun onPageFinished(view: WebView, url: String) {
                            if (!url.startsWith("http")) return
                            view.evaluateJavascript(script, null)
                            onPageChanged(url, view.title.orEmpty())
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
                        }

                        override fun onHideCustomView() {
                            fullscreenView = null
                        }

                        override fun onReceivedTitle(view: WebView, title: String?) {
                            onPageChanged(view.url.orEmpty(), title.orEmpty())
                        }

                        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message?) = false
                    }
                    onWebView(this)
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
        fullscreenView?.let { view ->
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
        }
    }
    DisposableEffect(Unit) { onDispose { fullscreenView = null } }
}
