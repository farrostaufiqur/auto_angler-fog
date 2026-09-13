package tf.fog.autoangler.injector

import tf.fog.autoangler.overlay.FishingAccessibilityService
import tf.fog.autoangler.util.FileLog as Log

class A11yTouchInjector : TouchInjector {
    override val modeName = "Accessibility"
    override val isActive get() = FishingAccessibilityService.isEnabled

    override fun tap(x: Int, y: Int, holdMs: Long) {
        val svc = FishingAccessibilityService.instance
        if (svc != null) {
            svc.performTap(x, y, holdMs)
        } else {
            Log.w("A11yInjector", "Service belum aktif — aktifkan di Pengaturan > Aksesibilitas")
        }
    }
}
