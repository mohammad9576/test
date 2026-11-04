package com.kodomo.app.kiosk

import android.app.admin.DeviceAdminReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log

/** Device admin entry point. Declared with `BIND_DEVICE_ADMIN` and `@xml/device_admin_policies`. */
class KodomoDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "Kodomo device admin enabled")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i(TAG, "Kodomo device admin disabled")
    }

    override fun onDisableRequested(context: Context, intent: Intent): CharSequence =
        "با غیرفعال کردن محافظ کدومو، قفل امن محیط کودک برداشته می‌شود. این کار فقط باید توسط والدین انجام شود."

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        DeviceOwnerManager(context.applicationContext).applyKioskPolicies()
        context.startActivity(
            Intent(context, KidsLauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    override fun onLockTaskModeEntering(context: Context, intent: Intent, pkg: String) {
        Log.i(TAG, "Lock task entering for $pkg")
    }

    override fun onLockTaskModeExiting(context: Context, intent: Intent) {
        Log.i(TAG, "Lock task exiting")
    }

    companion object {
        private const val TAG = "KodomoAdmin"
        fun component(context: Context): ComponentName = ComponentName(context.applicationContext, KodomoDeviceAdminReceiver::class.java)
    }
}
