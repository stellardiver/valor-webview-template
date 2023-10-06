package com.example.myapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class KotlinActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val webView = findViewById<WebView>(R.id.web_view).apply {

            CookieManager
                .getInstance()
                .setAcceptThirdPartyCookies(
                    this,
                    true
                )

            this.settings.apply {
                javaScriptEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                domStorageEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                mediaPlaybackRequiresUserGesture = false
            }

            webChromeClient = object : android.webkit.WebChromeClient() {

                override fun onJsAlert(
                    view: WebView?,
                    url: String?,
                    message: String?,
                    result: JsResult?
                ): Boolean {
                    return super.onJsAlert(view, url, message, result)
                }
            }

            webViewClient = object : WebViewClient() {

                @Deprecated("DEPRECATION", ReplaceWith("false"))
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    url: String?
                ): Boolean {
                    return url?.let { manageNavigation(it) } ?: false
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    return request?.let { manageNavigation(it) } ?: false
                }
            }
        }
    }

    private fun manageNavigation(webResource: Any): Boolean {

        val loadingUri = Uri.parse(
            when (webResource) {
                is WebResourceRequest -> { webResource.url.toString() }
                else -> { webResource as String }
            }
        )

        if (loadingUri.scheme == "valor") {

            runCatching {

                val uriRedirectPage = Uri.parse(loadingUri.getQueryParameter("redirect_page"))
                val uriPaymentLink = Uri.parse(loadingUri.getQueryParameter("payment_link"))
                val uriForIntent = Uri.Builder().apply {

                    scheme(uriRedirectPage.scheme)
                    authority(uriRedirectPage.authority)
                    path(uriRedirectPage.path)

                    appendQueryParameter("payment_link", uriPaymentLink.toString())
                    appendQueryParameter("is_supported_link", isIntentActionAvailableForUri(uriPaymentLink).toString())
                    appendQueryParameter("comeback_deeplink", "valor://return")

                }.build()

                fireIntentActionView(uriForIntent)

            }.onFailure { e ->
                e.printStackTrace()
            }

            return true
        }

        if (loadingUri.scheme != "http" && loadingUri.scheme != "https") {
            fireIntentActionView(loadingUri)
            return true
        }

        return false
    }

    private fun fireIntentActionView(uri: Uri) {

        val intent = Intent(
            Intent.ACTION_VIEW,
            uri
        )

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(
                this,
                "Application not installed",
                Toast.LENGTH_SHORT
            ).show()
        }

    }

    private fun isIntentActionAvailableForUri(uri: Uri): Boolean {

        val intent = Intent(
            Intent.ACTION_VIEW,
            uri
        )

        return intent.resolveActivity(packageManager) != null
    }
}
