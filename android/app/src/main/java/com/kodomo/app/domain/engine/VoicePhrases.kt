package com.kodomo.app.domain.engine

import com.kodomo.app.domain.model.Interest
import com.kodomo.app.domain.model.MotionGame

/**
 * Fixed sentences spoken in the kids space. Keeping them here (instead of inline string literals) guarantees that the
 * build-time narration exporter records exactly what the app later asks the voice engine to say.
 */
object VoicePhrases {
    const val PROJECT_DONE = "آفرین! پروژه‌ات عالی شد!"
    const val GAME_DONE = "آفرین! ستاره گرفتی!"
    const val LEVEL_UP = "آفرین! مرحله‌ی بعد باز شد!"
    const val TRY_AGAIN = "نزدیک بود! یک بار دیگر تلاش کن."
    const val NOT_ENOUGH_STARS = "ستاره‌هایت هنوز کافی نیست؛ ادامه بده! 💪"
    const val FEEDBACK_MEDAL = "آفرین! مدال «منتقد کوچک» و ۵ ستاره گرفتی! 🏅"
    const val FEEDBACK_THANKS = "ممنون که نظرت را گفتی! 💚"
    const val LIKED = "دوست داشتم"
    const val DISLIKED = "دوست نداشتم"

    const val SIMON_READY = "آماده‌ای؟ گوشی را در دست بگیر!"
    const val SIMON_GOOD = "عالی بود! 🎉"
    const val SIMON_NOT_FOOLED = "آفرین! گول نخوردی 😎"
    const val SIMON_SHOULD_MOVE = "سایمون گفته بود! باید حرکت می‌کردی 🙂"
    const val SIMON_FOOLED = "اوه! سایمون نگفته بود 😄"

    const val BODY_STEP_BACK = "کمی عقب برو تا همه‌ی بدنت در تصویر دیده شود."
    const val BODY_READY = "عالی! حالا دیده می‌شوی. شروع کنیم!"
    const val BALLOON_JUMP = "پرش عالی! همه‌ی بادکنک‌ها ترکیدند!"
    const val STAR_CLOUD = "مواظب ابر بارانی باش!"
    const val FREEZE_DANCE = "برقص! برقص!"
    const val FREEZE_NOW = "مجسمه شو!"
    const val FREEZE_MOVED = "اوه! تکان خوردی!"
    const val FREEZE_GREAT = "آفرین! مثل یک مجسمه‌ی واقعی!"
    const val POSE_HOLD = "همین‌طور نگه دار!"
    const val POSE_DONE = "ژست عالی بود!"
    const val JACKS_START = "دست‌ها بالا، پاها باز! مثل پروانه!"
    const val PAINT_SAVED = "نقاشی‌ات در گالری خاطرات ذخیره شد! 🎨"
    const val COLORING_DONE = "هورااا! همه‌ی تصویر رنگی شد!"
    const val GOAL = "گُل! چه شوت قشنگی!"
    const val SAVED_BY_KEEPER = "دروازه‌بان گرفت! دوباره امتحان کن!"
    const val GREAT_SAVE = "عجب مهاری! آفرین دروازه‌بان!"
    const val PONG_POINT = "امتیاز برای تو!"
    const val PONG_ROBOT_POINT = "امتیاز برای ربات! ادامه بده!"
    const val YOU_WIN = "تو برنده شدی! قهرمانی!"
    const val NICE_TRY = "بازی خوبی بود! دفعه‌ی بعد می‌بری!"

    val simonCommands = listOf(
        "دست‌هات رو ببر بالا و پایین!", "سه بار بپر!", "دور خودت بچرخ!", "مثل خرگوش بپر!",
        "دست‌هات رو مثل پرنده تکان بده!", "بنشین و پاشو!", "روی یک پا لی‌لی کن!", "مثل درخت در باد تکان بخور!",
    )

    fun simon(command: String) = "سایمون می‌گه: $command"

    val colorNames = listOf("قرمز", "نارنجی", "زرد", "سبز", "آبی", "بنفش")
    fun findColor(label: String) = "یک چیز $label پیدا کن!"
    fun foundColor(label: String) = "آفرین! $label را پیدا کردی!"

    val poses = listOf(
        "هر دو دست را بالا ببر!", "دست‌ها را مثل هواپیما باز کن!", "دست راستت را بالا ببر!",
        "دست چپت را بالا ببر!", "دست‌ها را روی سرت بگذار!",
    )

    val numbers = listOf(
        "یک", "دو", "سه", "چهار", "پنج", "شش", "هفت", "هشت", "نه", "ده",
        "یازده", "دوازده", "سیزده", "چهارده", "پانزده", "شانزده", "هفده", "هجده", "نوزده", "بیست",
    )

    fun rewardRequested(title: String) = "درخواست «$title» برای مامان و بابا فرستاده شد! 🎉"

    val homeModules = listOf(
        "فیلم و انیمیشن", "شبکه پویا", "بازی و نقاشی", "کتاب", "موسیقی", "آموزش", "تفکر و یادگیری", "مدرسه من", "کشف محتوا",
        "مرورگر امن من", "پروژه‌های من", "برنامه‌های من", "کمک به خانواده", "پاداش‌ها", "علایق من",
    )

    /** All static phrases of the UI layer (content narration is exported separately from the catalogs). */
    fun all(): List<String> = buildList {
        addAll(
            listOf(
                PROJECT_DONE, GAME_DONE, LEVEL_UP, TRY_AGAIN, NOT_ENOUGH_STARS, FEEDBACK_MEDAL, FEEDBACK_THANKS, LIKED, DISLIKED,
                SIMON_READY, SIMON_GOOD, SIMON_NOT_FOOLED, SIMON_SHOULD_MOVE, SIMON_FOOLED, BODY_STEP_BACK, BODY_READY,
                BALLOON_JUMP, STAR_CLOUD, FREEZE_DANCE, FREEZE_NOW, FREEZE_MOVED, FREEZE_GREAT, POSE_HOLD, POSE_DONE,
                JACKS_START, PAINT_SAVED, COLORING_DONE, GOAL, SAVED_BY_KEEPER, GREAT_SAVE, PONG_POINT, PONG_ROBOT_POINT, YOU_WIN, NICE_TRY,
            ),
        )
        addAll(simonCommands)
        addAll(simonCommands.map(::simon))
        colorNames.forEach { add(findColor(it)); add(foundColor(it)); add(it) }
        addAll(poses)
        addAll(numbers)
        addAll(FamilyRewardCatalog.all.map { rewardRequested(it.title) })
        addAll(homeModules)
        addAll(Interest.entries.map { it.label })
        MotionGame.entries.forEach { add(it.title); add(it.description) }
        addAll(MascotDialogueEngine.allSpeechPhrases())
    }
}
