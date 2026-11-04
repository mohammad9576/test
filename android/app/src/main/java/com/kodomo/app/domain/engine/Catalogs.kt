package com.kodomo.app.domain.engine

import com.kodomo.app.domain.model.Achievement
import com.kodomo.app.domain.model.FamilyReward

object AchievementCatalog {
    const val FIRST_FEEDBACK = "first_feedback"
    const val BOOKWORM = "bookworm"
    const val ACTIVE_BODY = "active_body"
    const val CREATOR = "creator"
    const val SCHOOL_STAR = "school_star"
    const val EXPLORER = "explorer"
    const val MUSIC_LOVER = "music_lover"
    const val SCIENTIST = "scientist"

    val all: List<Achievement> = listOf(
        Achievement(FIRST_FEEDBACK, "منتقد کوچک", "🗣️", "اولین نظرت را درباره‌ی یک انیمیشن ثبت کردی.", 5),
        Achievement(BOOKWORM, "کتاب‌خوان قهرمان", "📚", "سه کتاب را تا آخر خواندی.", 15),
        Achievement(ACTIVE_BODY, "بدن فعال", "🏃", "پنج بازی حرکتی را کامل کردی.", 15),
        Achievement(CREATOR, "سازنده‌ی خلاق", "🛠️", "اولین پروژه‌ی پنج‌مرحله‌ای را تمام کردی.", 20),
        Achievement(SCHOOL_STAR, "ستاره‌ی مدرسه", "🏅", "پنج تکلیف را به موقع انجام دادی.", 20),
        Achievement(EXPLORER, "کاشف کنجکاو", "🧭", "پنج مقاله در مرورگر امن خواندی.", 10),
        Achievement(MUSIC_LOVER, "عاشق موسیقی", "🎶", "پنج قطعه‌ی موسیقی گوش دادی.", 10),
        Achievement(SCIENTIST, "دانشمند جوان", "🔬", "سه درس آموزشی را کامل کردی.", 15),
    )

    fun byId(id: String): Achievement? = all.firstOrNull { it.id == id }
}

object FamilyRewardCatalog {
    val all: List<FamilyReward> = listOf(
        FamilyReward("movie_night", "شب فیلم خانوادگی", "🍿", "یک شب فیلم با انتخاب خودت و پاپ‌کورن!", 30),
        FamilyReward("nature_trip", "طبیعت‌گردی", "🌲", "یک روز پیاده‌روی و پیک‌نیک در طبیعت با خانواده.", 60),
        FamilyReward("shared_reading", "کتاب‌خوانی مشترک", "📖", "پدر یا مادر یک کتاب انتخابی تو را برایت می‌خوانند.", 15),
        FamilyReward("board_game", "شب بازی رومیزی", "🎲", "یک شب بازی رومیزی با همه‌ی اعضای خانواده.", 25),
        FamilyReward("cooking", "آشپزی با مامان و بابا", "🧁", "با هم یک کیک یا غذای ساده درست کنید.", 35),
        FamilyReward("park_day", "روز پارک", "🛝", "یک عصر کامل بازی در پارک محله.", 20),
    )

    fun byId(id: String): FamilyReward? = all.firstOrNull { it.id == id }
}
