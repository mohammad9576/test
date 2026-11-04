package com.kodomo.app.data.seed

import com.kodomo.app.data.local.entity.ForumPostEntity
import com.kodomo.app.data.local.entity.ForumTopicEntity
import com.kodomo.app.domain.model.AuthorRole
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentType.ARTICLE
import com.kodomo.app.domain.model.ContentType.WEEKEND_IDEA
import java.time.Instant
import java.time.temporal.ChronoUnit

internal object ParentCatalog {

    val articles: List<ContentItem> = listOf(
        content(
            "article_screen_time", ARTICLE, "قرارداد خانوادگی زمان صفحه‌نمایش", "چطور با مشارکت خود کودک برای استفاده از گوشی قانون بگذاریم.",
            "فرزندپروری دیجیتال", "📱", Palette.mint, rating(minAge = 0),
            body = """
                کودکان وقتی در قانون‌گذاری مشارکت داشته باشند، آن را بهتر می‌پذیرند. یک «قرارداد خانوادگی» ساده بنویسید:

                ۱. زمان مجاز روزانه را با توجه به سن کودک تعیین کنید. انجمن پزشکان کودکان برای ۲ تا ۵ سال حداکثر یک ساعت محتوای باکیفیت را توصیه می‌کند.
                ۲. مکان‌های بدون گوشی را مشخص کنید: سر میز غذا و اتاق خواب.
                ۳. یک ساعت قبل از خواب همه‌ی صفحه‌ها خاموش شوند؛ نور آبی خواب را عقب می‌اندازد.
                ۴. قانون برای بزرگ‌ترها هم هست. کودکان بیش از حرف ما، رفتار ما را تقلید می‌کنند.
                ۵. هشدار پایان زمان را از قبل بدهید. قطع ناگهانی، حس سرخوردگی و مقاومت ایجاد می‌کند؛ کدومو این کار را با پیام ۵ دقیقه‌ای ربات انجام می‌دهد.

                قرارداد را امضا کنید و روی یخچال بچسبانید. هر سه ماه آن را با هم بازبینی کنید.
            """.trimIndent(),
            author = "دکتر سارا محمدی - روانشناس کودک", durationMinutes = 6,
        ),
        content(
            "article_tantrum", ARTICLE, "وقتی زمان تمام می‌شود و کودک گریه می‌کند", "راهکارهای علمی برای مدیریت قشقرق پس از خاموش کردن تبلت.",
            "مدیریت هیجان", "🌧️", Palette.sky, rating(minAge = 0),
            body = """
                بسیاری از کودکان هنگام قطع شدن بازی یا انیمیشن واکنش شدید نشان می‌دهند. این واکنش طبیعی است؛ مغز در حال تجربه‌ی لذت بوده و ناگهان متوقف شده است.

                • هیجان را نام ببرید: «می‌دانم ناراحتی چون انیمیشن تمام شد.»
                • جایگزین بدهید: یک فعالیت جذاب بدون صفحه‌نمایش از قبل آماده کنید.
                • ثابت‌قدم باشید: اگر با گریه زمان اضافه بدهید، کودک می‌آموزد گریه راه افزایش زمان است.
                • از «پایان طبیعی» استفاده کنید: اجازه دهید قسمت فعلی تمام شود، نه اینکه وسط آن قطع شود.
            """.trimIndent(),
            author = "دکتر مریم احمدی - مشاور کودک", durationMinutes = 5,
        ),
        content(
            "article_anime_bulletin", ARTICLE, "نشریه‌ی انیمه و فرهنگ رسانه - شماره‌ی ۱", "تحلیلی بر محبوبیت انیمه‌ها میان نوجوانان و نکاتی برای انتخاب آگاهانه.",
            "بولتن تحلیلی", "🗞️", Palette.lavender, rating(minAge = 0),
            body = """
                انیمه‌ها با داستان‌های چندلایه و شخصیت‌های پیچیده، بخش بزرگی از سبد رسانه‌ای نوجوانان را تشکیل داده‌اند.

                نکات مثبت: روایت‌های طولانی، تأکید بر دوستی و تلاش، و تصویرسازی هنری.
                نکات قابل توجه: برخی آثار رده‌ی سنی بالاتری دارند و شامل خشونت تصویری یا مضامین بزرگسالانه هستند.

                پیشنهاد کدومو: پیش از انتخاب، شناسنامه‌ی ارزیابی اثر را در بخش «ارزیابی» ببینید و قسمت اول را همراه نوجوان تماشا کنید. گفت‌وگو درباره‌ی شخصیت‌ها، نوجوان را به تحلیلگر رسانه تبدیل می‌کند.
            """.trimIndent(),
            author = "مهندس رضا کریمی - کارشناس رسانه", durationMinutes = 8,
        ),
        content(
            "article_reading_habit", ARTICLE, "۷ راه برای کتاب‌خوان کردن کودک", "عادت مطالعه از خانه شروع می‌شود؛ قدم‌های کوچک و ماندگار.",
            "آموزش والدین", "📚", Palette.sun, rating(minAge = 0),
            body = """
                ۱. هر شب ۱۰ دقیقه کتاب‌خوانی مشترک داشته باشید.
                ۲. اجازه دهید کودک خودش کتاب را انتخاب کند.
                ۳. با صداهای مختلف شخصیت‌ها را بخوانید.
                ۴. درباره‌ی تصاویر سؤال بپرسید.
                ۵. یک گوشه‌ی دنج مطالعه بسازید.
                ۶. خودتان جلوی کودک کتاب بخوانید.
                ۷. از حالت «برای من بخوان» کدومو استفاده کنید تا صدای شما حتی وقتی سر کار هستید همراه کودک باشد.
            """.trimIndent(),
            author = "تیم آموزش کدومو", durationMinutes = 4,
        ),
        content(
            "article_online_safety", ARTICLE, "ایمنی آنلاین برای کودکان دبستانی", "چه چیزهایی را قبل از ورود کودک به اینترنت آزاد باید آموزش دهیم.",
            "فرزندپروری دیجیتال", "🛡️", Palette.forest, rating(minAge = 0),
            body = """
                • هرگز اطلاعات شخصی (نام کامل، آدرس، مدرسه) را با غریبه‌ها به اشتراک نگذارد.
                • اگر چیزی دید که حالش را بد کرد، بلافاصله به والد بگوید؛ بدون ترس از تنبیه.
                • همه‌ی آدم‌های آنلاین آن کسی نیستند که ادعا می‌کنند.
                • رمز عبور فقط مال خودش و والدینش است.

                «مرورگر امن من» در کدومو، تمرین این مهارت‌ها در محیطی بسته و پالایش‌شده است.
            """.trimIndent(),
            author = "مهندس رضا کریمی - کارشناس رسانه", durationMinutes = 5,
        ),
    )

