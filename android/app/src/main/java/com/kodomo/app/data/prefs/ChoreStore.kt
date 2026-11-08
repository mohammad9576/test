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

/** Behaviour the parents note down: costs play time today (and a gentle talk, not shame). */
data class BadDeed(val id: String, val title: String, val emoji: String, val minutes: Int, val fix: String)

data class BadDeedEntry(val deed: BadDeed, val at: Long)

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

    private fun today() = java.time.LocalDate.now().toString()
    private fun deltaKey(childId: Long, date: String = today()) = intPreferencesKey("delta_${childId}_$date")
    private fun badKey(childId: Long, date: String = today()) = androidx.datastore.preferences.core.stringPreferencesKey("bad_${childId}_$date")

    /** Minutes added (good deeds) or removed (bad deeds) from each child's play time today. */
    val todayDeltas: Flow<Map<Long, Int>> = context.choreStore.data.map { p ->
        val suffix = "_" + today()
        p.asMap().mapNotNull { (k, v) ->
            val n = k.name
            if (n.startsWith("delta_") && n.endsWith(suffix)) n.removePrefix("delta_").removeSuffix(suffix).toLongOrNull()?.let { it to (v as? Int ?: 0) } else null
        }.toMap()
    }

    fun badToday(childId: Long): Flow<List<BadDeedEntry>> = context.choreStore.data.map { p ->
        p[badKey(childId)].orEmpty().lines().mapNotNull { l ->
            val parts = l.split('|')
            val deed = badCatalog.firstOrNull { it.id == parts.getOrNull(0) } ?: return@mapNotNull null
            BadDeedEntry(deed, parts.getOrNull(1)?.toLongOrNull() ?: 0)
        }
    }

    suspend fun addMinutes(childId: Long, minutes: Int) = context.choreStore.edit { p ->
        p[deltaKey(childId)] = ((p[deltaKey(childId)] ?: 0) + minutes).coerceIn(-180, 180)
    }

    suspend fun recordBad(childId: Long, deed: BadDeed) = context.choreStore.edit { p ->
        p[badKey(childId)] = (p[badKey(childId)].orEmpty().lines().filter { it.isNotBlank() } + "${deed.id}|${System.currentTimeMillis()}").joinToString("\n")
        p[deltaKey(childId)] = ((p[deltaKey(childId)] ?: 0) - deed.minutes).coerceIn(-180, 180)
    }

    /** Undo the last note (a parent tapped by mistake or the child made it right). */
    suspend fun undoBad(childId: Long, entry: BadDeedEntry) = context.choreStore.edit { p ->
        val lines = p[badKey(childId)].orEmpty().lines().filter { it.isNotBlank() }
        val target = "${entry.deed.id}|${entry.at}"
        if (target in lines) {
            p[badKey(childId)] = (lines - target).joinToString("\n")
            p[deltaKey(childId)] = ((p[deltaKey(childId)] ?: 0) + entry.deed.minutes).coerceIn(-180, 180)
        }
    }

    suspend fun markDone(childId: Long, chore: Chore) = context.choreStore.edit { p ->
        p[pendingKey] = p[pendingKey].orEmpty() + "$childId|${chore.id}|${System.currentTimeMillis()}"
    }

    suspend fun resolve(item: PendingChore, approved: Boolean) = context.choreStore.edit { p ->
        p[pendingKey] = p[pendingKey].orEmpty().filterNot { it.startsWith("${item.childId}|${item.chore.id}|${item.at}") }.toSet()
        if (approved) p[doneKey(item.childId)] = (p[doneKey(item.childId)] ?: 0) + 1
    }

    companion object {
        /** Minutes of extra play an approved good deed earns. */
        const val GOOD_DEED_MINUTES = 5

        val badCatalog = listOf(
            BadDeed("fight", "دعوا با خواهر یا برادر", "😤", 10, "از او عذرخواهی کن و با هم یک بازی آرام انجام بدهید."),
            BadDeed("rude", "بی‌احترامی به بزرگ‌ترها", "🙊", 10, "با آرامش حرفت را بزن و بگو «ببخشید»."),
            BadDeed("lie", "راست نگفتن", "🤥", 15, "گفتن حقیقت شجاعت می‌خواهد؛ دفعه‌ی بعد راستش را بگو."),
            BadDeed("mess", "جمع نکردن وسایل بعد از بازی", "🧸", 5, "همین حالا وسایلت را جمع کن تا وقتت برگردد."),
            BadDeed("homework", "انجام ندادن تکلیف", "📚", 15, "اول تکلیف، بعد بازی!"),
            BadDeed("promise", "بدقولی (مثلاً سر وقت گوشی را ندادن)", "⏰", 10, "قول یعنی قول؛ دفعه‌ی بعد سر وقت گوشی را تحویل بده."),
            BadDeed("hit", "زدن یا اذیت کردن دیگران", "✋", 15, "با کلمه‌ها بگو چه حسی داری، نه با دست."),
            BadDeed("food", "بازی با غذا یا غذا نخوردن", "🍽️", 5, "یک لقمه‌ی دیگر امتحان کن؛ غذا انرژی بازی است."),
            BadDeed("sleep", "دیر خوابیدن یا لج کردن موقع خواب", "🌙", 10, "بدن تو برای قد کشیدن خواب کافی لازم دارد."),
            BadDeed("scream", "داد زدن و جیغ کشیدن", "📢", 5, "سه نفس عمیق بکش و آرام حرف بزن."),
        )

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
