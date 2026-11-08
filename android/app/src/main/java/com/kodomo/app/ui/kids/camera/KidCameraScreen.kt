package com.kodomo.app.ui.kids.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import com.kodomo.app.data.prefs.KidMedia
import com.kodomo.app.data.prefs.KidMediaStore
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.fx.Emoji
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.childId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class KidCameraViewModel @Inject constructor(savedState: SavedStateHandle, private val store: KidMediaStore) : ViewModel() {
    val childId = savedState.childId
    val media = store.observe(childId)
    fun add(uri: Uri, video: Boolean) = viewModelScope.launch { store.add(childId, KidMedia(uri.toString(), video, System.currentTimeMillis())) }
    fun remove(m: KidMedia) = viewModelScope.launch { store.remove(childId, m.uri) }
}

private fun stamp() = "kodomo_${System.currentTimeMillis()}"

/** Where a new photo goes: the phone gallery (Pictures/Kodomo) on Android 10+, otherwise the app's own picture folder. */
private fun photoOutput(context: Context): ImageCapture.OutputFileOptions =
    if (Build.VERSION.SDK_INT >= 29) {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${stamp()}.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Kodomo")
        }
        ImageCapture.OutputFileOptions.Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values).build()
    } else {
        val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Kodomo").apply { mkdirs() }
        ImageCapture.OutputFileOptions.Builder(File(dir, "${stamp()}.jpg")).build()
    }

