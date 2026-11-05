package com.kodomo.app.ui.kids.games.adventure

import androidx.compose.ui.graphics.Color
import com.kodomo.app.ui.fx.GameMusic

/** The ten mission types. Every stage of an adventure uses a different one, so stage 5 never feels like stage 1. */
enum class Mission(val skill: String, val emoji: String) {
    CATCH("دست‌ها", "✋"),
    SORT("دقت", "🎯"),
    TWO_HANDS("هماهنگی دو دست", "🙌"),
    DODGE("خم شدن و جاخالی", "🤸"),
    RESCUE("نجات", "🆘"),
    MEMORY("حافظه‌ی حرکتی", "🧠"),
    BALANCE("تعادل", "🧘"),
    REACTION("واکنش سریع", "⚡"),
    COMBO("مأموریت ترکیبی", "🌀"),
    BOSS("نبرد نهایی", "👑"),
}

data class Stage(val mission: Mission, val title: String, val story: String, val goal: String, val power: String)

enum class Decor { FOREST, CITY, SPACE, OCEAN }

data class AdventureTheme(
    val id: String,
    val title: String,
    val emoji: String,
    val music: GameMusic.Theme,
    val decor: Decor,
    val skyTop: Color,
    val skyBottom: Color,
    val accent: Color,
    /** Things to collect; the first three are used as the «three colours» of the sorting stage. */
    val good: List<String>,
    val sortColors: List<Pair<String, Color>>,
    val bad: List<String>,
    val big: String,
    val friend: String,
    val cage: String,
    val lowObstacle: String,
    val sideObstacle: String,
    val boss: String,
    val bossName: String,
    val stages: List<Stage>,
) {
    init { require(stages.size == 10) }
}

object AdventureThemes {
    val forest = AdventureTheme(
        id = "forest", title = "سفر به جنگل اسرارآمیز", emoji = "🌳", music = GameMusic.Theme.FOREST, decor = Decor.FOREST,
        skyTop = Color(0xFF1B5E20), skyBottom = Color(0xFF8BC34A), accent = Color(0xFFFFEB3B),
        good = listOf("🍄", "🌰", "🍓", "🦋", "🍃"),
        sortColors = listOf("قرمز" to Color(0xFFE53935), "زرد" to Color(0xFFFDD835), "آبی" to Color(0xFF1E88E5)),
        bad = listOf("🐝", "🕷️", "🌵"), big = "🍯", friend = "🦊", cage = "🕸️",
        lowObstacle = "🌿", sideObstacle = "🪵", boss = "🐺", bossName = "گرگ سایه",
        stages = listOf(
            Stage(Mission.CATCH, "ورود به جنگل", "سلام کاشف کوچولو! جنگل اسرارآمیز پر از قارچ و میوه‌ی جادویی است. با دست‌هایت آن‌ها را جمع کن.", "۸ چیز جادویی جمع کن", "🎒 کوله‌ی کاشف"),
            Stage(Mission.SORT, "میوه‌های رنگی", "سنجاب گرسنه فقط میوه‌ی یک رنگ می‌خورد. به رنگی که می‌گویم دقت کن و فقط همان را بگیر.", "میوه‌ی رنگ درست را بگیر", "🔍 ذره‌بین جادویی"),
            Stage(Mission.TWO_HANDS, "کندوی عسل", "خرس مهربان کوزه‌های عسل سنگین دارد. هر کوزه را با هر دو دست بگیر!", "۵ کوزه‌ی عسل را با دو دست بگیر", "🧤 دستکش قدرت"),
            Stage(Mission.DODGE, "شاخه‌های پایین", "راه پر از شاخه و تنه‌ی درخت است. وقتی شاخه آمد خم شو، وقتی تنه آمد کنار برو.", "از ۶ مانع رد شو", "👟 کفش چابک"),
            Stage(Mission.RESCUE, "نجات روباه کوچولو", "روباه کوچولو در تار عنکبوت گیر افتاده! حباب‌های تار را با دست بترکان و از زنبورها دوری کن.", "۳ دوست را نجات بده", "🦊 دوست روباه"),
            Stage(Mission.MEMORY, "درخت‌های آوازه‌خوان", "درخت‌های جادویی به ترتیب روشن می‌شوند. ترتیب را به خاطر بسپار و به همان ترتیب لمسشان کن.", "۳ آهنگ را تکرار کن", "🎵 فلوت جنگل"),
            Stage(Mission.BALANCE, "پل لرزان", "برای رد شدن از پل لرزان باید تعادلت را حفظ کنی. ژست‌ها را چند ثانیه نگه دار.", "۳ ژست تعادل را نگه دار", "🪶 پر سبک‌پایی"),
            Stage(Mission.REACTION, "صداهای ناگهانی", "در جنگل اتفاق‌های ناگهانی می‌افتد! وقتی گفتم سپر، دست‌ها را بالا ببر؛ وقتی گفتم جاخالی، کنار برو.", "به ۸ اتفاق درست واکنش نشان بده", "⚡ چشم تیزبین"),
            Stage(Mission.COMBO, "شب جنگل", "شب شده و کرم‌های شب‌تاب آمده‌اند. آن‌ها را جمع کن اما مراقب خارها باش!", "۱۲ کرم شب‌تاب جمع کن", "🏮 فانوس نور"),
            Stage(Mission.BOSS, "گرگ سایه", "گرگ سایه نور جنگل را دزدیده! با مشت‌های نورانی به نقطه‌های درخشانش بزن و از گلوله‌های سایه جاخالی بده.", "گرگ سایه را شکست بده", "👑 تاج نگهبان جنگل"),
        ),
    )

