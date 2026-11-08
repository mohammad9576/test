package com.kodomo.app.ui.kids.games.art

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Shape helpers in the 1000×1000 colouring space. Every returned path is one tap-to-fill region. */
internal object Shapes {
    fun rect(l: Float, t: Float, r: Float, b: Float) = Path().apply { addRect(l, t, r, b, Path.Direction.CW) }
    fun round(l: Float, t: Float, r: Float, b: Float, rad: Float) = Path().apply { addRoundRect(l, t, r, b, rad, rad, Path.Direction.CW) }
    fun circle(cx: Float, cy: Float, r: Float) = Path().apply { addCircle(cx, cy, r, Path.Direction.CW) }
    fun oval(l: Float, t: Float, r: Float, b: Float) = Path().apply { addOval(l, t, r, b, Path.Direction.CW) }
    fun ovalC(cx: Float, cy: Float, rx: Float, ry: Float) = oval(cx - rx, cy - ry, cx + rx, cy + ry)
    fun poly(vararg xy: Float) = Path().apply {
        moveTo(xy[0], xy[1]); var i = 2
        while (i < xy.size) { lineTo(xy[i], xy[i + 1]); i += 2 }
        close()
    }
    fun star(cx: Float, cy: Float, outer: Float, inner: Float, points: Int = 5, rot: Float = -90f) = Path().apply {
        for (i in 0 until points * 2) {
            val r = if (i % 2 == 0) outer else inner
            val a = Math.toRadians((rot + i * 180.0 / points)).toFloat()
            val x = cx + r * cos(a); val y = cy + r * sin(a)
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
    fun heart(cx: Float, cy: Float, s: Float) = Path().apply {
        moveTo(cx, cy + s * 0.9f)
        cubicTo(cx - s * 1.3f, cy + s * 0.1f, cx - s * 0.9f, cy - s * 0.9f, cx, cy - s * 0.35f)
        cubicTo(cx + s * 0.9f, cy - s * 0.9f, cx + s * 1.3f, cy + s * 0.1f, cx, cy + s * 0.9f)
        close()
    }
    /** An annulus (the hole is not part of the region). */
    fun ring(cx: Float, cy: Float, r1: Float, r2: Float) = Path().apply {
        fillType = Path.FillType.EVEN_ODD
        addCircle(cx, cy, r2, Path.Direction.CW); addCircle(cx, cy, r1, Path.Direction.CW)
    }
    /** One sector of a ring, from angle [a0] to [a1] in degrees. */
    fun sector(cx: Float, cy: Float, r1: Float, r2: Float, a0: Float, a1: Float) = Path().apply {
        arcTo(RectF(cx - r2, cy - r2, cx + r2, cy + r2), a0, a1 - a0, true)
        arcTo(RectF(cx - r1, cy - r1, cx + r1, cy + r1), a1, a0 - a1)
        close()
    }
    /** [n] petals (ellipses) around a centre. */
    fun petals(cx: Float, cy: Float, dist: Float, len: Float, wid: Float, n: Int, rot: Float = 0f): List<Path> = (0 until n).map { i ->
        val a = rot + i * 360f / n
        val rad = Math.toRadians(a.toDouble())
        val px = cx + (dist * cos(rad)).toFloat(); val py = cy + (dist * sin(rad)).toFloat()
        Path().apply {
            addOval(px - len, py - wid, px + len, py + wid, Path.Direction.CW)
            transform(Matrix().apply { setRotate(a, px, py) })
        }
    }
    /** A teardrop leaf from (x,y) pointing towards angle [a]. */
    fun leaf(x: Float, y: Float, len: Float, wid: Float, a: Float) = Path().apply {
        moveTo(x, y)
        quadTo(x + len * 0.5f, y - wid, x + len, y)
        quadTo(x + len * 0.5f, y + wid, x, y)
        close()
        transform(Matrix().apply { setRotate(a, x, y) })
    }
    fun cloud(cx: Float, cy: Float, s: Float) = union(
        circle(cx - s * 0.6f, cy, s * 0.45f), circle(cx, cy - s * 0.25f, s * 0.6f), circle(cx + s * 0.6f, cy, s * 0.45f),
        rect(cx - s * 0.6f, cy, cx + s * 0.6f, cy + s * 0.45f),
    )
    fun union(vararg parts: Path) = Path().apply { parts.forEach { op(it, Path.Op.UNION) } }
    fun pointsOnCircle(cx: Float, cy: Float, r: Float, n: Int, rot: Float = -90f) =
        (0 until n).map { i -> val a = (rot + i * 360f / n) * PI.toFloat() / 180f; floatArrayOf(cx + r * cos(a), cy + r * sin(a)) }
}

enum class ColoringCategory(val label: String, val emoji: String) {
    ANIMALS("حیوانات", "🐾"), NATURE("طبیعت", "🌳"), VEHICLES("وسیله‌ها", "🚗"), SPACE("فضا", "🚀"),
    FOOD("خوراکی‌ها", "🍉"), MANDALA("ماندالا", "🌸"), HOME("خانه و شهر", "🏠"), PATTERN("طرح‌ها", "💠"),
}