    val weekendIdeas: List<ContentItem> = listOf(
        content("weekend_movie_night", WEEKEND_IDEA, "شب فیلم خانوادگی", "یک انیمیشن با رده‌ی سنی مناسب انتخاب کنید، چراغ‌ها را کم کنید و پاپ‌کورن درست کنید. بعد از فیلم، هر کس شخصیت محبوبش را بگوید.", "شب فیلم", "🍿", Palette.night, rating(minAge = 3), durationMinutes = 120),
        content("weekend_nature_walk", WEEKEND_IDEA, "شکار گنج در طبیعت", "فهرستی از ۱۰ چیز طبیعی (برگ قلبی، سنگ صاف، پر پرنده...) بنویسید و در پارک دنبالشان بگردید.", "طبیعت‌گردی", "🌲", Palette.forest, rating(minAge = 3), durationMinutes = 90),
        content("weekend_board_games", WEEKEND_IDEA, "مسابقه‌ی بازی‌های رومیزی", "منچ، مار و پله یا بازی دلخواه؛ برنده‌ی هر دور، بازی بعدی را انتخاب می‌کند.", "بازی رومیزی", "🎲", Palette.sky, rating(minAge = 4), durationMinutes = 60),
        content("weekend_cooking", WEEKEND_IDEA, "سرآشپزهای کوچک", "یک پیتزای خانگی درست کنید و اجازه دهید هر کودک روی بخش خودش مواد دلخواه بچیند.", "آشپزی", "🍕", Palette.peach, rating(minAge = 4), durationMinutes = 75),
        content("weekend_stargazing", WEEKEND_IDEA, "رصد ستاره‌ها", "شب صاف به پشت‌بام یا بیرون شهر بروید و صورت‌های فلکی را پیدا کنید. «دب اکبر» را از همه زودتر پیدا کنید!", "نجوم", "🔭", Palette.night, rating(minAge = 6), durationMinutes = 60),
        content("weekend_puppet_show", WEEKEND_IDEA, "تئاتر عروسکی جورابی", "با جوراب‌های قدیمی و دکمه عروسک بسازید و یک نمایش کوتاه برای پدربزرگ و مادربزرگ اجرا کنید.", "هنر", "🧦", Palette.rose, rating(minAge = 3), durationMinutes = 90),
        content("weekend_garden", WEEKEND_IDEA, "باغچه‌ی کوچک بالکن", "در گلدان‌های کوچک سبزی یا لوبیا بکارید و هر روز رشدش را در دفترچه ثبت کنید.", "طبیعت", "🌱", Palette.mint, rating(minAge = 3), durationMinutes = 45),
        content("weekend_family_olympics", WEEKEND_IDEA, "المپیک خانوادگی", "مسابقه‌ی دوی قاشق و تخم‌مرغ، پرتاب توپ در سبد و طناب‌کشی. به همه مدال کاغذی بدهید!", "ورزش", "🏅", Palette.sun, rating(minAge = 4), durationMinutes = 60),
    )

