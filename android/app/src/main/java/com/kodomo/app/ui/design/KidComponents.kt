package com.kodomo.app.ui.design

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.ui.fx.Emoji

/** Frosted glass surface of the child space (cards, widgets, dock). */
fun Modifier.frosted(corner: Dp = KodomoTokens.RadiusCard, dark: Boolean = false) = this
    .shadow(10.dp, RoundedCornerShape(corner), ambientColor = Color(0x33000000), spotColor = Color(0x22000000))
    .clip(RoundedCornerShape(corner))
    .background(
        if (dark) Brush.verticalGradient(listOf(Color(0x664A4B85), Color(0x55252650)))
        else Brush.verticalGradient(listOf(Color(0xEEFFFFFF), Color(0xC8F4FAF7))),
    )
    .border(1.2.dp, if (dark) Color(0x55B7B9FF) else Color(0xCCFFFFFF), RoundedCornerShape(corner))

/** A glossy colour "clay" icon: rounded square in the section colour with a top highlight and the 3D picture. */
@Composable
fun Premium3DIcon(picture: String, color: Color, size: Dp, modifier: Modifier = Modifier, artKey: String? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    if (artKey != null && ArtAssets.has(context, "icons/$artKey")) { Art("icons/$artKey", size, modifier) {}; return }
    Box(
        modifier.size(size)
            .shadow(8.dp, RoundedCornerShape(size * 0.32f), ambientColor = color, spotColor = color)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(listOf(lighten(color, 0.35f), color, darken(color, 0.12f)), start = Offset.Zero, end = Offset(0f, Float.POSITIVE_INFINITY))),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawOval(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent)), Offset(this.size.width * 0.1f, this.size.height * 0.04f), androidx.compose.ui.geometry.Size(this.size.width * 0.8f, this.size.height * 0.45f))
        }
        Emoji(picture, size * 0.64f)
    }
}

/** A child home tile: frosted card, 3D icon and the section name inside (big touch target, pops in, squashes on tap). */
@Composable
fun ChildTile(title: String, picture: String, color: Color, index: Int, dark: Boolean, iconSize: Dp = 54.dp, artKey: String? = null, onClick: () -> Unit) {
    // A light pop-in that never leaves a tile invisible, even on a slow phone.
    val appear = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 30L); appear.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 420f)) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        Modifier.fillMaxWidth().aspectRatio(0.95f)
            .graphicsLayer { val a = appear.value; scaleX = 0.6f + 0.4f * a; scaleY = 0.6f + 0.4f * a; alpha = a.coerceIn(0f, 1f) }
            .scale(if (pressed) 0.92f else 1f)
            .frosted(24.dp, dark)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = title }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Premium3DIcon(picture, color, iconSize, artKey = artKey)
        Spacer(Modifier.height(8.dp))
        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (dark) Color.White else KodomoTokens.Ink))
    }
}

/** Glass dock at the bottom of the child home (quick sections: camera, gallery, call…). */
@Composable
fun ChildDock(items: List<Triple<String, String, Color>>, dark: Boolean, modifier: Modifier = Modifier, onClick: (Int) -> Unit) {
    Row(modifier.fillMaxWidth().frosted(30.dp, dark).padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
        items.forEachIndexed { i, (title, picture, color) ->
            Box(Modifier.size(KodomoTokens.TouchChild).clip(RoundedCornerShape(20.dp)).clickable(role = Role.Button) { onClick(i) }.semantics { contentDescription = title }, contentAlignment = Alignment.Center) {
                Premium3DIcon(picture, color, 56.dp)
            }
        }
    }
}

/** Minutes-left ring (like a phone's battery widget). */
@Composable
fun ProgressRing(progress: Float, centre: String, caption: String, dark: Boolean, modifier: Modifier = Modifier) {
    val ink = if (dark) Color.White else KodomoTokens.Ink
    Column(modifier.frosted(24.dp, dark).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(ink.copy(alpha = 0.15f), -90f, 360f, false, style = Stroke(9f, cap = StrokeCap.Round))
                drawArc(Brush.sweepGradient(listOf(Color(0xFF6DD5A0), KodomoTokens.Primary, Color(0xFF6DD5A0))), -90f, 360f * progress.coerceIn(0f, 1f), false, style = Stroke(9f, cap = StrokeCap.Round))
            }
            Text(centre, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, color = ink))
        }
        Text(caption, style = TextStyle(fontSize = 12.sp, color = ink.copy(alpha = 0.8f)))
    }
}

/** Speech bubble of the companion (robot / chosen buddy). */
@Composable
fun MascotBubble(text: String, dark: Boolean, modifier: Modifier = Modifier, mascot: @Composable () -> Unit) {
    Row(modifier.frosted(24.dp, dark).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        mascot()
        Spacer(Modifier.size(6.dp))
        Text(text, maxLines = 3, overflow = TextOverflow.Ellipsis, style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (dark) Color.White else KodomoTokens.Ink))
    }
}

@Composable
fun RewardBadge(stars: Int, medals: Int, dark: Boolean, modifier: Modifier = Modifier) {
    val ink = if (dark) Color.White else KodomoTokens.Ink
    Column(modifier.frosted(24.dp, dark).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Emoji("⭐", 26.dp); Text(" ${com.kodomo.app.core.util.PersianDigits.toPersian(stars)}", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black, color = ink)) }
        Row(verticalAlignment = Alignment.CenterVertically) { Emoji("🏅", 22.dp); Text(" ${com.kodomo.app.core.util.PersianDigits.toPersian(medals)}", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ink)) }
    }
}

/** Round character avatar in a white ring (header of the child home). */
@Composable
fun CharacterAvatar(kind: CharacterKind, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).shadow(8.dp, CircleShape).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF7FB), Color(0xFFE9F6FF)))).border(3.dp, Color.White, CircleShape),
        contentAlignment = Alignment.BottomCenter,
    ) { Art("characters/" + kind.name.lowercase(), size * 0.96f) { KidCharacter(kind, size * 0.96f) } }
}

val HeaderShadow = Shadow(Color(0x55000000), Offset(0f, 2f), 8f)

internal fun lighten(c: Color, f: Float) = Color(c.red + (1 - c.red) * f, c.green + (1 - c.green) * f, c.blue + (1 - c.blue) * f, c.alpha)
internal fun darken(c: Color, f: Float) = Color(c.red * (1 - f), c.green * (1 - f), c.blue * (1 - f), c.alpha)
