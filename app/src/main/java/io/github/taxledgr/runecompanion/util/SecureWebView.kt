package io.github.taxledgr.runecompanion.util

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream

fun WebView.applyPrivateWebSettings() {
    settings.javaScriptEnabled = false
    settings.javaScriptCanOpenWindowsAutomatically = false
    settings.setSupportMultipleWindows(false)
    settings.domStorageEnabled = false
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
    settings.safeBrowsingEnabled = true
    settings.setGeolocationEnabled(false)
    settings.mediaPlaybackRequiresUserGesture = true
    settings.cacheMode = WebSettings.LOAD_NO_CACHE
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
}

class TrustedWikiWebViewClient(
    private val onExternalUrl: (String) -> Unit,
    private val onPageFinishedCallback: (WebView, String) -> Unit,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean = handle(request.url.toString())

    @Suppress("DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
        handle(url)

    override fun onPageFinished(view: WebView, url: String) {
        if (TrustedUrlPolicy.isWikiUrl(url)) {
            onPageFinishedCallback(view, url)
        }
    }

    private fun handle(url: String): Boolean {
        if (TrustedUrlPolicy.isWikiUrl(url)) return false
        TrustedUrlPolicy.normalizeHttpsUrl(url, TrustedUrlPolicy.externalHosts)
            ?.let(onExternalUrl)
        return true
    }
}

class AllowlistedResourceWebViewClient(
    private val allowedUrls: Set<String>,
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(
        view: WebView,
        request: WebResourceRequest,
    ): Boolean = true

    @Suppress("DEPRECATION")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean = true

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? =
        if (request.url.toString() in allowedUrls) {
            null
        } else {
            WebResourceResponse(
                "text/plain",
                "UTF-8",
                ByteArrayInputStream(ByteArray(0)),
            )
        }
}
