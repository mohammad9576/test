package com.kodomo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.wellbeingStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_wellbeing")

/** Family rules for health and safety in the kids space. */
data class Wellbeing(
    /** Every [eyeBreakMinutes] of use the child rests the eyes for 20 seconds (20-20-20 rule). 0 = off. */
    val eyeBreakMinutes: Int = 20,
    /** Media volume is kept at or below this percentage (hearing protection). 100 = no limit. */
    val maxVolumePercent: Int = 100,
    /** Games unlock only after this many correct practice answers today. 0 = off. */
    val studyFirstAnswers: Int = 0,
    val momPhone: String = "",
    val dadPhone: String = "",
    /** Extra minutes on the Iranian weekend (Thursday and Friday). */
    val weekendExtraMinutes: Int = 0,
    /** Locks the kids space during school hours, Saturday to Wednesday. */
    val schoolLock: Boolean = false,
    val schoolStartMinutes: Int = 7 * 60 + 30,
    val schoolEndMinutes: Int = 13 * 60,
    /** Allowed time windows in minutes of the day (empty = any time); e.g. 13:00-13:15 and 14:30-15:00. */
    val windows: List<IntRange> = emptyList(),
    /** Internet of other apps blocked while the kids space is open (Kodomo itself stays online). */
    val blockOtherInternet: Boolean = false,
    /** Screen brightness in the kids space in percent; 0 = the phone's own setting. */
    val brightnessPercent: Int = 0,
) {
    fun inWindow(now: java.time.LocalTime): Boolean {
        if (windows.isEmpty()) return true
        val m = now.hour * 60 + now.minute
        return windows.any { m in it.first until it.last }
    }

    /** Start of the next allowed window today, in minutes (null if none is left today). */
    fun nextWindow(now: java.time.LocalTime): Int? {
        val m = now.hour * 60 + now.minute
        return windows.map { it.first }.filter { it > m }.minOrNull()
    }

    companion object {
        fun encode(w: List<IntRange>) = w.joinToString(",") { "${it.first}-${it.last}" }
        fun decode(s: String?): List<IntRange> = s.orEmpty().split(',').mapNotNull { part ->
            val (a, b) = part.split('-').map { it.trim().toIntOrNull() }.let { (it.getOrNull(0) ?: return@mapNotNull null) to (it.getOrNull(1) ?: return@mapNotNull null) }
            if (a in 0 until b && b <= 24 * 60) a..b else null
        }.sortedBy { it.first }
        fun hhmm(m: Int) = "%02d:%02d".format(m / 60, m % 60)
        fun parseHhmm(s: String): Int? {
            val p = s.trim().replace('٫', ':').replace('.', ':').split(':')
            val h = p.getOrNull(0)?.toIntOrNull() ?: return null
            val mm = p.getOrNull(1)?.toIntOrNull() ?: 0
            return if (h in 0..24 && mm in 0..59 && h * 60 + mm <= 24 * 60) h * 60 + mm else null
        }
    }
    fun isWeekend(day: java.time.DayOfWeek) = day == java.time.DayOfWeek.THURSDAY || day == java.time.DayOfWeek.FRIDAY

    fun inSchoolHours(now: java.time.LocalDateTime): Boolean {
        if (!schoolLock || isWeekend(now.dayOfWeek)) return false
        val m = now.hour * 60 + now.minute
        return m in schoolStartMinutes until schoolEndMinutes
    }
}

@Singleton
class WellbeingStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val eyeKey = intPreferencesKey("eye_break")
    private val volKey = intPreferencesKey("max_volume")
    private val studyKey = intPreferencesKey("study_first")
    private val momKey = stringPreferencesKey("mom_phone")
    private val dadKey = stringPreferencesKey("dad_phone")
    private val weekendKey = intPreferencesKey("weekend_extra")
    private val schoolKey = booleanPreferencesKey("school_lock")
    private val schoolStartKey = intPreferencesKey("school_start")
    private val schoolEndKey = intPreferencesKey("school_end")
    private val windowsKey = stringPreferencesKey("windows")
    private val netKey = booleanPreferencesKey("block_net")
    private val brightKey = intPreferencesKey("brightness")

    val state: Flow<Wellbeing> = context.wellbeingStore.data.map { p ->
        Wellbeing(
            p[eyeKey] ?: 20, p[volKey] ?: 100, p[studyKey] ?: 0, p[momKey].orEmpty(), p[dadKey].orEmpty(),
            p[weekendKey] ?: 0, p[schoolKey] ?: false, p[schoolStartKey] ?: (7 * 60 + 30), p[schoolEndKey] ?: (13 * 60),
            Wellbeing.decode(p[windowsKey]), p[netKey] ?: false, p[brightKey] ?: 0,
        )
    }

    suspend fun current(): Wellbeing = state.first()

    suspend fun save(w: Wellbeing) = context.wellbeingStore.edit { p ->
        p[eyeKey] = w.eyeBreakMinutes.coerceIn(0, 60)
        p[volKey] = w.maxVolumePercent.coerceIn(20, 100)
        p[studyKey] = w.studyFirstAnswers.coerceIn(0, 50)
        p[momKey] = w.momPhone.filter { it.isDigit() || it == '+' }.take(16)
        p[dadKey] = w.dadPhone.filter { it.isDigit() || it == '+' }.take(16)
        p[weekendKey] = w.weekendExtraMinutes.coerceIn(0, 120)
        p[schoolKey] = w.schoolLock
        p[schoolStartKey] = w.schoolStartMinutes.coerceIn(0, 24 * 60 - 1)
        p[schoolEndKey] = w.schoolEndMinutes.coerceIn(0, 24 * 60)
        p[windowsKey] = Wellbeing.encode(w.windows)
        p[netKey] = w.blockOtherInternet
        p[brightKey] = w.brightnessPercent.coerceIn(0, 100)
    }
}
