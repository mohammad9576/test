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
}

enum class AuthorRole(val label: String, val isExpert: Boolean) {
    PARENT("والد", false),
    PSYCHOLOGIST("روانشناس کودک", true),
    COUNSELOR("مشاور خانواده", true),
    PEDIATRICIAN("پزشک کودکان", true),
    MEDIA_EXPERT("کارشناس رسانه", true),
}

enum class SchoolTaskKind(val label: String) { HOMEWORK("تکلیف"), PROJECT("پروژه آموزشی") }

enum class MemorySource(val label: String) { PROJECT("پروژه"), LAUGH("لحظه شاد"), MANUAL("ثبت والد") }

enum class MascotMood { HAPPY, EXCITED, SLEEPY, THINKING }

enum class MotionGame(val title: String, val emoji: String, val description: String, val needsCamera: Boolean) {
    TILT_RESCUE("نجات با کج کردن", "🐣", "گوشی را کج کن تا توپ نجات به جوجه‌ها برسد و از چاله‌ها دور بماند.", false),
    SHAKE_CLEAN("تکان بده و تمیز کن", "🫧", "گوشی را تکان بده تا گل‌ولای از روی توله‌سگ پاک شود.", false),
    JUMP_POP("بپر و بترکان", "🎈", "جلوی دوربین بالا بپر تا بادکنک‌ها بترکند.", true),
    SIMON_SAYS("سایمون می‌گه", "🕺", "فقط وقتی «سایمون می‌گه» حرکت کن، وگرنه بی‌حرکت بمان!", false),
    COLOR_HUNT("شکار رنگ", "🌈", "دوربین را به چیزی با رنگ خواسته‌شده بگیر.", true),
}
