package com.kodomo.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.choreStore: DataStore<Preferences> by preferencesDataStore(name = "kodomo_chores")

/** A real household task a child can do; the parent confirms it before the stars are given. */
data class Chore(val id: String, val title: String, val emoji: String, val minAge: Int, val stars: Int, val tip: String)

data class PendingChore(val childId: Long, val chore: Chore, val at: Long)

/** «کمک به خانواده»: chores marked as done by the child, waiting for the parent's approval. */
@Singleton
class ChoreStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val pendingKey = stringSetPreferencesKey("pending")
    private fun doneKey(childId: Long) = intPreferencesKey("done_$childId")

    val pending: Flow<List<PendingChore>> = context.choreStore.data.map { p ->
        p[pendingKey].orEmpty().mapNotNull { raw ->
            val parts = raw.split('|')
            val chore = catalog.firstOrNull { it.id == parts.getOrNull(1) } ?: return@mapNotNull null
            PendingChore(parts[0].toLongOrNull() ?: return@mapNotNull null, chore, parts.getOrNull(2)?.toLongOrNull() ?: 0L)
        }.sortedByDescending { it.at }
    }

    fun doneCount(childId: Long): Flow<Int> = context.choreStore.data.map { it[doneKey(childId)] ?: 0 }

    suspend fun markDone(childId: Long, chore: Chore) = context.choreStore.edit { p ->
        p[pendingKey] = p[pendingKey].orEmpty() + "$childId|${chore.id}|${System.currentTimeMillis()}"
    }

    suspend fun resolve(item: PendingChore, approved: Boolean) = context.choreStore.edit { p ->
        p[pendingKey] = p[pendingKey].orEmpty().filterNot { it.startsWith("${item.childId}|${item.chore.id}|${item.at}") }.toSet()
        if (approved) p[doneKey(item.childId)] = (p[doneKey(item.childId)] ?: 0) + 1
    }

    companion object {
        val catalog = listOf(
            Chore("toys", "جمع کردن اسباب‌بازی‌ها", "🧸", 2, 2, "همه را در جعبه‌ی خودشان بگذار."),
            Chore("shoes", "جفت کردن کفش‌ها در جاکفشی", "👟", 2, 2, "هر کفش را با جفتش کنار هم بگذار."),
            Chore("plants", "آب دادن به گلدان (با کمک بزرگ‌تر)", "🪴", 3, 2, "اول با انگشت ببین خاک خشک است یا نه."),
            Chore("table", "کمک در چیدن سفره", "🍽️", 3, 3, "برای هر نفر یک قاشق و دستمال بگذار."),
            Chore("bed", "مرتب کردن تخت", "🛏️", 5, 3, "پتو را صاف کن و بالش را سر جایش بگذار."),
            Chore("towels", "تا کردن حوله‌ها", "🧺", 5, 3, "حوله را دو بار از وسط تا کن."),
            Chore("recycle", "تفکیک زباله‌ی خشک و تر", "♻️", 6, 3, "کاغذ و پلاستیک در سطل خشک."),
            Chore("bag", "آماده کردن کیف مدرسه", "🎒", 6, 3, "برنامه‌ی فردا را نگاه کن."),
            Chore("laundry", "گذاشتن لباس‌ها در سبد", "👕", 4, 2, "لباس‌های کثیف در سبد، نه روی زمین!"),
            Chore("dishes", "شستن ظرف‌های خودم", "🧽", 9, 4, "ظرف‌های نشکن را بشوی و آب را باز نگذار."),
            Chore("sweep", "جارو کردن اتاقم", "🧹", 9, 4, "زیر میز را فراموش نکن."),
            Chore("cook", "کمک در آماده کردن غذا", "🥗", 9, 5, "سبزی شستن و چیدن سالاد عالی است."),
            Chore("closet", "مرتب کردن کمد لباس", "🗄️", 10, 5, "لباس‌هایی که کوچک شده را جدا کن."),
            Chore("grandma", "تماس با پدربزرگ یا مادربزرگ", "☎️", 4, 3, "از حالشان بپرس و از روزت تعریف کن."),
            Chore("help", "کمک به خواهر یا برادر", "🤝", 5, 3, "در تکلیف یا بازی همراهش باش."),
        )
    }
}
