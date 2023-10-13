package com.example.myapplication

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

                if (uriPaymentLink.scheme == "upi") {

                    val upiApps = mutableMapOf(
                        "PhonePe" to "com.phonepe.app",
                        "Paytm" to "net.one97.paytm",
                        "Google Pay" to "com.google.android.apps.nbu.paisa.user"
                    )

                    val appsToChoose = arrayListOf<String>()

                    for (app in upiApps) {

                        runCatching {
                            packageManager.getPackageInfo(app.value, 0)
                            appsToChoose.add(app.key)
                        }
                    }

                    if (appsToChoose.isNotEmpty()) {

                        val builder: AlertDialog.Builder = AlertDialog.Builder(this)

                        builder
                            .setTitle("Choose app")
                            .setItems(
                                appsToChoose.toTypedArray(),
                            ) { _, which ->

                                val uri = Uri.Builder().apply {

                                    scheme(uriRedirectPage.scheme)
                                    authority(uriRedirectPage.authority)
                                    path(uriRedirectPage.path)

                                    appendQueryParameter(
                                        "payment_link",
                                        preparePaymentLink(
                                            link = uriPaymentLink,
                                            appName = appsToChoose[which]
                                        )
                                    )
                                    appendQueryParameter(
                                        "is_supported_link",
                                        true.toString()
                                    )
                                    appendQueryParameter("comeback_deeplink", "valor://return")

                                }.build()

                                fireIntentActionView(uri)
                            }

                        val dialog: AlertDialog = builder.create()
                        dialog.show()

                    } else {

                        fireIntentActionView(
                            Uri.Builder().apply {

                                scheme(uriRedirectPage.scheme)
                                authority(uriRedirectPage.authority)
                                path(uriRedirectPage.path)

                                appendQueryParameter("payment_link", uriPaymentLink.toString())
                                appendQueryParameter("is_supported_link", false.toString())
                                appendQueryParameter("comeback_deeplink", "valor://return")

                            }.build()
                        )
                    }

                    return true
                }

                fireIntentActionView(
                    Uri.Builder().apply {

                        scheme(uriRedirectPage.scheme)
                        authority(uriRedirectPage.authority)
                        path(uriRedirectPage.path)

                        appendQueryParameter("payment_link", uriPaymentLink.toString())
                        appendQueryParameter("is_supported_link", isIntentActionAvailableForUri(uriPaymentLink).toString())
                        appendQueryParameter("comeback_deeplink", "valor://return")

                    }.build()
                )

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

    private fun preparePaymentLink(link: Uri, appName: String): String {

        return Uri.Builder().apply {

            when (appName) {
                "PhonePe" -> {
                    scheme("phonepe")
                    authority(link.authority)
                    path(link.path)
                    encodedQuery(link.query)
                }
                "Google Pay" -> {
                    scheme("tez")
                    authority("upi")
                    path("pay")
                    encodedQuery(link.query)
                }
                "Paytm" -> {
                    scheme("paytmmp")
                    authority(link.authority)
                    path(link.path)
                    encodedQuery(link.query)
                }
                else -> { link.toString() }
            }

        }.build().toString()
    }

    private fun isIntentActionAvailableForUri(uri: Uri): Boolean {

        val intent = Intent(
            Intent.ACTION_VIEW,
            uri
        )

        return intent.resolveActivity(packageManager) != null
    }
}
