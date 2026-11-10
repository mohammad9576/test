package com.kodomo.app.ui.design

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Design tokens of the approved Kodomo style (handoff V4, docs/design/handoff_v4/04_design_system/tokens.json).
 * Child spaces are colourful 3D scenes; the parent area stays white and calm.
 */
object KodomoTokens {
    // light / dark / night (20:00–06:00)
    val Primary = Color(0xFF217A61); val PrimaryDark = Color(0xFF76D6B4); val PrimaryNight = Color(0xFF9CBEA5)
    val Background = Color(0xFFF4FAF7); val BackgroundDark = Color(0xFF101D22); val BackgroundNight = Color(0xFF211F2A)
    val Surface = Color(0xFFFFFFFF); val SurfaceDark = Color(0xFF1B2A30); val SurfaceNight = Color(0xFF302C38)
    val Ink = Color(0xFF173B35); val InkDark = Color(0xFFF0F7F4); val InkNight = Color(0xFFF0E6D8)
    val Sun = Color(0xFFFFBC71); val SunDark = Color(0xFFF6B776); val SunNight = Color(0xFFD5A77F)

    // identity colour of each child section (icon, header and buttons repeat it)
    val Video = Color(0xFF478ED5)
    val Games = Color(0xFFE877AC)
    val Books = Color(0xFFE99B4C)
    val Learning = Color(0xFFDCB13F)
    val English = Color(0xFF8E6BE8)
    val Music = Color(0xFF9B7FE6)
    val Art = Color(0xFFF2994A)
    val Nature = Color(0xFF3FA977)

    val SpaceXs = 4.dp; val SpaceS = 8.dp; val SpaceM = 12.dp; val SpaceL = 16.dp; val SpaceXl = 24.dp; val Space2xl = 32.dp; val Space3xl = 48.dp
    val RadiusSmall = 12.dp; val RadiusCard = 26.dp; val RadiusSheet = 32.dp; val RadiusIcon = 22.dp
    val TouchParent = 48.dp; val TouchChild = 64.dp; val TouchToddler = 80.dp
    val ChildTitle = 24.sp; val Body = 16.sp; val Label = 14.sp
}

/** The four age bands of the brief (by birth date; parents can override the look). */
enum class AgeBand(val label: String, val columns: Int) {
    UNDER_5("کمتر از ۵ سال", 3), FIVE_TO_7("۵ تا ۷ سال", 3), SEVEN_TO_10("۷ تا ۱۰ سال", 3), TEEN("۱۰ تا ۱۵ سال", 4);

    companion object {
        fun of(age: Int) = when { age < 5 -> UNDER_5; age < 7 -> FIVE_TO_7; age < 10 -> SEVEN_TO_10; else -> TEEN }
    }
}

/** Visual style of a child's space. The style never limits content — it only changes colours, scene and character. */
enum class KidStyle(val label: String) {
    MEADOW("چمنزار سبز"),      // default, boys 7–10 in the boards
    BLOSSOM("قصر صورتی"),      // default, girls 5–7 in the boards
    SKY("آسمان آبی"),
    GALAXY("کهکشان (نوجوان)"); // teens 10–15

    companion object {
        fun default(age: Int, girl: Boolean) = when {
            age >= 10 -> GALAXY
            girl -> BLOSSOM
            else -> MEADOW
        }
    }
}
