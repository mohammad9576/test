package com.kodomo.app.kiosk

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-applies Device Owner policies after reboot/update so the kiosk survives restarts. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_LOCKED_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val manager = DeviceOwnerManager(context.applicationContext)
                if (manager.isDeviceOwner) manager.applyKioskPolicies()
            }
        }
    }
}
