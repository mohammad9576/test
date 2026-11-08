package com.kodomo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

private val Context.plannerStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_planner")

/** One item of the child's daily routine (minutes of the day). */
data class RoutineItem(val minute: Int, val emoji: String, val title: String) {
    val id: String get() = "$minute|$title"
}

/** «برنامه‌ی من»: the daily routine (school days and weekend) and what was ticked off today. */
@Singleton
class PlannerStore @Inject constructor(@ApplicationContext private val context: Context) {
    private fun routineKey(childId: Long) = stringPreferencesKey("routine_$childId")
    private fun doneKey(childId: Long) = stringSetPreferencesKey("done_${childId}_${LocalDate.now()}")

    fun routine(childId: Long, age: Int): Flow<List<RoutineItem>> = context.plannerStore.data.map { p ->
        p[routineKey(childId)]?.let(::decode) ?: defaultRoutine(age)
    }

    fun doneToday(childId: Long): Flow<Set<String>> = context.plannerStore.data.map { it[doneKey(childId)].orEmpty() }

    suspend fun save(childId: Long, items: List<RoutineItem>) = context.plannerStore.edit { it[routineKey(childId)] = encode(items.sortedBy { r -> r.minute }) }

    suspend fun toggle(childId: Long, item: RoutineItem) = context.plannerStore.edit { p ->
        val cur = p[doneKey(childId)].orEmpty()
        p[doneKey(childId)] = if (item.id in cur) cur - item.id else cur + item.id
    }

    companion object {
        private fun encode(l: List<RoutineItem>) = l.joinToString("\n") { "${it.minute}|${it.emoji}|${it.title.replace('\n', ' ')}" }
        private fun decode(s: String): List<RoutineItem> = s.lines().mapNotNull { l ->
            val p = l.split('|', limit = 3)
            if (p.size < 3) null else RoutineItem(p[0].toIntOrNull() ?: return@mapNotNull null, p[1], p[2])
        }.sortedBy { it.minute }

        private fun at(h: Int, m: Int = 0) = h * 60 + m

        fun defaultRoutine(age: Int): List<RoutineItem> = if (age < 6) listOf(
            RoutineItem(at(7, 30), "🌞", "بیدار شدن و شستن صورت"),
            RoutineItem(at(8), "🥣", "صبحانه"),
            RoutineItem(at(8, 30), "🪥", "مسواک زدن"),
            RoutineItem(at(10), "🧩", "بازی و یادگیری"),
            RoutineItem(at(12, 30), "🍲", "ناهار"),
            RoutineItem(at(13, 30), "😴", "خواب نیمروز"),
            RoutineItem(at(16), "🌳", "بازی بیرون از خانه"),
            RoutineItem(at(18), "🧸", "جمع کردن اسباب‌بازی‌ها"),
            RoutineItem(at(19), "🍽️", "شام"),
            RoutineItem(at(20), "🛁", "حمام و مسواک"),
            RoutineItem(at(20, 30), "📖", "قصه‌ی شب"),
            RoutineItem(at(21), "🌙", "خواب"),
        ) else listOf(
            RoutineItem(at(6, 45), "🌞", "بیدار شدن"),
            RoutineItem(at(7), "🥣", "صبحانه و مسواک"),
            RoutineItem(at(7, 30), "🏫", "مدرسه"),
            RoutineItem(at(13, 30), "🍲", "ناهار و استراحت"),
            RoutineItem(at(15), "📚", "انجام تکالیف"),
            RoutineItem(at(16, 30), "🏃", "ورزش یا بازی بیرون"),
            RoutineItem(at(18), "🎒", "آماده کردن کیف فردا"),
            RoutineItem(at(18, 30), "📱", "وقت آزاد (گوشی)"),
            RoutineItem(at(19, 30), "🍽️", "شام با خانواده"),
            RoutineItem(at(20, 30), "📖", "کتاب خواندن"),
            RoutineItem(at(21), "🪥", "مسواک"),
            RoutineItem(at(21, 30), "🌙", "خواب"),
        )
    }
}
