package tf.fog.autoangler.injector

interface TouchInjector {
    val isActive: Boolean
    val modeName: String
    fun tap(x: Int, y: Int, holdMs: Long = 50L)
}

enum class InjectorMode { ROOT, ACCESSIBILITY }
