package com.kodomo.app.ui.kids.games.art

import android.graphics.Path
import com.kodomo.app.ui.kids.games.art.ColoringCategory.ANIMALS
import com.kodomo.app.ui.kids.games.art.ColoringCategory.FOOD
import com.kodomo.app.ui.kids.games.art.ColoringCategory.HOME
import com.kodomo.app.ui.kids.games.art.ColoringCategory.MANDALA
import com.kodomo.app.ui.kids.games.art.ColoringCategory.NATURE
import com.kodomo.app.ui.kids.games.art.ColoringCategory.PATTERN
import com.kodomo.app.ui.kids.games.art.ColoringCategory.SPACE
import com.kodomo.app.ui.kids.games.art.ColoringCategory.VEHICLES
import com.kodomo.app.ui.kids.games.art.Shapes.circle
import com.kodomo.app.ui.kids.games.art.Shapes.cloud
import com.kodomo.app.ui.kids.games.art.Shapes.heart
import com.kodomo.app.ui.kids.games.art.Shapes.leaf
import com.kodomo.app.ui.kids.games.art.Shapes.oval
import com.kodomo.app.ui.kids.games.art.Shapes.ovalC
import com.kodomo.app.ui.kids.games.art.Shapes.petals
import com.kodomo.app.ui.kids.games.art.Shapes.poly
import com.kodomo.app.ui.kids.games.art.Shapes.rect
import com.kodomo.app.ui.kids.games.art.Shapes.ring
import com.kodomo.app.ui.kids.games.art.Shapes.round
import com.kodomo.app.ui.kids.games.art.Shapes.sector
import com.kodomo.app.ui.kids.games.art.Shapes.star

/** 36 colouring pages in eight categories (plus the three classic ones), drawn from simple vector shapes. */
internal object ColoringBookPages {
    private fun page(title: String, cat: ColoringCategory, minAge: Int, vararg parts: Any): ColoringPage {
        val list = mutableListOf<Path>()
        parts.forEach { p -> when (p) { is Path -> list += p; is List<*> -> p.filterIsInstance<Path>().forEach { list += it } } }
        return ColoringPage(title, list, cat, minAge)
    }
    private fun sky() = rect(0f, 0f, 1000f, 700f)
    private fun grass() = rect(0f, 700f, 1000f, 1000f)
    private fun sun(x: Float = 840f, y: Float = 150f) = listOf(star(x, y, 120f, 85f, 12), circle(x, y, 70f))

    /** Concentric rings split into sectors and petals: endless fine regions for patient colourists. */
    private fun mandala(title: String, petalsN: Int, rings: Int, minAge: Int): ColoringPage {
        val parts = mutableListOf<Path>(rect(0f, 0f, 1000f, 1000f))
        parts += petals(500f, 500f, 330f, 120f, 55f, petalsN * 2, 360f / (petalsN * 4))
        parts += petals(500f, 500f, 250f, 110f, 60f, petalsN)
        var r = 230f
        val step = 160f / rings
        for (k in 0 until rings) {
            val n = petalsN * (if (k % 2 == 0) 2 else 1)
            for (i in 0 until n) parts += sector(500f, 500f, r - step, r, i * 360f / n + k * 7f, (i + 1) * 360f / n + k * 7f)
            r -= step
        }
        parts += petals(500f, 500f, 50f, 45f, 22f, petalsN)
        parts += circle(500f, 500f, 30f)
        return ColoringPage(title, parts, MANDALA, minAge)
    }

