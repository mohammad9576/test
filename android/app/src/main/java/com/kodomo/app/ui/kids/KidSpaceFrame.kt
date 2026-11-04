package com.kodomo.app.ui.kids

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodomo.app.domain.engine.AgeUiSpec
import com.kodomo.app.domain.engine.MascotDialogueEngine
import com.kodomo.app.domain.engine.MascotLine
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.kiosk.LocalKioskController
import com.kodomo.app.kiosk.ScreenTimeManager
import com.kodomo.app.ui.components.KodomoPrimaryButton
import com.kodomo.app.ui.components.ParentGateDialog
import com.kodomo.app.ui.mascot.KodomoMascot
import com.kodomo.app.ui.mascot.MascotVoice
import com.kodomo.app.ui.theme.KidsTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class KidSpaceViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val screenTime: ScreenTimeManager,
    val voice: MascotVoice,
) : ViewModel() {
    val dialogue = MascotDialogueEngine()
    val screenState: StateFlow<ScreenTimeManager.State> = screenTime.state
    val events = screenTime.events

    fun child(childId: Long): Flow<Child?> = familyRepository.observeChild(childId)

    fun start(childId: Long) = screenTime.startSession(childId)

    fun setActivity(category: ActivityCategory, title: String, contentId: String?) = screenTime.setActivity(category, title, contentId)

    fun grantExtra(minutes: Int) = screenTime.grantExtraMinutes(minutes)

    suspend fun verifyPin(pin: String) = familyRepository.verifyPin(pin)
}

/** Services the kids space offers to every screen inside it. */
class KidSpaceScope(
    val child: Child,
    val spec: AgeUiSpec,
    val voice: MascotVoice,
    val requestParentExit: () -> Unit,
    val setActivity: (ActivityCategory, String, String?) -> Unit,
) {
    /** Speaks [text] when the age tier wants voice guidance for every event (toddlers). */
    fun guide(text: String) {
        if (spec.voiceGuidance) voice.speak(text)
    }
}

val LocalKidSpace = staticCompositionLocalOf<KidSpaceScope> { error("Not inside the kids space") }

/** Records the active screen in the screen-time engine. */
@Composable
fun TrackActivity(category: ActivityCategory, title: String, contentId: String? = null) {
    val scope = LocalKidSpace.current
    LaunchedEffect(category, title, contentId) { scope.setActivity(category, title, contentId) }
}

/**
 * Wraps every kids-space destination: applies the age-adaptive theme, enters kiosk mode, drives the screen-time
 * lock overlays (daily limit / bedtime) and the friendly 5-minute mascot warning.
 */
@Composable
fun KidSpaceFrame(childId: Long, onParentExit: () -> Unit, content: @Composable () -> Unit) {
    val vm: KidSpaceViewModel = hiltViewModel()
    val child by remember(childId) { vm.child(childId) }.collectAsStateWithLifecycle(null)
    val state by vm.screenState.collectAsStateWithLifecycle()
    val kiosk = LocalKioskController.current
    var showGate by remember { mutableStateOf<GatePurpose?>(null) }
    var warning by remember { mutableStateOf<MascotLine?>(null) }

    LaunchedEffect(childId) {
        kiosk.enterKidSpace()
        vm.start(childId)
    }
    LaunchedEffect(state.lock) { kiosk.setBedtimeDim(state.lock == ScreenTimeManager.LockReason.BEDTIME) }
    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                is ScreenTimeManager.Event.FiveMinuteWarning -> {
                    val line = vm.dialogue.fiveMinuteWarning(event.child)
                    warning = line
                    vm.voice.speakLine(line)
                }
                is ScreenTimeManager.Event.TimeUp -> vm.voice.speakLine(vm.dialogue.timeIsUp(event.child))
                is ScreenTimeManager.Event.Bedtime -> vm.voice.speakLine(vm.dialogue.bedtime(event.child))
            }
        }
    }

    val current = child ?: return
    val spec = AgeUiSpec.forGroup(current.ageGroup())
    val scope = remember(current, spec) {
        KidSpaceScope(
            child = current,
            spec = spec,
            voice = vm.voice,
            requestParentExit = { showGate = GatePurpose.EXIT },
            setActivity = vm::setActivity,
        )
    }

    KidsTheme(spec = spec, darkMode = current.darkMode) {
        CompositionLocalProvider(LocalKidSpace provides scope) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                content()
                if (state.child?.id == childId && state.lock != ScreenTimeManager.LockReason.NONE) {
                    LockOverlay(
                        child = current,
                        reason = state.lock,
                        line = if (state.lock == ScreenTimeManager.LockReason.BEDTIME) vm.dialogue.bedtime(current) else vm.dialogue.timeIsUp(current),
                        onParent = { showGate = GatePurpose.EXTEND },
                    )
                }
            }
            warning?.let { line ->
                AlertDialog(
                    onDismissRequest = { warning = null },
                    icon = { KodomoMascot(line.mood, size = 96.dp) },
                    text = { Text(line.text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center) },
                    confirmButton = { TextButton(onClick = { warning = null }) { Text("باشه، دوست من!") } },
                )
            }
        }
    }

    showGate?.let { purpose ->
        ParentGateDialog(
            title = if (purpose == GatePurpose.EXIT) "خروج از محیط کودک" else "افزایش زمان",
            verifyPin = vm::verifyPin,
            onDismiss = { showGate = null },
            onVerified = {
                showGate = null
                when (purpose) {
                    GatePurpose.EXIT -> {
                        kiosk.exitKidSpace()
                        onParentExit()
                    }
                    GatePurpose.EXTEND -> vm.grantExtra(15)
                }
            },
        )
    }
}

private enum class GatePurpose { EXIT, EXTEND }

@Composable
private fun LockOverlay(child: Child, reason: ScreenTimeManager.LockReason, line: MascotLine, onParent: () -> Unit) {
    BackHandler(enabled = true) { }
    val bedtime = reason == ScreenTimeManager.LockReason.BEDTIME
    val gradient = if (bedtime) {
        Brush.verticalGradient(listOf(Color(0xFF14213D), Color(0xFF283A5B)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE8F5EE), Color(0xFFCFEBDC)))
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(gradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (bedtime) "🌙" else "🌳", style = MaterialTheme.typography.displayMedium)
            KodomoMascot(line.mood, size = 200.dp)
            Text(
                line.text,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = if (bedtime) Color.White else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                if (bedtime) "وقت خواب ${child.name} رسیده است." else "زمان امروز ${child.name} تمام شد. بیا یک بازی بیرون از صفحه انجام بدیم!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = if (bedtime) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onParent, modifier = Modifier.fillMaxWidth()) {
                Text("ورود والدین برای ۱۵ دقیقه‌ی بیشتر", color = if (bedtime) Color.White else MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun KidPrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) = KodomoPrimaryButton(text, onClick, modifier)
