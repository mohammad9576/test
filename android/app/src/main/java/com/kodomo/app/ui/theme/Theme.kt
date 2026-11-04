package com.kodomo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.R
import com.kodomo.app.domain.engine.AgeUiSpec
import com.kodomo.app.domain.model.AgeGroup

object KodomoColors {
    val Green = Color(0xFF2E7D5B)
    val GreenDark = Color(0xFF1F5C42)
    val GreenLight = Color(0xFF5FB98A)
    val Mint = Color(0xFFE8F5EE)
    val MintDeep = Color(0xFFCFEBDC)
    val Cream = Color(0xFFFAFAF5)
    val Ink = Color(0xFF1B2B26)
    val InkSoft = Color(0xFF5C6E67)
    val Glow = Color(0xFF5CE0A0)
    val Sun = Color(0xFFFFB648)
    val Rose = Color(0xFFF28AA6)
    val Sky = Color(0xFF5AA9E6)
    val Lavender = Color(0xFF9B7FE6)
    val Peach = Color(0xFFF59E6C)
    val Danger = Color(0xFFD9534F)
    val Star = Color(0xFFFFC83D)

    // Toddler pastel palette
    val PastelPink = Color(0xFFFFE3EC)
    val PastelYellow = Color(0xFFFFF4CC)
    val PastelBlue = Color(0xFFDDF0FF)
    val PastelMint = Color(0xFFDDF7E8)
}

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_extrabold, FontWeight.ExtraBold),
    Font(R.font.vazirmatn_black, FontWeight.Black),
)

private fun kodomoTypography(scale: Float): Typography {
    fun style(size: Int, weight: FontWeight, line: Int = (size * 1.45f).toInt()) =
        TextStyle(fontFamily = Vazirmatn, fontWeight = weight, fontSize = (size * scale).sp, lineHeight = (line * scale).sp)
    return Typography(
        displayLarge = style(48, FontWeight.Black),
        displayMedium = style(40, FontWeight.Black),
        displaySmall = style(32, FontWeight.ExtraBold),
        headlineLarge = style(28, FontWeight.ExtraBold),
        headlineMedium = style(24, FontWeight.ExtraBold),
        headlineSmall = style(21, FontWeight.Bold),
        titleLarge = style(19, FontWeight.Bold),
        titleMedium = style(16, FontWeight.Bold),
        titleSmall = style(14, FontWeight.Bold),
        bodyLarge = style(16, FontWeight.Normal, 26),
        bodyMedium = style(14, FontWeight.Normal, 23),
        bodySmall = style(12, FontWeight.Normal, 19),
        labelLarge = style(14, FontWeight.Bold),
        labelMedium = style(12, FontWeight.Medium),
        labelSmall = style(11, FontWeight.Medium),
    )
}

private val ParentLight = lightColorScheme(
    primary = KodomoColors.Green,
    onPrimary = Color.White,
    primaryContainer = KodomoColors.MintDeep,
    onPrimaryContainer = KodomoColors.GreenDark,
    secondary = KodomoColors.GreenLight,
    onSecondary = Color.White,
    secondaryContainer = KodomoColors.Mint,
    onSecondaryContainer = KodomoColors.GreenDark,
    tertiary = KodomoColors.Sun,
    background = KodomoColors.Cream,
    onBackground = KodomoColors.Ink,
    surface = Color.White,
    onSurface = KodomoColors.Ink,
    surfaceVariant = KodomoColors.Mint,
    onSurfaceVariant = KodomoColors.InkSoft,
    outline = Color(0xFFB7CEC3),
    outlineVariant = Color(0xFFDCE9E2),
    error = KodomoColors.Danger,
)

private val ParentDark = darkColorScheme(
    primary = KodomoColors.Glow,
    onPrimary = Color(0xFF00391F),
    primaryContainer = KodomoColors.GreenDark,
    onPrimaryContainer = KodomoColors.Mint,
    secondary = KodomoColors.GreenLight,
    secondaryContainer = Color(0xFF23402F),
    onSecondaryContainer = KodomoColors.Mint,
    tertiary = KodomoColors.Sun,
    background = Color(0xFF0F1A16),
    onBackground = Color(0xFFE3EFE8),
    surface = Color(0xFF17241F),
    onSurface = Color(0xFFE3EFE8),
    surfaceVariant = Color(0xFF22332C),
    onSurfaceVariant = Color(0xFFB4C8BE),
    outline = Color(0xFF41574C),
    outlineVariant = Color(0xFF2C3E36),
    error = Color(0xFFFF8A80),
)

private val ToddlerScheme = ParentLight.copy(
    primary = Color(0xFF3FA977),
    primaryContainer = KodomoColors.PastelMint,
    secondaryContainer = KodomoColors.PastelYellow,
    tertiary = KodomoColors.Rose,
    tertiaryContainer = KodomoColors.PastelPink,
    background = Color(0xFFFFFBF2),
    surfaceVariant = KodomoColors.PastelBlue,
)

private val KidScheme = ParentLight.copy(
    tertiary = KodomoColors.Star,
    tertiaryContainer = Color(0xFFFFF1C7),
    background = Color(0xFFF5FBF7),
)

val LocalAgeUiSpec = staticCompositionLocalOf { AgeUiSpec.forGroup(AgeGroup.PRIMARY_KID) }

private fun shapesFor(radius: Int) = Shapes(
    extraSmall = RoundedCornerShape((radius / 3).dp),
    small = RoundedCornerShape((radius / 2).dp),
    medium = RoundedCornerShape((radius * 0.75f).dp),
    large = RoundedCornerShape(radius.dp),
    extraLarge = RoundedCornerShape((radius * 1.3f).dp),
)

@Composable
private fun RtlMaterial(colorScheme: ColorScheme, typography: Typography, shapes: Shapes, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, shapes = shapes, content = content)
    }
}

/** Theme for onboarding, profile picker and the parent hub. */
@Composable
fun KodomoTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    RtlMaterial(if (darkTheme) ParentDark else ParentLight, kodomoTypography(1f), shapesFor(20), content)
}

/** Age-Adaptive theme for the kids space. */
@Composable
fun KidsTheme(spec: AgeUiSpec, darkMode: Boolean, content: @Composable () -> Unit) {
    val scheme = when (spec.group) {
        AgeGroup.TODDLER -> ToddlerScheme
        AgeGroup.PRIMARY_KID -> KidScheme
        AgeGroup.PRE_TEEN -> if (darkMode && spec.darkThemeAllowed) ParentDark else ParentLight
    }
    CompositionLocalProvider(LocalAgeUiSpec provides spec) {
        RtlMaterial(scheme, kodomoTypography(spec.titleScale), shapesFor(spec.cornerRadiusDp), content)
    }
}
