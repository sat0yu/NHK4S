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
    
    // Configuration
    private val showDebugInfo = false
    private val movementSpeed = 20f
    private val scrollThreshold = 200f
    private val scrollAmount = 60
    private val cursorMargin = 50f

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
    
    private fun moveCursor(keyCode: Int) {
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
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    moveCursor(event.keyCode)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    simulateClick(virtualCursor.translationX, virtualCursor.translationY)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}