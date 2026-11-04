package com.kodomo.app.data.seed

import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentPage
import com.kodomo.app.domain.model.ContentType.EXPLORE
import com.kodomo.app.domain.model.ContentType.LEARNING
import com.kodomo.app.domain.model.Interest

internal object LearningCatalog {
    const val SCIENCE = "علوم"
    const val MEDIA_LITERACY = "سواد رسانه‌ای"
    const val CRAFTS = "کاردستی"
    const val ORIGAMI = "اوریگامی"
    const val BUILDING = "ساخت‌وساز و لگو"

    val categories = listOf(SCIENCE, MEDIA_LITERACY, CRAFTS, ORIGAMI, BUILDING)

    val lessons: List<ContentItem> = listOf(
        content(
            "lesson_volcano", LEARNING, "آتشفشان جوش‌شیرین", "با جوش‌شیرین و سرکه یک آتشفشان کف‌دار بساز و واکنش شیمیایی را ببین.",
            SCIENCE, "🌋", Palette.peach, rating(minAge = 5, educational = 5, creativity = 4), durationMinutes = 15,
            positives = listOf("آزمایش امن خانگی", "آشنایی با واکنش اسید و باز"), expertName = "واحد آموزش علوم کدومو",
            interests = setOf(Interest.SCIENCE),
        ),
        content(
            "lesson_rainbow_water", LEARNING, "رنگین‌کمان در لیوان", "با آب، شکر و رنگ خوراکی لایه‌های رنگی بساز و چگالی را کشف کن.",
            SCIENCE, "🌈", Palette.sky, rating(minAge = 6, educational = 5, creativity = 4), durationMinutes = 20,
            positives = listOf("مفهوم چگالی"), expertName = "واحد آموزش علوم کدومو", interests = setOf(Interest.SCIENCE, Interest.ART),
        ),
        content(
            "lesson_ad_detective", LEARNING, "کارآگاه تبلیغات", "یاد بگیر تبلیغ‌ها چطور توجه ما را جلب می‌کنند و چه سؤال‌هایی باید از خودمان بپرسیم.",
            MEDIA_LITERACY, "🕵️", Palette.lavender, rating(minAge = 7, educational = 5, familyValues = 4), durationMinutes = 10,
            positives = listOf("تفکر نقادانه", "تشخیص تبلیغ"), expertName = "مهندس رضا کریمی - کارشناس رسانه",
            interests = setOf(Interest.MOVIES, Interest.GAMES),
        ),
        content(
            "lesson_real_or_fake", LEARNING, "واقعی یا ساختگی؟", "تمرین تشخیص خبر و تصویر واقعی از ساختگی با چهار سؤال طلایی.",
            MEDIA_LITERACY, "🔍", Palette.mint, rating(minAge = 9, educational = 5), durationMinutes = 12,
            positives = listOf("تفکر نقادانه", "سواد رسانه‌ای"), expertName = "مهندس رضا کریمی - کارشناس رسانه",
            interests = setOf(Interest.SCIENCE),
        ),
        content(
            "lesson_paper_plate_lion", LEARNING, "شیر بشقابی", "با یک بشقاب کاغذی و کاغذ رنگی یک شیر خندان بساز.",
            CRAFTS, "🦁", Palette.sun, rating(minAge = 3, educational = 3, creativity = 5), durationMinutes = 20,
            positives = listOf("مهارت حرکتی ظریف"), expertName = "واحد هنر کدومو", interests = setOf(Interest.ART, Interest.ANIMALS),
        ),
        content(
            "lesson_origami_boat", LEARNING, "قایق کاغذی", "با یک برگ کاغذ A4 قایقی بساز که روی آب شناور می‌ماند.",
            ORIGAMI, "⛵", Palette.sky, rating(minAge = 5, educational = 3, creativity = 4), durationMinutes = 10,
            positives = listOf("دقت و تمرکز"), expertName = "واحد هنر کدومو", interests = setOf(Interest.ART),
        ),
        content(
            "lesson_origami_crane", LEARNING, "درنای کاغذی", "اوریگامی کلاسیک درنا؛ نماد آرزو و صبر.",
            ORIGAMI, "🕊️", Palette.rose, rating(minAge = 8, educational = 3, creativity = 5), durationMinutes = 25,
            positives = listOf("صبر و دقت", "هندسه‌ی تا"), expertName = "واحد هنر کدومو", interests = setOf(Interest.ART),
        ),
        content(
            "lesson_spaghetti_tower", LEARNING, "برج ماکارونی", "با ماکارونی خام و خمیر بازی بلندترین برج ممکن را بساز و مثلث‌های محکم را کشف کن.",
            BUILDING, "🏗️", Palette.forest, rating(minAge = 6, educational = 5, creativity = 5), durationMinutes = 25,
            positives = listOf("مهندسی سازه", "کار گروهی"), expertName = "واحد آموزش علوم کدومو",
            interests = setOf(Interest.SCIENCE, Interest.GAMES),
        ),
    )

