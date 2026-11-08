package com.kodomo.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** Requests the camera permission once and reports the current grant state. */
@Composable
fun rememberCameraPermission(): Boolean {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(context.hasPermission(Manifest.permission.CAMERA)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    return granted
}

/** Handle used by screens to trigger a still capture from [CameraView]. */
class CameraCaptureHandle {
    internal var imageCapture: ImageCapture? = null

    fun capture(context: Context, onJpeg: (ByteArray) -> Unit, onError: (String) -> Unit) {
        val capture = imageCapture ?: return onError("دوربین آماده نیست")
        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val rotation = image.imageInfo.rotationDegrees
                    val bitmap = image.toBitmap()
                    image.close()
                    val rotated = if (rotation != 0) {
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
                    } else {
                        bitmap
                    }
                    val out = ByteArrayOutputStream()
                    rotated.compress(Bitmap.CompressFormat.JPEG, 88, out)
                    onJpeg(out.toByteArray())
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception.message ?: "خطا در عکاسی")
                }
            },
        )
    }
}

/**
 * CameraX preview with optional RGBA frame analysis (for Color Hunt / Jump & Pop) and optional still capture
 * (for «پروژه‌های من»).
 */
@Composable
fun CameraView(
    modifier: Modifier = Modifier,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    captureHandle: CameraCaptureHandle? = null,
    analyzer: ((ImageProxy) -> Unit)? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentAnalyzer by rememberUpdatedState(analyzer)
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    val executor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(lensFacing, lifecycleOwner) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        var disposed = false
        providerFuture.addListener({
            if (disposed) return@addListener
            val p = runCatching { providerFuture.get() }.getOrNull() ?: return@addListener
            provider = p
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)
            if (analyzer != null) {
                useCases += ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(executor) { image ->
                            try {
                                runCatching { currentAnalyzer?.invoke(image) }
                            } finally {
                                runCatching { image.close() }
                            }
                        }
                    }
            }
            if (captureHandle != null) {
                val capture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
                captureHandle.imageCapture = capture
                useCases += capture
            }
            val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            runCatching {
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, selector, *useCases.toTypedArray())
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            runCatching { provider?.unbindAll() }
            captureHandle?.imageCapture = null
        }
    }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    AndroidView(factory = { previewView }, modifier = modifier)
}
