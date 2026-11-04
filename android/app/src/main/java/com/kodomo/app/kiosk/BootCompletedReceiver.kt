package com.kodomo.app.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kodomo.app.data.prefs.SessionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-applies Device Owner policies after reboot/update so the kiosk survives restarts. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val manager = DeviceOwnerManager(context.applicationContext)
                if (!manager.isDeviceOwner) {
                    // Without Device Owner, Android shows the default Home app after boot. If Kodomo is that app it starts
                    // on its own; otherwise try to open it (allowed on many ROMs right after boot, ignored where blocked).
                    if (!manager.isDefaultHome && intent.action == Intent.ACTION_BOOT_COMPLETED) {
                        runCatching {
                            context.startActivity(Intent(context, KidsLauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                    return
                }
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val state = SessionPreferences(context.applicationContext).current()
                        if (state.kioskEnabled) manager.applyKioskPolicies(state.allowedApps)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
