package com.kodomo.app.ui.kids.projects

import com.kodomo.app.domain.engine.VoicePhrases
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.engine.AchievementCatalog
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.CraftProject
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.domain.model.MemorySource
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.MemoryRepository
import com.kodomo.app.domain.repository.ProjectRepository
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.ui.components.CameraCaptureHandle
import com.kodomo.app.ui.components.CameraView
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.KodomoPrimaryButton
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.ui.components.rememberCameraPermission
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.mascot.MascotSpeech
import com.kodomo.app.ui.navigation.Routes
import com.kodomo.app.ui.theme.KodomoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProjectsViewModel @Inject constructor(savedState: SavedStateHandle, private val projectRepository: ProjectRepository) : ViewModel() {
    private val childId = savedState.childId
    val projects: StateFlow<List<CraftProject>> = projectRepository.observeProjects(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(title: String, emoji: String, onCreated: (Long) -> Unit) = viewModelScope.launch {
        onCreated(projectRepository.create(childId, title, emoji))
    }
}

@HiltViewModel
class ProjectWizardViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val memoryRepository: MemoryRepository,
    private val familyRepository: FamilyRepository,
    private val rewardRepository: RewardRepository,
) : ViewModel() {
    private val childId = savedState.childId
    private val projectId = savedState.get<Long>(Routes.ARG_ID) ?: 0L
    val project: StateFlow<CraftProject?> = projectRepository.observeProject(projectId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun save(updated: CraftProject) = viewModelScope.launch { projectRepository.update(updated) }

    /** Step 5: stores the encrypted photo in the parents-only memories gallery and completes the project. */
    fun completeWithPhoto(jpeg: ByteArray, onDone: (medal: Boolean) -> Unit) = viewModelScope.launch {
        val p = project.value ?: return@launch
        val memoryId = memoryRepository.saveJpeg(childId, jpeg, "پروژه: ${p.title}", MemorySource.PROJECT)
        projectRepository.complete(p.id, memoryId)
        familyRepository.addStars(childId, 10)
        onDone(rewardRepository.award(childId, AchievementCatalog.CREATOR))
    }
}

private val projectIdeas = listOf("🛩️" to "ساخت هواپیمای چوبی", "🏠" to "خانه‌ی مقوایی", "🎨" to "نقاشی بزرگ", "🤖" to "ربات جعبه‌ای", "🧱" to "ساخت لگو", "🦋" to "پروانه‌ی کاغذی")

@Composable
fun ProjectsScreen(onBack: () -> Unit, onOpen: (Long) -> Unit, viewModel: ProjectsViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    TrackActivity(ActivityCategory.PROJECT, "پروژه‌های من")

    KidScreenScaffold(title = "پروژه‌های من", onBack = onBack) { padding ->
        Scaffold(
            modifier = Modifier.padding(padding),
            floatingActionButton = {
                ExtendedFloatingActionButton(onClick = { creating = true }, icon = { Icon(Icons.Rounded.Add, null) }, text = { Text("پروژه‌ی جدید") })
            },
        ) { inner ->
            LazyColumn(Modifier.fillMaxSize().padding(inner), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { MascotSpeech("داشبورد پروژه‌های ${space.child.name}: هر پروژه ۵ گام دارد؛ آخرش یک عکس یادگاری می‌گیریم! 📸", MascotMood.HAPPY) }
                if (projects.isEmpty()) item { EmptyState("هنوز پروژه‌ای نساختی. با دکمه‌ی پایین شروع کن!") }
                items(projects, key = { it.id }) { project ->
                    KodomoCard(onClick = { onOpen(project.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(project.emoji, fontSize = 40.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(project.title, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (project.isCompleted) "تمام شد ✅" else "گام ${(project.currentStep + 1).fa()}: ${CraftProject.stepTitles[project.currentStep]}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        StepIndicator(if (project.isCompleted) CraftProject.STEP_COUNT else project.currentStep)
                    }
                }
            }
        }
    }

    if (creating) {
        var title by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf("🛩️") }
        val templates = com.kodomo.app.domain.engine.ProjectTemplates.all.filter { it.minAge <= space.child.ageYears() + 1 }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text("یک پروژه انتخاب کن") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("با هم قدم به قدم می‌سازیم:", style = MaterialTheme.typography.labelLarge)
                    LazyColumn(Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(templates, key = { it.title }) { t ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .background(if (title == t.title) KodomoColors.MintDeep else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                                    .bouncyClick { title = t.title; emoji = t.emoji }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(t.emoji, fontSize = 30.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, style = MaterialTheme.typography.titleSmall)
                                    Text("⏱ حدود ${t.minutes.fa()} دقیقه • ${t.materials.size.fa()} وسیله", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                    Text("یا پروژه‌ی آزاد خودت:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(projectIdeas) { (e, _) ->
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .background(if (emoji == e) KodomoColors.MintDeep else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                    .bouncyClick { emoji = e },
                                contentAlignment = Alignment.Center,
                            ) { Text(e, fontSize = 22.sp) }
                        }
                    }
                    OutlinedTextField(title, { title = it.take(40) }, label = { Text("اسم پروژه") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(enabled = title.isNotBlank(), onClick = {
                    creating = false
                    viewModel.create(title, emoji, onOpen)
                }) { Text("شروع") }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text("انصراف") } },
        )
    }
}

@Composable
private fun StepIndicator(current: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        CraftProject.stepTitles.forEachIndexed { i, title ->
            val done = i < current
            val active = i == current
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(if (done) KodomoColors.Green else if (active) KodomoColors.MintDeep else MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .border(if (active) 2.dp else 0.dp, KodomoColors.Green, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(if (done) "✓" else (i + 1).fa(), color = if (done) Color.White else KodomoColors.GreenDark) }
                Text(title, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun ProjectWizardScreen(projectId: Long, onBack: () -> Unit, viewModel: ProjectWizardViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val context = LocalContext.current
    val project by viewModel.project.collectAsStateWithLifecycle()
    var celebrated by remember { mutableStateOf<Boolean?>(null) }
    val p = project
    TrackActivity(ActivityCategory.PROJECT, p?.title ?: "پروژه", "project_$projectId")

    KidScreenScaffold(title = p?.let { "${it.emoji} ${it.title}" } ?: "", onBack = onBack) { padding ->
        if (p == null) return@KidScreenScaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StepIndicator(if (p.isCompleted) CraftProject.STEP_COUNT else p.currentStep)
            if (p.isCompleted || celebrated != null) {
                MascotSpeech(
                    if (celebrated == true) "هورااا! مدال «سازنده‌ی خلاق» و ۱۰ ستاره گرفتی! 🏅" else "پروژه‌ات تمام شد و عکسش در گالری خاطرات خانواده ذخیره شد! 💚",
                    MascotMood.EXCITED,
                )
                KodomoPrimaryButton("بازگشت به پروژه‌ها", onClick = onBack)
            } else {
            val step = p.currentStep
            Text("${CraftProject.stepEmojis[step]} گام ${(step + 1).fa()}: ${CraftProject.stepTitles[step]}", style = MaterialTheme.typography.headlineSmall)
            val template = com.kodomo.app.domain.engine.ProjectTemplates.byTitle(p.title)
            var checkedAll by remember(step) { mutableStateOf(template == null) }
            if (template != null && step < CraftProject.STEP_COUNT - 1) {
                GuidedChecklist(step, template.phase(step), onAllChecked = { checkedAll = it })
                Text("یادداشت خودت (اختیاری):", style = MaterialTheme.typography.labelLarge)
            }
            when (step) {
                0 -> StepNotes(if (template != null) "چیز دیگری هم اضافه کردی؟" else "چه وسایلی لازم داری؟ (مثلاً مقوا، چسب، قیچی)", p.materials) { viewModel.save(p.copy(materials = it)) }
                1 -> StepNotes("چه قسمت‌هایی را باید ببری؟ یادت باشد با کمک بزرگ‌ترها از قیچی استفاده کنی ✂️", p.cuttingNotes) { viewModel.save(p.copy(cuttingNotes = it)) }
                2 -> StepNotes("قطعه‌ها را چطور به هم وصل کردی؟", p.assemblyNotes) { viewModel.save(p.copy(assemblyNotes = it)) }
                3 -> StepNotes("از چه رنگ‌هایی استفاده کردی؟ 🎨", p.paintingNotes) { viewModel.save(p.copy(paintingNotes = it)) }
                else -> PhotoStep { jpeg ->
                    viewModel.completeWithPhoto(jpeg) { medal ->
                        celebrated = medal
                        space.celebrate()
                        space.voice.speak(VoicePhrases.PROJECT_DONE)
                    }
                }
            }
            if (step < CraftProject.STEP_COUNT - 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { viewModel.save(p.copy(currentStep = step - 1)) }, enabled = step > 0, modifier = Modifier.weight(1f)) { Text("گام قبل") }
                    KodomoPrimaryButton(if (checkedAll) "ثبت گام ✅" else "اول همه را تیک بزن", enabled = checkedAll, onClick = {
                        viewModel.save(p.copy(currentStep = step + 1))
                        Toast.makeText(context, "گام ${(step + 1).fa()} ثبت شد!", Toast.LENGTH_SHORT).show()
                    }, modifier = Modifier.weight(1f))
                }
            }
            }
        }
    }
}

/** The template's sub-steps for this phase; each must be ticked (and can be read aloud) before moving on. */
@Composable
private fun GuidedChecklist(step: Int, items: List<String>, onAllChecked: (Boolean) -> Unit) {
    val space = LocalKidSpace.current
    val checked = remember(step) { androidx.compose.runtime.mutableStateListOf<Int>() }
    LaunchedEffect(checked.size) { onAllChecked(checked.size == items.size) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, text ->
            val on = i in checked
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(if (on) KodomoColors.Mint else MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                    .border(1.dp, if (on) KodomoColors.Green else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .bouncyClick { if (on) checked.remove(i) else { checked.add(i); if (checked.size == items.size) space.celebrate() } }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (on) "✅" else "⬜", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                androidx.compose.material3.IconButton(onClick = { space.voice.speak(text) }) { Text("🔊") }
            }
        }
    }
}

@Composable
private fun StepNotes(hint: String, value: String, onSave: (String) -> Unit) {
    var text by remember(hint) { mutableStateOf(value) }
    Text(hint, style = MaterialTheme.typography.bodyLarge)
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.take(400)
            onSave(text)
        },
        modifier = Modifier.fillMaxWidth(),
        minLines = 4,
        shape = RoundedCornerShape(18.dp),
    )
}

@Composable
private fun PhotoStep(onCaptured: (ByteArray) -> Unit) {
    val context = LocalContext.current
    val granted = rememberCameraPermission()
    val handle = remember { CameraCaptureHandle() }
    var busy by remember { mutableStateOf(false) }
    if (!granted) {
        Text("برای عکاسی اجازه‌ی دوربین لازم است.", textAlign = TextAlign.Center)
        return
    }
    Text("از کاردستی‌ات یک عکس قشنگ بگیر! عکس فقط برای مامان و بابا در گالری خاطرات ذخیره می‌شود.", style = MaterialTheme.typography.bodyLarge)
    Box(Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(28.dp)).background(Color.Black)) {
        CameraView(modifier = Modifier.fillMaxSize(), captureHandle = handle)
    }
    KodomoPrimaryButton(if (busy) "در حال ذخیره..." else "📸 عکس بگیر", enabled = !busy, onClick = {
        busy = true
        handle.capture(
            context,
            onJpeg = { onCaptured(it) },
            onError = { message ->
                busy = false
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            },
        )
    })
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = KodomoColors.Green)
        Spacer(Modifier.width(6.dp))
        Text("عکس‌ها رمزنگاری می‌شوند 🔒", style = MaterialTheme.typography.labelMedium)
    }
}
