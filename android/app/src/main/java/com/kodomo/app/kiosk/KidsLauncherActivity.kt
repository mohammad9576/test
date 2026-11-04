package com.kodomo.app.kiosk

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.kodomo.app.data.prefs.SessionPreferences
import com.kodomo.app.ui.AppViewModel
import com.kodomo.app.ui.KodomoRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single activity of Kodomo. Registered as HOME + LAUNCHER so that, once chosen as the default launcher
 * (or pinned by Device Owner policy), the child cannot leave the Kodomo space without the parent gate.
 */
@AndroidEntryPoint
class KidsLauncherActivity : ComponentActivity(), KioskController {

    @Inject lateinit var deviceOwnerManager: DeviceOwnerManager
    @Inject lateinit var screenTimeManager: ScreenTimeManager
    @Inject lateinit var session: SessionPreferences

    private val appViewModel: AppViewModel by viewModels()
    private var inKidSpace = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) window.setHideOverlayWindows(true)
        if (deviceOwnerManager.isDeviceOwner) {
            lifecycleScope.launch { if (session.current().kioskEnabled) deviceOwnerManager.applyKioskPolicies() }
        }
        setContent {
            CompositionLocalProvider(LocalKioskController provides this) {
                KodomoRoot(appViewModel)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        screenTimeManager.resume()
    }

    override fun onStop() {
        screenTimeManager.pause()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && inKidSpace) applyImmersive(true)
    }

    override fun enterKidSpace() {
        inKidSpace = true
        applyImmersive(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        lifecycleScope.launch {
            if (session.current().kioskEnabled) deviceOwnerManager.enterLockTask(this@KidsLauncherActivity)
        }
    }

    override fun exitKidSpace() {
        inKidSpace = false
        applyImmersive(false)
        setBedtimeDim(false)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        deviceOwnerManager.exitLockTask(this)
        screenTimeManager.endSession()
    }

    override fun setBedtimeDim(enabled: Boolean) {
        window.attributes = window.attributes.apply {
            screenBrightness = if (enabled) 0.05f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
    }

    override fun applyDeviceOwnerPolicies(): Boolean = deviceOwnerManager.applyKioskPolicies()

    override fun requestDeviceAdmin() {
        if (!deviceOwnerManager.isAdminActive) startActivity(deviceOwnerManager.adminActivationIntent())
    }

    private fun applyImmersive(enabled: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (enabled) {
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }
}
