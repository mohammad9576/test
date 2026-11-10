package com.kodomo.app.ui.kids.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.core.util.PersianCalendar
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.fx.Emoji
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.sin

private val textShadow = Shadow(Color.Black.copy(alpha = 0.35f), Offset(0f, 2f), 8f)

/** Wallpaper colours by time of day (morning, day, evening, night) — the launcher feels alive like a phone's. */
private fun wallpaper(hour: Int, night: Boolean): List<Color> = when {
    night || hour >= 21 || hour < 5 -> listOf(Color(0xFF0B1026), Color(0xFF1B2B5A), Color(0xFF3A2D6B))
    hour < 9 -> listOf(Color(0xFFFF7E67), Color(0xFFD9577F), Color(0xFF5B6CDB))
    hour < 17 -> listOf(Color(0xFF2F80ED), Color(0xFF1FA2C9), Color(0xFF2BB59A))
    else -> listOf(Color(0xFF7F53AC), Color(0xFFE96443), Color(0xFFFFB25B))
}

/** Full-screen animated wallpaper: gradient + soft floating light blobs (+ twinkling stars at night). */
@Composable
fun LauncherWallpaper(night: Boolean) {
    val hour = remember { LocalTime.now().hour }
    val colors = wallpaper(hour, night)
    val t by rememberInfiniteTransition(label = "wp").animateFloat(0f, 1f, infiniteRepeatable(tween(14_000), RepeatMode.Reverse), label = "t")
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.linearGradient(colors, start = Offset(0f, 0f), end = Offset(size.width, size.height)))
        val blobs = listOf(Triple(0.15f, 0.2f, 0.55f), Triple(0.85f, 0.35f, 0.45f), Triple(0.4f, 0.85f, 0.6f))
        blobs.forEachIndexed { i, (x, y, r) ->
            val dx = sin((t + i) * 3.14f) * size.width * 0.05f
            val c = Offset(size.width * x + dx, size.height * y - dx)
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), center = c, radius = size.minDimension * r), radius = size.minDimension * r, center = c)
        }
        if (night || hour >= 21 || hour < 5) {
            val rnd = java.util.Random(7)
            repeat(40) {
                val p = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height * 0.6f)
                val a = 0.3f + 0.6f * ((sin((t * 6 + it) * 2f) + 1f) / 2f)
                drawCircle(Color.White.copy(alpha = a), radius = 1.5f + rnd.nextFloat() * 2f, center = p)
            }
        }
    }
}

/** Frosted-glass card used by the widgets and the dock. */
fun Modifier.glass(corner: Dp = 28.dp) = this
    .clip(RoundedCornerShape(corner))
    .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.26f), Color.Black.copy(alpha = 0.08f))))
    .border(1.dp, Color.White.copy(alpha = 0.38f), RoundedCornerShape(corner))

@Composable
fun ClockHeader(name: String) {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(15_000); now = LocalTime.now() } }
    val today = LocalDate.now()
    val j = PersianCalendar.toJalali(today)
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "%02d:%02d".format(now.hour, now.minute).fa(),
            style = TextStyle(fontSize = 72.sp, fontWeight = FontWeight.Light, color = Color.White, shadow = textShadow, letterSpacing = 2.sp),
        )
        Text(
            "${PersianCalendar.dayName(today.dayOfWeek)}، ${j.day.fa()} ${PersianCalendar.monthName(j.month)}",
            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.White, shadow = textShadow),
        )
        Text("سلام $name 👋", style = TextStyle(fontSize = 15.sp, color = Color.White.copy(alpha = 0.9f), shadow = textShadow))
    }
}

/** Time-left ring, like a phone's battery widget. */
@Composable
fun TimeWidget(remainingSeconds: Long, progress: Float, modifier: Modifier = Modifier) {
    val minutes = (remainingSeconds / 60).toInt()
    Column(modifier.glass().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawArc(Color.White.copy(alpha = 0.25f), -90f, 360f, false, style = Stroke(9f, cap = StrokeCap.Round))
                drawArc(if (progress > 0.85f) Color(0xFFFF8A80) else Color.White, -90f, 360f * (1f - progress), false, style = Stroke(9f, cap = StrokeCap.Round))
            }
            Text(minutes.fa(), style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White))
        }
        Text("دقیقه مانده", style = TextStyle(fontSize = 12.sp, color = Color.White))
    }
}

@Composable
fun StarsWidget(stars: Int, medals: Int, modifier: Modifier = Modifier) {
    Column(modifier.glass().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Emoji("⭐", 30.dp); Text(" ${stars.fa()}", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)) }
        Row(verticalAlignment = Alignment.CenterVertically) { Emoji("🏅", 26.dp); Text(" ${medals.fa()}", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)) }
    }
}

