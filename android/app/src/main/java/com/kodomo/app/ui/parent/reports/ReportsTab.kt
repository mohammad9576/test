package com.kodomo.app.ui.parent.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import com.kodomo.app.ui.theme.KodomoColors
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodomo.app.core.util.PersianCalendar
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.ActivityLog
import com.kodomo.app.ui.components.DonutChart
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.Legend
import com.kodomo.app.ui.components.SectionTitle
import com.kodomo.app.ui.components.StackedBar
import com.kodomo.app.ui.components.StackedBarChart
import com.kodomo.app.ui.components.categoryColor
import com.kodomo.app.ui.parent.ChildSwitcher
import com.kodomo.app.ui.parent.ParentHubViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Merges consecutive log segments of the same activity into a single timeline entry. */
private fun categoryIcon(c: ActivityCategory): androidx.compose.ui.graphics.vector.ImageVector = when (c) {
    ActivityCategory.VIDEO -> androidx.compose.material.icons.Icons.Outlined.PlayCircle
    ActivityCategory.BOOK -> androidx.compose.material.icons.Icons.AutoMirrored.Outlined.MenuBook
    ActivityCategory.GAME -> androidx.compose.material.icons.Icons.Outlined.SportsEsports
    ActivityCategory.MUSIC -> androidx.compose.material.icons.Icons.Outlined.MusicNote
    ActivityCategory.LEARNING, ActivityCategory.SCHOOL -> androidx.compose.material.icons.Icons.Outlined.School
    ActivityCategory.EXPLORE, ActivityCategory.BROWSER -> androidx.compose.material.icons.Icons.Outlined.Language
    ActivityCategory.PROJECT, ActivityCategory.ART -> androidx.compose.material.icons.Icons.Outlined.Palette
    ActivityCategory.APP -> androidx.compose.material.icons.Icons.Outlined.Apps
    else -> androidx.compose.material.icons.Icons.Outlined.Home
}

private fun mergeTimeline(logs: List<ActivityLog>): List<ActivityLog> {
    val result = mutableListOf<ActivityLog>()
    for (log in logs) {
        val last = result.lastOrNull()
        if (last != null && last.title == log.title && last.category == log.category &&
            last.startedAt.epochSecond - (log.startedAt.epochSecond + log.durationSeconds) < 120
        ) {
            result[result.lastIndex] = last.copy(startedAt = log.startedAt, durationSeconds = last.durationSeconds + log.durationSeconds)
        } else {
            result += log
        }
    }
    return result
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ReportsTab(viewModel: ParentHubViewModel, modifier: Modifier = Modifier) {
    val children by viewModel.children.collectAsStateWithLifecycle()
    val child by viewModel.selectedChild.collectAsStateWithLifecycle()
    val week by viewModel.weekUsage.collectAsStateWithLifecycle()
    val timelineRaw by viewModel.timeline.collectAsStateWithLifecycle()
    val timeline = remember(timelineRaw) { mergeTimeline(timelineRaw).filter { it.category != ActivityCategory.HOME || it.durationSeconds >= 60 } }
    val tracked = listOf(ActivityCategory.VIDEO, ActivityCategory.BOOK, ActivityCategory.GAME, ActivityCategory.MUSIC, ActivityCategory.LEARNING, ActivityCategory.SCHOOL, ActivityCategory.EXPLORE, ActivityCategory.BROWSER, ActivityCategory.PROJECT, ActivityCategory.ART, ActivityCategory.APP)
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }

    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ChildSwitcher(children, child, viewModel::selectChild) }
        if (child == null) {
            item { EmptyState("فرزندی ثبت نشده است.") }
            return@LazyColumn
        }
        val today = week.lastOrNull()
        item {
            // V5 (D_PARENT report): one segmented choice, simple bars, two summary numbers, category rows.
            val weeklyState = androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
            val weekly = weeklyState.value
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                Text("گزارش ${child?.name.orEmpty()}", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                com.kodomo.app.ui.components.SegmentedChoice(listOf("امروز", "هفته"), if (weekly) 1 else 0, { weeklyState.value = it == 1 })
                Spacer(Modifier.height(12.dp))
                val days = if (weekly) week else listOfNotNull(today)
                val byCat = tracked.map { cat -> cat to days.sumOf { d -> d.byCategory.firstOrNull { it.category == cat }?.seconds ?: 0L } }
                    .filter { it.second > 0 }.sortedByDescending { it.second }
                KodomoCard(Modifier.fillMaxWidth()) {
                    if (weekly) {
                        val maxMin = (week.maxOfOrNull { it.totalSeconds } ?: 0L).coerceAtLeast(60L) / 60f
                        Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                            week.forEachIndexed { i, d ->
                                val m = d.totalSeconds / 60f
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom, modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    Box(
                                        Modifier.width(22.dp).fillMaxHeight((m / maxMin).coerceIn(0.03f, 1f) * 0.8f)
                                            .background(if (i == week.lastIndex) KodomoColors.Green else KodomoColors.Green.copy(alpha = 0.35f), androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
                                    )
                                    Text(PersianCalendar.dayName(d.date.dayOfWeek).take(3), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        val total = week.sumOf { it.totalSeconds }
                        Row(Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f)) {
                                Text("میانگین روزانه", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${(total / 60 / week.size.coerceAtLeast(1)).fa()} دقیقه", style = MaterialTheme.typography.titleLarge)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("مجموع هفته", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(PersianCalendar.formatDuration(total), style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    } else {
                        val total = today?.byCategory?.filter { it.category != ActivityCategory.HOME }?.sumOf { it.seconds } ?: 0L
                        Text("مجموع امروز", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (total == 0L) "هنوز فعالیتی نبوده" else PersianCalendar.formatDuration(total), style = MaterialTheme.typography.headlineMedium)
                    }
                }
                if (byCat.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    com.kodomo.app.ui.components.SettingsGroup {
                        byCat.forEachIndexed { i, (cat, sec) ->
                            com.kodomo.app.ui.components.SettingsRow(
                                icon = categoryIcon(cat), tint = categoryColor(cat), title = cat.label,
                                value = PersianCalendar.formatDuration(sec), divider = i < byCat.lastIndex,
                            )
                        }
                    }
                }
            }
        }
        item { SectionTitle("تایم‌لاین فعالیت‌های اخیر", Modifier.padding(horizontal = 16.dp)) }
        if (timeline.isEmpty()) item { Text("فعالیتی ثبت نشده است.", Modifier.padding(16.dp)) }
        items(timeline, key = { it.id }) { log ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).background(categoryColor(log.category), CircleShape))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("${log.category.emoji} ${log.title}", style = MaterialTheme.typography.titleSmall)
                    val zoned = log.startedAt.atZone(ZoneId.systemDefault())
                    Text(
                        "${PersianCalendar.format(zoned.toLocalDate())} • ${zoned.format(timeFormat).fa()} • ${PersianCalendar.formatDuration(log.durationSeconds)}",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
    }
}
