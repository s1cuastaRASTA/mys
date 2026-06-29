package com.dashboard.app.ui.components

import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.dashboard.app.ui.theme.*

@Composable
fun CbWebSdr(
    freqKhz: Int,
    serverUrl: String,
    urlParam: String,
    isPlaying: Boolean,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        if (isPlaying) {
            val targetUrl = "$serverUrl/?$urlParam=$freqKhz"
            var isLoading by remember { mutableStateOf(true) }
            var webView by remember { mutableStateOf<WebView?>(null) }

            DisposableEffect(Unit) {
                onDispose {
                    webView?.apply {
                        stopLoading()
                        loadUrl("about:blank")
                        onPause()
                        destroy()
                    }
                    webView = null
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            setBackgroundColor(android.graphics.Color.BLACK)
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    isLoading = true
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    view?.loadUrl(request?.url.toString())
                                    return true
                                }
                            }
                            webChromeClient = WebChromeClient()
                            webView = this
                            loadUrl(targetUrl)
                        }
                    },
                    update = { view ->
                        if (view.url != targetUrl) {
                            view.loadUrl(targetUrl)
                        }
                        webView = view
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = AccentBlue
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AccentRed.copy(alpha = 0.15f))
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = onStop,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Default.Stop, "Oprește WebSDR", tint = AccentRed, modifier = Modifier.height(28.dp))
                }
            }
        } else {
            Spacer(Modifier.height(4.dp))
        }
    }
}
