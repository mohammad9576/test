package com.kodomo.app.ui.kids.school

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.PersianCalendar
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.engine.AchievementCatalog
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.ClassSession
import com.kodomo.app.domain.model.Exam
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.domain.model.SchoolTask
import com.kodomo.app.domain.model.SchoolTaskKind
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.domain.repository.SchoolRepository
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.PersianDatePickerDialog
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.mascot.MascotSpeech
import com.kodomo.app.ui.theme.KodomoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class SchoolViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val schoolRepository: SchoolRepository,
    private val familyRepository: FamilyRepository,
    private val rewardRepository: RewardRepository,
) : ViewModel() {
    private val childId = savedState.childId
    val schedule: StateFlow<List<ClassSession>> = schoolRepository.observeSchedule(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tasks: StateFlow<List<SchoolTask>> = schoolRepository.observeTasks(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val exams: StateFlow<List<Exam>> = schoolRepository.observeExams(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addClass(day: DayOfWeek, subject: String, time: String) = viewModelScope.launch {
        val period = schedule.value.count { it.day == day } + 1
        schoolRepository.addClass(childId, day, period, subject, time)
    }

    fun deleteClass(id: Long) = viewModelScope.launch { schoolRepository.deleteClass(id) }

    fun addTask(kind: SchoolTaskKind, subject: String, title: String, due: LocalDate) = viewModelScope.launch {
        schoolRepository.addTask(childId, kind, subject, title, due)
    }

    fun toggleTask(task: SchoolTask) = viewModelScope.launch {
        schoolRepository.setTaskDone(task.id, !task.done)
        if (!task.done) {
            familyRepository.addStars(childId, 1)
            if (schoolRepository.countDoneTasks(childId) >= 5) rewardRepository.award(childId, AchievementCatalog.SCHOOL_STAR)
        }
    }

    fun deleteTask(id: Long) = viewModelScope.launch { schoolRepository.deleteTask(id) }

    fun addExam(subject: String, date: LocalDate, note: String) = viewModelScope.launch { schoolRepository.addExam(childId, subject, date, note) }

    fun deleteExam(id: Long) = viewModelScope.launch { schoolRepository.deleteExam(id) }
}

private enum class SchoolTab(val label: String) { SCHEDULE("برنامه هفتگی"), HOMEWORK("تکالیف"), EXAMS("امتحان‌ها"), PROJECTS("پروژه‌ها") }

@Composable
fun SchoolScreen(onBack: () -> Unit, viewModel: SchoolViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(SchoolTab.SCHEDULE) }
    var adding by remember { mutableStateOf(false) }
    TrackActivity(ActivityCategory.SCHOOL, "مدرسه من")

    val todayTasks = tasks.filter { !it.done }.size
    val nextExam = exams.firstOrNull { !it.date.isBefore(LocalDate.now()) }

    KidScreenScaffold(title = "مدرسه من", onBack = onBack) { padding ->
        Scaffold(
            modifier = Modifier.padding(padding),
            floatingActionButton = {
                FloatingActionButton(onClick = { adding = true }, containerColor = MaterialTheme.colorScheme.primary) {
                    Icon(Icons.Rounded.Add, contentDescription = "افزودن")
                }
            },
        ) { inner ->
            Column(Modifier.fillMaxSize().padding(inner)) {
                TabRow(selectedTabIndex = tab.ordinal) {
                    SchoolTab.entries.forEach { t -> Tab(selected = tab == t, onClick = { tab = t }, text = { Text(t.label, maxLines = 1) }) }
                }
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        val message = buildString {
                            append("${space.child.name} جان، ")
                            append(if (todayTasks == 0) "همه‌ی تکالیفت انجام شده! 🎉" else "${todayTasks.fa()} تکلیف انجام‌نشده داری.")
                            nextExam?.let {
                                val days = ChronoUnit.DAYS.between(LocalDate.now(), it.date)
                                append(" امتحان ${it.subject} ${if (days == 0L) "امروز" else "${days.fa()} روز دیگر"} است.")
                            }
                        }
                        MascotSpeech(message, if (todayTasks == 0) MascotMood.EXCITED else MascotMood.THINKING)
                    }
                    when (tab) {
                        SchoolTab.SCHEDULE -> {
                            PersianCalendar.iranianWeek.filter { it != DayOfWeek.FRIDAY }.forEach { day ->
                                val classes = schedule.filter { it.day == day }
                                item(key = "day_$day") {
                                    KodomoCard(Modifier.fillMaxWidth()) {
                                        Text(PersianCalendar.dayName(day), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                                        if (classes.isEmpty()) Text("کلاسی ثبت نشده", style = MaterialTheme.typography.bodySmall)
                                        classes.forEach { c ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text("${c.period.fa()}. ${c.subject}", modifier = Modifier.weight(1f))
                                                Text(c.startTime.fa(), style = MaterialTheme.typography.labelMedium)
                                                IconButton(onClick = { viewModel.deleteClass(c.id) }) { Icon(Icons.Rounded.Delete, contentDescription = "حذف") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        SchoolTab.HOMEWORK, SchoolTab.PROJECTS -> {
                            val kind = if (tab == SchoolTab.HOMEWORK) SchoolTaskKind.HOMEWORK else SchoolTaskKind.PROJECT
                            val list = tasks.filter { it.kind == kind }
                            if (list.isEmpty()) item { EmptyState("هنوز ${kind.label}ی ثبت نکرده‌ای. با دکمه‌ی + اضافه کن.") }
                            items(list, key = { it.id }) { task -> TaskRow(task, onToggle = { viewModel.toggleTask(task) }, onDelete = { viewModel.deleteTask(task.id) }) }
                        }
                        SchoolTab.EXAMS -> {
                            if (exams.isEmpty()) item { EmptyState("امتحانی در تقویم نیست.") }
                            items(exams, key = { it.id }) { exam ->
                                KodomoCard(Modifier.fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text("📝 ${exam.subject}", style = MaterialTheme.typography.titleMedium)
                                            Text(PersianCalendar.format(exam.date), style = MaterialTheme.typography.bodySmall)
                                            if (exam.note.isNotBlank()) Text(exam.note, style = MaterialTheme.typography.bodySmall)
                                        }
                                        IconButton(onClick = { viewModel.deleteExam(exam.id) }) { Icon(Icons.Rounded.Delete, contentDescription = "حذف") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        AddSchoolItemDialog(
            tab = tab,
            onDismiss = { adding = false },
            onAddClass = { day, subject, time -> viewModel.addClass(day, subject, time); adding = false },
            onAddTask = { kind, subject, title, due -> viewModel.addTask(kind, subject, title, due); adding = false },
            onAddExam = { subject, date, note -> viewModel.addExam(subject, date, note); adding = false },
        )
    }
}

@Composable
private fun TaskRow(task: SchoolTask, onToggle: () -> Unit, onDelete: () -> Unit) {
    val overdue = !task.done && task.dueDate.isBefore(LocalDate.now())
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (task.done) KodomoColors.Mint else MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle() }, colors = CheckboxDefaults.colors(checkedColor = KodomoColors.Green))
        Column(Modifier.weight(1f)) {
            Text(task.title, style = MaterialTheme.typography.titleSmall, textDecoration = if (task.done) TextDecoration.LineThrough else null)
            Text(
                "${task.subject} • تا ${PersianCalendar.format(task.dueDate)}${if (overdue) " (گذشته)" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "حذف") }
    }
}

@Composable
private fun AddSchoolItemDialog(
    tab: SchoolTab,
    onDismiss: () -> Unit,
    onAddClass: (DayOfWeek, String, String) -> Unit,
    onAddTask: (SchoolTaskKind, String, String, LocalDate) -> Unit,
    onAddExam: (String, LocalDate, String) -> Unit,
) {
    var subject by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("08:00") }
    var dayIndex by remember { mutableIntStateOf(0) }
    var date by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var pickDate by remember { mutableStateOf(false) }
    val days = PersianCalendar.iranianWeek.filter { it != DayOfWeek.FRIDAY }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(when (tab) { SchoolTab.SCHEDULE -> "کلاس جدید"; SchoolTab.HOMEWORK -> "تکلیف جدید"; SchoolTab.EXAMS -> "امتحان جدید"; SchoolTab.PROJECTS -> "پروژه‌ی جدید" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(subject, { subject = it.take(30) }, label = { Text("درس (مثلاً ریاضی)") }, singleLine = true)
                when (tab) {
                    SchoolTab.SCHEDULE -> {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(days.size) { i -> FilterChip(selected = dayIndex == i, onClick = { dayIndex = i }, label = { Text(PersianCalendar.dayName(days[i])) }) }
                        }
                        OutlinedTextField(time, { time = it.take(5) }, label = { Text("ساعت شروع") }, singleLine = true)
                    }
                    else -> {
                        OutlinedTextField(title, { title = it.take(80) }, label = { Text(if (tab == SchoolTab.EXAMS) "توضیح" else "عنوان") })
                        OutlinedButton(onClick = { pickDate = true }) { Text("📅 ${PersianCalendar.format(date)}") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = subject.isNotBlank() && (tab == SchoolTab.SCHEDULE || tab == SchoolTab.EXAMS || title.isNotBlank()),
                onClick = {
                    when (tab) {
                        SchoolTab.SCHEDULE -> onAddClass(days[dayIndex], subject, time)
                        SchoolTab.HOMEWORK -> onAddTask(SchoolTaskKind.HOMEWORK, subject, title, date)
                        SchoolTab.PROJECTS -> onAddTask(SchoolTaskKind.PROJECT, subject, title, date)
                        SchoolTab.EXAMS -> onAddExam(subject, date, title)
                    }
                },
            ) { Text("افزودن") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
    if (pickDate) {
        val year = PersianCalendar.toJalali(LocalDate.now()).year
        PersianDatePickerDialog(initial = date, minYear = year, maxYear = year + 1, onDismiss = { pickDate = false }, onConfirm = { date = it; pickDate = false })
    }
}

