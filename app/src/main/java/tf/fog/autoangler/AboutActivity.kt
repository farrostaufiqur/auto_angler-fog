package tf.fog.autoangler

import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Static info screen: app name, what it does, trial vs registration, Root vs Non-Root
 * mode, and credits. Surface/text colors use adaptive tokens that follow the system
 * light / dark theme.
 */
class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.screen_bg_alt))
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }

        fun heading(text: String) = TextView(this).apply {
            this.text = text; setTextColor(color(R.color.text_primary)); textSize = 18f
            setTypeface(Typeface.DEFAULT_BOLD)
            setPadding(0, dp(16), 0, dp(6))
        }

        fun body(text: String) = TextView(this).apply {
            this.text = text; setTextColor(color(R.color.text_secondary)); textSize = 13f
            setLineSpacing(dp(2).toFloat(), 1f)
        }

        root.addView(TextView(this).apply {
            text = "AutoAngler"
            setTextColor(color(R.color.text_primary)); textSize = 24f
            setTypeface(Typeface.DEFAULT_BOLD)
        })
        root.addView(TextView(this).apply {
            text = "About this app"
            setTextColor(color(R.color.text_tertiary)); textSize = 12f
            setPadding(0, 0, 0, dp(8))
        })

        root.addView(heading("What it does"))
        root.addView(body(
            "AutoAngler is an overlay assistant that automates a fishing " +
            "mini-game. It captures the screen, detects the moving cursor and " +
            "the target zone in the fishing bar, and taps the on-screen arrow buttons " +
            "to keep the cursor centered — handling casting, reeling, and catching " +
            "automatically while a floating HUD shows live status over the game."
        ))

        root.addView(heading("Trial vs registration"))
        root.addView(body(
            "Without a registration code the app runs in trial mode: the bot stops " +
            "automatically 3 minutes after fishing starts. Tap START again for another " +
            "3-minute session.\n\n" +
            "Enter a registration code on the main screen to unlock full mode with no time " +
            "limit. The app verifies the code online once (needs internet); after that " +
            "full mode stays on, even offline."
        ))

        root.addView(heading("Full-mode registration"))
        root.addView(body(
            "To get a full-mode registration code (no time limit), contact the developer " +
            "on Telegram: @fog2x."
        ))

        root.addView(heading("Root mode vs Non-Root mode"))
        root.addView(body(
            "Root mode sends taps through a persistent root shell (su / input tap). " +
            "It requires a rooted device (e.g. via KernelSU) and SU permission granted " +
            "to the app, but does not need any extra Android permission beyond that.\n\n" +
            "Non-Root mode uses Android's built-in Accessibility Service to dispatch " +
            "taps directly — no root access is needed at all. It must be enabled once " +
            "in Settings > Accessibility before use.\n\n" +
            "Both modes share the exact same detection logic and timing, so gameplay " +
            "behavior is identical between them — only the way taps are delivered to " +
            "the screen differs."
        ))

        root.addView(TextView(this).apply {
            text = "Made by FOG and Claude AI CLI"
            setTextColor(color(R.color.text_tertiary)); textSize = 12f
            setPadding(0, dp(24), 0, 0)
        })

        val scroll = ScrollView(this).apply { addView(root) }
        setContentView(scroll, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
    }

    private fun color(@ColorRes id: Int) = ContextCompat.getColor(this, id)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
