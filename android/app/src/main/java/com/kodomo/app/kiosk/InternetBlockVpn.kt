package com.kodomo.app.kiosk

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

/**
 * «اینترنت خاموش» for the kids space: a local VPN that swallows the traffic of every other app (nothing leaves the
 * phone), while Kodomo itself stays online so approved cartoons and books still work. Android does not let an app
 * switch Wi-Fi or mobile data off, so this is the reliable way to cut the internet of games and other apps.
 * The parent confirms Android's VPN dialog once; afterwards it is switched on and off from the parent hub.
 */
class InternetBlockVpn : VpnService() {
    private var tun: ParcelFileDescriptor? = null
    private var current: Set<String> = emptySet()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) { stopVpn(); stopSelf(); return START_NOT_STICKY }
        val keepOnline = intent?.getStringArrayExtra(EXTRA_KEEP)?.toSet().orEmpty()
        if (tun != null && keepOnline != current) stopVpn()
        current = keepOnline
        if (tun == null) {
            tun = runCatching {
                Builder()
                    .apply { keepOnline.forEach { pkg -> runCatching { addDisallowedApplication(pkg) } } }
                    .setSession("کدومو: اینترنت برنامه‌های دیگر خاموش")
                    .addAddress("10.111.0.2", 32)
                    .addAddress("fd00:111::2", 128)
                    .addRoute("0.0.0.0", 0)
                    .addRoute("::", 0)
                    .addDisallowedApplication(packageName)
                    .setBlocking(false)
                    .establish()
            }.onFailure { Log.w("InternetBlockVpn", "VPN not started", it) }.getOrNull()
            running = tun != null
        }
        // The tunnel is never read: packets of other apps simply go nowhere.
        return START_STICKY
    }

    private fun stopVpn() {
        runCatching { tun?.close() }
        tun = null
        running = false
    }

    override fun onRevoke() { stopVpn(); super.onRevoke() }

    override fun onDestroy() { stopVpn(); super.onDestroy() }

    companion object {
        private const val ACTION_STOP = "com.kodomo.app.STOP_BLOCK"
        private const val EXTRA_KEEP = "keep_online"
        @Volatile var running = false
            private set

        /** The consent intent to show first, or null when Android already allows Kodomo's VPN. */
        fun consentIntent(context: Context): Intent? = VpnService.prepare(context)

        /** Blocks every app except Kodomo and [keepOnline] (the parent-approved apps, e.g. Telewebion or Filimo). */
        fun start(context: Context, keepOnline: Set<String> = emptySet()) {
            if (VpnService.prepare(context) != null) return
            runCatching { context.startService(Intent(context, InternetBlockVpn::class.java).putExtra(EXTRA_KEEP, keepOnline.toTypedArray())) }
        }

        fun stop(context: Context) {
            runCatching { context.startService(Intent(context, InternetBlockVpn::class.java).setAction(ACTION_STOP)) }
        }
    }
}
