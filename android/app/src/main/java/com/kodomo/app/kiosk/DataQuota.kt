package com.kodomo.app.kiosk

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Process
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.ZoneId

/**
 * Daily internet allowance for the kids space. Counts what Kodomo itself downloads while the kids space is open
 * (online cartoons, live TV, store books) plus what the parent-approved apps used today (when usage access is
 * granted). When the allowance is used up, online sections show «اینترنت امروز تمام شد» while games, offline books
 * and lessons keep working.
 */
object DataQuota {
    data class State(val usedMb: Int = 0, val limitMb: Int = 0) {
        val unlimited get() = limitMb <= 0
        val exhausted get() = !unlimited && usedMb >= limitMb
        val leftMb get() = (limitMb - usedMb).coerceAtLeast(0)
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state

    private const val PREFS = "kodomo_data_quota"
    private fun today() = LocalDate.now().toString()
    private var lastOwnBytes = -1L

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Extra megabytes a parent gave today (from the parent hub). */
    fun addBonus(context: Context, mb: Int) {
        val p = prefs(context)
        p.edit().putInt("bonus_${today()}", p.getInt("bonus_${today()}", 0) + mb).apply()
    }

    fun bonusToday(context: Context) = prefs(context).getInt("bonus_${today()}", 0)

    /** Kodomo's own bytes during kids time today (kept across restarts). */
    private fun ownBytesToday(context: Context, counting: Boolean): Long {
        val p = prefs(context)
        val key = "own_${today()}"
        val now = (TrafficStats.getUidRxBytes(Process.myUid()).coerceAtLeast(0) + TrafficStats.getUidTxBytes(Process.myUid()).coerceAtLeast(0))
        var total = p.getLong(key, 0)
        if (counting && lastOwnBytes in 0..now) total += now - lastOwnBytes
        lastOwnBytes = now
        p.edit().putLong(key, total).apply()
        return total
    }

    /** Bytes the approved apps used since midnight (needs «usage access»; 0 otherwise). */
    private fun appsBytesToday(context: Context, packages: Set<String>): Long {
        if (packages.isEmpty() || !KioskPermissions.hasUsageAccess(context)) return 0
        val nsm = context.getSystemService(NetworkStatsManager::class.java) ?: return 0
        val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = System.currentTimeMillis()
        var sum = 0L
        packages.forEach { pkg ->
            val uid = runCatching { context.packageManager.getApplicationInfo(pkg, 0).uid }.getOrNull() ?: return@forEach
            listOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE).forEach { type ->
                runCatching {
                    @Suppress("DEPRECATION")
                    nsm.queryDetailsForUid(type, null, start, end, uid).use { stats ->
                        val b = NetworkStats.Bucket()
                        while (stats.hasNextBucket()) { stats.getNextBucket(b); sum += b.rxBytes + b.txBytes }
                    }
                }
            }
        }
        return sum
    }

    /** Called every few seconds while the kids space is open. */
    fun refresh(context: Context, limitMb: Int, allowedApps: Set<String>, counting: Boolean = true) {
        val bytes = ownBytesToday(context, counting) + appsBytesToday(context, allowedApps)
        val limit = if (limitMb <= 0) 0 else limitMb + bonusToday(context)
        _state.value = State((bytes / (1024 * 1024)).toInt(), limit)
    }
}
