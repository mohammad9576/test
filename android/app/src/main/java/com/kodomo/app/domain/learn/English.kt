package com.kodomo.app.domain.learn

/**
 * English for children from absolute zero: picture words with their Persian meaning. Pronunciation comes from the
 * phone's English text-to-speech voice (available on practically every Android phone).
 */
data class Word(val en: String, val fa: String, val emoji: String)

data class EnglishUnit(val id: String, val title: String, val emoji: String, val words: List<Word>, val minAge: Int = 3)

object English {
    val alphabet: List<Word> = listOf(
        Word("A", "اِی – Apple (سیب)", "🍎"), Word("B", "بی – Ball (توپ)", "⚽"), Word("C", "سی – Cat (گربه)", "🐱"),
        Word("D", "دی – Dog (سگ)", "🐶"), Word("E", "ای – Egg (تخم‌مرغ)", "🥚"), Word("F", "اِف – Fish (ماهی)", "🐟"),
        Word("G", "جی – Goat (بز)", "🐐"), Word("H", "اِیچ – Hat (کلاه)", "🎩"), Word("I", "آی – Ice cream (بستنی)", "🍦"),
        Word("J", "جِی – Juice (آب‌میوه)", "🧃"), Word("K", "کِی – Kite (بادبادک)", "🪁"), Word("L", "اِل – Lion (شیر)", "🦁"),
        Word("M", "اِم – Moon (ماه)", "🌙"), Word("N", "اِن – Nest (لانه)", "🪺"), Word("O", "اُو – Orange (پرتقال)", "🍊"),
        Word("P", "پی – Pen (خودکار)", "🖊️"), Word("Q", "کیو – Queen (ملکه)", "👑"), Word("R", "آر – Rabbit (خرگوش)", "🐰"),
        Word("S", "اِس – Sun (خورشید)", "☀️"), Word("T", "تی – Tree (درخت)", "🌳"), Word("U", "یو – Umbrella (چتر)", "☂️"),
        Word("V", "وی – Van (ون)", "🚐"), Word("W", "دابلیو – Water (آب)", "💧"), Word("X", "اِکس – Xylophone (بلز)", "🎼"),
        Word("Y", "وای – Yo-yo (یویو)", "🪀"), Word("Z", "زِد – Zebra (گورخر)", "🦓"),
    )

