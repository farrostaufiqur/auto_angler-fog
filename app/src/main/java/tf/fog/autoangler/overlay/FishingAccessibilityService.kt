package tf.fog.autoangler.overlay

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent
import tf.fog.autoangler.util.FileLog as Log

class FishingAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FishingA11y"
        var instance: FishingAccessibilityService? = null
            private set
        val isEnabled get() = instance != null
    }

    override fun onServiceConnected() {
        instance = this
        Log.d(TAG, "Accessibility service connected — canPerformGestures=${serviceInfo?.capabilities}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    fun performTap(x: Int, y: Int, holdMs: Long) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, holdMs.coerceAtLeast(1L))
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }
}