    val hero = AdventureTheme(
        id = "hero", title = "آکادمی ابرقهرمان‌ها", emoji = "🦸", music = GameMusic.Theme.HERO, decor = Decor.CITY,
        skyTop = Color(0xFF1A237E), skyBottom = Color(0xFFE91E63), accent = Color(0xFFFFC107),
        good = listOf("⭐", "💎", "⚡", "🛡️", "🏅"),
        sortColors = listOf("قرمز" to Color(0xFFE53935), "زرد" to Color(0xFFFFC107), "آبی" to Color(0xFF2979FF)),
        bad = listOf("💣", "☄️", "🔥"), big = "🚗", friend = "🐱", cage = "🫧",
        lowObstacle = "🚧", sideObstacle = "📦", boss = "🤖", bossName = "ربات آشوبگر",
        stages = listOf(
            Stage(Mission.CATCH, "روز اول آکادمی", "به آکادمی ابرقهرمان‌ها خوش آمدی! اولین تمرین: ستاره‌های انرژی را با دست بگیر.", "۸ ستاره‌ی انرژی بگیر", "🦸 شنل قهرمانی"),
            Stage(Mission.SORT, "کریستال‌های قدرت", "هر قدرت یک رنگ دارد. فقط کریستال رنگی را بگیر که مربی می‌گوید.", "کریستال رنگ درست را بگیر", "🔷 کریستال آبی"),
            Stage(Mission.TWO_HANDS, "قهرمان زورمند", "یک ماشین در حال افتادن است! با هر دو دست بگیرش تا کسی آسیب نبیند.", "۵ ماشین را با دو دست بگیر", "💪 بازوی فولادی"),
            Stage(Mission.DODGE, "تمرین جاخالی", "در میدان تمرین، مانع‌ها به سمتت می‌آیند. خم شو یا کنار برو!", "از ۶ مانع رد شو", "🌀 سرعت باد"),
            Stage(Mission.RESCUE, "نجات بچه‌گربه", "بچه‌گربه‌ها در حباب‌ها گیر افتاده‌اند! حباب‌ها را بترکان و از بمب‌ها دوری کن.", "۳ بچه‌گربه را نجات بده", "🐱 دوست گربه"),
            Stage(Mission.MEMORY, "رمز مخفی", "در پایگاه مخفی، چراغ‌ها رمز را نشان می‌دهند. همان ترتیب را لمس کن تا در باز شود.", "۳ رمز را درست وارد کن", "🔐 کلید مخفی"),
            Stage(Mission.BALANCE, "ژست قهرمانی", "هر قهرمان یک ژست مخصوص دارد. ژست‌ها را محکم و بی‌حرکت نگه دار.", "۳ ژست قهرمانی را نگه دار", "🧘 تمرکز آهنین"),
            Stage(Mission.REACTION, "زنگ خطر شهر", "زنگ خطر به صدا درآمد! سپر بگیر، جاخالی بده و به موقع واکنش نشان بده.", "به ۸ هشدار درست واکنش نشان بده", "🛡️ سپر انرژی"),
            Stage(Mission.COMBO, "شب پرماجرا", "ستاره‌ها از آسمان می‌بارند، اما شهاب‌سنگ‌ها هم هستند! ستاره بگیر و از شهاب‌ها دور شو.", "۱۲ ستاره بگیر", "🌟 چشم ستاره‌ای"),
            Stage(Mission.BOSS, "ربات آشوبگر", "ربات آشوبگر به شهر حمله کرده! با ضربه‌های سریع به نقطه‌های ضعفش بزن و از لیزرهایش جاخالی بده.", "ربات آشوبگر را متوقف کن", "🏆 مدال ابرقهرمان"),
        ),
    )