    val all: List<ColoringPage> = listOf(
        // ---------- animals
        page("گربه‌ی ملوس", ANIMALS, 3, rect(0f, 0f, 1000f, 1000f), ovalC(500f, 700f, 230f, 220f), circle(500f, 380f, 190f),
            poly(340f, 290f, 360f, 120f, 450f, 230f), poly(660f, 290f, 640f, 120f, 550f, 230f), ovalC(430f, 360f, 30f, 42f), ovalC(570f, 360f, 30f, 42f),
            poly(475f, 430f, 525f, 430f, 500f, 460f), ovalC(410f, 880f, 70f, 45f), ovalC(590f, 880f, 70f, 45f), Shapes.leaf(720f, 760f, 220f, 40f, -60f)),
        page("سگ مهربان", ANIMALS, 3, rect(0f, 0f, 1000f, 1000f), ovalC(500f, 720f, 250f, 200f), circle(500f, 400f, 180f),
            ovalC(320f, 420f, 70f, 150f), ovalC(680f, 420f, 70f, 150f), ovalC(500f, 470f, 90f, 65f), ovalC(500f, 440f, 35f, 25f),
            circle(430f, 360f, 28f), circle(570f, 360f, 28f), ovalC(560f, 540f, 30f, 45f), rect(380f, 880f, 450f, 960f), rect(550f, 880f, 620f, 960f)),
        page("فیل بزرگ", ANIMALS, 4, sky(), grass(), ovalC(520f, 560f, 280f, 200f), circle(330f, 430f, 150f), ovalC(250f, 450f, 110f, 150f),
            Shapes.round(210f, 470f, 270f, 800f, 30f), rect(380f, 700f, 460f, 880f), rect(560f, 700f, 640f, 880f), rect(680f, 680f, 760f, 860f), circle(330f, 400f, 18f), sun()),
        page("خرگوش", ANIMALS, 3, sky(), grass(), ovalC(500f, 680f, 190f, 200f), circle(500f, 400f, 140f), ovalC(440f, 170f, 45f, 140f), ovalC(560f, 170f, 45f, 140f),
            ovalC(440f, 180f, 22f, 100f), ovalC(560f, 180f, 22f, 100f), circle(455f, 380f, 18f), circle(545f, 380f, 18f), circle(500f, 430f, 16f), circle(690f, 760f, 50f),
            Shapes.round(780f, 760f, 800f, 900f, 10f), leaf(790f, 760f, 120f, 30f, -120f)),
        page("جغد دانا", ANIMALS, 5, rect(0f, 0f, 1000f, 1000f), round(100f, 780f, 900f, 830f, 20f), ovalC(500f, 520f, 230f, 280f), ovalC(500f, 600f, 150f, 170f),
            circle(410f, 380f, 85f), circle(590f, 380f, 85f), circle(410f, 380f, 40f), circle(590f, 380f, 40f), poly(470f, 450f, 530f, 450f, 500f, 510f),
            poly(290f, 300f, 350f, 200f, 400f, 290f), poly(710f, 300f, 650f, 200f, 600f, 290f), ovalC(280f, 560f, 60f, 150f), ovalC(720f, 560f, 60f, 150f)),
        page("لاک‌پشت", ANIMALS, 3, sky(), grass(), ovalC(500f, 600f, 300f, 190f), ovalC(500f, 600f, 120f, 80f),
            poly(380f, 540f, 300f, 600f, 380f, 660f), poly(620f, 540f, 700f, 600f, 620f, 660f), circle(860f, 580f, 85f), circle(880f, 560f, 15f),
            ovalC(330f, 790f, 60f, 45f), ovalC(670f, 790f, 60f, 45f), poly(200f, 620f, 140f, 660f, 210f, 680f), sun(150f, 150f)),
        page("ماهی و حباب", ANIMALS, 3, rect(0f, 0f, 1000f, 1000f), ovalC(450f, 500f, 260f, 170f), poly(690f, 500f, 900f, 330f, 900f, 670f),
            poly(400f, 340f, 470f, 220f, 540f, 340f), poly(400f, 660f, 470f, 760f, 540f, 660f), circle(330f, 460f, 35f),
            ring(450f, 500f, 60f, 110f), circle(150f, 280f, 40f), circle(200f, 170f, 28f), circle(130f, 90f, 18f), rect(0f, 900f, 1000f, 1000f)),
        page("زرافه", ANIMALS, 5, sky(), grass(), ovalC(480f, 640f, 220f, 120f), Shapes.round(560f, 200f, 640f, 640f, 30f), ovalC(640f, 200f, 110f, 70f),
            rect(320f, 700f, 370f, 930f), rect(420f, 700f, 470f, 930f), rect(520f, 700f, 570f, 930f), rect(600f, 700f, 650f, 930f),
            circle(420f, 620f, 30f), circle(520f, 660f, 25f), circle(600f, 330f, 22f), circle(600f, 460f, 26f), circle(680f, 190f, 12f), sun(150f, 150f)),
        page("پنگوئن", ANIMALS, 3, rect(0f, 0f, 1000f, 1000f), rect(0f, 820f, 1000f, 1000f), ovalC(500f, 520f, 210f, 300f), ovalC(500f, 580f, 140f, 220f),
            circle(440f, 350f, 22f), circle(560f, 350f, 22f), poly(470f, 410f, 530f, 410f, 500f, 450f), ovalC(280f, 560f, 50f, 150f), ovalC(720f, 560f, 50f, 150f),
            ovalC(430f, 830f, 70f, 30f), ovalC(570f, 830f, 70f, 30f)),
        page("شیر جنگل", ANIMALS, 4, rect(0f, 0f, 1000f, 1000f), petals(500f, 450f, 200f, 90f, 55f, 14), circle(500f, 450f, 200f), circle(430f, 410f, 22f), circle(570f, 410f, 22f),
            ovalC(500f, 510f, 70f, 50f), poly(470f, 480f, 530f, 480f, 500f, 510f), circle(350f, 290f, 45f), circle(650f, 290f, 45f)),
        // ---------- nature
        page("درخت سیب", NATURE, 3, sky(), grass(), rect(440f, 480f, 560f, 820f), Shapes.cloud(500f, 380f, 300f), circle(380f, 360f, 32f), circle(560f, 290f, 32f),
            circle(620f, 430f, 32f), circle(450f, 470f, 32f), circle(300f, 900f, 30f), sun()),
        page("گل‌های باغچه", NATURE, 3, sky(), grass(), rect(190f, 500f, 210f, 800f), petals(200f, 450f, 70f, 55f, 32f, 6), circle(200f, 450f, 40f),
            rect(490f, 420f, 510f, 800f), petals(500f, 370f, 80f, 60f, 35f, 8), circle(500f, 370f, 45f), rect(790f, 520f, 810f, 800f),
            petals(800f, 470f, 70f, 55f, 32f, 5), circle(800f, 470f, 40f), leaf(500f, 650f, 130f, 35f, -30f), leaf(500f, 650f, 130f, 35f, -150f)),
        page("کوه و رودخانه", NATURE, 5, sky(), poly(0f, 700f, 250f, 300f, 500f, 700f), poly(300f, 700f, 600f, 220f, 900f, 700f), poly(520f, 340f, 600f, 220f, 680f, 340f),
            poly(170f, 430f, 250f, 300f, 330f, 430f), grass(), poly(420f, 1000f, 520f, 700f, 620f, 700f, 700f, 1000f), Shapes.cloud(250f, 150f, 120f), sun()),
        page("رنگین‌کمان", NATURE, 3, rect(0f, 0f, 1000f, 1000f), Shapes.sector(500f, 800f, 380f, 440f, 180f, 360f), Shapes.sector(500f, 800f, 320f, 380f, 180f, 360f),
            Shapes.sector(500f, 800f, 260f, 320f, 180f, 360f), Shapes.sector(500f, 800f, 200f, 260f, 180f, 360f), Shapes.sector(500f, 800f, 140f, 200f, 180f, 360f),
            cloud(170f, 790f, 150f), cloud(830f, 790f, 150f), rect(0f, 880f, 1000f, 1000f)),
        page("قارچ‌های جنگل", NATURE, 3, sky(), grass(), rect(300f, 560f, 400f, 800f), poly(180f, 570f, 350f, 380f, 520f, 570f), circle(300f, 500f, 22f), circle(400f, 470f, 18f),
            rect(620f, 620f, 700f, 800f), poly(520f, 630f, 660f, 470f, 800f, 630f), circle(650f, 560f, 18f), circle(720f, 590f, 16f), leaf(100f, 800f, 150f, 30f, -70f), sun()),
        page("ساحل و صدف", NATURE, 4, sky(), rect(0f, 520f, 1000f, 720f), rect(0f, 720f, 1000f, 1000f), star(250f, 860f, 80f, 35f), Shapes.heart(700f, 860f, 70f),
            poly(500f, 380f, 650f, 520f, 350f, 520f), Shapes.round(495f, 380f, 505f, 520f, 4f), cloud(200f, 180f, 120f), sun()),
        // ---------- vehicles
        page("ماشین قرمز", VEHICLES, 3, sky(), rect(0f, 760f, 1000f, 1000f), round(140f, 480f, 860f, 720f, 60f), poly(280f, 480f, 360f, 330f, 640f, 330f, 720f, 480f),
            rect(380f, 360f, 490f, 470f), rect(510f, 360f, 620f, 470f), circle(300f, 740f, 85f), circle(700f, 740f, 85f), circle(300f, 740f, 35f), circle(700f, 740f, 35f),
            rect(820f, 560f, 860f, 600f), sun()),
        page("قطار شاد", VEHICLES, 3, sky(), rect(0f, 800f, 1000f, 1000f), rect(80f, 450f, 360f, 740f), rect(120f, 330f, 220f, 450f), rect(390f, 520f, 640f, 740f),
            rect(670f, 520f, 920f, 740f), circle(150f, 780f, 50f), circle(290f, 780f, 50f), circle(460f, 780f, 45f), circle(570f, 780f, 45f), circle(740f, 780f, 45f),
            circle(850f, 780f, 45f), rect(230f, 490f, 330f, 580f), cloud(170f, 220f, 80f)),
        page("هواپیما", VEHICLES, 4, rect(0f, 0f, 1000f, 1000f), ovalC(500f, 500f, 380f, 90f), poly(450f, 470f, 300f, 200f, 380f, 200f, 560f, 470f),
            poly(450f, 530f, 300f, 800f, 380f, 800f, 560f, 530f), poly(130f, 480f, 80f, 330f, 160f, 330f, 220f, 470f), circle(700f, 480f, 25f), circle(620f, 480f, 25f),
            circle(540f, 480f, 25f), cloud(200f, 850f, 110f), cloud(820f, 180f, 110f)),
        page("کشتی بادبانی", VEHICLES, 4, sky(), rect(0f, 700f, 1000f, 1000f), poly(200f, 620f, 800f, 620f, 700f, 760f, 300f, 760f), rect(490f, 200f, 510f, 620f),
            poly(510f, 220f, 760f, 580f, 510f, 580f), poly(490f, 260f, 290f, 580f, 490f, 580f), poly(510f, 200f, 600f, 230f, 510f, 260f), circle(380f, 690f, 22f), circle(620f, 690f, 22f), sun(150f, 150f)),
        page("موشک", VEHICLES, 4, rect(0f, 0f, 1000f, 1000f), round(400f, 300f, 600f, 750f, 60f), poly(400f, 330f, 500f, 120f, 600f, 330f), circle(500f, 430f, 60f),
            poly(400f, 600f, 290f, 780f, 400f, 750f), poly(600f, 600f, 710f, 780f, 600f, 750f), poly(430f, 750f, 500f, 920f, 570f, 750f), star(180f, 200f, 50f, 20f), star(830f, 330f, 40f, 16f)),
        page("دوچرخه", VEHICLES, 5, rect(0f, 0f, 1000f, 1000f), ring(260f, 650f, 120f, 150f), ring(740f, 650f, 120f, 150f), poly(260f, 650f, 450f, 650f, 600f, 420f, 380f, 420f),
            poly(600f, 420f, 740f, 650f, 720f, 660f, 585f, 440f), round(340f, 380f, 440f, 410f, 12f), round(560f, 360f, 660f, 390f, 12f), circle(450f, 650f, 30f)),
        // ---------- space
        page("فضانورد", SPACE, 4, rect(0f, 0f, 1000f, 1000f), round(330f, 450f, 670f, 820f, 60f), circle(500f, 340f, 160f), ovalC(500f, 350f, 110f, 80f),
            round(220f, 480f, 330f, 720f, 40f), round(670f, 480f, 780f, 720f, 40f), rect(370f, 820f, 470f, 930f), rect(530f, 820f, 630f, 930f), round(430f, 560f, 570f, 650f, 15f),
            star(150f, 150f, 50f, 20f), star(860f, 220f, 40f, 16f), circle(850f, 820f, 60f)),
        page("سیاره‌ی حلقه‌دار", SPACE, 4, rect(0f, 0f, 1000f, 1000f), circle(500f, 500f, 220f), Shapes.ring(500f, 500f, 260f, 330f), star(150f, 180f, 60f, 24f),
            star(820f, 150f, 45f, 18f), star(180f, 820f, 40f, 16f), circle(820f, 820f, 70f), circle(440f, 440f, 40f), circle(580f, 560f, 30f)),
        page("ماه و ستاره‌ها", SPACE, 3, rect(0f, 0f, 1000f, 1000f), Shapes.union(circle(450f, 450f, 260f)).apply { op(circle(570f, 380f, 230f), Path.Op.DIFFERENCE) },
            star(720f, 250f, 70f, 28f), star(800f, 600f, 55f, 22f), star(600f, 820f, 45f, 18f), star(200f, 820f, 60f, 24f), star(200f, 150f, 40f, 16f)),
        page("ربات فضایی", SPACE, 3, rect(0f, 0f, 1000f, 1000f), round(320f, 220f, 680f, 480f, 50f), round(280f, 500f, 720f, 840f, 40f), circle(420f, 340f, 45f), circle(580f, 340f, 45f),
            round(430f, 410f, 570f, 440f, 12f), rect(490f, 120f, 510f, 220f), circle(500f, 110f, 30f), round(180f, 540f, 270f, 760f, 30f), round(730f, 540f, 820f, 760f, 30f),
            round(380f, 580f, 620f, 740f, 20f), circle(440f, 660f, 30f), circle(560f, 660f, 30f)),
        // ---------- food
        page("هندوانه", FOOD, 3, rect(0f, 0f, 1000f, 1000f), Shapes.sector(500f, 350f, 330f, 400f, 0f, 180f), Shapes.sector(500f, 350f, 0f, 330f, 0f, 180f),
            ovalC(400f, 480f, 16f, 26f), ovalC(500f, 540f, 16f, 26f), ovalC(600f, 480f, 16f, 26f), ovalC(450f, 620f, 16f, 26f), ovalC(560f, 620f, 16f, 26f)),
        page("کیک تولد", FOOD, 3, rect(0f, 0f, 1000f, 1000f), round(220f, 600f, 780f, 850f, 30f), round(290f, 420f, 710f, 600f, 30f), rect(360f, 290f, 390f, 420f),
            rect(485f, 270f, 515f, 420f), rect(610f, 290f, 640f, 420f), ovalC(375f, 260f, 18f, 30f), ovalC(500f, 240f, 18f, 30f), ovalC(625f, 260f, 18f, 30f),
            round(160f, 850f, 840f, 900f, 20f), Shapes.heart(500f, 720f, 60f)),
        page("بستنی", FOOD, 3, rect(0f, 0f, 1000f, 1000f), poly(380f, 520f, 620f, 520f, 500f, 920f), circle(430f, 460f, 110f), circle(570f, 460f, 110f), circle(500f, 340f, 110f),
            circle(500f, 230f, 30f), circle(420f, 470f, 15f), circle(560f, 330f, 15f)),
        page("سبد میوه", FOOD, 4, rect(0f, 0f, 1000f, 1000f), poly(200f, 560f, 800f, 560f, 720f, 860f, 280f, 860f), circle(360f, 470f, 100f), circle(560f, 450f, 110f),
            ovalC(700f, 500f, 80f, 60f), circle(460f, 380f, 70f), leaf(560f, 340f, 90f, 25f, -60f), Shapes.round(200f, 520f, 800f, 580f, 20f)),
        // ---------- home & city
        page("خانه‌ی دوطبقه", HOME, 4, sky(), grass(), rect(250f, 350f, 750f, 820f), poly(210f, 360f, 500f, 150f, 790f, 360f), rect(450f, 640f, 550f, 820f),
            rect(300f, 420f, 420f, 520f), rect(580f, 420f, 700f, 520f), rect(300f, 580f, 400f, 680f), rect(600f, 580f, 700f, 680f), rect(640f, 180f, 700f, 300f), cloud(180f, 180f, 100f)),
        page("مدرسه", HOME, 5, sky(), grass(), rect(150f, 400f, 850f, 820f), poly(120f, 410f, 500f, 230f, 880f, 410f), circle(500f, 330f, 50f), rect(440f, 640f, 560f, 820f),
            rect(200f, 470f, 300f, 560f), rect(350f, 470f, 450f, 560f), rect(550f, 470f, 650f, 560f), rect(700f, 470f, 800f, 560f), rect(495f, 120f, 505f, 230f),
            poly(505f, 120f, 600f, 145f, 505f, 170f), sun()),
        page("پارک بازی", HOME, 4, sky(), grass(), poly(150f, 760f, 300f, 400f, 330f, 400f, 210f, 760f), rect(280f, 380f, 520f, 420f), poly(480f, 420f, 700f, 760f, 750f, 760f, 520f, 420f),
            rect(780f, 400f, 800f, 760f), rect(900f, 400f, 920f, 760f), rect(760f, 380f, 940f, 410f), round(810f, 600f, 890f, 630f, 10f), sun(150f, 150f)),
        // ---------- mandalas (6–12 years)
        mandala("ماندالای گل", 8, 3, 6),
        mandala("ماندالای خورشید", 12, 4, 7),
        mandala("ماندالای ستاره", 6, 5, 8),
        mandala("ماندالای جادویی", 10, 6, 9),
        // ---------- patterns
        page("قلب‌ها", PATTERN, 3, rect(0f, 0f, 1000f, 1000f), (0 until 3).flatMap { r -> (0 until 3).map { c -> heart(200f + c * 300f, 200f + r * 300f, 110f) } }),
        page("ستاره‌باران", PATTERN, 4, rect(0f, 0f, 1000f, 1000f), (0 until 4).flatMap { r -> (0 until 4).map { c -> star(140f + c * 240f, 140f + r * 240f, 95f, 40f) } }),
        page("کاشی ایرانی", PATTERN, 7, rect(0f, 0f, 1000f, 1000f), (0 until 3).flatMap { r -> (0 until 3).flatMap { c ->
            val x = 170f + c * 330f; val y = 170f + r * 330f
            listOf(star(x, y, 150f, 95f, 8, -90f + 22.5f), star(x, y, 90f, 55f, 8), circle(x, y, 35f))
        } }),
    )
}