    val units: List<EnglishUnit> = listOf(
        EnglishUnit("alphabet", "حروف الفبا A تا Z", "🔤", alphabet),
        EnglishUnit("hello", "سلام و احوال‌پرسی", "👋", listOf(
            Word("Hello", "سلام", "👋"), Word("Goodbye", "خداحافظ", "🙋"), Word("Thank you", "ممنونم", "🙏"), Word("Please", "لطفاً", "🥺"),
            Word("Yes", "بله", "👍"), Word("No", "نه", "👎"), Word("Good morning", "صبح بخیر", "🌅"), Word("Good night", "شب بخیر", "🌙"),
            Word("Sorry", "ببخشید", "😔"), Word("My name is…", "اسم من … است", "🙂"),
        )),
        EnglishUnit("numbers", "اعداد ۱ تا ۱۰", "🔢", listOf(
            Word("One", "یک", "1️⃣"), Word("Two", "دو", "2️⃣"), Word("Three", "سه", "3️⃣"), Word("Four", "چهار", "4️⃣"), Word("Five", "پنج", "5️⃣"),
            Word("Six", "شش", "6️⃣"), Word("Seven", "هفت", "7️⃣"), Word("Eight", "هشت", "8️⃣"), Word("Nine", "نه", "9️⃣"), Word("Ten", "ده", "🔟"),
        )),
        EnglishUnit("colors", "رنگ‌ها", "🎨", listOf(
            Word("Red", "قرمز", "🔴"), Word("Blue", "آبی", "🔵"), Word("Green", "سبز", "🟢"), Word("Yellow", "زرد", "🟡"), Word("Orange", "نارنجی", "🟠"),
            Word("Purple", "بنفش", "🟣"), Word("Black", "سیاه", "⚫"), Word("White", "سفید", "⚪"), Word("Brown", "قهوه‌ای", "🟤"), Word("Pink", "صورتی", "🩷"),
        )),
        EnglishUnit("animals", "حیوانات", "🦁", listOf(
            Word("Cat", "گربه", "🐱"), Word("Dog", "سگ", "🐶"), Word("Bird", "پرنده", "🐦"), Word("Fish", "ماهی", "🐟"), Word("Cow", "گاو", "🐮"),
            Word("Horse", "اسب", "🐴"), Word("Lion", "شیر", "🦁"), Word("Elephant", "فیل", "🐘"), Word("Monkey", "میمون", "🐒"), Word("Rabbit", "خرگوش", "🐰"),
            Word("Duck", "اردک", "🦆"), Word("Bear", "خرس", "🐻"),
        )),
        EnglishUnit("family", "خانواده", "👨‍👩‍👧", listOf(
            Word("Mother", "مادر", "👩"), Word("Father", "پدر", "👨"), Word("Sister", "خواهر", "👧"), Word("Brother", "برادر", "👦"),
            Word("Baby", "نوزاد", "👶"), Word("Grandmother", "مادربزرگ", "👵"), Word("Grandfather", "پدربزرگ", "👴"), Word("Family", "خانواده", "👨‍👩‍👧‍👦"),
        )),
        EnglishUnit("body", "بدن من", "🧍", listOf(
            Word("Head", "سر", "🙂"), Word("Eye", "چشم", "👁️"), Word("Ear", "گوش", "👂"), Word("Nose", "بینی", "👃"), Word("Mouth", "دهان", "👄"),
            Word("Hand", "دست", "✋"), Word("Foot", "پا", "🦶"), Word("Hair", "مو", "💇"), Word("Tooth", "دندان", "🦷"), Word("Leg", "ساق پا", "🦵"),
        )),
        EnglishUnit("food", "خوراکی‌ها", "🍎", listOf(
            Word("Apple", "سیب", "🍎"), Word("Banana", "موز", "🍌"), Word("Bread", "نان", "🍞"), Word("Milk", "شیر", "🥛"), Word("Water", "آب", "💧"),
            Word("Cheese", "پنیر", "🧀"), Word("Egg", "تخم‌مرغ", "🥚"), Word("Rice", "برنج", "🍚"), Word("Cake", "کیک", "🍰"), Word("Grapes", "انگور", "🍇"),
        ), minAge = 4),
        EnglishUnit("school", "وسایل مدرسه", "🎒", listOf(
            Word("Book", "کتاب", "📕"), Word("Pen", "خودکار", "🖊️"), Word("Pencil", "مداد", "✏️"), Word("Bag", "کیف", "🎒"), Word("Desk", "میز", "🪑"),
            Word("Teacher", "معلم", "👩‍🏫"), Word("Ruler", "خط‌کش", "📏"), Word("Eraser", "پاک‌کن", "🧽"), Word("Scissors", "قیچی", "✂️"), Word("Computer", "رایانه", "💻"),
        ), minAge = 5),
        EnglishUnit("toys", "اسباب‌بازی‌ها", "🧸", listOf(
            Word("Ball", "توپ", "⚽"), Word("Doll", "عروسک", "🪆"), Word("Car", "ماشین", "🚗"), Word("Teddy bear", "خرس عروسکی", "🧸"), Word("Kite", "بادبادک", "🪁"),
            Word("Train", "قطار", "🚂"), Word("Robot", "ربات", "🤖"), Word("Balloon", "بادکنک", "🎈"),
        )),
        EnglishUnit("weather", "آب‌وهوا و طبیعت", "🌦️", listOf(
            Word("Sun", "خورشید", "☀️"), Word("Rain", "باران", "🌧️"), Word("Snow", "برف", "❄️"), Word("Cloud", "ابر", "☁️"), Word("Wind", "باد", "🌬️"),
            Word("Tree", "درخت", "🌳"), Word("Flower", "گل", "🌸"), Word("Star", "ستاره", "⭐"), Word("Moon", "ماه", "🌙"), Word("Sea", "دریا", "🌊"),
        ), minAge = 5),
        EnglishUnit("verbs", "کارها (فعل‌ها)", "🏃", listOf(
            Word("Run", "دویدن", "🏃"), Word("Jump", "پریدن", "🤸"), Word("Eat", "خوردن", "🍽️"), Word("Drink", "نوشیدن", "🥤"), Word("Sleep", "خوابیدن", "😴"),
            Word("Read", "خواندن", "📖"), Word("Write", "نوشتن", "✍️"), Word("Swim", "شنا کردن", "🏊"), Word("Sing", "آواز خواندن", "🎤"), Word("Play", "بازی کردن", "🎮"),
        ), minAge = 6),
        EnglishUnit("sentences", "جمله‌های ساده", "💬", listOf(
            Word("I am happy.", "من خوشحالم.", "😊"), Word("I like apples.", "من سیب دوست دارم.", "🍎"), Word("This is my cat.", "این گربه‌ی من است.", "🐱"),
            Word("I have a ball.", "من یک توپ دارم.", "⚽"), Word("The sky is blue.", "آسمان آبی است.", "🌤️"), Word("How are you?", "حالت چطور است؟", "🙂"),
            Word("I am fine.", "من خوبم.", "👍"), Word("I can run.", "من می‌توانم بدوم.", "🏃"),
        ), minAge = 7),
    )

    fun byId(id: String) = units.firstOrNull { it.id == id }
}