    val space = AdventureTheme(
        id = "space", title = "فضانورد کوچولو", emoji = "🚀", music = GameMusic.Theme.SPACE, decor = Decor.SPACE,
        skyTop = Color(0xFF0D1B3E), skyBottom = Color(0xFF4A148C), accent = Color(0xFF69F0AE),
        good = listOf("⭐", "🪐", "🛰️", "💫", "🌙"),
        sortColors = listOf("بنفش" to Color(0xFFAB47BC), "سبز" to Color(0xFF66BB6A), "نارنجی" to Color(0xFFFF7043)),
        bad = listOf("☄️", "🪨", "👾"), big = "🛸", friend = "👽", cage = "🫧",
        lowObstacle = "🛰️", sideObstacle = "🪨", boss = "👾", bossName = "هیولای سیاره‌خوار",
        stages = listOf(
            Stage(Mission.CATCH, "پرتاب!", "سه، دو، یک، پرتاب! در فضا ستاره‌های سوخت شناورند. آن‌ها را برای سفینه جمع کن.", "۸ ستاره‌ی سوخت جمع کن", "🧑‍🚀 کلاه فضانوردی"),
            Stage(Mission.SORT, "سیاره‌های رنگی", "هر سیاره یک رنگ دارد. فقط سیاره‌ی رنگی را که مرکز فرماندهی می‌گوید بگیر.", "سیاره‌ی رنگ درست را بگیر", "🗺️ نقشه‌ی کهکشان"),
            Stage(Mission.TWO_HANDS, "بشقاب‌پرنده‌ی سرگردان", "بشقاب‌پرنده‌ها سرگردان‌اند. هر کدام را با دو دست بگیر و آرامش کن.", "۵ بشقاب‌پرنده را با دو دست بگیر", "🧲 دستکش مغناطیسی"),
            Stage(Mission.DODGE, "کمربند سیارک‌ها", "سفینه وارد کمربند سیارک‌ها شده! خم شو یا کنار برو تا به سیارک‌ها نخوری.", "از ۶ سیارک رد شو", "🛡️ سپر سفینه"),
            Stage(Mission.RESCUE, "نجات آدم‌فضایی‌ها", "آدم‌فضایی‌های کوچولو در حباب‌ها گیر افتاده‌اند. حباب‌ها را بترکان!", "۳ آدم‌فضایی را نجات بده", "👽 دوست فضایی"),
            Stage(Mission.MEMORY, "تعمیر سفینه", "چراغ‌های موتور خراب شده. به ترتیبی که روشن می‌شوند لمسشان کن تا تعمیر شوند.", "۳ بار موتور را تعمیر کن", "🔧 جعبه‌ابزار طلایی"),
            Stage(Mission.BALANCE, "بی‌وزنی", "در بی‌وزنی باید بدنت را ثابت نگه داری. ژست‌ها را نگه دار.", "۳ ژست را در بی‌وزنی نگه دار", "🪐 کفش جاذبه"),
            Stage(Mission.REACTION, "هشدار شهاب", "شهاب‌ها ناگهانی می‌آیند! به موقع سپر بگیر یا جاخالی بده.", "به ۸ هشدار درست واکنش نشان بده", "📡 رادار"),
            Stage(Mission.COMBO, "باران ستاره", "باران ستاره شروع شده! ستاره‌ها را بگیر و از سنگ‌های فضایی دوری کن.", "۱۲ ستاره بگیر", "✨ بال ستاره‌ای"),
            Stage(Mission.BOSS, "هیولای سیاره‌خوار", "هیولای سیاره‌خوار می‌خواهد ماه را بخورد! به نقطه‌های درخشانش ضربه بزن.", "هیولا را از منظومه بیرون کن", "🌟 نشان قهرمان کهکشان"),
        ),
    )