/** An app icon: squircle with a soft gradient, glossy top, 3D picture; the label sits underneath like on a phone. */
@Composable
fun LauncherIcon(module: HomeModule, iconSize: Dp, index: Int, showLabel: Boolean = true, onClick: () -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 40L); appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 380f)) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        Modifier.graphicsLayer { val a = appear.value; scaleX = 0.7f + 0.3f * a; scaleY = 0.7f + 0.3f * a; alpha = a.coerceIn(0f, 1f) }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(iconSize).scale(if (pressed) 0.9f else 1f)
                .shadow(10.dp, RoundedCornerShape(iconSize * 0.3f), ambientColor = Color(module.colors.second), spotColor = Color(module.colors.second))
                .clip(RoundedCornerShape(iconSize * 0.3f))
                .background(Brush.linearGradient(listOf(Color(module.colors.first), Color(module.colors.second)))),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.35f), Color.Transparent), endY = size.height * 0.5f))
            }
            Emoji(module.emoji, iconSize * 0.62f)
        }
        if (showLabel) {
            Spacer(Modifier.height(6.dp))
            Text(
                module.title, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White, shadow = textShadow),
                modifier = Modifier.width(iconSize + 20.dp),
            )
        }
    }
}

/**
 * The kids launcher in the approved Kodomo style (docs/design/handoff_v4): a living 3D scene (sky, sun or moon,
 * clouds, hills, flowers; castle or galaxy by style), big clock with Persian date and greeting, the child's own
 * character, glass widgets, frosted tiles with glossy 3D icons and a glass dock.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LauncherHome(
    name: String,
    modules: List<HomeModule>,
    columns: Int,
    remainingSeconds: Long?,
    progress: Float,
    stars: Int,
    medals: Int,
    night: Boolean,
    style: com.kodomo.app.ui.design.KidStyle = com.kodomo.app.ui.design.KidStyle.MEADOW,
    character: com.kodomo.app.ui.design.CharacterKind = com.kodomo.app.ui.design.CharacterKind.BOY,
    buddy: @Composable (Modifier) -> Unit,
    extras: @Composable () -> Unit,
    onParentLongPress: () -> Unit,
    onOpen: (HomeModule) -> Unit,
) {
    val dark = night || style == com.kodomo.app.ui.design.KidStyle.GALAXY
    val ink = if (dark) Color.White else com.kodomo.app.ui.design.KodomoTokens.Ink
    val dockKeys = listOf("camera", "call", "rewards")
    val dock = modules.filter { it.key in dockKeys }.sortedBy { dockKeys.indexOf(it.key) }
    val grid = modules.filter { it !in dock }
    Box(Modifier.fillMaxSize()) {
        com.kodomo.app.ui.design.KidScene(style, night)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                var now by remember { mutableStateOf(LocalTime.now()) }
                LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(15_000); now = LocalTime.now() } }
                val today = LocalDate.now()
                val j = PersianCalendar.toJalali(today)
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("%d:%02d".format(now.hour, now.minute).fa(), style = TextStyle(fontSize = 58.sp, fontWeight = FontWeight.Black, color = ink, shadow = if (dark) textShadow else null))
                        Text("${PersianCalendar.dayName(today.dayOfWeek)} ${j.day.fa()} ${PersianCalendar.monthName(j.month)}", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = ink))
                        Text("سلام $name!", style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = ink))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        com.kodomo.app.ui.design.CharacterAvatar(character, 78.dp)
                        Spacer(Modifier.height(6.dp))
                        // Parents: long-press (a tap does nothing, so children cannot open it).
                        Box(
                            Modifier.size(40.dp).glass(20.dp).combinedClickable(onClick = {}, onLongClick = onParentLongPress),
                            contentAlignment = Alignment.Center,
                        ) { Text("🔒", fontSize = 16.sp) }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (remainingSeconds != null) com.kodomo.app.ui.design.ProgressRing(1f - progress, (remainingSeconds / 60).toInt().fa(), "دقیقه مانده", dark, Modifier.weight(1f))
                    buddy(Modifier.weight(1.7f))
                    com.kodomo.app.ui.design.RewardBadge(stars, medals, dark, Modifier.weight(1f))
                }
            }
            items(grid.size, key = { grid[it].key }) { i ->
                val m = grid[i]
                com.kodomo.app.ui.design.ChildTile(m.title, m.emoji, Color(m.colors.second), i, dark, iconSize = if (columns <= 3) 58.dp else 48.dp, artKey = m.key) { onOpen(m) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { extras() }
        }
        if (dock.isNotEmpty()) {
            com.kodomo.app.ui.design.ChildDock(
                dock.map { Triple(it.title, it.emoji, Color(it.colors.second)) }, dark,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp),
            ) { i -> onOpen(dock[i]) }
        }
    }
}
