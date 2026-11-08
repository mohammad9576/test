package com.kodomo.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kodomo.app.domain.model.AvatarKind
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.mascot.KodomoMascot
import com.kodomo.app.ui.theme.KodomoColors

private data class AvatarStyle(val bg: Color, val skin: Color, val hair: Color)

private fun styleOf(kind: AvatarKind) = when (kind) {
    AvatarKind.BOY_CURLY -> AvatarStyle(Color(0xFFDDF0FF), Color(0xFFF6CFAE), Color(0xFF4A2C1D))
    AvatarKind.BOY_SHORT -> AvatarStyle(Color(0xFFDDF7E8), Color(0xFFEFC19B), Color(0xFF2F1E14))
    AvatarKind.GIRL_BOW -> AvatarStyle(Color(0xFFFFE3EC), Color(0xFFF7D2B6), Color(0xFF6B3A22))
    AvatarKind.GIRL_BRAIDS -> AvatarStyle(Color(0xFFFFF4CC), Color(0xFFEBBE98), Color(0xFF3B2416))
    AvatarKind.BABY -> AvatarStyle(Color(0xFFFFF1E0), Color(0xFFF8D9BF), Color(0xFFC79A6B))
    AvatarKind.FATHER -> AvatarStyle(Color(0xFFE3ECF7), Color(0xFFE7B892), Color(0xFF3A2A20))
    AvatarKind.MOTHER -> AvatarStyle(Color(0xFFDDF7E8), Color(0xFFF3CBAA), Color(0xFF3F7D5C))
    AvatarKind.ROBOT -> AvatarStyle(KodomoColors.Mint, Color.White, KodomoColors.Green)
    AvatarKind.CAT -> AvatarStyle(Color(0xFFFFEBD9), Color(0xFFF4A259), Color(0xFFD9843A))
    AvatarKind.BEAR -> AvatarStyle(Color(0xFFF1E6DA), Color(0xFFB07D56), Color(0xFF8A5A39))
}

