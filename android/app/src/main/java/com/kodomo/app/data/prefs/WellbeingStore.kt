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
    val maxVolumePercent: Int = 60,
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
) {
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

    val state: Flow<Wellbeing> = context.wellbeingStore.data.map { p ->
        Wellbeing(
            p[eyeKey] ?: 20, p[volKey] ?: 60, p[studyKey] ?: 0, p[momKey].orEmpty(), p[dadKey].orEmpty(),
            p[weekendKey] ?: 0, p[schoolKey] ?: false, p[schoolStartKey] ?: (7 * 60 + 30), p[schoolEndKey] ?: (13 * 60),
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
    }
}
