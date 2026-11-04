package com.kodomo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kodomo.app.core.util.PersianCalendar
import com.kodomo.app.core.util.fa
import java.time.LocalDate

/** Solar Hijri (Jalali) date picker with three steppers: day, month, year. */
@Composable
fun PersianDatePickerDialog(
    initial: LocalDate,
    minYear: Int,
    maxYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val start = PersianCalendar.toJalali(initial)
    var year by remember { mutableIntStateOf(start.year.coerceIn(minYear, maxYear)) }
    var month by remember { mutableIntStateOf(start.month) }
    var day by remember { mutableIntStateOf(start.day) }
    val maxDay = PersianCalendar.daysInJalaliMonth(year, month)
    if (day > maxDay) day = maxDay

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب تاریخ تولد") },
        text = {
            Row(horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Stepper(day.fa(), onUp = { day = if (day >= maxDay) 1 else day + 1 }, onDown = { day = if (day <= 1) maxDay else day - 1 })
                Stepper(PersianCalendar.monthName(month), onUp = { month = if (month >= 12) 1 else month + 1 }, onDown = { month = if (month <= 1) 12 else month - 1 })
                Stepper(year.fa(), onUp = { if (year < maxYear) year++ }, onDown = { if (year > minYear) year-- })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(PersianCalendar.toGregorian(year, month, day)) }) { Text("تأیید") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
private fun Stepper(value: String, onUp: () -> Unit, onDown: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onUp) { Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "افزایش") }
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        IconButton(onClick = onDown) { Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "کاهش") }
    }
}

/** Material 3 clock picker wrapped in a dialog; result in minutes-of-day. */
@Composable
fun TimeOfDayPickerDialog(title: String, initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val state = rememberTimePickerState(initialHour = initialMinutes / 60, initialMinute = initialMinutes % 60, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(4.dp))
                TimePicker(state = state)
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(state.hour * 60 + state.minute) }) { Text("تأیید") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } },
    )
}

@Composable
fun LabeledValue(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}
