package com.gauravhanna.spy

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class KeyloggerService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}