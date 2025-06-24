package com.sat0yu.nhk4s

import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var virtualCursor: ImageView
    
    // Virtual cursor movement and scroll parameters
    private val movementSpeed = 20f
    private val scrollThreshold = 200f
    private val scrollAmount = 60

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupWebView()
        setupFocus()
    }
    
    private fun initViews() {
        webView = findViewById(R.id.webView)
        virtualCursor = findViewById(R.id.virtualCursor)
        
        // Center cursor on screen
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        
        virtualCursor.translationX = screenWidth / 2f
        virtualCursor.translationY = screenHeight / 2f
    }
    
    private fun setupWebView() {
        webView.apply {
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.setSupportZoom(false)
            
            webViewClient = WebViewClient()
            loadUrl("https://www.nhk.or.jp/school/")
        }
    }
    
    private fun setupFocus() {
        // Ensure D-Pad events are captured
        window.decorView.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        
        // Prevent WebView from stealing focus
        webView.isFocusable = false
        webView.isFocusableInTouchMode = false
    }
    
    private fun scrollWebView(deltaY: Int) {
        webView.scrollBy(0, deltaY)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_PAGE_UP -> {
                scrollWebView(-(resources.displayMetrics.heightPixels / 2))
                return true
            }
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                scrollWebView(resources.displayMetrics.heightPixels / 2)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val screenWidth = resources.displayMetrics.widthPixels.toFloat()
            val screenHeight = resources.displayMetrics.heightPixels.toFloat()
            
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    val newY = (virtualCursor.translationY - movementSpeed).coerceAtLeast(0f)
                    virtualCursor.translationY = newY
                    
                    if (newY <= scrollThreshold) {
                        scrollWebView(-scrollAmount)
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    val maxY = screenHeight - 50f
                    val newY = (virtualCursor.translationY + movementSpeed).coerceAtMost(maxY)
                    virtualCursor.translationY = newY
                    
                    if (screenHeight - newY <= scrollThreshold) {
                        scrollWebView(scrollAmount)
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    val newX = (virtualCursor.translationX - movementSpeed).coerceAtLeast(0f)
                    virtualCursor.translationX = newX
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    val maxX = screenWidth - 50f
                    val newX = (virtualCursor.translationX + movementSpeed).coerceAtMost(maxX)
                    virtualCursor.translationX = newX
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // Handle back button for WebView navigation
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}