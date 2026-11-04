package com.kodomo.app.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.kodomo.app.data.prefs.SessionPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class InstalledApp(val packageName: String, val label: String, val icon: ImageBitmap)

/** Lists launcher apps on the device and manages the parent-approved subset the child may open from Kodomo. */
@Singleton
class InstalledAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: SessionPreferences,
) {
    private val pm: PackageManager get() = context.packageManager

    val allowedPackages: Flow<Set<String>> = session.state.map { it.allowedApps }

    suspend fun launchableApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, 0)
            .asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map { info ->
                InstalledApp(
                    packageName = info.packageName,
                    label = info.loadLabel(pm).toString(),
                    icon = info.loadIcon(pm).toBitmap(width = 128, height = 128).asImageBitmap(),
                )
            }
            .sortedBy { it.label }
            .toList()
    }

    suspend fun allowedApps(allowed: Set<String>): List<InstalledApp> = launchableApps().filter { it.packageName in allowed }

    suspend fun setAllowed(packageName: String, allowed: Boolean) {
        session.setAppAllowed(packageName, allowed)
    }

    fun launchIntent(packageName: String): Intent? =
        pm.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