    val pages: List<ContentPage> =
        pages(
            "lesson_volcano",
            "🧰" to "وسایل: یک بطری کوچک، ۲ قاشق جوش‌شیرین، نصف لیوان سرکه، چند قطره مایع ظرفشویی و رنگ خوراکی قرمز.",
            "🏔️" to "بطری را در یک سینی بگذار و دورش را با خمیر بازی به شکل کوه بپوشان.",
            "🥄" to "جوش‌شیرین، مایع ظرفشویی و رنگ را داخل بطری بریز.",
            "🧪" to "با کمک بزرگ‌تر، سرکه را آرام داخل بطری بریز و عقب بایست!",
            "🌋" to "کف قرمز بیرون می‌آید! سرکه (اسید) و جوش‌شیرین (باز) با هم گاز دی‌اکسید کربن می‌سازند.",
        ) + pages(
            "lesson_rainbow_water",
            "🥛" to "چهار لیوان آب بردار و در هر کدام مقدار متفاوتی شکر حل کن: ۰، ۱، ۲ و ۳ قاشق.",
            "🎨" to "به هر لیوان یک رنگ خوراکی متفاوت اضافه کن.",
            "🧃" to "با قطره‌چکان، ابتدا پرشکرترین آب را در یک لیوان بلند بریز.",
            "🌈" to "بقیه را به ترتیب و خیلی آرام روی قاشق بریز. لایه‌ها روی هم می‌ایستند چون آب شیرین‌تر سنگین‌تر است!",
        ) + pages(
            "lesson_ad_detective",
            "📺" to "یک تبلیغ را با هم تماشا کنید. چه کسی این تبلیغ را ساخته است؟",
            "🎯" to "تبلیغ می‌خواهد ما چه کاری انجام دهیم؟ بخریم؟ امتحان کنیم؟",
            "✨" to "از چه ترفندهایی استفاده کرده؟ موسیقی شاد، رنگ‌های درخشان یا شخصیت محبوب؟",
            "🤔" to "چه چیزهایی را به ما نگفته است؟ قیمت؟ مضرات؟",
            "🕵️" to "آفرین کارآگاه! حالا می‌دانی که هر تبلیغ یک پیام فروش دارد.",
        ) + pages(
            "lesson_real_or_fake",
            "❓" to "سؤال اول: منبع این خبر کیست و آیا قابل اعتماد است؟",
            "📅" to "سؤال دوم: این خبر یا تصویر مربوط به چه زمانی است؟",
            "🔁" to "سؤال سوم: آیا منبع معتبر دیگری هم همین را گفته است؟",
            "😮" to "سؤال چهارم: آیا خبر می‌خواهد مرا خیلی عصبانی یا هیجان‌زده کند؟ این نشانه‌ی هشدار است.",
            "✅" to "قبل از باور کردن یا فرستادن هر خبر، این چهار سؤال را از خودت بپرس.",
        ) + pages(
            "lesson_paper_plate_lion",
            "🍽️" to "یک بشقاب کاغذی را با مداد شمعی زرد رنگ کن.",
            "✂️" to "با کمک بزرگ‌تر، نوارهای کاغذ نارنجی و قهوه‌ای ببر.",
            "🦁" to "نوارها را دور بشقاب بچسبان تا یال شیر درست شود.",
            "😺" to "چشم، بینی و سبیل بکش. شیر خندان تو آماده است!",
        ) + pages(
            "lesson_origami_boat",
            "📄" to "کاغذ را از وسط تا کن تا یک مستطیل شود.",
            "📐" to "دو گوشه‌ی بالا را به سمت خط وسط تا کن تا یک کلاه درست شود.",
            "⬆️" to "لبه‌های پایین را از هر دو طرف به بالا تا کن.",
            "🔷" to "کلاه را از وسط باز کن و به شکل لوزی صاف کن، دو بار تکرار کن.",
            "⛵" to "دو طرف بالا را آرام بکش تا قایق باز شود. حالا آن را روی آب بگذار!",
        ) + pages(
            "lesson_origami_crane",
            "🟥" to "از یک کاغذ مربع شروع کن و آن را به شکل قطری و مستقیم تا کن.",
            "🔶" to "با تاهای قبلی، پایه‌ی مربعی (پایه‌ی پرنده) را بساز.",
            "💎" to "لبه‌ها را به خط وسط تا کن تا شکل بادبادک درست شود؛ پشت را هم تکرار کن.",
            "🦢" to "دو بخش پایین را به بالا تا کن تا گردن و دم ساخته شود.",
            "🕊️" to "سر را با یک تای معکوس بساز و بال‌ها را آرام باز کن. درنای تو آماده‌ی پرواز است!",
        ) + pages(
            "lesson_spaghetti_tower",
            "🍝" to "۲۰ رشته ماکارونی خام و یک بسته خمیر بازی آماده کن.",
            "🔺" to "با سه ماکارونی و سه گلوله‌ی خمیر یک مثلث بساز. مثلث محکم‌ترین شکل است!",
            "🏗️" to "مثلث‌ها را به هم وصل کن تا یک هرم بسازی.",
            "📏" to "طبقه‌های بیشتری اضافه کن و ارتفاع را با خط‌کش اندازه بگیر.",
            "🏆" to "رکوردت را ثبت کن و دفعه‌ی بعد سعی کن بلندتر بسازی!",
        )

