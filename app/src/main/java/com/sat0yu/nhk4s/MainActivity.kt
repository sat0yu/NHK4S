package com.sat0yu.nhk4s

import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.tv.material3.ExperimentalTvMaterial3Api

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        // Enable JavaScript (important for most modern websites)
        webView.settings.javaScriptEnabled = true

        // Configure WebView for video playback and other common needs
        webView.settings.mediaPlaybackRequiresUserGesture = false // May be needed for autoplay
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.allowFileAccess = true // Be cautious with this setting
        webView.settings.loadsImagesAutomatically = true
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW // If NHK uses HTTP resources on an HTTPS page

        // Set a WebViewClient to handle page navigation within the WebView
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { view?.loadUrl(it) }
                return true // Indicates that the WebView handles the URL
            }
        }

        // Load the NHK for School website
        // IMPORTANT: Replace with the actual URL
        webView.loadUrl("https://www.nhk.or.jp/school/") // Example URL, verify the correct one
    }

    // Handle back button presses to navigate within WebView history
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}