/** Hand-drawn vector avatars for family members – no bitmap assets required. */
@Composable
fun KodomoAvatar(kind: AvatarKind, modifier: Modifier = Modifier, size: Dp = 64.dp, selected: Boolean = false, photoId: Long? = null) {
    if (photoId != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val v = AvatarPhotos.version.intValue
        val photo = androidx.compose.runtime.remember(photoId, v) { AvatarPhotos.load(context, photoId)?.asImageBitmap() }
        if (photo != null) {
            androidx.compose.foundation.Image(
                photo, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = modifier.size(size).clip(CircleShape).then(if (selected) Modifier.border(3.dp, KodomoColors.Green, CircleShape) else Modifier),
            )
            return
        }
    }
    val style = styleOf(kind)
    Box(
        modifier = modifier
            .size(size)
            .background(style.bg, CircleShape)
            .then(if (selected) Modifier.border(3.dp, KodomoColors.Green, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (kind == AvatarKind.ROBOT) {
            KodomoMascot(MascotMood.HAPPY, size = size * 0.9f, modifier = Modifier.padding(top = 2.dp))
        } else {
            Canvas(Modifier.size(size)) { drawAvatar(kind, style) }
        }
    }
}

private fun DrawScope.drawAvatar(kind: AvatarKind, style: AvatarStyle) {
    val s = size.minDimension
    val faceCenter = Offset(s * 0.5f, s * 0.54f)
    val faceR = s * if (kind == AvatarKind.BABY) 0.3f else 0.27f
    val ink = Color(0xFF2B2B2B)

    when (kind) {
        AvatarKind.CAT, AvatarKind.BEAR -> drawAnimal(kind, style, faceCenter, faceR)
        else -> {
            // shoulders
            val shirt = when (kind) {
                AvatarKind.GIRL_BOW, AvatarKind.GIRL_BRAIDS -> KodomoColors.Rose
                AvatarKind.MOTHER -> KodomoColors.Green
                AvatarKind.FATHER -> Color(0xFF4C6A92)
                else -> KodomoColors.Sky
            }
            drawRoundRect(shirt, topLeft = Offset(s * 0.2f, s * 0.8f), size = Size(s * 0.6f, s * 0.3f), cornerRadius = CornerRadius(s * 0.2f))
            // hair back layer
            when (kind) {
                AvatarKind.GIRL_BRAIDS -> {
                    drawCircle(style.hair, s * 0.09f, Offset(s * 0.22f, s * 0.7f))
                    drawCircle(style.hair, s * 0.09f, Offset(s * 0.78f, s * 0.7f))
                }
                AvatarKind.MOTHER -> drawCircle(style.hair, faceR * 1.35f, faceCenter + Offset(0f, s * 0.04f))
                AvatarKind.GIRL_BOW -> drawCircle(style.hair, faceR * 1.18f, faceCenter + Offset(0f, -s * 0.01f))
                else -> Unit
            }
            drawCircle(style.skin, faceR, faceCenter)
            // ears
            if (kind != AvatarKind.MOTHER) {
                drawCircle(style.skin, s * 0.045f, faceCenter + Offset(-faceR, s * 0.02f))
                drawCircle(style.skin, s * 0.045f, faceCenter + Offset(faceR, s * 0.02f))
            }
            // front hair
            when (kind) {
                AvatarKind.BOY_SHORT, AvatarKind.FATHER -> drawArc(style.hair, 180f, 180f, true, topLeft = faceCenter - Offset(faceR, faceR * 1.05f), size = Size(faceR * 2, faceR * 1.2f))
                AvatarKind.BOY_CURLY -> repeat(7) { i ->
                    val x = faceCenter.x - faceR + i * faceR / 3f
                    drawCircle(style.hair, s * 0.07f, Offset(x, faceCenter.y - faceR * 0.78f))
                }
                AvatarKind.GIRL_BOW, AvatarKind.GIRL_BRAIDS -> drawArc(style.hair, 180f, 180f, true, topLeft = faceCenter - Offset(faceR * 1.02f, faceR * 1.08f), size = Size(faceR * 2.04f, faceR * 1.3f))
                AvatarKind.MOTHER -> {
                    val scarf = Path().apply {
                        moveTo(faceCenter.x - faceR * 1.15f, faceCenter.y + faceR * 0.2f)
                        quadraticTo(faceCenter.x, faceCenter.y - faceR * 1.9f, faceCenter.x + faceR * 1.15f, faceCenter.y + faceR * 0.2f)
                        lineTo(faceCenter.x + faceR * 0.85f, faceCenter.y - faceR * 0.1f)
                        quadraticTo(faceCenter.x, faceCenter.y - faceR * 1.3f, faceCenter.x - faceR * 0.85f, faceCenter.y - faceR * 0.1f)
                        close()
                    }
                    drawPath(scarf, style.hair)
                }
                AvatarKind.BABY -> drawArc(style.hair, 250f, 40f, false, topLeft = faceCenter - Offset(faceR * 0.3f, faceR * 1.1f), size = Size(faceR * 0.6f, faceR * 0.6f), style = Stroke(s * 0.02f, cap = StrokeCap.Round))
                else -> Unit
            }
            if (kind == AvatarKind.GIRL_BOW) {
                val bow = Offset(faceCenter.x + faceR * 0.6f, faceCenter.y - faceR * 0.85f)
                drawCircle(KodomoColors.Rose, s * 0.06f, bow + Offset(-s * 0.05f, 0f))
                drawCircle(KodomoColors.Rose, s * 0.06f, bow + Offset(s * 0.05f, 0f))
                drawCircle(Color(0xFFD9577A), s * 0.03f, bow)
            }
            if (kind == AvatarKind.FATHER) {
                drawArc(style.hair, 0f, 180f, true, topLeft = faceCenter - Offset(faceR * 0.85f, -faceR * 0.05f), size = Size(faceR * 1.7f, faceR * 1.0f))
                drawCircle(style.skin, faceR * 0.3f, faceCenter + Offset(0f, faceR * 0.35f))
            }
            drawFace(faceCenter, faceR, ink)
        }
    }
}

private fun DrawScope.drawFace(c: Offset, r: Float, ink: Color) {
    drawCircle(ink, r * 0.1f, c + Offset(-r * 0.38f, -r * 0.05f))
    drawCircle(ink, r * 0.1f, c + Offset(r * 0.38f, -r * 0.05f))
    drawCircle(Color.White, r * 0.035f, c + Offset(-r * 0.35f, -r * 0.09f))
    drawCircle(Color.White, r * 0.035f, c + Offset(r * 0.41f, -r * 0.09f))
    drawCircle(KodomoColors.Rose.copy(alpha = 0.4f), r * 0.13f, c + Offset(-r * 0.58f, r * 0.25f))
    drawCircle(KodomoColors.Rose.copy(alpha = 0.4f), r * 0.13f, c + Offset(r * 0.58f, r * 0.25f))
    drawArc(ink, 20f, 140f, false, topLeft = c + Offset(-r * 0.3f, r * 0.05f), size = Size(r * 0.6f, r * 0.42f), style = Stroke(r * 0.08f, cap = StrokeCap.Round))
}

private fun DrawScope.drawAnimal(kind: AvatarKind, style: AvatarStyle, c: Offset, r: Float) {
    if (kind == AvatarKind.CAT) {
        val left = Path().apply {
            moveTo(c.x - r * 0.95f, c.y - r * 0.2f); lineTo(c.x - r * 0.75f, c.y - r * 1.25f); lineTo(c.x - r * 0.2f, c.y - r * 0.85f); close()
        }
        val right = Path().apply {
            moveTo(c.x + r * 0.95f, c.y - r * 0.2f); lineTo(c.x + r * 0.75f, c.y - r * 1.25f); lineTo(c.x + r * 0.2f, c.y - r * 0.85f); close()
        }
        drawPath(left, style.hair)
        drawPath(right, style.hair)
    } else {
        drawCircle(style.hair, r * 0.38f, c + Offset(-r * 0.8f, -r * 0.8f))
        drawCircle(style.hair, r * 0.38f, c + Offset(r * 0.8f, -r * 0.8f))
    }
    drawCircle(style.skin, r * 1.1f, c)
    drawOval(Color.White.copy(alpha = 0.7f), topLeft = c + Offset(-r * 0.45f, r * 0.05f), size = Size(r * 0.9f, r * 0.65f))
    drawCircle(Color(0xFF2B2B2B), r * 0.14f, c + Offset(0f, r * 0.15f))
    drawFace(c + Offset(0f, -r * 0.1f), r, Color(0xFF2B2B2B))
}
