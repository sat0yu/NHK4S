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
    
    // Auto-scroll parameters
    private val scrollThreshold = 200f // Distance from edge to trigger scroll
    private val scrollAmount = 60 // Pixels to scroll when auto-scrolling (reduced for smoother experience)
    
    // Cursor dimensions (dp converted to pixels)
    private val cursorSizePx by lazy {
        val dp = 32f
        (dp * resources.displayMetrics.density).toInt()
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
        
        // Initialize cursor position at screen center
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        
        cursorX = screenWidth / 2f
        cursorY = screenHeight / 2f
        
        // Set initial position immediately
        virtualCursor.translationX = cursorX
        virtualCursor.translationY = cursorY
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
        
        // Enable scrolling
        webView.isVerticalScrollBarEnabled = true
        webView.isHorizontalScrollBarEnabled = true
        webView.settings.setSupportZoom(false) // Disable zoom to prevent scroll conflicts

        // Set a WebViewClient to handle page navigation within the WebView
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                url?.let { view?.loadUrl(it) }
                return true // Indicates that the WebView handles the URL
            }
            
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // Debug: Check if page is scrollable
                checkScrollability()
            }
        }

        // Load the NHK for School website
        // IMPORTANT: Replace with the actual URL
        webView.loadUrl("https://www.nhk.or.jp/school/") // Example URL, verify the correct one
    }
    
    private fun setupVirtualCursor() {
        // Multiple approaches to ensure key event capture
        
        // Method 1: Make activity focusable
        this.window.decorView.isFocusable = true
        this.window.decorView.isFocusableInTouchMode = true
        this.window.decorView.requestFocus()
        
        // Method 2: Also make the cursor view focusable
        virtualCursor.isFocusable = true
        virtualCursor.isFocusableInTouchMode = true
        virtualCursor.requestFocus()
        
        // Method 3: Ensure WebView doesn't steal focus
        webView.isFocusable = false
        webView.isFocusableInTouchMode = false
        
        // Set initial position
        updateCursorPosition()
    }
    
    private fun updateCursorPosition() {
        // Get screen dimensions
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        
        // Apply safe boundary constraints (use smaller safety margin)
        val safetyMargin = 50f // Use fixed safety margin instead of cursorSizePx
        cursorX = cursorX.coerceIn(0f, screenWidth - safetyMargin)
        cursorY = cursorY.coerceIn(0f, screenHeight - safetyMargin)
        
        // Update cursor position
        virtualCursor.translationX = cursorX
        virtualCursor.translationY = cursorY
    }
    
    private fun smoothScrollBy(deltaX: Int, deltaY: Int) {
        // Try multiple scroll methods to ensure it works
        
        // Method 1: Native WebView scroll
        webView.scrollBy(deltaX, deltaY)
        
        // Method 2: JavaScript scroll (as backup)
        if (deltaY != 0) {
            val jsCode = "window.scrollBy(0, $deltaY);"
            webView.evaluateJavascript(jsCode, null)
        }
        
        // Debug output
        println("Scrolling by deltaY: $deltaY")
    }
    
    private fun checkScrollability() {
        // Check if the page is scrollable
        val javascript = """
            (function() {
                var body = document.body;
                var html = document.documentElement;
                var pageHeight = Math.max(body.scrollHeight, body.offsetHeight, 
                                         html.clientHeight, html.scrollHeight, html.offsetHeight);
                var viewHeight = window.innerHeight;
                return {
                    pageHeight: pageHeight,
                    viewHeight: viewHeight,
                    isScrollable: pageHeight > viewHeight,
                    currentScrollTop: window.pageYOffset || document.documentElement.scrollTop
                };
            })();
        """
        
        webView.evaluateJavascript(javascript) { result ->
            // This will help debug if the page is actually scrollable
            println("Page scroll info: $result")
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle non-D-Pad keys here (volume keys, page keys, etc.)
        when (keyCode) {
            // Test movement with volume keys (direct translation)
            KeyEvent.KEYCODE_VOLUME_UP -> {
                virtualCursor.translationY -= 50f
                return true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                virtualCursor.translationY += 50f
                return true
            }
            // Additional scroll controls for faster navigation
            KeyEvent.KEYCODE_PAGE_UP -> {
                val screenHeight = resources.displayMetrics.heightPixels.toFloat()
                smoothScrollBy(0, -(screenHeight / 2).toInt())
                return true
            }
            KeyEvent.KEYCODE_PAGE_DOWN -> {
                val screenHeight = resources.displayMetrics.heightPixels.toFloat()
                smoothScrollBy(0, (screenHeight / 2).toInt())
                return true
            }
            KeyEvent.KEYCODE_HOME -> {
                // Scroll to top
                webView.evaluateJavascript("window.scrollTo(0, 0);", null)
                return true
            }
            KeyEvent.KEYCODE_MOVE_END -> {
                // Scroll to bottom
                webView.evaluateJavascript("window.scrollTo(0, document.body.scrollHeight);", null)
                return true
            }
            // Let D-Pad keys fall through to dispatchKeyEvent
            KeyEvent.KEYCODE_DPAD_UP, 
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // Don't handle D-Pad here, let dispatchKeyEvent handle them
                return super.onKeyDown(keyCode, event)
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    // Try alternative key event method
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            val screenWidth = resources.displayMetrics.widthPixels.toFloat()
            val screenHeight = resources.displayMetrics.heightPixels.toFloat()
            
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    // Move cursor up
                    val newY = virtualCursor.translationY - movementSpeed
                    virtualCursor.translationY = newY.coerceAtLeast(0f)
                    
                    // Check for scroll when near top edge
                    if (virtualCursor.translationY <= scrollThreshold) {
                        println("Cursor near top (${virtualCursor.translationY}), scrolling up")
                        smoothScrollBy(0, -scrollAmount)
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    // Move cursor down
                    val newY = virtualCursor.translationY + movementSpeed
                    val maxY = screenHeight - 50f // safety margin
                    virtualCursor.translationY = newY.coerceAtMost(maxY)
                    
                    // Check for scroll when near bottom edge
                    val distanceFromBottom = screenHeight - virtualCursor.translationY
                    if (distanceFromBottom <= scrollThreshold) {
                        println("Cursor near bottom (distance: $distanceFromBottom), scrolling down")
                        smoothScrollBy(0, scrollAmount)
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    // Move cursor left
                    val newX = virtualCursor.translationX - movementSpeed
                    virtualCursor.translationX = newX.coerceAtLeast(0f)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    // Move cursor right
                    val newX = virtualCursor.translationX + movementSpeed
                    val maxX = screenWidth - 50f // safety margin
                    virtualCursor.translationX = newX.coerceAtMost(maxX)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
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