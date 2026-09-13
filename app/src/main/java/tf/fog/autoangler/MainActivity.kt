package tf.fog.autoangler

import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import tf.fog.autoangler.injector.InjectorMode
import tf.fog.autoangler.overlay.OverlayService
import tf.fog.autoangler.util.Config
import tf.fog.autoangler.util.License

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLicenseStatus: TextView
    private lateinit var tvTrialInfo: TextView
    private lateinit var etRegCode: EditText
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tv_status)
        tvLicenseStatus = findViewById(R.id.tv_license_status)
        tvTrialInfo = findViewById(R.id.tv_trial_info)
        etRegCode = findViewById(R.id.et_reg_code)
        btnRegister = findViewById(R.id.btn_register)

        val btnGrantOverlay = findViewById<Button>(R.id.btn_grant_overlay)
        val btnStartService = findViewById<Button>(R.id.btn_start_service)
        val btnStopService = findViewById<Button>(R.id.btn_stop_service)
        val btnViewLog = findViewById<Button>(R.id.btn_view_log)

        // ── Tap control mode — the ONLY place to pick the mode (the HUD no longer has a
        // mode picker). Saved straight to Config; OverlayService reads it on every START.
        val rgMode = findViewById<RadioGroup>(R.id.rg_mode)
        val rbRoot = findViewById<RadioButton>(R.id.rb_mode_root)
        val rbNonRoot = findViewById<RadioButton>(R.id.rb_mode_a11y)
        when (Config.loadInjectorMode(this)) {
            InjectorMode.ACCESSIBILITY -> rbNonRoot.isChecked = true
            InjectorMode.ROOT -> rbRoot.isChecked = true
        }
        rgMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = if (checkedId == R.id.rb_mode_a11y) InjectorMode.ACCESSIBILITY else InjectorMode.ROOT
            Config.saveInjectorMode(this, mode)
            toast("Mode: ${if (mode == InjectorMode.ACCESSIBILITY) "Non-Root (Accessibility)" else "Root"}")
        }

        btnGrantOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            } else {
                toast("Overlay permission already granted")
            }
        }

        btnStartService.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                toast("Grant overlay permission first!")
                return@setOnClickListener
            }
            requestNotificationPermission()
            ContextCompat.startForegroundService(this,
                Intent(this, OverlayService::class.java))
            toast("Overlay service started")
        }

        btnStopService.setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            toast("Service stopped")
        }

        btnViewLog.setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        btnRegister.setOnClickListener { tryRegister() }

        findViewById<Button>(R.id.btn_about).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        refreshLicenseUi()
        // Re-check the stored code with the server (revocation); never blocks the UI and
        // only clears the code on an explicit rejection — offline stays registered.
        if (License.isRegistered(this)) {
            Thread {
                License.refresh(this)
                runOnUiThread { refreshLicenseUi() }
            }.also { it.isDaemon = true }.start()
        }

        // Dibuka otomatis oleh FileLog setelah force-close (lihat FileLog.scheduleAutoRestart).
        // Kalau izin overlay sudah ada, langsung start service — restart tinggal tap
        // START di HUD + izin capture (izin capture tak bisa diotomatisasi, wajib
        // persetujuan user tiap kali demi keamanan Android).
        if (intent?.getBooleanExtra("AUTO_RESTART", false) == true) {
            toast("App force-closed — reopened automatically")
            if (Settings.canDrawOverlays(this)) {
                requestNotificationPermission()
                ContextCompat.startForegroundService(this, Intent(this, OverlayService::class.java))
                toast("Service started automatically — tap START on the HUD to continue")
            }
        }
    }

    private fun tryRegister() {
        val code = etRegCode.text.toString().trim()
        if (code.isEmpty()) { toast("Enter a registration code first"); return }
        btnRegister.isEnabled = false
        toast("Verifying…")
        Thread {
            val outcome = License.verify(this, code)
            runOnUiThread {
                btnRegister.isEnabled = true
                when (outcome) {
                    License.Outcome.VALID -> {
                        toast("Registered — full mode active")
                        etRegCode.setText("")
                    }
                    License.Outcome.INVALID -> toast("Invalid registration code")
                    License.Outcome.NETWORK_ERROR -> toast("No connection — check internet and try again")
                }
                refreshLicenseUi()
            }
        }.also { it.isDaemon = true }.start()
    }

    private fun refreshLicenseUi() {
        val registered = License.isRegistered(this)
        if (registered) {
            tvLicenseStatus.text = "Mode: Full (registered)"
            tvLicenseStatus.setTextColor(ContextCompat.getColor(this, R.color.status_full))
            tvTrialInfo.visibility = View.GONE
            etRegCode.visibility = View.GONE
            btnRegister.visibility = View.GONE
        } else {
            tvLicenseStatus.text = "Mode: Trial"
            tvLicenseStatus.setTextColor(ContextCompat.getColor(this, R.color.status_trial))
            tvTrialInfo.visibility = View.VISIBLE
            etRegCode.visibility = View.VISIBLE
            btnRegister.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        refreshLicenseUi()
        val overlayOk = Settings.canDrawOverlays(this)
        tvStatus.text = buildString {
            appendLine("Overlay permission: ${if (overlayOk) "OK" else "NOT GRANTED"}")
            append("Check the HUD overlay once the service is running")
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            if (!nm.areNotificationsEnabled()) {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
            }
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
