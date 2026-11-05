package com.kodomo.app.data.prefs

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kodomo.app.R
import com.kodomo.app.domain.learn.Grade
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.learnStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_learning")

/** Parent's rule: every [every] correct answers earn [minutes] of extra play time (granted automatically or by the parent). */
data class StudyReward(val every: Int = 10, val minutes: Int = 10, val automatic: Boolean = true)

data class StudyToday(val correct: Int, val rewardsGranted: Int, val rewardsPending: Int)

data class StudyLog(val at: Long, val childId: Long, val text: String)

/** Progress of «درس و تمرین» and «انگلیسی»: stars per unit, today's correct answers and the play-time reward. */
@Singleton
class LearningStore @Inject constructor(@ApplicationContext private val context: Context) {
    private fun gradeKey(c: Long) = intPreferencesKey("grade_$c")
    private fun starsKey(c: Long, unit: String) = intPreferencesKey("stars_${c}_$unit")
    private fun todayKey(c: Long) = intPreferencesKey("today_$c")
    private fun dateKey(c: Long) = stringPreferencesKey("date_$c")
    private fun grantedKey(c: Long) = intPreferencesKey("granted_$c")
    private fun pendingKey(c: Long) = intPreferencesKey("pending_$c")
    private fun totalKey(c: Long) = intPreferencesKey("total_$c")
    private val everyKey = intPreferencesKey("reward_every")
    private val minutesKey = intPreferencesKey("reward_minutes")
    private val autoKey = booleanPreferencesKey("reward_auto")
    private val logKey = stringPreferencesKey("log")

    fun grade(childId: Long, age: Int): Flow<Grade> = context.learnStore.data.map { p -> p[gradeKey(childId)]?.let { Grade.entries.getOrNull(it) } ?: Grade.forAge(age) }

    suspend fun setGrade(childId: Long, grade: Grade?) = context.learnStore.edit { p -> if (grade == null) p.remove(gradeKey(childId)) else p[gradeKey(childId)] = grade.ordinal }

    fun stars(childId: Long): Flow<Map<String, Int>> = context.learnStore.data.map { p ->
        p.asMap().entries.filter { it.key.name.startsWith("stars_${childId}_") }.associate { it.key.name.removePrefix("stars_${childId}_") to (it.value as? Int ?: 0) }
    }

    suspend fun saveStars(childId: Long, unit: String, stars: Int) = context.learnStore.edit { p ->
        p[starsKey(childId, unit)] = maxOf(p[starsKey(childId, unit)] ?: 0, stars)
    }

    val reward: Flow<StudyReward> = context.learnStore.data.map { p -> StudyReward(p[everyKey] ?: 10, p[minutesKey] ?: 10, p[autoKey] ?: true) }

    suspend fun setReward(r: StudyReward) = context.learnStore.edit { p ->
        p[everyKey] = r.every.coerceIn(3, 50); p[minutesKey] = r.minutes.coerceIn(1, 60); p[autoKey] = r.automatic
    }

    fun today(childId: Long): Flow<StudyToday> = context.learnStore.data.map { p ->
        if (p[dateKey(childId)] != LocalDate.now().toString()) StudyToday(0, 0, 0)
        else StudyToday(p[todayKey(childId)] ?: 0, p[grantedKey(childId)] ?: 0, p[pendingKey(childId)] ?: 0)
    }

    fun totalCorrect(childId: Long): Flow<Int> = context.learnStore.data.map { it[totalKey(childId)] ?: 0 }

    val log: Flow<List<StudyLog>> = context.learnStore.data.map { p ->
        p[logKey].orEmpty().lines().mapNotNull { l -> l.split('|', limit = 3).takeIf { it.size == 3 }?.let { StudyLog(it[0].toLongOrNull() ?: 0, it[1].toLongOrNull() ?: 0, it[2]) } }
    }

    private fun MutablePrefs.addLog(childId: Long, text: String) {
        val lines = this[logKey].orEmpty().lines().filter { it.isNotBlank() }
        this[logKey] = (listOf("${System.currentTimeMillis()}|$childId|$text") + lines).take(60).joinToString("\n")
    }

    /**
     * Counts one correct answer. Returns the minutes to grant now when the rule is reached and set to automatic;
     * otherwise null (a pending reward is recorded for the parent when the rule is manual).
     */
    suspend fun recordCorrect(childId: Long, childName: String): Int? {
        var grant: Int? = null
        var notify: String? = null
        context.learnStore.edit { p ->
            val today = LocalDate.now().toString()
            if (p[dateKey(childId)] != today) {
                p[dateKey(childId)] = today; p[todayKey(childId)] = 0; p[grantedKey(childId)] = 0; p[pendingKey(childId)] = 0
            }
            val n = (p[todayKey(childId)] ?: 0) + 1
            p[todayKey(childId)] = n
            p[totalKey(childId)] = (p[totalKey(childId)] ?: 0) + 1
            val every = p[everyKey] ?: 10
            val minutes = p[minutesKey] ?: 10
            if (n % every == 0) {
                if (p[autoKey] ?: true) {
                    p[grantedKey(childId)] = (p[grantedKey(childId)] ?: 0) + 1
                    grant = minutes
                    notify = "$childName امروز $n تمرین را درست حل کرد و $minutes دقیقه زمان بازی جایزه گرفت."
                } else {
                    p[pendingKey(childId)] = (p[pendingKey(childId)] ?: 0) + 1
                    notify = "$childName امروز $n تمرین را درست حل کرد. جایزه‌ی $minutes دقیقه‌ای منتظر تأیید شماست."
                }
                p.addLog(childId, notify!!)
            }
        }
        notify?.let { notifyParent(it) }
        return grant
    }

    /** Parent approves a pending (manual) reward: returns the minutes to grant. */
    suspend fun approvePending(childId: Long): Int {
        var minutes = 0
        context.learnStore.edit { p ->
            val pending = p[pendingKey(childId)] ?: 0
            if (pending > 0) {
                p[pendingKey(childId)] = pending - 1
                p[grantedKey(childId)] = (p[grantedKey(childId)] ?: 0) + 1
                minutes = p[minutesKey] ?: 10
            }
        }
        return minutes
    }

    suspend fun currentReward(): StudyReward = reward.first()

    private fun notifyParent(text: String) = runCatching {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) nm.createNotificationChannel(NotificationChannel(CHANNEL, "پیشرفت درسی کودک", NotificationManager.IMPORTANCE_DEFAULT))
        nm.notify(
            (System.currentTimeMillis() % 100_000).toInt(),
            NotificationCompat.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("کدومو • درس و تمرین 📚")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setAutoCancel(true)
                .build(),
        )
    }

    private companion object { const val CHANNEL = "kodomo_learning" }
}

private typealias MutablePrefs = androidx.datastore.preferences.core.MutablePreferences
