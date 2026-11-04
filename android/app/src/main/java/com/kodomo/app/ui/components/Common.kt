package com.kodomo.app.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.mascot.KodomoMascot
import com.kodomo.app.ui.theme.KodomoColors
import com.kodomo.app.ui.theme.LocalAgeUiSpec

fun gradientOf(start: Long, end: Long): Brush = Brush.linearGradient(listOf(Color(start), Color(end)))

/** Rounded gradient tile with a big emoji – the visual "cover" for content items. */
@Composable
fun EmojiCover(emoji: String, colorStart: Long, colorEnd: Long, modifier: Modifier = Modifier, emojiSize: Int = 40, corner: Dp = 20.dp) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(gradientOf(colorStart, colorEnd)),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = emojiSize.sp)
    }
}

/** Press-to-squish modifier that gives every kids-space tile a bouncy, toy-like feel. */
@Composable
fun Modifier.bouncyClick(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.4f, stiffness = 600f), label = "bounce")
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    return this
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null) {
            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            scope.launch { com.kodomo.app.ui.fx.Sfx.pop() }
            onClick()
        }
}

/** Home-screen module tile, sized by the active [com.kodomo.app.domain.engine.AgeUiSpec]. */
@Composable
fun ModuleTile(
    title: String,
    emoji: String,
    colors: Pair<Long, Long>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    badge: String? = null,
    index: Int = 0,
    onClick: () -> Unit,
) {
    val spec = LocalAgeUiSpec.current
    // pop-in entrance (staggered by position) + a gentle idle wiggle of the icon and a light sweep across the card
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 70L)
        appear.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f))
    }
    val idle = androidx.compose.animation.core.rememberInfiniteTransition(label = "tile")
    val wiggle by idle.animateFloat(
        -6f, 6f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(1_400 + index * 90), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "wiggle",
    )
    val shine by idle.animateFloat(
        -1f, 2f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(3_800, delayMillis = index * 250, easing = androidx.compose.animation.core.LinearEasing)),
        label = "shine",
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                val a = appear.value
                scaleX = 0.6f + 0.4f * a
                scaleY = 0.6f + 0.4f * a
                alpha = a.coerceIn(0f, 1f)
            }
            .height(spec.cardHeightDp.dp)
            .shadow(6.dp, RoundedCornerShape(spec.cornerRadiusDp.dp), ambientColor = Color(colors.second), spotColor = Color(colors.second))
            .clip(RoundedCornerShape(spec.cornerRadiusDp.dp))
            .background(gradientOf(colors.first, colors.second))
            .bouncyClick(onClick)
            .padding(12.dp),
    ) {
        androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
            val x = size.width * shine
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
                    start = androidx.compose.ui.geometry.Offset(x - size.width * 0.3f, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                ),
            )
        }
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = spec.iconSizeSp.sp, modifier = Modifier.graphicsLayer { rotationZ = wiggle })
            if (spec.showCardLabels) {
                Spacer(Modifier.height(6.dp))
                Text(
                    title, style = MaterialTheme.typography.titleMedium, color = Color.White, textAlign = TextAlign.Center,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.ExtraBold,
                )
            }
            if (spec.showDescriptions && subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center, maxLines = 2)
            }
        }
        if (badge != null) {
            Text(
                badge,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = KodomoColors.GreenDark,
            )
        }
    }
}

@Composable
fun KodomoPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun KodomoCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation) { Column(Modifier.padding(16.dp), content = content) }
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation) { Column(Modifier.padding(16.dp), content = content) }
    }
}

@Composable
fun <T> ChipSelector(
    options: List<T>,
    selected: Set<T>,
    label: (T) -> String,
    onToggle: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option in selected,
                onClick = { onToggle(option) },
                label = { Text(label(option)) },
                shape = RoundedCornerShape(14.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier, mood: MascotMood = MascotMood.THINKING) {
    Column(modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        KodomoMascot(mood, size = 110.dp)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Remaining screen-time chip with a mini progress bar, shown at the top of every kids screen. */
@Composable
fun TimeLeftChip(remainingSeconds: Long, progress: Float, modifier: Modifier = Modifier) {
    val minutes = (remainingSeconds + 59) / 60
    val warn = remainingSeconds <= 5 * 60
    Column(
        modifier
            .background(if (warn) Color(0xFFFFE9D6) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("⏰ ${minutes.fa()} دقیقه", style = MaterialTheme.typography.labelMedium, color = if (warn) Color(0xFFB4561C) else MaterialTheme.colorScheme.onSurfaceVariant)
        LinearProgressIndicator(
            progress = { 1f - progress },
            modifier = Modifier.width(72.dp).height(4.dp).clip(CircleShape),
            color = if (warn) KodomoColors.Peach else MaterialTheme.colorScheme.primary,
            trackColor = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
fun KidScreenScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "بازگشت") }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent),
            )
        },
        content = content,
    )
}

@Composable
fun StarBadge(stars: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .background(Color(0xFFFFF1C7), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⭐", fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text(stars.fa(), style = MaterialTheme.typography.labelLarge, color = Color(0xFF8A6100))
    }
}

@Composable
fun RatingPill(label: String, value: Int, modifier: Modifier = Modifier, positive: Boolean = false) {
    val color = when {
        positive -> KodomoColors.Green
        value <= 1 -> KodomoColors.Green
        value <= 3 -> KodomoColors.Sun
        else -> KodomoColors.Danger
    }
    Row(
        modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$label: ${value.fa()}/۵", style = MaterialTheme.typography.labelMedium, color = color)
    }
}
