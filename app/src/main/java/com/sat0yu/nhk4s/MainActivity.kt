package com.sat0yu.nhk4s

import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
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
    
    // Debug flag - set to true to show cursor coordinates
    private val showDebugInfo = false
    
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
        debugCoordinates = findViewById(R.id.debugCoordinates)
        
        // Show/hide debug info based on flag
        debugCoordinates.visibility = if (showDebugInfo) android.view.View.VISIBLE else android.view.View.GONE
        
        // Center cursor on screen
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        
        virtualCursor.translationX = screenWidth / 2f
        virtualCursor.translationY = screenHeight / 2f
        
        updateDebugInfo()
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
    
    private fun updateDebugInfo() {
        val cursorCenterX = virtualCursor.translationX + (virtualCursor.width / 2f)
        val cursorCenterY = virtualCursor.translationY + (virtualCursor.height / 2f)
        debugCoordinates.text = "Cursor: X=${cursorCenterX.toInt()}, Y=${cursorCenterY.toInt()}"
    }
    
    private fun simulateClick(x: Float, y: Float) {
        // Calculate cursor center position for accurate clicking
        val cursorCenterX = x + (virtualCursor.width / 2f)
        val cursorCenterY = y + (virtualCursor.height / 2f)
        
        // Use native Android touch events
        simulateNativeTouch(cursorCenterX, cursorCenterY)
    }
    
    private fun simulateNativeTouch(x: Float, y: Float) {
        // Create native Android touch events
        val downTime = SystemClock.uptimeMillis()
        val eventTime = SystemClock.uptimeMillis()
        
        // Touch down event
        val downEvent = MotionEvent.obtain(
            downTime, eventTime, MotionEvent.ACTION_DOWN, x, y, 0
        )
        
        // Touch up event
        val upEvent = MotionEvent.obtain(
            downTime, eventTime + 100, MotionEvent.ACTION_UP, x, y, 0
        )
        
        try {
            // Dispatch touch events to WebView
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
                    updateDebugInfo()
                    
                    if (newY <= scrollThreshold) {
                        scrollWebView(-scrollAmount)
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    val maxY = screenHeight - 50f
                    val newY = (virtualCursor.translationY + movementSpeed).coerceAtMost(maxY)
                    virtualCursor.translationY = newY
                    updateDebugInfo()
                    
                    if (screenHeight - newY <= scrollThreshold) {
                        scrollWebView(scrollAmount)
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    val newX = (virtualCursor.translationX - movementSpeed).coerceAtLeast(0f)
                    virtualCursor.translationX = newX
                    updateDebugInfo()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    val maxX = screenWidth - 50f
                    val newX = (virtualCursor.translationX + movementSpeed).coerceAtMost(maxX)
                    virtualCursor.translationX = newX
                    updateDebugInfo()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    // Simulate click at cursor position
                    simulateClick(virtualCursor.translationX, virtualCursor.translationY)
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