/** «دوربین من»: photos and short videos (up to one minute) that land in the child's own gallery and the phone's. */
@SuppressLint("MissingPermission")
@Composable
fun KidCameraScreen(onBack: () -> Unit, vm: KidCameraViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    TrackActivity(ActivityCategory.ART, "دوربین من")
    val media by vm.media.collectAsStateWithLifecycle(emptyList())
    var gallery by remember { mutableStateOf(false) }
    var videoMode by remember { mutableStateOf(false) }
    var front by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var seconds by remember { mutableIntStateOf(0) }
    var flash by remember { mutableIntStateOf(0) }
    var viewing by remember { mutableStateOf<KidMedia?>(null) }
    val scope = rememberCoroutineScope()

    val controller = remember {
        LifecycleCameraController(context).apply { setEnabledUseCases(CameraController.IMAGE_CAPTURE or CameraController.VIDEO_CAPTURE) }
    }
    DisposableEffect(lifecycle) {
        runCatching { controller.bindToLifecycle(lifecycle) }
        onDispose { runCatching { recording?.stop() }; runCatching { controller.unbind() } }
    }
    LaunchedEffect(front) {
        controller.cameraSelector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
    }
    LaunchedEffect(recording) {
        seconds = 0
        while (recording != null) {
            delay(1000); seconds++
            if (seconds >= 60) { recording?.stop(); recording = null }
        }
    }

    fun takePhoto() {
        val out = photoOutput(context)
        controller.takePicture(out, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(r: ImageCapture.OutputFileResults) {
                r.savedUri?.let { vm.add(it, video = false) }
                flash++
                com.kodomo.app.ui.fx.GameSfx.sparkle()
            }
            override fun onError(e: ImageCaptureException) { space.voice.speak("عکس گرفته نشد، دوباره امتحان کن") }
        })
    }

    fun toggleVideo() {
        recording?.let { it.stop(); recording = null; return }
        val audio = if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) AudioConfig.create(true) else AudioConfig.AUDIO_DISABLED
        val listener = androidx.core.util.Consumer<VideoRecordEvent> { e ->
            if (e is VideoRecordEvent.Finalize && !e.hasError()) vm.add(e.outputResults.outputUri, video = true)
        }
        recording = runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, "${stamp()}.mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/Kodomo")
                }
                val opts = MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(values).build()
                controller.startRecording(opts, audio, ContextCompat.getMainExecutor(context), listener)
            } else {
                val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "Kodomo").apply { mkdirs() }
                controller.startRecording(FileOutputOptions.Builder(File(dir, "${stamp()}.mp4")).build(), audio, ContextCompat.getMainExecutor(context), listener)
            }
        }.getOrNull()
    }

    KidScreenScaffold(title = if (gallery) "گالری من" else "دوربین من", onBack = { if (viewing != null) viewing = null else if (gallery) gallery = false else onBack() }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!gallery) {
                AndroidView(factory = { PreviewView(it).apply { this.controller = controller; scaleType = PreviewView.ScaleType.FILL_CENTER } }, modifier = Modifier.fillMaxSize())
                if (flash > 0) {
                    var white by remember(flash) { mutableStateOf(true) }
                    LaunchedEffect(flash) { delay(120); white = false }
                    if (white) Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.8f)))
                }
                if (recording != null) {
                    Text("🔴 ${seconds}/60", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.TopCenter).padding(12.dp).clip(RoundedCornerShape(50)).background(Color(0xAAD32F2F)).padding(horizontal = 14.dp, vertical = 6.dp))
                }
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0x66000000)).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterChip(selected = !videoMode, onClick = { if (recording == null) videoMode = false }, label = { Text("📷 عکس") })
                        FilterChip(selected = videoMode, onClick = { if (recording == null) videoMode = true }, label = { Text("🎥 فیلم") })
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        val lastPic = media.firstOrNull()
                        Box(Modifier.size(58.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.3f)).clickable { gallery = true }, contentAlignment = Alignment.Center) {
                            if (lastPic != null) Thumb(lastPic, Modifier.fillMaxSize()) else Emoji("🖼️", 34.dp)
                        }
                        Box(
                            Modifier.size(84.dp).clip(CircleShape).background(if (videoMode) Color(0xFFE53935) else Color.White).border(5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                                .clickable { if (videoMode) toggleVideo() else takePhoto() },
                            contentAlignment = Alignment.Center,
                        ) { if (recording != null) Box(Modifier.size(30.dp).clip(RoundedCornerShape(6.dp)).background(Color.White)) }
                        Box(Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)).clickable { if (recording == null) front = !front }, contentAlignment = Alignment.Center) {
                            Text("🔄", fontSize = 28.sp)
                        }
                    }
                }
            } else {
                val v = viewing
                if (v != null) {
                    Column(Modifier.fillMaxSize().background(Color.Black)) {
                        if (v.video) {
                            AndroidView(factory = { ctx ->
                                android.widget.VideoView(ctx).apply {
                                    setVideoURI(Uri.parse(v.uri)); setMediaController(android.widget.MediaController(ctx)); start()
                                }
                            }, modifier = Modifier.weight(1f).fillMaxWidth())
                        } else {
                            AsyncImage(model = Uri.parse(v.uri), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.weight(1f).fillMaxWidth())
                        }
                        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(onClick = { viewing = null }) { Text("بستن", color = Color.White) }
                            TextButton(onClick = { vm.remove(v); viewing = null }) { Text("🗑️ برداشتن از گالری من", color = Color.White) }
                        }
                    }
                } else if (media.isEmpty()) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Emoji("📸", 110.dp)
                        Text("هنوز عکسی نگرفتی. برگرد و اولین عکست را بگیر!", fontSize = 18.sp)
                    }
                } else {
                    LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(media, key = { it.uri }) { m ->
                            Box(Modifier.aspectRatio(1f).clip(RoundedCornerShape(14.dp)).clickable { viewing = m }) {
                                Thumb(m, Modifier.fillMaxSize())
                                if (m.video) Text("▶️", fontSize = 26.sp, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Thumb(m: KidMedia, modifier: Modifier) {
    val context = LocalContext.current
    val request = remember(m.uri) {
        ImageRequest.Builder(context).data(Uri.parse(m.uri)).apply { if (m.video) decoderFactory(VideoFrameDecoder.Factory()) }.crossfade(true).build()
    }
    AsyncImage(model = request, contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier.background(Color(0xFFECEFF1)))
}
