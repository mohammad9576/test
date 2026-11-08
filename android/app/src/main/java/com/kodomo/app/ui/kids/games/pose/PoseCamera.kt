package com.kodomo.app.ui.kids.games.pose

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.Executors
import kotlin.math.hypot
import kotlin.math.max

/** A body landmark in upright, mirrored (selfie) image space, normalised to 0..1. */
data class PosePoint(val x: Float, val y: Float, val likelihood: Float)

/** One detected body pose. Distances are computed in pixel space so that the image aspect ratio does not distort them. */
data class BodyPose(val points: Map<Int, PosePoint>, val imageWidth: Int, val imageHeight: Int, val timeMillis: Long) {

    fun visible(type: Int, minLikelihood: Float = 0.5f): PosePoint? = points[type]?.takeIf { it.likelihood >= minLikelihood }

    fun distance(a: PosePoint, b: PosePoint): Float = hypot((a.x - b.x) * imageWidth, (a.y - b.y) * imageHeight)

    fun mid(a: PosePoint, b: PosePoint) = PosePoint((a.x + b.x) / 2, (a.y + b.y) / 2, minOf(a.likelihood, b.likelihood))

    /** Shoulder-to-hip length in pixels: the yard-stick for every gesture threshold. */
    val torso: Float?
        get() {
            val ls = visible(PoseLandmark.LEFT_SHOULDER); val rs = visible(PoseLandmark.RIGHT_SHOULDER)
            val lh = visible(PoseLandmark.LEFT_HIP); val rh = visible(PoseLandmark.RIGHT_HIP)
            if (ls == null || rs == null || lh == null || rh == null) return null
            return distance(mid(ls, rs), mid(lh, rh)).takeIf { it > 1f }
        }

    val upperBodyVisible: Boolean get() = torso != null

    val hands: List<PosePoint>
        get() = listOfNotNull(
            visible(PoseLandmark.LEFT_WRIST, 0.4f), visible(PoseLandmark.RIGHT_WRIST, 0.4f),
            visible(PoseLandmark.LEFT_INDEX, 0.4f), visible(PoseLandmark.RIGHT_INDEX, 0.4f),
        )

    /** Maps a landmark onto a view that shows the camera preview with FILL_CENTER scaling. */
    fun toScreen(p: PosePoint, size: Size): Offset {
        val scale = max(size.width / imageWidth, size.height / imageHeight)
        val dx = (size.width - imageWidth * scale) / 2f
        val dy = (size.height - imageHeight * scale) / 2f
        return Offset(dx + p.x * imageWidth * scale, dy + p.y * imageHeight * scale)
    }

    /** Converts a screen position back to normalised pose space (inverse of [toScreen]). */
    fun fromScreen(o: Offset, size: Size): Offset {
        val scale = max(size.width / imageWidth, size.height / imageHeight)
        val dx = (size.width - imageWidth * scale) / 2f
        val dy = (size.height - imageHeight * scale) / 2f
        return Offset((o.x - dx) / (imageWidth * scale), (o.y - dy) / (imageHeight * scale))
    }
}

/** Holds the most recent pose for game loops; updated on the main thread. */
@Stable
class PoseTracker {
    var pose by mutableStateOf<BodyPose?>(null)
        internal set
    var frames by mutableStateOf(0)
        internal set
}

/**
 * Front camera + ML Kit pose detection (bundled model, works offline). The preview is mirrored like a selfie and
 * landmarks are mirrored to match, so a child's raised right hand appears on the right side of the screen.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun PoseCamera(tracker: PoseTracker, modifier: Modifier = Modifier, showSkeleton: Boolean = true) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val pool = remember { Executors.newSingleThreadExecutor() }
    // Frames still being analysed when the game ends must not crash the app: drop work once the camera is closed.
    val closed = remember { java.util.concurrent.atomic.AtomicBoolean(false) }
    val executor = remember { java.util.concurrent.Executor { r -> if (!closed.get()) runCatching { pool.execute(r) } } }
    val detector = remember {
        PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build())
    }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val main = ContextCompat.getMainExecutor(context)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            if (closed.get()) return@addListener
            val p = runCatching { future.get() }.getOrNull() ?: return@addListener
            provider = p
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
            analysis.setAnalyzer(executor) { proxy ->
                val media = proxy.image
                if (media == null || closed.get()) {
                    proxy.close()
                    return@setAnalyzer
                }
                val rotation = proxy.imageInfo.rotationDegrees
                val w = if (rotation % 180 == 0) proxy.width else proxy.height
                val h = if (rotation % 180 == 0) proxy.height else proxy.width
                runCatching { detector.process(InputImage.fromMediaImage(media, rotation)) }.getOrElse { proxy.close(); return@setAnalyzer }
                    .addOnSuccessListener(executor) { pose ->
                        val points = pose.allPoseLandmarks.associate { lm ->
                            lm.landmarkType to PosePoint(1f - lm.position.x / w, lm.position.y / h, lm.inFrameLikelihood)
                        }
                        val body = if (points.isEmpty()) null else BodyPose(points, w, h, System.currentTimeMillis())
                        if (!closed.get()) main.execute {
                            tracker.pose = body
                            tracker.frames++
                        }
                    }
                    .addOnCompleteListener(main) { runCatching { proxy.close() } }
            }
            runCatching {
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
            }
        }, main)
        onDispose {
            runCatching { provider?.unbindAll() }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            closed.set(true)
            runCatching { detector.close() }
            pool.shutdown()
        }
    }

    Box(modifier) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        if (showSkeleton) {
            Canvas(Modifier.fillMaxSize()) { tracker.pose?.let { drawSkeleton(it) } }
        }
    }
}

private val bones = listOf(
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
    PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
    PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
    PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
    PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE,
)

/** Friendly glowing skeleton with big «magic hands» so children see what the game sees. */
fun DrawScope.drawSkeleton(pose: BodyPose) {
    bones.forEach { (a, b) ->
        val pa = pose.visible(a); val pb = pose.visible(b)
        if (pa != null && pb != null) {
            drawLine(Color.White.copy(alpha = 0.7f), pose.toScreen(pa, size), pose.toScreen(pb, size), strokeWidth = 10f, cap = StrokeCap.Round)
        }
    }
    listOf(PoseLandmark.LEFT_WRIST, PoseLandmark.RIGHT_WRIST).forEach { type ->
        pose.visible(type, 0.4f)?.let {
            val c = pose.toScreen(it, size)
            drawCircle(Color(0xFF5CE0A0).copy(alpha = 0.35f), 70f, c)
            drawCircle(Color(0xFF5CE0A0), 30f, c)
        }
    }
    pose.visible(PoseLandmark.NOSE)?.let { drawCircle(Color(0xFFFFC83D), 18f, pose.toScreen(it, size)) }
}
