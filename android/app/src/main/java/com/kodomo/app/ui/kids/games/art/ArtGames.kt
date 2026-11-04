package com.kodomo.app.ui.kids.games.art

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Region
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kodomo.app.domain.engine.VoicePhrases
import com.kodomo.app.domain.model.MemorySource
import com.kodomo.app.domain.repository.MemoryRepository
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.childId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.min

@HiltViewModel
class ArtViewModel @Inject constructor(savedState: SavedStateHandle, private val memories: MemoryRepository) : ViewModel() {
    private val childId = savedState.childId

    /** Saves the artwork (rendered off-screen) to the encrypted, parents-only memories gallery. */
    fun save(caption: String, render: () -> Bitmap, onSaved: () -> Unit) = viewModelScope.launch {
        val jpeg = withContext(Dispatchers.Default) {
            val bitmap = render()
            ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
        }
        memories.saveJpeg(childId, jpeg, caption, MemorySource.ART)
        onSaved()
    }
}

val artPalette = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF00ACC1),
    Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFFF06292), Color(0xFF8D6E63), Color(0xFF212121), Color(0xFFFFFFFF),
)

@Composable
private fun Palette(selected: Color, onSelect: (Color) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
        items(artPalette) { c ->
            Box(
                Modifier
                    .size(if (c == selected) 46.dp else 38.dp)
                    .background(c, CircleShape)
                    .border(if (c == selected) 4.dp else 1.dp, if (c == selected) Color(0xFF2E7D5B) else Color.LightGray, CircleShape)
                    .pointerInput(c) { detectTapGestures { onSelect(c) } },
            )
        }
    }
}

private data class PaintStroke(val color: Color, val width: Float, val points: List<Offset>)

