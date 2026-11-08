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
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map

@HiltViewModel
class KidSpaceViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val screenTime: ScreenTimeManager,
    val voice: MascotVoice,
    val buddies: com.kodomo.app.data.prefs.BuddyStore,
    wellbeingStore: com.kodomo.app.data.prefs.WellbeingStore,
    private val session: com.kodomo.app.data.prefs.SessionPreferences,
) : ViewModel() {
    fun markKidSpace(childId: Long?) = kotlinx.coroutines.runBlocking { session.setKidSpaceChild(childId) }
    val wellbeing = wellbeingStore.state

    val dialogue = MascotDialogueEngine()
    val screenState: StateFlow<ScreenTimeManager.State> = screenTime.state
    val events = screenTime.events

    fun child(childId: Long): Flow<Child?> = familyRepository.observeChild(childId)

    fun start(childId: Long) = screenTime.startSession(childId)

    fun setActivity(category: ActivityCategory, title: String, contentId: String?) = screenTime.setActivity(category, title, contentId)

    fun grantExtra(minutes: Int) = screenTime.grantExtraMinutes(minutes, liftLocks = true)

    suspend fun verifyPin(pin: String) = familyRepository.verifyPin(pin)
}

/** Services the kids space offers to every screen inside it. */
class KidSpaceScope(
    val child: Child,
    val spec: AgeUiSpec,
    val voice: MascotVoice,
    val requestParentExit: () -> Unit,
    val setActivity: (ActivityCategory, String, String?) -> Unit,
    /** Confetti + success jingle over the whole kids space. */
    val celebrate: () -> Unit = {},
    /** Saves the companion character the child picked. */
    val chooseBuddy: (com.kodomo.app.domain.model.Buddy) -> Unit = {},
    /** Opens the dialer for an emergency call to a parent. */
    val callParent: (String) -> Unit = {},
) {
    /** Speaks [text] when the age tier wants voice guidance for every event (toddlers). */
    fun guide(text: String) {
        if (spec.voiceGuidance) voice.speak(text)
    }
}

