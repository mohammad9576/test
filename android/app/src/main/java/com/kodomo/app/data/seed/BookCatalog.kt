package com.kodomo.app.data.seed

import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentPage
import com.kodomo.app.domain.model.ContentType.BOOK
import com.kodomo.app.domain.model.Interest

/** Original short stories written for Kodomo, each with 6 illustrated pages. */
internal object BookCatalog {

    val books: List<ContentItem> = listOf(
        content(
            id = "book_little_elephant", type = BOOK, title = "فیل کوچولو و باران", category = "قصه‌های شب",
            description = "فیل کوچولو از صدای رعد می‌ترسد، اما با کمک دوستانش یاد می‌گیرد که باران چه هدیه‌ی قشنگی است.",
            emoji = "🐘", colors = Palette.sky, rating = rating(minAge = 2, maxAge = 8, educational = 4, familyValues = 5),
            durationMinutes = 5, positives = listOf("کنار آمدن با ترس", "دوستی و همدلی"),
            expertReview = "داستانی ساده و آرام برای پیش از خواب که به کودک کمک می‌کند احساس ترس را نام ببرد و با آن کنار بیاید.",
            expertName = "دکتر مریم احمدی - مشاور کودک", peerLikes = 230, peerDislikes = 8,
            interests = setOf(Interest.ANIMALS, Interest.BOOKS), author = "تیم نویسندگان کدومو",
        ),
        content(
            id = "book_star_seed", type = BOOK, title = "دانه‌ای که ستاره شد", category = "علمی و طبیعت",
            description = "دانه‌ی کوچکی در خاک، آب و نور را پیدا می‌کند و قدم‌به‌قدم به یک گل آفتاب‌گردان بزرگ تبدیل می‌شود.",
            emoji = "🌻", colors = Palette.sun, rating = rating(minAge = 3, maxAge = 9, educational = 5, creativity = 3),
            durationMinutes = 5, positives = listOf("آشنایی با رشد گیاه", "صبر و تلاش"),
            expertReview = "معرفی ساده و دقیق چرخه‌ی رشد گیاه؛ پیشنهاد می‌شود پس از خواندن، با کودک یک دانه‌ی لوبیا بکارید.",
            expertName = "واحد آموزش علوم کدومو", peerLikes = 175, peerDislikes = 6,
            interests = setOf(Interest.SCIENCE, Interest.BOOKS), author = "تیم نویسندگان کدومو",
        ),
        content(
            id = "book_robot_friend", type = BOOK, title = "ربات کوچولو دوست پیدا می‌کند", category = "دوستی و مهارت‌های اجتماعی",
            description = "ربات کوچولو در مدرسه‌ی جدید تنهاست؛ تا اینکه یاد می‌گیرد چطور سلام کند، گوش بدهد و کمک کند.",
            emoji = "🤖", colors = Palette.mint, rating = rating(minAge = 4, maxAge = 10, educational = 4, familyValues = 4, creativity = 4),
            durationMinutes = 6, positives = listOf("مهارت دوست‌یابی", "مهربانی"),
            expertReview = "مدل‌سازی رفتارهای اجتماعی مثبت برای کودکانی که تازه وارد مهد یا مدرسه شده‌اند.",
            expertName = "دکتر سارا محمدی - روانشناس کودک", peerLikes = 198, peerDislikes = 9,
            interests = setOf(Interest.GAMES, Interest.BOOKS, Interest.SCIENCE), author = "تیم نویسندگان کدومو",
        ),
        content(
            id = "book_moon_trip", type = BOOK, title = "سفر آرمان به ماه", category = "علمی و طبیعت",
            description = "آرمان با موشک مقوایی‌اش در خیال به ماه سفر می‌کند و درباره‌ی گرانش، دهانه‌ها و زمین آبی چیزهای تازه یاد می‌گیرد.",
            emoji = "🚀", colors = Palette.night, rating = rating(minAge = 6, maxAge = 12, educational = 5, creativity = 5),
            durationMinutes = 7, positives = listOf("مفاهیم پایه‌ی نجوم", "تخیل علمی"),
            expertReview = "ترکیب تخیل و دانش دقیق نجوم؛ برای کودکان علاقه‌مند به فضا عالی است.",
            expertName = "واحد آموزش علوم کدومو", peerLikes = 264, peerDislikes = 11,
            interests = setOf(Interest.ASTRONOMY, Interest.SCIENCE, Interest.BOOKS), author = "تیم نویسندگان کدومو",
        ),
    )