/** Free drawing with colours, three brush sizes, eraser, undo and «save to the family gallery». */
@Composable
fun FreeDrawGame(onFinish: (Int) -> Unit, viewModel: ArtViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val strokes = remember { mutableStateListOf<PaintStroke>() }
    var current by remember { mutableStateOf<PaintStroke?>(null) }
    var color by remember { mutableStateOf(artPalette[5]) }
    var width by remember { mutableStateOf(18f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var saving by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .border(2.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                .onSizeChanged { canvasSize = it }
                .pointerInput(color, width) {
                    detectDragGestures(
                        onDragStart = { start -> current = PaintStroke(color, width, listOf(start)) },
                        onDrag = { change, _ ->
                            change.consume()
                            current = current?.let { it.copy(points = it.points + change.position) }
                        },
                        onDragEnd = {
                            current?.let { strokes += it }
                            current = null
                        },
                        onDragCancel = {
                            current?.let { strokes += it }
                            current = null
                        },
                    )
                }
                .pointerInput(color, width) {
                    detectTapGestures { p -> strokes += PaintStroke(color, width, listOf(p, p + Offset(0.1f, 0.1f))) }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                (strokes + listOfNotNull(current)).forEach { b ->
                    val path = androidx.compose.ui.graphics.Path()
                    b.points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
                    drawPath(path, b.color, style = Stroke(width = b.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
        Palette(color) { color = it }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(8f to "نازک", 18f to "متوسط", 36f to "پهن").forEach { (w, label) ->
                FilterChip(selected = width == w, onClick = { width = w }, label = { Text(label) })
            }
            FilterChip(selected = color == Color.White, onClick = { color = Color.White }, label = { Text("پاک‌کن") })
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }, modifier = Modifier.weight(1f)) { Text("↩️ برگرد") }
            FilledTonalButton(onClick = { strokes.clear() }, modifier = Modifier.weight(1f)) { Text("🗑️ از نو") }
            FilledTonalButton(
                enabled = strokes.isNotEmpty() && !saving,
                onClick = {
                    saving = true
                    val snapshot = strokes.toList()
                    val size = canvasSize
                    viewModel.save("نقاشی آزاد", render = { renderStrokes(snapshot, size) }) {
                        space.voice.speak(VoicePhrases.PAINT_SAVED)
                        onFinish(10)
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("💾 ذخیره") }
        }
    }
}

private fun renderStrokes(strokes: List<PaintStroke>, size: IntSize): Bitmap {
    val bitmap = Bitmap.createBitmap(size.width.coerceAtLeast(1), size.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    strokes.forEach { b ->
        paint.color = b.color.toArgb()
        paint.strokeWidth = b.width
        val path = android.graphics.Path()
        b.points.forEachIndexed { i, p -> if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
        canvas.drawPath(path, paint)
    }
    return bitmap
}

/** Tap-to-fill colouring page. Regions are vector paths in a 1000×1000 design space; later regions sit on top. */
@Composable
fun ColoringGame(onFinish: (Int) -> Unit, viewModel: ArtViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    var pageIndex by remember { mutableIntStateOf(0) }
    val page = ColoringPages.all[pageIndex]
    val fills = remember(pageIndex) { mutableStateListOf<Color?>().apply { repeat(page.regions.size) { add(null) } } }
    var color by remember { mutableStateOf(artPalette[0]) }
    var celebrated by remember(pageIndex) { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 12.dp)) {
            items(ColoringPages.all.size) { i ->
                FilterChip(selected = i == pageIndex, onClick = { pageIndex = i }, label = { Text(ColoringPages.all[i].title) })
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White),
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(pageIndex, color) {
                        detectTapGestures { tap ->
                            val k = min(size.width, size.height) / ColoringPages.SPACE
                            val dx = (size.width - ColoringPages.SPACE * k) / 2f
                            val dy = (size.height - ColoringPages.SPACE * k) / 2f
                            val x = ((tap.x - dx) / k).toInt()
                            val y = ((tap.y - dy) / k).toInt()
                            val hit = page.hitRegions.indices.reversed().firstOrNull { page.hitRegions[it].contains(x, y) }
                            if (hit != null) {
                                fills[hit] = color
                                if (!celebrated && fills.all { it != null }) {
                                    celebrated = true
                                    space.voice.speak(VoicePhrases.COLORING_DONE)
                                }
                            }
                        }
                    },
            ) {
                val k = min(size.width, size.height) / ColoringPages.SPACE
                val dx = (size.width - ColoringPages.SPACE * k) / 2f
                val dy = (size.height - ColoringPages.SPACE * k) / 2f
                translate(dx, dy) {
                    scale(k, k, pivot = Offset.Zero) {
                        page.regions.forEachIndexed { i, region ->
                            val p = region.asComposePath()
                            drawPath(p, fills.getOrNull(i) ?: Color.White)
                            drawPath(p, Color(0xFF263238), style = Stroke(width = 6f, join = StrokeJoin.Round))
                        }
                    }
                }
            }
        }
        Palette(color) { color = it }
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { for (i in fills.indices) fills[i] = null }, modifier = Modifier.weight(1f)) { Text("🗑️ از نو") }
            FilledTonalButton(
                enabled = fills.any { it != null } && !saving,
                onClick = {
                    saving = true
                    val snapshot = fills.toList()
                    viewModel.save("رنگ‌آمیزی: ${page.title}", render = { ColoringPages.render(page, snapshot) }) {
                        space.voice.speak(VoicePhrases.PAINT_SAVED)
                        onFinish(if (snapshot.all { it != null }) 12 else 6)
                    }
                },
                modifier = Modifier.weight(1f),
            ) { Text("💾 ذخیره") }
        }
    }
}

class ColoringPage(val title: String, val regions: List<android.graphics.Path>) {
    val hitRegions: List<Region> = regions.map { path ->
        Region().apply { setPath(path, Region(0, 0, ColoringPages.SPACE.toInt(), ColoringPages.SPACE.toInt())) }
    }
}

object ColoringPages {
    const val SPACE = 1000f

    private fun rect(l: Float, t: Float, r: Float, b: Float) = android.graphics.Path().apply { addRect(l, t, r, b, android.graphics.Path.Direction.CW) }
    private fun round(l: Float, t: Float, r: Float, b: Float, rad: Float) =
        android.graphics.Path().apply { addRoundRect(l, t, r, b, rad, rad, android.graphics.Path.Direction.CW) }
    private fun circle(cx: Float, cy: Float, r: Float) = android.graphics.Path().apply { addCircle(cx, cy, r, android.graphics.Path.Direction.CW) }
    private fun oval(l: Float, t: Float, r: Float, b: Float) = android.graphics.Path().apply { addOval(l, t, r, b, android.graphics.Path.Direction.CW) }
    private fun poly(vararg xy: Float) = android.graphics.Path().apply {
        moveTo(xy[0], xy[1])
        var i = 2
        while (i < xy.size) {
            lineTo(xy[i], xy[i + 1])
            i += 2
        }
        close()
    }

    val all: List<ColoringPage> = listOf(
        ColoringPage(
            "خانه‌ی من",
            listOf(
                rect(0f, 0f, 1000f, 640f), rect(0f, 640f, 1000f, 1000f), circle(830f, 150f, 90f),
                circle(200f, 160f, 60f), circle(270f, 140f, 75f), circle(345f, 165f, 55f),
                rect(260f, 430f, 640f, 760f), poly(220f, 440f, 450f, 250f, 680f, 440f), rect(410f, 590f, 500f, 760f),
                round(295f, 480f, 375f, 560f, 10f), round(530f, 480f, 610f, 560f, 10f),
                rect(785f, 560f, 835f, 780f), circle(810f, 480f, 120f), circle(120f, 820f, 40f), circle(560f, 860f, 35f),
            ),
        ),
        ColoringPage(
            "ماهی کوچولو",
            listOf(
                rect(0f, 0f, 1000f, 820f), rect(0f, 820f, 1000f, 1000f), oval(260f, 330f, 700f, 620f),
                poly(680f, 475f, 880f, 340f, 880f, 610f), poly(430f, 340f, 520f, 240f, 560f, 345f), circle(360f, 440f, 32f),
                circle(170f, 260f, 34f), circle(130f, 160f, 24f), circle(200f, 90f, 16f),
                round(100f, 640f, 140f, 900f, 20f), round(840f, 600f, 880f, 900f, 20f), oval(480f, 520f, 600f, 590f),
            ),
        ),
        ColoringPage(
            "گل و پروانه",
            listOf(
                rect(0f, 0f, 1000f, 1000f), rect(0f, 820f, 1000f, 1000f), round(330f, 560f, 360f, 900f, 14f),
                oval(360f, 680f, 500f, 740f), circle(345f, 330f, 75f), circle(345f, 520f, 75f), circle(250f, 425f, 75f),
                circle(440f, 425f, 75f), circle(345f, 425f, 60f), oval(600f, 240f, 720f, 380f), oval(780f, 240f, 900f, 380f),
                oval(620f, 380f, 720f, 470f), oval(780f, 380f, 880f, 470f), round(735f, 240f, 765f, 480f, 15f),
            ),
        ),
    )

    fun render(page: ColoringPage, fills: List<Color?>): Bitmap {
        val bitmap = Bitmap.createBitmap(SPACE.toInt(), SPACE.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = 0xFF263238.toInt()
        }
        page.regions.forEachIndexed { i, region ->
            fill.color = (fills.getOrNull(i) ?: Color.White).toArgb()
            canvas.drawPath(region, fill)
            canvas.drawPath(region, line)
        }
        return bitmap
    }
}
