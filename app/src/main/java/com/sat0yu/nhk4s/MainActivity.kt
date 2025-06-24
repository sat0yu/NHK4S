package com.sat0yu.nhk4s

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.tv.material3.ExperimentalTvMaterial3Api

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var virtualCursor: ImageView
    
    // Virtual cursor position
    private var cursorX = 200f
    private var cursorY = 200f
    
    // Movement speed
    private val movementSpeed = 20f
    
    // Cursor dimensions (dp converted to pixels)
    private val cursorSizePx by lazy {
        (32 * resources.displayMetrics.density).toInt()
    }

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupWebView()
        setupVirtualCursor()
    }
    
    private fun initViews() {
        webView = findViewById(R.id.webView)
        virtualCursor = findViewById(R.id.virtualCursor)
        
        // Initialize cursor position based on screen size
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        
        // Start at center of screen
        cursorX = (screenWidth / 2) - (cursorSizePx / 2)
        cursorY = (screenHeight / 2) - (cursorSizePx / 2)
    }
    
    private fun setupWebView() {
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
    
    private fun setupVirtualCursor() {
        // Make the cursor focusable to capture key events
        virtualCursor.isFocusable = true
        virtualCursor.isFocusableInTouchMode = true
        virtualCursor.requestFocus()
        
        // Wait for layout to complete, then set initial position
        virtualCursor.viewTreeObserver.addOnGlobalLayoutListener {
            updateCursorPosition()
        }
    }
    
    private fun updateCursorPosition() {
        // Get screen dimensions
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        
        // Ensure cursor stays within screen bounds
        // Use actual cursor size instead of view dimensions
        cursorX = cursorX.coerceIn(0f, screenWidth - cursorSizePx)
        cursorY = cursorY.coerceIn(0f, screenHeight - cursorSizePx)
        
        // Update cursor position directly without animation to avoid lag
        virtualCursor.translationX = cursorX
        virtualCursor.translationY = cursorY
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                val newY = cursorY - movementSpeed
                if (newY >= 0f) {
                    cursorY = newY
                    updateCursorPosition()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                val newY = cursorY + movementSpeed
                if (newY <= screenHeight - cursorSizePx) {
                    cursorY = newY
                    updateCursorPosition()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                val newX = cursorX - movementSpeed
                if (newX >= 0f) {
                    cursorX = newX
                    updateCursorPosition()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val newX = cursorX + movementSpeed
                if (newX <= screenWidth - cursorSizePx) {
                    cursorX = newX
                    updateCursorPosition()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                // TODO
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
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