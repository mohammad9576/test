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

@Composable
fun ReportsTab(viewModel: ParentHubViewModel, modifier: Modifier = Modifier) {
    val children by viewModel.children.collectAsStateWithLifecycle()
    val child by viewModel.selectedChild.collectAsStateWithLifecycle()
    val week by viewModel.weekUsage.collectAsStateWithLifecycle()
    val timelineRaw by viewModel.timeline.collectAsStateWithLifecycle()
    val timeline = remember(timelineRaw) { mergeTimeline(timelineRaw).filter { it.category != ActivityCategory.HOME || it.durationSeconds >= 60 } }
    val tracked = listOf(ActivityCategory.VIDEO, ActivityCategory.BOOK, ActivityCategory.GAME, ActivityCategory.MUSIC, ActivityCategory.LEARNING, ActivityCategory.SCHOOL, ActivityCategory.EXPLORE, ActivityCategory.BROWSER, ActivityCategory.PROJECT)
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }

    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ChildSwitcher(children, child, viewModel::selectChild) }
        if (child == null) {
            item { EmptyState("فرزندی ثبت نشده است.") }
            return@LazyColumn
        }
        val today = week.lastOrNull()
        item {
            KodomoCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                SectionTitle("مصرف امروز به تفکیک")
                val slices = today?.byCategory?.filter { it.category != ActivityCategory.HOME }.orEmpty()
                if (slices.isEmpty()) {
                    Text("امروز هنوز فعالیتی ثبت نشده است.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    DonutChart(
                        values = slices.map { Triple("${it.category.emoji} ${it.category.label}: ${(it.seconds / 60).fa()} دقیقه", categoryColor(it.category), it.seconds.toFloat()) },
                        centerLabel = PersianCalendar.formatDuration(slices.sumOf { it.seconds }),
                    )
                }
            }
        }
        item {
            KodomoCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
                SectionTitle("۷ روز گذشته (دقیقه)")
                StackedBarChart(
                    bars = week.map { day ->
                        StackedBar(
                            label = PersianCalendar.dayName(day.date.dayOfWeek).take(3),
                            segments = tracked.map { cat -> categoryColor(cat) to ((day.byCategory.firstOrNull { it.category == cat }?.seconds ?: 0L) / 60f) },
                        )
                    },
                )
                Spacer(Modifier.height(8.dp))
                Legend(tracked.map { it.label to categoryColor(it) })
                val total = week.sumOf { it.totalSeconds }
                Spacer(Modifier.height(8.dp))
                Text("میانگین روزانه: ${(total / 60 / week.size.coerceAtLeast(1)).fa()} دقیقه", style = MaterialTheme.typography.titleSmall)
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
