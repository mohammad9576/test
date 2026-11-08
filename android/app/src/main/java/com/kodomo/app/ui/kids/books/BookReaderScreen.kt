package com.kodomo.app.ui.kids.books

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.components.EmojiCover
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoPrimaryButton
import com.kodomo.app.ui.components.ParentGateDialog
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.ui.components.hasPermission
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.mascot.KodomoMascot
import com.kodomo.app.ui.mascot.MascotVoice
import com.kodomo.app.ui.navigation.ReadingMode
import com.kodomo.app.ui.theme.KodomoColors
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import com.kodomo.app.domain.repository.FamilyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderGateViewModel @Inject constructor(private val familyRepository: FamilyRepository) : ViewModel() {
    suspend fun verifyPin(pin: String) = familyRepository.verifyPin(pin)
}

@Composable
fun BookReaderScreen(
    contentId: String,
    mode: ReadingMode,
    onBack: () -> Unit,
    viewModel: BookReaderViewModel = hiltViewModel(),
    gate: ReaderGateViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val space = LocalKidSpace.current
    val context = LocalContext.current
    val book = state.book
    val pages = state.pages
    TrackActivity(ActivityCategory.BOOK, book?.title ?: "کتاب", contentId)

    // A book approved from Taaghche / Fidibo: read on its own page (with the family's account), locked to that page.
    val store = book?.mediaUrl?.takeIf { it.startsWith("http") && book.id.startsWith(com.kodomo.app.domain.engine.ContentPolicy.PARENT_APPROVED_PREFIX) }
    if (book != null && store != null) {
        KidScreenScaffold(title = book.title, onBack = onBack) { pad ->
            com.kodomo.app.ui.kids.video.LockedWebPlayer(com.kodomo.app.ui.kids.video.WebLock.Pinned(store), Modifier.fillMaxSize().padding(pad))
        }
        return
    }
    if (book == null || pages.isEmpty()) {
        KidScreenScaffold(title = "کتاب", onBack = onBack) { Box(Modifier.fillMaxSize().padding(it)) }
        return
    }

    val pager = rememberPagerState { pages.size + 1 }
    val scope = rememberCoroutineScope()
    var highlight by remember { mutableStateOf<IntRange?>(null) }
    var tappedWord by remember { mutableStateOf<Int?>(null) }
    var parentUnlocked by remember { mutableStateOf(false) }
    var showGate by remember { mutableStateOf(false) }
    var micGranted by remember { mutableStateOf(context.hasPermission(Manifest.permission.RECORD_AUDIO)) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { micGranted = it }

    val page = pager.currentPage
    val isEnd = page >= pages.size

    fun goNext() {
        scope.launch { if (pager.currentPage < pages.size) pager.animateScrollToPage(pager.currentPage + 1) }
    }

    LaunchedEffect(page, mode) {
        highlight = null
        tappedWord = null
        space.voice.stop()
        viewModel.stopPlayback()
        if (isEnd) {
            viewModel.onFinished()
            return@LaunchedEffect
        }
        val text = pages[page].text
        when (mode) {
            ReadingMode.AUDIO -> {
                delay(400)
                space.voice.speak(text, object : MascotVoice.Listener {
                    override fun onWord(start: Int, end: Int) { highlight = start until end }
                    override fun onDone() {
                        highlight = null
                        scope.launch {
                            delay(900)
                            goNext()
                        }
                    }
                })
            }
            ReadingMode.PARENT_VOICE -> if (page in state.recordedPages && state.recordingPage == null) {
                delay(300)
                viewModel.play(page) { goNext() }
            }
            ReadingMode.SELF -> Unit
        }
    }
    DisposableEffect(Unit) { onDispose { space.voice.stop() } }

    KidScreenScaffold(title = "${mode.emoji} ${book.title}", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f), reverseLayout = false) { index ->
                if (index >= pages.size) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) {
                        androidx.compose.runtime.LaunchedEffect(Unit) { space.celebrate() }
                        KodomoMascot(MascotMood.EXCITED, size = 180.dp)
                        Text("پایان قصه! 🎉", style = MaterialTheme.typography.headlineMedium)
                        Text("۳ ستاره گرفتی ⭐⭐⭐", style = MaterialTheme.typography.titleLarge)
                        if (state.medal) Text("🏅 مدال «کتاب‌خوان قهرمان» مال تو شد!", style = MaterialTheme.typography.titleMedium)
                        KodomoPrimaryButton("بازگشت به قفسه", onClick = onBack)
                    }
                } else {
                    val p = pages[index]
                    Column(
                        Modifier.fillMaxSize().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (p.image != null) {
                            coil.compose.AsyncImage(
                                model = p.image,
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(Color.White),
                            )
                        } else {
                            EmojiCover(p.emoji, book.colorStart, book.colorEnd, Modifier.fillMaxWidth().height(220.dp), emojiSize = 110, corner = 32.dp)
                        }
                        if (mode == ReadingMode.SELF) {
                            val words = p.text.split(" ")
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                words.forEachIndexed { wi, word ->
                                    Text(
                                        word,
                                        fontSize = (22 * space.spec.titleScale).sp,
                                        modifier = Modifier
                                            .background(if (tappedWord == wi && index == page) KodomoColors.Sun.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(8.dp))
                                            .bouncyClick {
                                                tappedWord = wi
                                                space.voice.speak(word)
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            Text("روی هر واژه بزن تا آن را بشنوی 👆", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val range = if (index == page) highlight else null
                            Text(
                                buildAnnotatedString {
                                    val t = p.text
                                    if (range == null || range.first >= t.length) {
                                        append(t)
                                    } else {
                                        append(t.substring(0, range.first))
                                        withStyle(SpanStyle(background = KodomoColors.Sun.copy(alpha = 0.55f), color = KodomoColors.GreenDark)) {
                                            append(t.substring(range.first, (range.last + 1).coerceAtMost(t.length)))
                                        }
                                        append(t.substring((range.last + 1).coerceAtMost(t.length)))
                                    }
                                },
                                fontSize = (22 * space.spec.titleScale).sp,
                                lineHeight = (36 * space.spec.titleScale).sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                        if (mode == ReadingMode.PARENT_VOICE) {
                            ParentVoiceControls(
                                recorded = index in state.recordedPages,
                                recording = state.recordingPage == index,
                                playing = state.playingPage == index,
                                parentUnlocked = parentUnlocked,
                                onUnlock = { showGate = true },
                                onRecord = {
                                    if (!micGranted) micLauncher.launch(Manifest.permission.RECORD_AUDIO) else viewModel.startRecording(index)
                                },
                                onStopRecording = viewModel::stopRecording,
                                onPlay = { viewModel.play(index) { goNext() } },
                                onStopPlay = viewModel::stopPlayback,
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { scope.launch { pager.animateScrollToPage((page - 1).coerceAtLeast(0)) } }, enabled = page > 0) { Text("صفحه‌ی قبل") }
                Text("${(page + 1).coerceAtMost(pages.size).fa()} / ${pages.size.fa()}", style = MaterialTheme.typography.titleMedium)
                Button(onClick = ::goNext, enabled = !isEnd) { Text("ورق بزن") }
            }
        }
    }

    if (showGate) {
        ParentGateDialog(
            title = "ضبط صدای والد",
            verifyPin = gate::verifyPin,
            onDismiss = { showGate = false },
            onVerified = {
                showGate = false
                parentUnlocked = true
            },
        )
    }
}

@Composable
private fun ParentVoiceControls(
    recorded: Boolean,
    recording: Boolean,
    playing: Boolean,
    parentUnlocked: Boolean,
    onUnlock: () -> Unit,
    onRecord: () -> Unit,
    onStopRecording: () -> Unit,
    onPlay: () -> Unit,
    onStopPlay: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when {
            playing -> Button(onClick = onStopPlay) { Icon(Icons.Rounded.Stop, null); Text(" توقف صدای مامان/بابا") }
            recorded -> Button(onClick = onPlay) { Icon(Icons.Rounded.PlayArrow, null); Text(" پخش صدای مامان/بابا") }
            else -> Text("هنوز صدای والد برای این صفحه ضبط نشده است.", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(4.dp))
        if (!parentUnlocked) {
            OutlinedButton(onClick = onUnlock) { Text("👩‍👧 ضبط صدا توسط والد") }
        } else if (recording) {
            Button(onClick = onStopRecording, colors = ButtonDefaults.buttonColors(containerColor = KodomoColors.Danger)) {
                Icon(Icons.Rounded.Stop, null); Text(" پایان ضبط")
            }
        } else {
            OutlinedButton(onClick = onRecord) { Icon(Icons.Rounded.Mic, null); Text(if (recorded) " ضبط دوباره" else " ضبط این صفحه") }
        }
    }
}
