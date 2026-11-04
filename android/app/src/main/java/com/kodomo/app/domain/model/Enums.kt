package com.kodomo.app.domain.model

enum class Gender(val label: String) { BOY("پسر"), GIRL("دختر") }

enum class AvatarKind(val label: String) {
    BOY_CURLY("پسر مو فرفری"),
    BOY_SHORT("پسر مو کوتاه"),
    GIRL_BOW("دختر با پاپیون"),
    GIRL_BRAIDS("دختر با موی بافته"),
    BABY("نی‌نی"),
    FATHER("پدر"),
    MOTHER("مادر"),
    ROBOT("ربات"),
    CAT("گربه"),
    BEAR("خرس"),
    ;

    companion object {
        val forChildren = listOf(BOY_CURLY, BOY_SHORT, GIRL_BOW, GIRL_BRAIDS, BABY, ROBOT, CAT, BEAR)
    }
}

enum class Interest(val label: String, val emoji: String) {
    SPORT("ورزش", "⚽"),
    ASTRONOMY("فضا و نجوم", "🚀"),
    ART("هنر و خلاقیت", "🎨"),
    GAMES("بازی و سرگرمی", "🧩"),
    SCIENCE("علوم و تجربه", "🔬"),
    ANIMALS("حیوانات", "🦊"),
    MUSIC("موسیقی", "🎵"),
    BOOKS("کتاب و داستان", "📚"),
    MOVIES("فیلم و انیمیشن", "🎬"),
}

enum class ContentType(val label: String) {
    VIDEO("فیلم و انیمیشن"),
    BOOK("کتاب"),
    MUSIC("موسیقی"),
    LEARNING("آموزش"),
    EXPLORE("کشف محتوا"),
    ARTICLE("مقاله والدین"),
    WEEKEND_IDEA("دورهمی آخر هفته"),
}

enum class ActivityCategory(val label: String, val emoji: String) {
    VIDEO("فیلم", "🎬"),
    BOOK("کتاب", "📚"),
    GAME("بازی", "🎮"),
    MUSIC("موسیقی", "🎵"),
    LEARNING("آموزش", "🔬"),
    SCHOOL("مدرسه", "🏫"),
    EXPLORE("کشف", "🧭"),
    BROWSER("مرورگر", "🌍"),
    PROJECT("پروژه", "✂️"),
    HOME("خانه", "🏠"),
    APP("برنامه‌ها", "📱"),
    ART("نقاشی", "🖍️"),
}

enum class AuthorRole(val label: String, val isExpert: Boolean) {
    PARENT("والد", false),
    PSYCHOLOGIST("روانشناس کودک", true),
    COUNSELOR("مشاور خانواده", true),
    PEDIATRICIAN("پزشک کودکان", true),
    MEDIA_EXPERT("کارشناس رسانه", true),
}

enum class SchoolTaskKind(val label: String) { HOMEWORK("تکلیف"), PROJECT("پروژه آموزشی") }

enum class MemorySource(val label: String) { PROJECT("پروژه"), LAUGH("لحظه شاد"), MANUAL("ثبت والد"), ART("نقاشی") }

enum class MascotMood { HAPPY, EXCITED, SLEEPY, THINKING }

enum class GameKind(val label: String) { BODY("بازی‌های حرکتی با دوربین"), SENSOR("بازی با حسگر گوشی"), ART("نقاشی و رنگ‌آمیزی") }

enum class MotionGame(val title: String, val emoji: String, val description: String, val needsCamera: Boolean, val kind: GameKind) {
    JUMP_POP("بپر و بترکان", "🎈", "جلوی دوربین بایست؛ با دست بادکنک‌ها را بترکان یا بالا بپر تا همه‌شان یکجا بترکند!", true, GameKind.BODY),
    STAR_CATCH("ستاره‌گیر", "⭐", "ستاره‌ها از آسمان می‌افتند؛ با دست‌هایت آن‌ها را بگیر و از ابرهای بارانی دور شو.", true, GameKind.BODY),
    POSE_MATCH("ژست بگیر!", "🤸", "ربات یک ژست نشان می‌دهد؛ مثل او بایست و چند ثانیه نگه دار.", true, GameKind.BODY),
    FREEZE_DANCE("رقص مجسمه", "🧊", "وقتی موسیقی پخش می‌شود برقص؛ وقتی قطع شد مثل مجسمه بی‌حرکت بمان!", true, GameKind.BODY),
    JUMPING_JACKS("پروانه‌ی پرانرژی", "🦋", "حرکت پروانه (جامپینگ جک) انجام بده تا پروانه‌ها پرواز کنند.", true, GameKind.BODY),
    COLOR_HUNT("شکار رنگ", "🌈", "دوربین را به چیزی با رنگ خواسته‌شده بگیر.", true, GameKind.BODY),
    TILT_RESCUE("نجات با کج کردن", "🐣", "گوشی را کج کن تا توپ نجات به جوجه‌ها برسد و از چاله‌ها دور بماند.", false, GameKind.SENSOR),
    SHAKE_CLEAN("تکان بده و تمیز کن", "🫧", "گوشی را تکان بده تا گل‌ولای از روی توله‌سگ پاک شود.", false, GameKind.SENSOR),
    SIMON_SAYS("سایمون می‌گه", "🕺", "فقط وقتی «سایمون می‌گه» حرکت کن، وگرنه بی‌حرکت بمان!", false, GameKind.SENSOR),
    FREE_DRAW("نقاشی آزاد", "🖌️", "با رنگ‌ها و قلم‌موهای مختلف هر چه دوست داری بکش.", false, GameKind.ART),
    COLORING("رنگ‌آمیزی", "🎨", "یک رنگ انتخاب کن و روی هر قسمت تصویر بزن تا رنگی شود.", false, GameKind.ART),
}
