package com.kodomo.app.ui.kids.school

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
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
    val planner: com.kodomo.app.data.prefs.PlannerStore,
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

private enum class SchoolTab(val label: String) { TODAY("امروز"), SCHEDULE("هفتگی"), HOMEWORK("تکالیف"), EXAMS("امتحان"), PROJECTS("پروژه") }

@Composable
fun SchoolScreen(onBack: () -> Unit, viewModel: SchoolViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val exams by viewModel.exams.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(SchoolTab.TODAY) }
    val routine by remember(space.child.id) { viewModel.planner.routine(space.child.id, space.child.ageYears()) }.collectAsStateWithLifecycle(emptyList())
    val doneToday by remember(space.child.id) { viewModel.planner.doneToday(space.child.id) }.collectAsStateWithLifecycle(emptySet())
    val plannerScope = androidx.compose.runtime.rememberCoroutineScope()
    var adding by remember { mutableStateOf(false) }
    TrackActivity(ActivityCategory.SCHOOL, "برنامه‌ی من")

    val todayTasks = tasks.filter { !it.done }.size
    val nextExam = exams.firstOrNull { !it.date.isBefore(LocalDate.now()) }

    KidScreenScaffold(title = "برنامه‌ی من", onBack = onBack) { padding ->
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
                        SchoolTab.TODAY -> {
                            item(key = "today") {
                                TodayPlan(
                                    routine = routine,
                                    done = doneToday,
                                    homeworkLeft = todayTasks,
                                    nextExam = nextExam?.let { it.subject to ChronoUnit.DAYS.between(LocalDate.now(), it.date) },
                                    onToggle = { item ->
                                        val wasDone = item.id in doneToday
                                        plannerScope.launch { viewModel.planner.toggle(space.child.id, item) }
                                        if (!wasDone) { com.kodomo.app.ui.fx.GameSfx.sparkle(); space.voice.speak("آفرین! ${item.title}") }
                                    },
                                    onRemove = { item -> plannerScope.launch { viewModel.planner.save(space.child.id, routine - item) } },
                                    onFocusDone = { space.celebrate(); space.voice.speak("آفرین! بیست دقیقه تمرکز کردی. حالا پنج دقیقه استراحت کن.") },
                                )
                            }
                        }
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

    if (adding && tab == SchoolTab.TODAY) {
        AddRoutineDialog(onDismiss = { adding = false }) { item ->
            plannerScope.launch { viewModel.planner.save(space.child.id, routine + item) }
            adding = false
        }
    } else if (adding) {
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
        title = { Text(when (tab) { SchoolTab.SCHEDULE, SchoolTab.TODAY -> "کلاس جدید"; SchoolTab.HOMEWORK -> "تکلیف جدید"; SchoolTab.EXAMS -> "امتحان جدید"; SchoolTab.PROJECTS -> "پروژه‌ی جدید" }) },
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
                        SchoolTab.SCHEDULE, SchoolTab.TODAY -> onAddClass(days[dayIndex], subject, time)
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



/** Today at a glance: what is now and next, tick-off routine, homework/exam reminders and a 20-minute focus timer. */
@Composable
private fun TodayPlan(
    routine: List<com.kodomo.app.data.prefs.RoutineItem>,
    done: Set<String>,
    homeworkLeft: Int,
    nextExam: Pair<String, Long>?,
    onToggle: (com.kodomo.app.data.prefs.RoutineItem) -> Unit,
    onRemove: (com.kodomo.app.data.prefs.RoutineItem) -> Unit,
    onFocusDone: () -> Unit,
) {
    var nowMinute by remember { mutableStateOf(java.time.LocalTime.now().let { it.hour * 60 + it.minute }) }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); nowMinute = java.time.LocalTime.now().let { it.hour * 60 + it.minute } } }
    val current = routine.lastOrNull { it.minute <= nowMinute }
    val next = routine.firstOrNull { it.minute > nowMinute }
    fun hhmm(m: Int) = "%02d:%02d".format(m / 60, m % 60).fa()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("الان", current?.let { "${it.emoji} ${it.title}" } ?: "—", androidx.compose.ui.graphics.Color(0xFFE3F2FD), Modifier.weight(1f))
            InfoTile("بعدی", next?.let { "${it.emoji} ${hhmm(it.minute)} ${it.title}" } ?: "پایان روز 🌙", androidx.compose.ui.graphics.Color(0xFFFFF8E1), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("تکالیف", if (homeworkLeft == 0) "همه انجام شده ✅" else "${homeworkLeft.fa()} تکلیف مانده 📚", androidx.compose.ui.graphics.Color(0xFFE8F5E9), Modifier.weight(1f))
            InfoTile("امتحان", nextExam?.let { (s, d) -> if (d <= 0L) "امروز: $s 🍀" else "$s تا ${d.fa()} روز دیگر" } ?: "امتحانی نیست", androidx.compose.ui.graphics.Color(0xFFFCE4EC), Modifier.weight(1f))
        }
        if (nextExam != null && nextExam.second in 1L..3L) {
            Text("💡 پیشنهاد امروز: ۲۰ دقیقه درس «${nextExam.first}» را مرور کن.", style = MaterialTheme.typography.bodyMedium)
        }
        FocusTimer(onFocusDone)
        Text("برنامه‌ی روزانه (${done.count { d -> routine.any { it.id == d } }.fa()} از ${routine.size.fa()} ✓)", style = MaterialTheme.typography.titleMedium)
        routine.forEach { item ->
            val isDone = item.id in done
            val isNow = item == current
            Row(
                Modifier.fillMaxWidth()
                    .background(
                        when { isDone -> androidx.compose.ui.graphics.Color(0xFFE8F5E9); isNow -> androidx.compose.ui.graphics.Color(0xFFFFF3E0); else -> MaterialTheme.colorScheme.surface },
                        androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                    )
                    .clickable { onToggle(item) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(hhmm(item.minute), style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(52.dp))
                com.kodomo.app.ui.fx.Emoji(item.emoji, 32.dp)
                Text(item.title, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.bodyLarge, fontWeight = if (isNow) androidx.compose.ui.text.font.FontWeight.Bold else null)
                Text(if (isDone) "✅" else "⬜", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { onRemove(item) }) { Icon(Icons.Rounded.Delete, contentDescription = "حذف", modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun InfoTile(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Column(modifier.background(color, androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).padding(12.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleSmall, maxLines = 2)
    }
}

/** A 20-minute focus timer for homework (then a 5-minute break). */
@Composable
private fun FocusTimer(onDone: () -> Unit) {
    var left by remember { mutableStateOf(0) }
    androidx.compose.runtime.LaunchedEffect(left > 0) {
        while (left > 0) { kotlinx.coroutines.delay(1000); left--; if (left == 0) onDone() }
    }
    Row(
        Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Color(0xFFEDE7F6), androidx.compose.foundation.shape.RoundedCornerShape(18.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.kodomo.app.ui.fx.Emoji("⏳", 36.dp)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text("زمان تمرکز", style = MaterialTheme.typography.titleSmall)
            Text(if (left > 0) "%02d:%02d".format(left / 60, left % 60).fa() + " — گوشی را کنار بگذار و درس بخوان" else "۲۰ دقیقه درس، بعد ۵ دقیقه استراحت", style = MaterialTheme.typography.bodySmall)
        }
        androidx.compose.material3.TextButton(onClick = { left = if (left > 0) 0 else 20 * 60 }) { Text(if (left > 0) "توقف" else "شروع") }
    }
}

@Composable
private fun AddRoutineDialog(onDismiss: () -> Unit, onAdd: (com.kodomo.app.data.prefs.RoutineItem) -> Unit) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("⭐") }
    val emojis = listOf("⭐", "📚", "🏃", "🎹", "🎨", "🧹", "🍎", "💧", "🛁", "📖", "🙏", "👪")
    val minute = time.map { if (it in '۰'..'۹') '0' + (it - '۰') else it }.joinToString("").split(':').let { p ->
        val h = p.getOrNull(0)?.trim()?.toIntOrNull(); val m = p.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
        if (h != null && h in 0..23 && m in 0..59) h * 60 + m else null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("کار تازه در برنامه‌ی روزانه") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it.take(40) }, label = { Text("چه کاری؟ (مثلاً تمرین پیانو)") }, singleLine = true)
                OutlinedTextField(time, { time = it.take(5) }, label = { Text("ساعت (مثلاً ۱۷:۳۰)") }, singleLine = true)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(emojis.size) { i -> FilterChip(selected = emoji == emojis[i], onClick = { emoji = emojis[i] }, label = { Text(emojis[i]) }) }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(enabled = title.isNotBlank() && minute != null, onClick = { onAdd(com.kodomo.app.data.prefs.RoutineItem(minute!!, emoji, title.trim())) }) { Text("افزودن") }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}
