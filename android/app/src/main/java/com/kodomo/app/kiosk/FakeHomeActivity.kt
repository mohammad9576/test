package com.kodomo.app.kiosk

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle

/**
 * Disabled-by-default second «home» screen. Briefly enabling it makes Android forget the current default launcher
 * and show its own «choose home app» list (with «Always»), which works on Samsung One UI and other skins where the
 * role dialog or the settings page do not offer Kodomo. It never shows UI itself.
 */
class FakeHomeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }

    companion object {
        fun showHomeChooser(context: Context): Boolean = runCatching {
            val pm = context.packageManager
            val fake = ComponentName(context, FakeHomeActivity::class.java)
            // Changing the set of home screens clears the saved default; the fake one is gone again before asking.
            pm.setComponentEnabledSetting(fake, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(fake, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }.isSuccess
    }
}
