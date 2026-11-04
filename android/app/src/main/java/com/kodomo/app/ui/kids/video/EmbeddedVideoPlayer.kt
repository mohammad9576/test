package com.kodomo.app.ui.kids.video

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
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
import com.kodomo.app.domain.model.VideoSource

/**
 * A WebView that can only show one iframe player. Top-level navigations, pop-ups, links to the platform's site and
 * file downloads are all blocked, so an approved Aparat / YouTube / Telewebion video cannot lead the child to the web.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedVideoPlayer(source: VideoSource.Embed, modifier: Modifier = Modifier) {
    var fullscreenView by remember { mutableStateOf<View?>(null) }
    val html = remember(source) {
        """
        <!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
        <style>html,body{margin:0;height:100%;background:#000;overflow:hidden}iframe{position:absolute;inset:0;width:100%;height:100%;border:0}</style>
        </head><body>
        <iframe src="${source.embedUrl}" allow="autoplay; encrypted-media; fullscreen; picture-in-picture" allowfullscreen referrerpolicy="strict-origin-when-cross-origin"></iframe>
        </body></html>
        """.trimIndent()
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
                    setDownloadListener { _, _, _, _, _ -> }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            // Allow only sub-frame loads of whitelisted player hosts; block every attempt to leave the player.
                            if (request.isForMainFrame) return true
                            val host = request.url.host.orEmpty()
                            return VideoSource.embedHosts.none { host == it || host.endsWith(".$it") }
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                            fullscreenView = view
                        }

                        override fun onHideCustomView() {
                            fullscreenView = null
                        }
                    }
                    loadDataWithBaseURL("https://kodomo.app/player", html, "text/html", "utf-8", null)
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
