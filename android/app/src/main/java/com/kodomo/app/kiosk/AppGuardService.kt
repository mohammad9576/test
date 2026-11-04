package com.kodomo.app.kiosk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.kodomo.app.R
import com.kodomo.app.data.prefs.SessionPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * «نگهبان برنامه‌ها»: while a child session is active, checks the foreground app every second (Usage Access).
 * Parent-approved apps may run and their time is counted; anything else – the phone's own launcher, Recents, Settings,
 * a non-approved app – or any app once the daily limit/bedtime is reached, sends the child straight back to Kodomo.
 * Works without Device Owner; requires «Usage access» and «Display over other apps» (granted from the parent hub).
 */
@AndroidEntryPoint
class AppGuardService : Service() {

    @Inject lateinit var screenTime: ScreenTimeManager
    @Inject lateinit var session: SessionPreferences

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var lastForeground: String? = null
    private var lastBringFront = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startAsForeground()
        scope.launch { guardLoop() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "محافظ کدومو", NotificationManager.IMPORTANCE_MIN))
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("محیط امن کودک فعال است")
            .setContentText("کدومو زمان و برنامه‌های مجاز را مدیریت می‌کند")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
    }

    private suspend fun guardLoop() {
        while (scope.isActive) {
            delay(900)
            val state = screenTime.state.value
            if (state.child == null) continue
            val fg = foregroundPackage() ?: continue
            if (fg == packageName || fg in ignoredPackages) continue
            val allowed = session.current().allowedApps
            val blocked = state.lock != ScreenTimeManager.LockReason.NONE || fg !in allowed
            if (blocked) {
                bringKodomoToFront()
            } else if (!screenTime.externalAppActive) {
                val label = runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(fg, 0)).toString() }.getOrDefault(fg)
                screenTime.enterExternalApp(label, fg)
            }
        }
    }

    /** Most recent app moved to the foreground, from UsageStats events of the last few seconds. */
    private fun foregroundPackage(): String? {
        val usage = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return lastForeground
        val now = System.currentTimeMillis()
        val events = runCatching { usage.queryEvents(now - 10_000, now) }.getOrNull() ?: return lastForeground
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) lastForeground = event.packageName
        }
        return lastForeground
    }

    private fun bringKodomoToFront() {
        val now = System.currentTimeMillis()
        if (now - lastBringFront < 1_500) return
        lastBringFront = now
        runCatching {
            startActivity(
                Intent(this, KidsLauncherActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }.onFailure { Log.w(TAG, "Cannot bring Kodomo to front (grant «display over other apps»)", it) }
        lastForeground = packageName
    }

    companion object {
        private const val TAG = "AppGuard"
        private const val CHANNEL = "kodomo_guard"
        private const val NOTIFICATION_ID = 41
        private const val ACTION_STOP = "com.kodomo.app.guard.STOP"

        /** System surfaces that may briefly take the foreground (permission dialogs, keyboard, system UI). */
        private val ignoredPackages = setOf(
            "android", "com.android.systemui", "com.android.permissioncontroller", "com.google.android.permissioncontroller",
            "com.google.android.inputmethod.latin", "com.android.inputmethod.latin", "com.miui.securitycenter",
            "com.lbe.security.miui", "com.samsung.android.honeyboard",
        )

        fun start(context: Context) {
            runCatching { androidx.core.content.ContextCompat.startForegroundService(context, Intent(context, AppGuardService::class.java)) }
                .onFailure { Log.w(TAG, "Unable to start guard", it) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AppGuardService::class.java))
        }
    }
}
