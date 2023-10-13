package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class JavaActivity extends AppCompatActivity {

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        WebView webView = findViewById(R.id.web_view);

        CookieManager
            .getInstance()
            .setAcceptThirdPartyCookies(
                    webView,
                    true
            );

        WebSettings webSettings = webView.getSettings();

        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebChromeClient(
            new WebChromeClient() {
                @Override
                public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                    return super.onJsAlert(view, url, message, result);
                }
            }
        );

        webView.setWebViewClient(
            new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    return manageNavigation(url);
                }

                @Override
                public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                    return manageNavigation(request);
                }
            }
        );
    }

    private Boolean manageNavigation(Object webResource) {

        Uri loadingUrl = Uri.parse(
            webResource instanceof String ? (String) webResource : ((WebResourceRequest) webResource).getUrl().toString()
        );

        if (Objects.equals(loadingUrl.getScheme(), "valor")) {

            try {

                Uri uriRedirectPage = Uri.parse(loadingUrl.getQueryParameter("redirect_page"));
                Uri uriPaymentLink = Uri.parse(loadingUrl.getQueryParameter("payment_link"));

                if (Objects.equals(uriPaymentLink.getScheme(), "upi")) {

                    HashMap<String, String> upiApps = new HashMap<String, String>() {{
                        put("PhonePe", "com.phonepe.app");
                        put("Paytm", "net.one97.paytm");
                        put("Google Pay", "com.google.android.apps.nbu.paisa.user");
                    }};

                    ArrayList<String> appsToChoose = new ArrayList<>();

                    for (Map.Entry<String, String> entry : upiApps.entrySet()) {
                        try {
                            getPackageManager().getPackageInfo(entry.getValue(), 0);
                            appsToChoose.add(entry.getKey());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    if (!appsToChoose.isEmpty()) {

                        AlertDialog.Builder builder = new AlertDialog.Builder(this);

                        builder
                            .setTitle("Choose app")
                            .setItems(
                                appsToChoose.toArray(new String[0]),
                                (dialog, which) -> {
                                    fireIntentActionView(
                                        new Uri.Builder()
                                            .scheme(uriRedirectPage.getScheme())
                                            .authority(uriRedirectPage.getAuthority())
                                            .path(uriRedirectPage.getPath())
                                            .appendQueryParameter(
                                                "payment_link",
                                                preparePaymentLink(
                                                    uriPaymentLink,
                                                    appsToChoose.get(which)
                                                )
                                            )
                                            .appendQueryParameter(
                                                "is_supported_link",
                                                String.valueOf(true)
                                            )
                                            .appendQueryParameter(
                                                "comeback_deeplink",
                                                "valor://return"
                                            )
                                            .build()
                                    );
                                }
                            );

                        AlertDialog dialog = builder.create();
                        dialog.show();

                    } else {

                        fireIntentActionView(
                            new Uri.Builder()
                                .scheme(uriRedirectPage.getScheme())
                                .authority(uriRedirectPage.getAuthority())
                                .path(uriRedirectPage.getPath())
                                .appendQueryParameter("payment_link", uriPaymentLink.toString())
                                .appendQueryParameter("is_supported_link", String.valueOf(false))
                                .appendQueryParameter("comeback_deeplink", "valor://return")
                                .build()
                        );
                    }

                    return true;
                }

                fireIntentActionView(
                    new Uri.Builder()
                        .scheme(uriRedirectPage.getScheme())
                        .authority(uriRedirectPage.getAuthority())
                        .path(uriRedirectPage.getPath())
                        .appendQueryParameter("payment_link", uriPaymentLink.toString())
                        .appendQueryParameter("is_supported_link", isIntentActionAvailableForUri(uriPaymentLink).toString())
                        .appendQueryParameter("comeback_deeplink", "valor://return")
                        .build()
                );

            } catch (Exception e) {
                e.printStackTrace();
            }

            return true;
        }

        if (!Objects.equals(loadingUrl.getScheme(), "http") && !Objects.equals(loadingUrl.getScheme(), "https")) {
            fireIntentActionView(loadingUrl);
            return true;
        }

        return false;
    }

    private void fireIntentActionView(Uri uri) {

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        if (intent.resolveActivity(getPackageManager()) != null) {

            startActivity(intent);

        } else {

            Toast.makeText(
                    this,
                    "Application not installed",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String preparePaymentLink(Uri link, String appName) {

        Uri.Builder paymentLinkBuilder = new Uri.Builder();

        switch (appName) {
            case "PhonePe":
                paymentLinkBuilder.scheme("phonepe")
                    .authority(link.getAuthority())
                    .path(link.getPath())
                    .encodedQuery(link.getQuery());
                break;
            case "Google Pay":
                paymentLinkBuilder.scheme("tez")
                    .authority("upi")
                    .path("pay")
                    .encodedQuery(link.getQuery());
                break;
            case "Paytm":
                paymentLinkBuilder.scheme("paytmmp")
                    .authority(link.getAuthority())
                    .path(link.getPath())
                    .encodedQuery(link.getQuery());
                break;
            default:
                paymentLinkBuilder.scheme(link.getScheme())
                    .authority(link.getAuthority())
                    .path(link.getPath())
                    .encodedQuery(link.getQuery());
                break;
        }

        return paymentLinkBuilder.build().toString();
    }

    private Boolean isIntentActionAvailableForUri(Uri uri) {

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        return intent.resolveActivity(getPackageManager()) != null;
    }
}
