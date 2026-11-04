package com.kodomo.app.data.seed

import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentType.MUSIC
import com.kodomo.app.domain.model.ContentType.VIDEO
import com.kodomo.app.domain.model.Interest

/**
 * Starter media shipped with the app. Videos are open-licensed Blender Foundation films (CC-BY); music items are
 * royalty-free instrumental pieces. The rest of the catalog is delivered and rated through the Kodomo CMS.
 */
internal object MediaCatalog {
    private const val VIDEO_BASE = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/"
    private const val MUSIC_BASE = "https://www.soundhelix.com/examples/mp3/"

    val videos: List<ContentItem> = listOf(
        content(
            id = "video_big_buck_bunny", type = VIDEO, title = "خرگوش بزرگ",
            description = "خرگوشی مهربان و تپل که با شیطنت سه جونده‌ی قلدر روبه‌رو می‌شود و با هوش و خلاقیت از دوستان جنگلی‌اش دفاع می‌کند.",
            category = "انیمیشن کوتاه", emoji = "🐰", colors = Palette.forest,
            rating = rating(minAge = 4, maxAge = 12, violence = 2, fear = 1, educational = 2, abnormality = 1, creativity = 4, familyValues = 3),
            mediaUrl = VIDEO_BASE + "BigBuckBunny.mp4", durationMinutes = 10,
            positives = listOf("طبیعت زیبا و رنگارنگ", "بدون دیالوگ؛ مناسب همه‌ی زبان‌ها", "حمایت از دوستان"),
            negatives = listOf("شوخی‌های فیزیکی و تلافی‌جویانه"),
            expertReview = "انیمیشنی شاد و بی‌کلام که برای گفت‌وگو درباره‌ی «قلدری» و «راه‌حل‌های بدون آسیب» فرصت خوبی می‌دهد. توصیه می‌شود والدین پس از تماشا درباره‌ی تلافی صحبت کنند.",
            expertName = "دکتر سارا محمدی - روانشناس کودک", peerLikes = 412, peerDislikes = 37,
            interests = setOf(Interest.ANIMALS, Interest.MOVIES), author = "Blender Foundation (CC-BY)",
        ),
        content(
            id = "video_elephants_dream", type = VIDEO, title = "رویای فیل",
            description = "دو شخصیت در دنیایی مکانیکی و خیال‌انگیز سفر می‌کنند و درباره‌ی واقعیت و تخیل به گفت‌وگو می‌نشینند.",
            category = "فیلم هنری", emoji = "🐘", colors = Palette.lavender,
            rating = rating(minAge = 10, maxAge = 18, violence = 1, fear = 3, educational = 3, abnormality = 1, creativity = 5, familyValues = 2),
            mediaUrl = VIDEO_BASE + "ElephantsDream.mp4", durationMinutes = 11,
            positives = listOf("طراحی هنری خلاقانه", "زمینه‌ی گفت‌وگو درباره‌ی تخیل"),
            negatives = listOf("فضای تاریک و صداهای ناگهانی"),
            expertReview = "برای نوجوانان علاقه‌مند به هنر و طراحی مناسب است؛ فضای گرفته‌ی آن برای کودکان کم‌سن ترسناک است.",
            expertName = "مهندس رضا کریمی - کارشناس رسانه", peerLikes = 96, peerDislikes = 41,
            interests = setOf(Interest.ART, Interest.MOVIES), author = "Blender Foundation (CC-BY)",
        ),
        content(
            id = "video_sintel", type = VIDEO, title = "سینتل و اژدهای کوچک",
            description = "دختری شجاع برای یافتن دوست اژدهای کوچکش به سفری طولانی می‌رود؛ داستانی درباره‌ی وفاداری و پذیرش.",
            category = "انیمیشن ماجرایی", emoji = "🐉", colors = Palette.peach,
            rating = rating(minAge = 11, maxAge = 18, violence = 3, fear = 3, educational = 3, abnormality = 1, creativity = 4, familyValues = 4),
            mediaUrl = VIDEO_BASE + "Sintel.mp4", durationMinutes = 15,
            positives = listOf("شخصیت اصلی شجاع و مسئول", "موسیقی و تصویرسازی درخشان"),
            negatives = listOf("صحنه‌های نبرد", "پایان غم‌انگیز"),
            expertReview = "برای نوجوانان ۱۱ سال به بالا؛ پایان تلخ آن فرصتی برای گفت‌وگو درباره‌ی سوگ و تغییر است. تماشای مشترک با والدین توصیه می‌شود.",
            expertName = "دکتر سارا محمدی - روانشناس کودک", peerLikes = 188, peerDislikes = 22,
            interests = setOf(Interest.ANIMALS, Interest.MOVIES, Interest.ART), author = "Blender Foundation (CC-BY)",
        ),
    )

    private fun track(index: Int, title: String, category: String, emoji: String, colors: Pair<Long, Long>, minAge: Int, description: String, minutes: Int) =
        content(
            id = "music_soundhelix_$index", type = MUSIC, title = title, description = description, category = category,
            emoji = emoji, colors = colors, rating = rating(minAge = minAge, maxAge = 18, educational = 2, creativity = 4),
            mediaUrl = "${MUSIC_BASE}SoundHelix-Song-$index.mp3", durationMinutes = minutes,
            positives = listOf("بی‌کلام", "مناسب تمرکز و بازی"), expertName = "واحد موسیقی کدومو",
            peerLikes = 40 + index * 13, peerDislikes = index, interests = setOf(Interest.MUSIC), author = "SoundHelix (royalty-free)",
        )

    val music: List<ContentItem> = listOf(
        track(1, "جشن رنگ‌ها", "شاد و پرانرژی", "🎉", Palette.sun, 2, "قطعه‌ای پرانرژی برای رقص و بازی‌های حرکتی در خانه.", 6),
        track(2, "دوچرخه‌سواری در پارک", "شاد و پرانرژی", "🚲", Palette.sky, 2, "ریتمی شاد برای همراهی با بازی و نرمش صبحگاهی.", 7),
        track(3, "قطار آهنگ‌ها", "شاد و پرانرژی", "🚂", Palette.peach, 3, "قطعه‌ای ریتمیک برای بازی «قطار بازی» در خانه.", 5),
        track(4, "آسمان پرستاره", "آرامش‌بخش", "🌙", Palette.night, 2, "موسیقی آرام برای پیش از خواب و استراحت.", 5),
        track(5, "باغ پروانه‌ها", "آرامش‌بخش", "🦋", Palette.lavender, 2, "نوایی آرام برای نقاشی و کارهای دستی.", 6),
        track(6, "کلاس ریاضی‌ها", "تمرکز و یادگیری", "🔢", Palette.mint, 6, "موسیقی بی‌کلام برای تمرکز هنگام انجام تکالیف.", 6),
        track(7, "کشف ستاره‌ها", "تمرکز و یادگیری", "🔭", Palette.night, 7, "همراهی آرام برای مطالعه و پروژه‌های علمی.", 7),
        track(8, "صبح بخیر خورشید", "شاد و پرانرژی", "☀️", Palette.sun, 2, "شروعی پرانرژی برای صبح‌های تعطیل.", 5),
    )
}
