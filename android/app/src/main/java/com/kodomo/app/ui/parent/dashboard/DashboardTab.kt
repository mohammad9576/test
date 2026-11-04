package com.kodomo.app.ui.parent.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodomo.app.core.util.PersianCalendar
import com.kodomo.app.core.util.fa
import com.kodomo.app.data.remote.LiveUsageSocket
import com.kodomo.app.domain.model.ParentalSettings
import com.kodomo.app.kiosk.LocalKioskController
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.SectionTitle
import com.kodomo.app.ui.components.TimeOfDayPickerDialog
import com.kodomo.app.ui.parent.ChildSwitcher
import com.kodomo.app.ui.parent.ParentHubViewModel
import com.kodomo.app.ui.theme.KodomoColors

@Composable
fun DashboardTab(viewModel: ParentHubViewModel, modifier: Modifier = Modifier) {
    val children by viewModel.children.collectAsStateWithLifecycle()
    val child by viewModel.selectedChild.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val used by viewModel.usedTodaySeconds.collectAsStateWithLifecycle()
    val redemptions by viewModel.redemptions.collectAsStateWithLifecycle()
    val session by viewModel.sessionState.collectAsStateWithLifecycle()
    val live by viewModel.liveStatus.collectAsStateWithLifecycle()
    val kiosk = LocalKioskController.current
    var timePicker by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier, contentPadding = PaddingValues(bottom = 24.dp)) {
        item { ChildSwitcher(children, child, viewModel::selectChild) }
        val s = settings
        val c = child
        if (c == null || s == null) {
            item { EmptyState("ابتدا از تب «خانواده» یک فرزند اضافه کنید.") }
            return@LazyColumn
        }
        item {
            KodomoCard(Modifier.fillMaxWidth().padding16()) {
                Text("مصرف امروز ${c.name}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                val usedMin = used / 60
                Text("${usedMin.fa()} از ${s.dailyLimitMinutes.fa()} دقیقه", style = MaterialTheme.typography.headlineSmall, color = KodomoColors.Green)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { (usedMin.toFloat() / s.dailyLimitMinutes).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(10.dp))
                Spacer(Modifier.height(6.dp))
                Text(
                    when (live) {
                        LiveUsageSocket.Status.CONNECTED -> "🟢 پایش زنده‌ی ابری فعال است"
                        LiveUsageSocket.Status.CONNECTING -> "🟡 در حال اتصال به پایش زنده..."
                        LiveUsageSocket.Status.OFFLINE -> "⚪ پایش زنده خاموش (حساب ابری متصل نیست)"
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        item {
            KodomoCard(Modifier.fillMaxWidth().padding16()) {
                SectionTitle("سقف مصرف روزانه")
                var limit by remember(s.childId, s.dailyLimitMinutes) { mutableFloatStateOf(s.dailyLimitMinutes.toFloat()) }
                Text("${limit.toInt().fa()} دقیقه", style = MaterialTheme.typography.headlineSmall)
                Slider(
                    value = limit,
                    onValueChange = { limit = it },
                    onValueChangeFinished = { viewModel.updateSettings { it.copy(dailyLimitMinutes = limit.toInt()) } },
                    valueRange = ParentalSettings.MIN_LIMIT.toFloat()..ParentalSettings.MAX_LIMIT.toFloat(),
                    steps = (ParentalSettings.MAX_LIMIT - ParentalSettings.MIN_LIMIT) / 5 - 1,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("۵ دقیقه", style = MaterialTheme.typography.labelSmall)
                    Text("۱۲۰ دقیقه", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            KodomoCard(Modifier.fillMaxWidth().padding16()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ساعت خواب خودکار 🌙", style = MaterialTheme.typography.titleMedium)
                        Text("قفل صفحه و کاهش نور در ساعت خواب", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = s.bedtimeEnabled, onCheckedChange = { on -> viewModel.updateSettings { it.copy(bedtimeEnabled = on) } })
                }
                if (s.bedtimeEnabled) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { timePicker = "start" }, modifier = Modifier.weight(1f)) {
                            Text("شروع: ${PersianCalendar.formatMinutesOfDay(s.bedtimeStartMinutes)}")
                        }
                        OutlinedButton(onClick = { timePicker = "end" }, modifier = Modifier.weight(1f)) {
                            Text("پایان: ${PersianCalendar.formatMinutesOfDay(s.bedtimeEndMinutes)}")
                        }
                    }
                }
            }
        }
        item {
            KodomoCard(Modifier.fillMaxWidth().padding16()) {
                SectionTitle("سطح مجاز محتوا (۱ تا ۵)")
                ThresholdSlider("خشونت", s.maxViolence) { v -> viewModel.updateSettings { it.copy(maxViolence = v) } }
                ThresholdSlider("ترس", s.maxFear) { v -> viewModel.updateSettings { it.copy(maxFear = v) } }
                ThresholdSlider("ناهنجاری", s.maxAbnormality) { v -> viewModel.updateSettings { it.copy(maxAbnormality = v) } }
                Text("محتوایی که امتیاز آن از این سطوح بالاتر باشد در محیط ${c.name} نمایش داده نمی‌شود.", style = MaterialTheme.typography.bodySmall)
            }
        }
        val pending = redemptions.filter { !it.fulfilled }
        if (pending.isNotEmpty()) {
            item { SectionTitle("درخواست‌های جایزه", Modifier.padding16()) }
            items(pending, key = { it.id }) { r ->
                KodomoCard(Modifier.fillMaxWidth().padding16()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.reward.emoji, fontSize = 30.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${r.childName}: ${r.reward.title}", style = MaterialTheme.typography.titleSmall)
                            Text(PersianCalendar.format(r.redeemedAt.atZone(java.time.ZoneId.systemDefault()).toLocalDate()), style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { viewModel.markFulfilled(r.id) }) { Text("انجام شد ✅") }
                    }
                }
            }
        }
        item {
            KodomoCard(Modifier.fillMaxWidth().padding16()) {
                SectionTitle("قفل امن کیوسک (Zero-Escape)")
                val owner = viewModel.deviceOwner.isDeviceOwner
                Text(
                    if (owner) "✅ کدومو مالک دستگاه (Device Owner) است؛ قفل کامل فعال‌پذیر است."
                    else "⚠️ کدومو مالک دستگاه نیست. قفل در حالت سنجاق صفحه (Screen Pinning) کار می‌کند. برای قفل کامل دستور زیر را یک بار با ADB اجرا کنید:",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (!owner) {
                    Text(
                        "adb shell dpm set-device-owner com.kodomo.app/.kiosk.KodomoDeviceAdminReceiver",
                        style = MaterialTheme.typography.labelSmall,
                        color = KodomoColors.GreenDark,
                        modifier = Modifier.padding(vertical = 6.dp),
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("فعال‌سازی قفل محیط کودک", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = session?.kioskEnabled == true,
                        onCheckedChange = { on -> viewModel.setKioskEnabled(on, kiosk::applyDeviceOwnerPolicies) },
                    )
                }
                if (!owner && !viewModel.deviceOwner.isAdminActive) {
                    Button(onClick = kiosk::requestDeviceAdmin) { Text("فعال‌سازی مدیر دستگاه") }
                }
                if (owner) {
                    Text("محدودیت‌های اعمال‌شده: حالت امن، بازنشانی کارخانه، نصب/حذف برنامه، حافظه‌ی USB، پنجره‌های شناور، تغییر ساعت.", style = MaterialTheme.typography.labelSmall)
                    TextButton(onClick = viewModel::releaseDeviceOwner) { Text("حذف کامل حالت مالک دستگاه", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }

    val s = settings
    when (timePicker) {
        "start" -> if (s != null) TimeOfDayPickerDialog("شروع ساعت خواب", s.bedtimeStartMinutes, { timePicker = null }) { m ->
            timePicker = null
            viewModel.updateSettings { it.copy(bedtimeStartMinutes = m) }
        }
        "end" -> if (s != null) TimeOfDayPickerDialog("پایان ساعت خواب", s.bedtimeEndMinutes, { timePicker = null }) { m ->
            timePicker = null
            viewModel.updateSettings { it.copy(bedtimeEndMinutes = m) }
        }
    }
}

@Composable
private fun ThresholdSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    var v by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(72.dp), style = MaterialTheme.typography.titleSmall)
        Slider(value = v, onValueChange = { v = it }, onValueChangeFinished = { onChange(v.toInt()) }, valueRange = 1f..5f, steps = 3, modifier = Modifier.weight(1f))
        Text(v.toInt().fa(), modifier = Modifier.width(24.dp), style = MaterialTheme.typography.titleMedium)
    }
}

private fun Modifier.padding16(): Modifier = this.padding(horizontal = 16.dp, vertical = 6.dp)