    val explore: List<ContentItem> = listOf(
        content("explore_octopus", EXPLORE, "هشت‌پا سه قلب دارد!", "هشت‌پاها سه قلب و خون آبی رنگ دارند و می‌توانند رنگ پوستشان را عوض کنند.", "حیوانات", "🐙", Palette.lavender, rating(minAge = 3, educational = 4), interests = setOf(Interest.ANIMALS, Interest.SCIENCE), peerLikes = 320),
        content("explore_saturn_rings", EXPLORE, "حلقه‌های زحل", "حلقه‌های زحل از میلیاردها تکه یخ و سنگ ساخته شده‌اند؛ بعضی به اندازه‌ی یک خانه!", "فضا", "🪐", Palette.night, rating(minAge = 5, educational = 5), interests = setOf(Interest.ASTRONOMY), peerLikes = 287),
        content("explore_finger_paint", EXPLORE, "نقاشی با اثر انگشت", "با اثر انگشت و کمی رنگ، یک باغ پر از کفشدوزک و گل بکش.", "هنر", "🖐️", Palette.rose, rating(minAge = 2, educational = 3, creativity = 5), interests = setOf(Interest.ART), peerLikes = 198),
        content("explore_honey_bee", EXPLORE, "زنبورها می‌رقصند", "زنبورهای عسل با رقص مخصوصی به هم می‌گویند گل‌ها کجا هستند.", "حیوانات", "🐝", Palette.sun, rating(minAge = 4, educational = 5), interests = setOf(Interest.ANIMALS, Interest.SCIENCE), peerLikes = 254),
        content("explore_paper_crane", EXPLORE, "هزار درنای کاغذی", "در ژاپن می‌گویند اگر هزار درنای کاغذی بسازی، آرزویت برآورده می‌شود.", "کاردستی", "🕊️", Palette.mint, rating(minAge = 6, educational = 3, creativity = 5), interests = setOf(Interest.ART), peerLikes = 176),
        content("explore_football_physics", EXPLORE, "چرا توپ می‌پیچد؟", "وقتی به توپ ضربه‌ی چرخشی می‌زنی، هوا دورش متفاوت جریان پیدا می‌کند و توپ مسیرش را کج می‌کند.", "ورزش", "⚽", Palette.forest, rating(minAge = 7, educational = 4), interests = setOf(Interest.SPORT, Interest.SCIENCE), peerLikes = 231),
        content("explore_kids_choir", EXPLORE, "سرود گروهی", "خواندن گروهی باعث می‌شود ضربان قلب خوانندگان با هم هماهنگ شود!", "موسیقی", "🎶", Palette.peach, rating(minAge = 4, educational = 3), interests = setOf(Interest.MUSIC), peerLikes = 143),
        content("explore_dino_feathers", EXPLORE, "دایناسورهای پردار", "بعضی دایناسورها پر داشتند و پرندگان امروزی نوه‌های آن‌ها هستند.", "علم", "🦖", Palette.forest, rating(minAge = 5, educational = 5), interests = setOf(Interest.SCIENCE, Interest.ANIMALS), peerLikes = 302),
        content("explore_moon_footprint", EXPLORE, "ردپای ماندگار", "ردپای فضانوردان روی ماه شاید میلیون‌ها سال باقی بماند؛ چون آنجا باد و بارانی نیست.", "فضا", "👣", Palette.night, rating(minAge = 6, educational = 5), interests = setOf(Interest.ASTRONOMY), peerLikes = 211),
        content("explore_board_game", EXPLORE, "بازی خودت را بساز", "با مقوا، تاس و چند مهره یک بازی رومیزی اختراع کن و قوانینش را بنویس.", "بازی", "🎲", Palette.sky, rating(minAge = 7, educational = 4, creativity = 5), interests = setOf(Interest.GAMES, Interest.ART), peerLikes = 167),
    )
}