    fun forumSeed(now: Instant = Instant.now()): List<Pair<ForumTopicEntity, List<ForumPostEntity>>> = listOf(
        ForumTopicEntity(
            remoteId = null, title = "فرزندم بعد از خاموش کردن تبلت گریه می‌کند",
            body = "پسرم ۵ ساله است و هر بار که زمان تبلتش تمام می‌شود قشقرق راه می‌اندازد. چه کنم؟",
            authorName = "مادر آرین", authorRole = AuthorRole.PARENT, category = "زمان صفحه‌نمایش", createdAt = now.minus(3, ChronoUnit.DAYS),
        ) to listOf(
            ForumPostEntity(topicId = 0, remoteId = null, body = "ما هم همین مشکل را داشتیم. هشدار ۵ دقیقه‌ای خیلی کمک کرد.", authorName = "پدر هستی", authorRole = AuthorRole.PARENT, createdAt = now.minus(2, ChronoUnit.DAYS), reported = false),
            ForumPostEntity(topicId = 0, remoteId = null, body = "این رفتار در این سن طبیعی است. پیش از پایان زمان هشدار بدهید، احساس او را نام ببرید و یک فعالیت جایگزین جذاب پیشنهاد کنید. مهم‌تر از همه، با گریه زمان اضافه ندهید تا الگو تثبیت نشود.", authorName = "دکتر سارا محمدی", authorRole = AuthorRole.PSYCHOLOGIST, createdAt = now.minus(1, ChronoUnit.DAYS), reported = false),
        ),
        ForumTopicEntity(
            remoteId = null, title = "انیمیشن مناسب برای ۳ ساله‌ها",
            body = "دخترم ۳ سال دارد. چه انیمیشن‌هایی برای این سن مناسب است که ریتم تند نداشته باشد؟",
            authorName = "مادر باران", authorRole = AuthorRole.PARENT, category = "انتخاب محتوا", createdAt = now.minus(5, ChronoUnit.DAYS),
        ) to listOf(
            ForumPostEntity(topicId = 0, remoteId = null, body = "برای این سن آثاری با ریتم آرام، رنگ‌های ملایم و قسمت‌های کوتاه (کمتر از ۱۰ دقیقه) انتخاب کنید. در کدومو فیلتر سنی «نوپا» را فعال کنید و حتماً کنار کودک تماشا کنید.", authorName = "مهندس رضا کریمی", authorRole = AuthorRole.MEDIA_EXPERT, createdAt = now.minus(4, ChronoUnit.DAYS), reported = false),
        ),
        ForumTopicEntity(
            remoteId = null, title = "اختلال خواب و استفاده از گوشی قبل از خواب",
            body = "پسرم ۹ ساله است و شب‌ها دیر خوابش می‌برد. آیا به گوشی ربط دارد؟",
            authorName = "پدر سامان", authorRole = AuthorRole.PARENT, category = "سلامت", createdAt = now.minus(7, ChronoUnit.DAYS),
        ) to listOf(
            ForumPostEntity(topicId = 0, remoteId = null, body = "نور آبی صفحه‌نمایش ترشح ملاتونین را کاهش می‌دهد. توصیه می‌شود حداقل یک ساعت پیش از خواب صفحه‌ها خاموش شوند. ساعت خواب خودکار کدومو را فعال کنید.", authorName = "دکتر علی رضایی", authorRole = AuthorRole.PEDIATRICIAN, createdAt = now.minus(6, ChronoUnit.DAYS), reported = false),
        ),
    )
}