val LocalWellbeing = androidx.compose.runtime.compositionLocalOf { com.kodomo.app.data.prefs.Wellbeing() }

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
    val loaded by remember(childId) { vm.child(childId).map { c -> Pair<Child?, Boolean>(c, true) } }.collectAsStateWithLifecycle(Pair<Child?, Boolean>(null, false))
    val child = loaded.first
    if (loaded.second && child == null) {
        // The child was removed (or never existed): never leave a blank, stuck screen.
        LaunchedEffect(Unit) { vm.markKidSpace(null); onParentExit() }
    }
    val state by vm.screenState.collectAsStateWithLifecycle()
    val kiosk = LocalKioskController.current
    var showGate by remember { mutableStateOf<GatePurpose?>(null) }
    var warning by remember { mutableStateOf<MascotLine?>(null) }
    var party by remember { mutableStateOf(0) }
    val partyScope = androidx.compose.runtime.rememberCoroutineScope()

    LaunchedEffect(childId) {
        kiosk.enterKidSpace()
        vm.start(childId)
        if (com.kodomo.app.core.demo.DemoMode.route == null) vm.markKidSpace(childId)
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

    val wellbeing by vm.wellbeing.collectAsStateWithLifecycle(com.kodomo.app.data.prefs.Wellbeing())
    var eyeBreak by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(wellbeing.eyeBreakMinutes) {
        val minutes = wellbeing.eyeBreakMinutes
        if (minutes <= 0) return@LaunchedEffect
        while (true) {
            kotlinx.coroutines.delay(minutes * 60_000L)
            if (vm.screenState.value.lock == ScreenTimeManager.LockReason.NONE) eyeBreak = true
            while (eyeBreak) kotlinx.coroutines.delay(1000)
        }
    }
    com.kodomo.app.ui.kids.wellbeing.VolumeGuard(wellbeing.maxVolumePercent)
    com.kodomo.app.ui.kids.wellbeing.BrightnessAndInternet(wellbeing.brightnessPercent, wellbeing.blockOtherInternet)

    val current = child ?: return
    val spec = AgeUiSpec.forGroup(current.ageGroup())
    val scope = remember(current, spec) {
        KidSpaceScope(
            child = current,
            spec = spec,
            voice = vm.voice,
            requestParentExit = { showGate = GatePurpose.EXIT },
            setActivity = vm::setActivity,
            celebrate = {
                party++
                partyScope.launch { com.kodomo.app.ui.fx.Sfx.success() }
            },
            chooseBuddy = { b ->
                vm.voice.style = com.kodomo.app.ui.mascot.MascotVoice.Style(b.voicePitch, b.voiceSpeed, b.male)
                vm.voice.speak(b.title)
                partyScope.launch { vm.buddies.set(current.id, b) }
            },
            callParent = { phone -> com.kodomo.app.ui.kids.wellbeing.EmergencyCall.dial(context, phone, kiosk::launchExternalApp) },
        )
    }

    val buddy by remember(current.id) { vm.buddies.observe(current.id) }.collectAsStateWithLifecycle(com.kodomo.app.domain.model.Buddy.ROBOT)
    LaunchedEffect(buddy) { vm.voice.style = com.kodomo.app.ui.mascot.MascotVoice.Style(buddy.voicePitch, buddy.voiceSpeed, buddy.male) }
    KidsTheme(spec = spec, darkMode = current.darkMode) {
        CompositionLocalProvider(LocalKidSpace provides scope, com.kodomo.app.ui.mascot.LocalBuddy provides buddy, LocalWellbeing provides wellbeing) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                com.kodomo.app.ui.fx.KidsBackground(spec.group, current.darkMode)
                content()
                if (party > 0) com.kodomo.app.ui.fx.ConfettiBurst(party)
                if (state.child?.id == childId && state.lock != ScreenTimeManager.LockReason.NONE) {
                    LockOverlay(
                        child = current,
                        reason = state.lock,
                        line = if (state.lock == ScreenTimeManager.LockReason.BEDTIME) vm.dialogue.bedtime(current) else vm.dialogue.timeIsUp(current),
                        onParent = { showGate = GatePurpose.EXTEND },
                        onExit = { showGate = GatePurpose.EXIT },
                        call = { com.kodomo.app.ui.kids.wellbeing.CallParentsRow(wellbeing, scope.callParent) },
                    )
                }
                if (eyeBreak && state.lock == ScreenTimeManager.LockReason.NONE) {
                    com.kodomo.app.ui.kids.wellbeing.EyeBreakOverlay(current.name, { vm.voice.speak(it) }) { eyeBreak = false }
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
                        vm.markKidSpace(null)
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
private fun LockOverlay(child: Child, reason: ScreenTimeManager.LockReason, line: MascotLine, onParent: () -> Unit, onExit: () -> Unit = {}, call: @Composable () -> Unit = {}) {
    BackHandler(enabled = true) { }
    val bedtime = reason == ScreenTimeManager.LockReason.BEDTIME
    val school = reason == ScreenTimeManager.LockReason.SCHOOL
    val window = reason == ScreenTimeManager.LockReason.WINDOW
    val w = LocalWellbeing.current
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
            Text(if (bedtime) "🌙" else if (school) "🏫" else if (window) "⏰" else "🌳", style = MaterialTheme.typography.displayMedium)
            KodomoMascot(line.mood, size = 200.dp)
            if (!school && !window) Text(
                line.text,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = if (bedtime) Color.White else MaterialTheme.colorScheme.onBackground,
            )
            Text(
                if (bedtime) "وقت خواب ${child.name} رسیده است."
                else if (school) "الان وقت مدرسه و درس است، ${child.name} جان. بعد از مدرسه دوباره همدیگر را می‌بینیم!"
                else if (window) (w.nextWindow(java.time.LocalTime.now())?.let { "الان وقت استراحت از گوشی است، ${child.name} جان. نوبت بعدی ساعت ${com.kodomo.app.data.prefs.Wellbeing.hhmm(it)} است ⏰" }
                    ?: "نوبت‌های گوشی امروز تمام شد، ${child.name} جان. فردا دوباره بازی می‌کنیم!")
                else "زمان امروز ${child.name} تمام شد. بیا یک بازی بیرون از صفحه انجام بدیم!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = if (bedtime) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            call()
            OutlinedButton(onClick = onParent, modifier = Modifier.fillMaxWidth()) {
                Text("ورود والدین برای ۱۵ دقیقه‌ی بیشتر", color = if (bedtime) Color.White else MaterialTheme.colorScheme.primary)
            }
            TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                Text("🔒 خروج والدین از محیط کودک", color = if (bedtime) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun KidPrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) = KodomoPrimaryButton(text, onClick, modifier)
