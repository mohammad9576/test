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
    BOOK_REVIEW("کتاب (ارزیابی کدومو)"),
    GAME_REVIEW("بازی دیجیتال"),
    TOY_REVIEW("اسباب‌بازی و بازی رومیزی"),
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

enum class GameKind(val label: String) {
    PARTY("بازی‌های ۲، ۳ و ۴ نفره (روی یک گوشی)"),
    BODY("بازی‌های حرکتی با دوربین"),
    SPORT("بازی‌های ورزشی"),
    ADVENTURE("ماجراجویی: ماشین، فضا، آشپزی و مد"),
    BRAIN("بازی‌های فکری و سرعتی"),
    SENSOR("بازی با حسگر گوشی"),
    ART("نقاشی و رنگ‌آمیزی"),
    ;

    /** Party and art games have no levels; all others get harder level by level. */
    val leveled: Boolean get() = this != PARTY && this != ART
}

enum class MotionGame(val title: String, val emoji: String, val description: String, val needsCamera: Boolean, val kind: GameKind) {
    PING_PONG("پینگ‌پنگ با ربات", "🏓", "راکت را با انگشت جابه‌جا کن و توپ را به ربات برگردان. هر کس زودتر ۵ امتیاز بگیرد برنده است!", false, GameKind.SPORT),
    PENALTY("ضربه‌ی پنالتی", "⚽", "انگشتت را روی توپ بکش و شوت بزن؛ دروازه‌بان را گول بزن و گل بزن!", false, GameKind.SPORT),
    PONG_BODY("پینگ‌پنگ با دست (دوربین)", "🙌", "جلوی دوربین بایست و دستت را به چپ و راست ببر تا راکت حرکت کند!", true, GameKind.BODY),
    GOALKEEPER("دروازه‌بان قهرمان", "🧤", "جلوی دوربین بایست و با دست‌هایت توپ‌ها را بگیر تا گل نشوند.", true, GameKind.BODY),
    JUMP_POP("بپر و بترکان", "🎈", "جلوی دوربین بایست؛ با دست بادکنک‌ها را بترکان یا بالا بپر تا همه‌شان یکجا بترکند!", true, GameKind.BODY),
    STAR_CATCH("ستاره‌گیر", "⭐", "ستاره‌ها از آسمان می‌افتند؛ با دست‌هایت آن‌ها را بگیر و از ابرهای بارانی دور شو.", true, GameKind.BODY),
    POSE_MATCH("ژست بگیر!", "🤸", "ربات یک ژست نشان می‌دهد؛ مثل او بایست و چند ثانیه نگه دار.", true, GameKind.BODY),
    FREEZE_DANCE("رقص مجسمه", "🧊", "وقتی موسیقی پخش می‌شود برقص؛ وقتی قطع شد مثل مجسمه بی‌حرکت بمان!", true, GameKind.BODY),
    JUMPING_JACKS("پروانه‌ی پرانرژی", "🦋", "حرکت پروانه (جامپینگ جک) انجام بده تا پروانه‌ها پرواز کنند.", true, GameKind.BODY),
    COLOR_HUNT("شکار رنگ", "🌈", "دوربین را به چیزی با رنگ خواسته‌شده بگیر.", true, GameKind.BODY),
    TILT_RESCUE("نجات با کج کردن", "🐣", "گوشی را کج کن تا توپ نجات به جوجه‌ها برسد و از چاله‌ها دور بماند.", false, GameKind.SENSOR),
    SHAKE_CLEAN("تکان بده و تمیز کن", "🫧", "گوشی را تکان بده تا گل‌ولای از روی توله‌سگ پاک شود.", false, GameKind.SENSOR),
    SIMON_SAYS("سایمون می‌گه", "🕺", "فقط وقتی «سایمون می‌گه» حرکت کن، وگرنه بی‌حرکت بمان!", false, GameKind.SENSOR),
    BOXING("بوکس قهرمان", "🥊", "جلوی دوربین بایست و با مشت‌های سریع به بالشتک‌ها ضربه بزن؛ طلایی‌ها سه امتیاز دارند!", true, GameKind.BODY),
    SKI("اسکی کوهستان", "⛷️", "بدنت را به چپ و راست خم کن تا اسکی‌باز از دروازه‌ها رد شود و به درخت نخورد.", true, GameKind.BODY),
    JUMP_ROPE("طناب‌بازی", "🪢", "هر بار که طناب زیر پایت می‌رسد، بپر! چند تا پشت سر هم می‌توانی؟", true, GameKind.BODY),
    SPRINT_RACE("دوی سرعت با ربات", "🏃", "درجا بدو و زانوهایت را بالا بیاور تا از ربات جلو بزنی!", true, GameKind.BODY),
    SQUAT("وزنه‌بردار کوچک", "🏋️", "بنشین و بلند شو؛ با هر اسکات، وزنه بالاتر می‌رود.", true, GameKind.BODY),
    BASKETBALL("بسکتبال", "🏀", "دست‌ها را بالای سر ببر و پرتاب کن؛ با جای دست‌هایت نشانه بگیر!", true, GameKind.BODY),
    HAND_SLICE("میوه‌بُر جادویی", "🍉", "میوه‌ها به هوا پرت می‌شوند؛ دستت را مثل شمشیر از وسطشان رد کن! مراقب بمب‌ها باش و ترکیب بساز.", true, GameKind.BODY),
    SHAPE_WALL("دیوار شکل‌ها", "🧱", "دیوار با یک سوراخ به سمتت می‌آید؛ به چپ و راست برو، بنشین یا دست‌ها را بالا ببر تا از آن رد شوی!", true, GameKind.BODY),
    TAP_RACE("مسابقه‌ی ضربه", "🏁", "۲ تا ۴ نفر: هر کس سریع‌تر روی رنگ خودش بزند، ماشینش زودتر می‌رسد.", false, GameKind.PARTY),
    REFLEX_DUEL("کی سریع‌تره؟", "⚡", "۲ تا ۴ نفر: وقتی دایره سبز شد زودتر از همه بزن؛ زود زدن امتیاز کم می‌کند!", false, GameKind.PARTY),
    AIR_HOCKEY("هاکی هوایی", "🏒", "۲ نفر روبه‌روی هم: با انگشت توپ را به دروازه‌ی حریف بزن.", false, GameKind.PARTY),
    TUG_OF_WAR("طناب‌کشی", "🪢", "۲ تا ۴ نفر در دو تیم: با ضربه‌های سریع طناب را به سمت تیمت بکش.", false, GameKind.PARTY),
    SNAP_MATCH("جفت‌یاب سریع", "👀", "۲ تا ۴ نفر: وقتی دو تصویر یکی شدند، زودتر از همه بزن!", false, GameKind.PARTY),
    CAR_RACE("مسابقه‌ی ماشین‌ها", "🏎️", "خط عوض کن، از ماشین‌ها جاخالی بده و سکه جمع کن.", false, GameKind.ADVENTURE),
    SPACE_GUARD("نگهبان فضا", "🚀", "سفینه را جابه‌جا کن و شهاب‌سنگ‌ها را پیش از رسیدن به سیاره بزن.", false, GameKind.ADVENTURE),
    BAKERY("قنادی کوچولو", "🎂", "کیک را دقیقاً همان‌طور که مشتری سفارش داده بساز و تحویل بده.", false, GameKind.ADVENTURE),
    STYLIST("استایل‌ساز", "👗", "برای برف، ساحل، جشن و ورزش لباس مناسب انتخاب کن.", false, GameKind.ADVENTURE),
    MEMORY_CARDS("کارت‌های حافظه", "🃏", "کارت‌ها را برگردان و جفت‌های یکسان را پیدا کن. هر مرحله کارت بیشتری دارد!", false, GameKind.BRAIN),
    WHACK_A_MOLE("موش‌های بازیگوش", "🐹", "موش‌ها از سوراخ بیرون می‌آیند؛ سریع رویشان بزن ولی به گربه دست نزن!", false, GameKind.BRAIN),
    FRUIT_CATCH("سبد میوه", "🍎", "سبد را با انگشت جابه‌جا کن و میوه‌ها را بگیر؛ مراقب بمب‌ها باش!", false, GameKind.BRAIN),
    FLAPPY_ROBOT("ربات پرنده", "🤖", "روی صفحه بزن تا ربات بالا بپرد و از بین ستون‌ها رد شود.", false, GameKind.BRAIN),
    SLIDE_PUZZLE("پازل کشویی", "🧩", "خانه‌ها را جابه‌جا کن تا عددها به ترتیب بچینند.", false, GameKind.BRAIN),
    COLOR_SEQUENCE("دنباله‌ی رنگ‌ها", "🔴", "به ترتیب روشن شدن رنگ‌ها دقت کن و همان را تکرار کن.", false, GameKind.BRAIN),
    MATH_BALLOONS("بادکنک‌های ریاضی", "➕", "جواب درست را روی بادکنک‌ها پیدا کن و بترکانش!", false, GameKind.BRAIN),
    FREE_DRAW("نقاشی آزاد", "🖌️", "با رنگ‌ها و قلم‌موهای مختلف هر چه دوست داری بکش.", false, GameKind.ART),
    COLORING("رنگ‌آمیزی", "🎨", "یک رنگ انتخاب کن و روی هر قسمت تصویر بزن تا رنگی شود.", false, GameKind.ART),
}