    val pages: List<ContentPage> =
        pages(
            "book_little_elephant",
            "🐘" to "در یک جنگل سبز و بزرگ، فیل کوچولویی به اسم «پیلو» زندگی می‌کرد.",
            "⛈️" to "یک روز آسمان تاریک شد و صدای رعد بلند آمد. پیلو ترسید و پشت مامانش قایم شد.",
            "🐒" to "میمون کوچولو گفت: «نترس پیلو! رعد فقط صدای بلند ابرهاست.»",
            "🌧️" to "قطره‌های باران روی برگ‌ها رقصیدند. پیلو خرطومش را بالا برد و قطره‌ها را مزه کرد.",
            "🌈" to "وقتی باران تمام شد، یک رنگین‌کمان بزرگ در آسمان پیدا شد.",
            "💚" to "پیلو لبخند زد و گفت: «حالا می‌دانم باران دوست جنگل است.» شب بخیر پیلو!",
        ) + pages(
            "book_star_seed",
            "🌰" to "دانه‌ی کوچکی زیر خاک خوابیده بود و خواب یک گل بزرگ را می‌دید.",
            "💧" to "باران آمد و دانه آب نوشید. پوسته‌اش نرم شد و یک ریشه‌ی کوچولو بیرون آمد.",
            "🌱" to "ریشه به پایین رفت و یک جوانه‌ی سبز به سمت بالا حرکت کرد.",
            "☀️" to "جوانه سر از خاک بیرون آورد و خورشید را دید. برگ‌هایش نور را مثل غذا خوردند.",
            "🌿" to "هر روز کمی بلندتر شد. باد با او بازی می‌کرد و زنبورها به دیدنش می‌آمدند.",
            "🌻" to "یک روز صبح، یک آفتاب‌گردان طلایی باز شد؛ مثل یک ستاره که روی زمین می‌درخشد!",
        ) + pages(
            "book_robot_friend",
            "🤖" to "ربات کوچولو اولین روزی بود که به مدرسه‌ی جدید می‌رفت.",
            "😟" to "همه با هم بازی می‌کردند، اما ربات کوچولو گوشه‌ای ایستاده بود و نمی‌دانست چه کند.",
            "👋" to "معلم گفت: «دوست‌یابی با یک سلام ساده شروع می‌شود.» ربات کوچولو گفت: «سلام! اسم من بیپ است.»",
            "👂" to "دختری به اسم نیلا از کتاب مورد علاقه‌اش گفت. بیپ با دقت گوش داد و سؤال پرسید.",
            "🧱" to "وقتی برج مکعب‌های بچه‌ها افتاد، بیپ کمک کرد تا دوباره آن را بسازند.",
            "💚" to "آخر روز، بیپ سه دوست جدید داشت. او یاد گرفت: سلام کن، گوش بده و کمک کن!",
        ) + pages(
            "book_moon_trip",
            "📦" to "آرمان با یک جعبه‌ی مقوایی بزرگ موشکی ساخت و اسمش را «امید» گذاشت.",
            "🚀" to "سه، دو، یک... پرتاب! آرمان در خیالش از جو زمین گذشت و آسمان سیاه و پرستاره را دید.",
            "🌍" to "از پنجره، زمین مثل یک تیله‌ی آبی و سفید می‌درخشید. بیشتر سطح زمین را آب پوشانده است.",
            "🌕" to "روی ماه، آرمان مثل یک بادکنک سبک بالا و پایین می‌پرید؛ چون گرانش ماه حدود یک‌ششم زمین است.",
            "🕳️" to "او دهانه‌های بزرگی دید که سنگ‌های فضایی میلیون‌ها سال پیش ساخته بودند.",
            "🛏️" to "صدای مامان آمد: «آرمان، شام!» سفر تمام شد، اما آرمان تصمیم گرفت فضانورد شود.",
        )
}