    val ocean = AdventureTheme(
        id = "ocean", title = "نجات اقیانوس", emoji = "🐋", music = GameMusic.Theme.OCEAN, decor = Decor.OCEAN,
        skyTop = Color(0xFF01579B), skyBottom = Color(0xFF26C6DA), accent = Color(0xFFFFF176),
        good = listOf("🐚", "🐠", "🦀", "⭐", "🫧"),
        sortColors = listOf("صورتی" to Color(0xFFEC407A), "زرد" to Color(0xFFFFEE58), "سبز" to Color(0xFF66BB6A)),
        bad = listOf("🦈", "🪼", "🛢️"), big = "🧰", friend = "🐢", cage = "🥅",
        lowObstacle = "🪸", sideObstacle = "⚓", boss = "🐙", bossName = "هشت‌پای غمگین",
        stages = listOf(
            Stage(Mission.CATCH, "پاک‌سازی ساحل", "اقیانوس به کمک تو نیاز دارد! صدف‌ها و ماهی‌های کوچولو را جمع کن و به خانه برسان.", "۸ موجود دریایی را نجات بده", "🤿 ماسک غواصی"),
            Stage(Mission.SORT, "مرجان‌های رنگی", "مرجان‌ها رنگ‌های مختلف دارند. فقط مرجان رنگی را لمس کن که می‌گویم تا شکوفا شوند.", "مرجان رنگ درست را لمس کن", "🪸 باغ مرجان"),
            Stage(Mission.TWO_HANDS, "صندوق‌های گنج", "صندوق‌های گنج سنگین‌اند. هر کدام را با دو دست بگیر تا باز شوند.", "۵ صندوق را با دو دست باز کن", "💰 گنج دریا"),
            Stage(Mission.DODGE, "تونل مرجانی", "در تونل مرجانی شنا می‌کنیم. خم شو یا کنار برو تا به مرجان‌ها و لنگرها نخوری.", "از ۶ مانع رد شو", "🧜 باله‌ی شنا"),
            Stage(Mission.RESCUE, "لاک‌پشت‌های گرفتار", "لاک‌پشت‌ها در تور ماهیگیری گیر افتاده‌اند. گره‌های تور را باز کن و از کوسه‌ها دور بمان.", "۳ لاک‌پشت را آزاد کن", "🐢 دوست لاک‌پشت"),
            Stage(Mission.MEMORY, "آواز نهنگ‌ها", "نهنگ‌ها با آواز حرف می‌زنند. چراغ‌ها را به همان ترتیب آوازشان لمس کن.", "۳ آواز را تکرار کن", "🎶 صدف آوازخوان"),
            Stage(Mission.BALANCE, "موج‌سواری", "روی موج‌ها باید تعادلت را نگه داری. ژست‌ها را چند ثانیه نگه دار.", "۳ ژست موج‌سواری را نگه دار", "🏄 تخته‌ی موج‌سواری"),
            Stage(Mission.REACTION, "هشدار کوسه", "کوسه‌ها گاهی ناگهانی می‌آیند! سپر بگیر، جاخالی بده و ماهی‌ها را بگیر.", "به ۸ هشدار درست واکنش نشان بده", "🔦 چراغ زیرآبی"),
            Stage(Mission.COMBO, "مهمانی ماهی‌ها", "ماهی‌های رنگی برای جشن آمده‌اند. آن‌ها را جمع کن اما به عروس دریایی و زباله دست نزن.", "۱۲ ماهی جمع کن", "🎉 جشن دریا"),
            Stage(Mission.BOSS, "هشت‌پای غمگین", "هشت‌پا به خاطر زباله‌ها عصبانی شده و جوهر پرتاب می‌کند! زباله‌های روی بدنش را با ضربه پاک کن تا دوباره شاد شود.", "هشت‌پا را شاد کن", "🐋 نشان نگهبان اقیانوس"),
        ),
    )

    val all = listOf(forest, hero, space, ocean)

    fun byId(id: String) = all.first { it.id == id }
}
