package com.sat0yu.nhk4s

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var virtualCursor: ImageView
    private lateinit var debugCoordinates: TextView
    
    // Configuration
    private val showDebugInfo = false
    private val movementSpeed = 20f
    private val scrollThreshold = 200f
    private val scrollAmount = 60
    private val cursorMargin = 50f
    private val cursorHideDelay = 5000L // 5 seconds
    private val doublePressDelay = 500L // 500ms for double press detection
    
    // Allowed domains for security
    private val allowedDomains = listOf(
        "nhk.or.jp",
        "www.nhk.or.jp"
    )
    
    // Cursor auto-hide functionality
    private val hideHandler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { hideCursor() }
    
    // Fullscreen + Play/pause toggle functionality  
    private var lastCenterPressTime = 0L

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
        debugCoordinates = findViewById(R.id.debugCoordinates)
        
        debugCoordinates.visibility = if (showDebugInfo) android.view.View.VISIBLE else android.view.View.GONE
        
        // Center cursor on screen
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        virtualCursor.translationX = screenWidth / 2f
        virtualCursor.translationY = screenHeight / 2f
        
        updateDebugInfo()
        startCursorHideTimer()
    }
    
    private fun setupWebView() {
        webView.apply {
            settings.javaScriptEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.domStorageEnabled = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.setSupportZoom(false)
            
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return true
                    return if (isUrlAllowed(url)) {
                        false // Allow the WebView to handle the URL
                    } else {
                        true // Block the URL
                    }
                }
                
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    return if (url != null && isUrlAllowed(url)) {
                        false // Allow the WebView to handle the URL
                    } else {
                        true // Block the URL
                    }
                }
            }
            loadUrl("https://www.nhk.or.jp/school/")
        }
    }
    
    private fun isUrlAllowed(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            allowedDomains.any { domain -> 
                host == domain || host.endsWith(".$domain")
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun setupFocus() {
        window.decorView.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        webView.isFocusable = false
        webView.isFocusableInTouchMode = false
    }
    
    private fun scrollWebView(deltaY: Int) {
        webView.scrollBy(0, deltaY)
    }
    
    private fun startCursorHideTimer() {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, cursorHideDelay)
    }
    
    private fun showCursor() {
        virtualCursor.visibility = android.view.View.VISIBLE
        startCursorHideTimer()
    }
    
    private fun hideCursor() {
        virtualCursor.visibility = android.view.View.INVISIBLE
    }
    
    private fun toggleFullscreenAndPlayPause() {
        val jsCode = """
            (function() {
                // Check if video is currently playing (has .play class)
                var mediaContainer = document.querySelector('div#media_container.play');
                var isPlaying = mediaContainer !== null;
                
                // Get fullscreen and play/pause buttons
                var fullscreenBtn = document.querySelector('div#video-controller div.fullsceen-btn > div.btn');
                var playBtn = document.querySelector('div#video-controller div.btn-wrap:not(.play) div.play');
                var pauseBtn = document.querySelector('div#video-controller div.btn-wrap.play div.pause');
                
                var results = [];
                
                if (isPlaying) {
                    // Currently playing - exit fullscreen and pause
                    if (fullscreenBtn) {
                        fullscreenBtn.click();
                        results.push("Fullscreen exited");
                    } else {
                        results.push("Fullscreen button not found");
                    }
                    
                    if (pauseBtn) {
                        pauseBtn.click();
                        results.push("Video paused");
                    } else {
                        results.push("Pause button not found");
                    }
                } else {
                    // Currently paused - enter fullscreen and play
                    if (fullscreenBtn) {
                        fullscreenBtn.click();
                        results.push("Fullscreen entered");
                    } else {
                        results.push("Fullscreen button not found");
                    }
                    
                    if (playBtn) {
                        playBtn.click();
                        results.push("Video playing");
                    } else {
                        results.push("Play button not found");
                    }
                }
                
                return results.join(", ");
            })();
        """
        
        webView.evaluateJavascript(jsCode) { result ->
            debugCoordinates.text = "Result: $result"
        }
    }
    
    private fun getCursorCenter(): Pair<Float, Float> {
        return Pair(
            virtualCursor.translationX + (virtualCursor.width / 2f),
            virtualCursor.translationY + (virtualCursor.height / 2f)
        )
    }
    
    private fun updateDebugInfo() {
        val (centerX, centerY) = getCursorCenter()
        debugCoordinates.text = "Cursor: X=${centerX.toInt()}, Y=${centerY.toInt()}"
    }
    
    private fun simulateClick(x: Float, y: Float) {
        val (centerX, centerY) = getCursorCenter()
        simulateNativeTouch(centerX, centerY)
    }
    
    private fun simulateNativeTouch(x: Float, y: Float) {
        val downTime = SystemClock.uptimeMillis()
        val downEvent = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0)
        val upEvent = MotionEvent.obtain(downTime, downTime + 100, MotionEvent.ACTION_UP, x, y, 0)
        
        try {
            webView.dispatchTouchEvent(downEvent)
            webView.dispatchTouchEvent(upEvent)
            debugCoordinates.text = "Native Touch: X=${x.toInt()}, Y=${y.toInt()}\nEvents dispatched"
        } catch (e: Exception) {
            debugCoordinates.text = "Native Touch failed: ${e.message}"
        } finally {
            downEvent.recycle()
            upEvent.recycle()
        }
    }
    
    private fun moveCursor(keyCode: Int) {
        showCursor() // Show cursor when moving
        
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> {
                val newY = (virtualCursor.translationY - movementSpeed).coerceAtLeast(0f)
                virtualCursor.translationY = newY
                if (newY <= scrollThreshold) scrollWebView(-scrollAmount)
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                val maxY = screenHeight - cursorMargin
                val newY = (virtualCursor.translationY + movementSpeed).coerceAtMost(maxY)
                virtualCursor.translationY = newY
                if (screenHeight - newY <= scrollThreshold) scrollWebView(scrollAmount)
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                val newX = (virtualCursor.translationX - movementSpeed).coerceAtLeast(0f)
                virtualCursor.translationX = newX
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val maxX = screenWidth - cursorMargin
                val newX = (virtualCursor.translationX + movementSpeed).coerceAtMost(maxX)
                virtualCursor.translationX = newX
            }
        }
        updateDebugInfo()
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                moveCursor(keyCode)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                showCursor() // Show cursor when clicking
                
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastCenterPressTime < doublePressDelay) {
                    // Double tap detected - toggle fullscreen and play/pause
                    toggleFullscreenAndPlayPause()
                    lastCenterPressTime = 0L // Reset to prevent triple-tap
                } else {
                    // Single tap - normal click behavior
                    simulateClick(virtualCursor.translationX, virtualCursor.translationY)
                    lastCenterPressTime = currentTime
                }
                return true
            }
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

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onDestroy() {
        hideHandler.removeCallbacks(hideRunnable)
        super.onDestroy()
    }
}