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
        item { child?.let { SetupChecklistCard(viewModel, it) } }
        item { PendingChoresCard(viewModel, children) }
        item { child?.let { BehaviourCard(viewModel, it) } }
        item { child?.let { StudyCard(viewModel, it) } }
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
        item { GuardSetupCard() }
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
                var isHome by remember { mutableStateOf(viewModel.deviceOwner.isDefaultHome) }
                androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    isHome = viewModel.deviceOwner.isDefaultHome
                }
                if (!isHome) {
                    Text(
                        "⚠️ کدومو هنوز لانچر پیش‌فرض گوشی نیست؛ به همین دلیل بعد از ری‌استارت، صفحه‌ی اصلی شیائومی/سامسونگ باز می‌شود. دکمه‌ی زیر را بزنید و «کدومو» را به‌عنوان برنامه‌ی خانه (Home) انتخاب کنید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = kiosk::requestDefaultHome, modifier = Modifier.fillMaxWidth()) { Text("تنظیم کدومو به‌عنوان لانچر پیش‌فرض") }
                    Text(
                        "در شیائومی اگر پنجره‌ای باز نشد: تنظیمات ← برنامه‌ها ← برنامه‌های پیش‌فرض ← لانچر ← کدومو",
                        style = MaterialTheme.typography.labelSmall,
                    )
                } else {
                    Text("✅ کدومو لانچر پیش‌فرض است و بعد از ری‌استارت خودکار باز می‌شود.", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("فعال‌سازی قفل محیط کودک", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = session?.kioskEnabled == true,
                        onCheckedChange = { on ->
                            viewModel.setKioskEnabled(on, kiosk::applyDeviceOwnerPolicies)
                            if (on) kiosk.requestDefaultHome()
                        },
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

/**
 * Checklist of the special permissions that keep the child inside Kodomo without Device Owner:
 * default launcher, usage access (app guard), display over other apps (return to Kodomo), Xiaomi extras.
 */
@Composable
private fun GuardSetupCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val kiosk = LocalKioskController.current
    var usage by remember { mutableStateOf(com.kodomo.app.kiosk.KioskPermissions.hasUsageAccess(context)) }
    var overlay by remember { mutableStateOf(com.kodomo.app.kiosk.KioskPermissions.canDrawOverlays(context)) }
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        usage = com.kodomo.app.kiosk.KioskPermissions.hasUsageAccess(context)
        overlay = com.kodomo.app.kiosk.KioskPermissions.canDrawOverlays(context)
    }
    fun open(intent: android.content.Intent) {
        runCatching { context.startActivity(intent) }.onFailure {
            runCatching { context.startActivity(com.kodomo.app.kiosk.KioskPermissions.appDetailsIntent(context)) }
        }
    }
    KodomoCard(Modifier.fillMaxWidth().padding16()) {
        SectionTitle("نگهبان برنامه‌ها")
        Text(
            "برای اینکه کودک با دکمه‌ی خانه، برنامه‌های اخیر یا بستن یک بازی از محیط کودک بیرون نرود و زمانش دقیق کنترل شود، این مجوزها را یک بار فعال کنید:",
            style = MaterialTheme.typography.bodySmall,
        )
        PermissionRow("۱. دسترسی به آمار استفاده (Usage access)", usage) { open(com.kodomo.app.kiosk.KioskPermissions.usageAccessIntent(context)) }
        PermissionRow("۲. نمایش روی سایر برنامه‌ها", overlay) { open(com.kodomo.app.kiosk.KioskPermissions.overlayIntent(context)) }
        if (com.kodomo.app.kiosk.KioskPermissions.isXiaomi) {
            PermissionRow("۳. شیائومی: «اجرای خودکار» و «باز کردن پنجره‌ها هنگام اجرا در پس‌زمینه»", null) {
                open(com.kodomo.app.kiosk.KioskPermissions.xiaomiPermissionsIntent(context))
            }
        }
        PermissionRow("لانچر پیش‌فرض", null) { kiosk.requestDefaultHome() }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            (if (granted == true) "✅ " else if (granted == false) "⚠️ " else "• ") + label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        if (granted != true) TextButton(onClick = onClick) { Text("تنظیم") }
    }
}

/** «کمک به خانواده»: chores waiting for approval; approving gives the child the chore's stars. */
@Composable
private fun PendingChoresCard(viewModel: ParentHubViewModel, children: List<com.kodomo.app.domain.model.Child>) {
    val pending by viewModel.pendingChores.collectAsStateWithLifecycle()
    if (pending.isEmpty()) return
    com.kodomo.app.ui.components.KodomoCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text("🧹 کارهای خانه که بچه‌ها انجام داده‌اند (${pending.size})", style = MaterialTheme.typography.titleMedium)
        pending.take(8).forEach { p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(p.chore.emoji, fontSize = 26.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.chore.title, style = MaterialTheme.typography.bodyMedium)
                    Text("${children.firstOrNull { it.id == p.childId }?.name.orEmpty()} • ⭐ ${p.chore.stars}", style = MaterialTheme.typography.labelSmall)
                }
                androidx.compose.material3.TextButton(onClick = { viewModel.resolveChore(p, true) }) { Text("تأیید ✅") }
                androidx.compose.material3.TextButton(onClick = { viewModel.resolveChore(p, false) }) { Text("نه") }
            }
        }
    }
}

/** «درس و تمرین»: today's correct answers, the play-time reward rule and the child's school grade. */
@Composable
private fun StudyCard(viewModel: ParentHubViewModel, child: com.kodomo.app.domain.model.Child) {
    val today by androidx.compose.runtime.remember(child.id) { viewModel.learning.today(child.id) }.collectAsStateWithLifecycle(com.kodomo.app.data.prefs.StudyToday(0, 0, 0))
    val total by androidx.compose.runtime.remember(child.id) { viewModel.learning.totalCorrect(child.id) }.collectAsStateWithLifecycle(0)
    val grade by androidx.compose.runtime.remember(child.id) { viewModel.learning.grade(child.id, child.ageYears()) }.collectAsStateWithLifecycle(com.kodomo.app.domain.learn.Grade.forAge(child.ageYears()))
    val reward by viewModel.studyReward.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }
    com.kodomo.app.ui.components.KodomoCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text("📝 درس و تمرین ${child.name} • ${grade.label}", style = MaterialTheme.typography.titleMedium)
        Text("امروز ${today.correct} جواب درست • تا حالا $total • جایزه‌های امروز: ${today.rewardsGranted}", style = MaterialTheme.typography.bodyMedium)
        Text("قانون جایزه: هر ${reward.every} جواب درست = ${reward.minutes} دقیقه بازی (${if (reward.automatic) "خودکار" else "با تأیید شما"})", style = MaterialTheme.typography.labelMedium)
        Row {
            if (today.rewardsPending > 0) androidx.compose.material3.Button(onClick = { viewModel.approveStudyReward(child.id) }) { Text("تأیید جایزه (${today.rewardsPending})") }
            androidx.compose.material3.TextButton(onClick = { editing = true }) { Text("تنظیمات") }
        }
    }
    if (editing) {
        var every by remember { mutableStateOf(reward.every.toFloat()) }
        var minutes by remember { mutableStateOf(reward.minutes.toFloat()) }
        var auto by remember { mutableStateOf(reward.automatic) }
        var g by remember { mutableStateOf(grade) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("تنظیمات درس و تمرین") },
            text = {
                Column {
                    Text("هر ${every.toInt()} جواب درست")
                    androidx.compose.material3.Slider(value = every, onValueChange = { every = it }, valueRange = 3f..30f, steps = 26)
                    Text("${minutes.toInt()} دقیقه زمان بازی جایزه")
                    androidx.compose.material3.Slider(value = minutes, onValueChange = { minutes = it }, valueRange = 1f..30f, steps = 28)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Switch(checked = auto, onCheckedChange = { auto = it })
                        Spacer(Modifier.width(8.dp))
                        Text(if (auto) "جایزه خودکار داده شود" else "جایزه بعد از تأیید من")
                    }
                    Text("پایه‌ی تحصیلی ${child.name}:", style = MaterialTheme.typography.labelLarge)
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                        items(com.kodomo.app.domain.learn.Grade.entries.size) { i ->
                            val gr = com.kodomo.app.domain.learn.Grade.entries[i]
                            androidx.compose.material3.FilterChip(selected = g == gr, onClick = { g = gr }, label = { Text(gr.label) })
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    viewModel.setStudyReward(com.kodomo.app.data.prefs.StudyReward(every.toInt(), minutes.toInt(), auto))
                    viewModel.setGrade(child.id, g)
                    editing = false
                }) { Text("ذخیره") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { editing = false }) { Text("انصراف") } },
        )
    }
}


/** First-week checklist: what a family still has to turn on for the phone to be really safe. Hidden when done. */
@Composable
private fun SetupChecklistCard(viewModel: ParentHubViewModel, child: com.kodomo.app.domain.model.Child) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val kiosk = LocalKioskController.current
    val wellbeing by viewModel.wellbeing.collectAsStateWithLifecycle()
    val videos by remember(child.id) { viewModel.allowedVideos(child.id) }.collectAsStateWithLifecycle(emptyList())
    var tick by remember { mutableStateOf(0) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e -> if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) tick++ }
        lifecycle.lifecycle.addObserver(obs)
        onDispose { lifecycle.lifecycle.removeObserver(obs) }
    }
    val guard = remember(tick) { com.kodomo.app.kiosk.KioskPermissions.hasUsageAccess(context) && com.kodomo.app.kiosk.KioskPermissions.canDrawOverlays(context) }
    val home = remember(tick) {
        val i = android.content.Intent(android.content.Intent.ACTION_MAIN).addCategory(android.content.Intent.CATEGORY_HOME)
        context.packageManager.resolveActivity(i, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == context.packageName
    }
    data class Step(val done: Boolean, val title: String, val how: String, val action: (() -> Unit)?)
    val steps = listOf(
        Step(home, "کدومو صفحه‌ی اصلی گوشی باشد", "تا کودک با دکمه‌ی Home از محیط امن خارج نشود.") { kiosk.openDefaultHomeSettings() },
        Step(guard, "نگهبان برنامه‌ها روشن باشد", "دسترسی «استفاده از برنامه» و «نمایش روی برنامه‌ها» لازم است.") {
            val intent = if (!com.kodomo.app.kiosk.KioskPermissions.hasUsageAccess(context)) com.kodomo.app.kiosk.KioskPermissions.usageAccessIntent(context) else com.kodomo.app.kiosk.KioskPermissions.overlayIntent(context)
            runCatching { context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
        },
        Step(videos.isNotEmpty(), "چند کارتون برای ${child.name} تأیید کنید", "تب «ارزیابی» ← کارتون‌های تلوبیون یا محتوای کودومو.", null),
        Step(wellbeing.momPhone.isNotBlank() || wellbeing.dadPhone.isNotBlank(), "شماره‌ی تماس اضطراری", "تب «خانواده» ← سلامت و ایمنی کودک.", null),
    )
    val left = steps.count { !it.done }
    if (left == 0) return
    KodomoCard(Modifier.fillMaxWidth().padding16()) {
        Text("🛡️ راه‌اندازی امن (${(steps.size - left).fa()} از ${steps.size.fa()})", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(progress = { (steps.size - left).toFloat() / steps.size }, modifier = Modifier.fillMaxWidth())
        steps.forEach { st ->
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(if (st.done) "✅" else "⬜", style = MaterialTheme.typography.titleMedium)
                androidx.compose.foundation.layout.Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(st.title, style = MaterialTheme.typography.titleSmall)
                    if (!st.done) Text(st.how, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!st.done && st.action != null) androidx.compose.material3.TextButton(onClick = st.action) { Text("انجام") }
            }
        }
    }
}


/** Good and bad behaviour: noting a bad deed takes play time away today (with a fix suggestion for the child). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun BehaviourCard(viewModel: ParentHubViewModel, child: com.kodomo.app.domain.model.Child) {
    val today by remember(child.id) { viewModel.badToday(child.id) }.collectAsStateWithLifecycle(emptyList())
    var open by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<com.kodomo.app.data.prefs.BadDeed?>(null) }
    KodomoCard(Modifier.fillMaxWidth().padding16()) {
        androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("⚖️ رفتار امروز ${child.name}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            androidx.compose.material3.TextButton(onClick = { open = !open }) { Text(if (open) "بستن" else "ثبت کار نادرست") }
        }
        Text("کار خوب تأییدشده = ستاره + ${com.kodomo.app.data.prefs.ChoreStore.GOOD_DEED_MINUTES.fa()} دقیقه. کار نادرست از وقت امروز کم می‌کند و کودک راه جبران را می‌بیند.", style = MaterialTheme.typography.bodySmall)
        today.forEach { e ->
            androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("${e.deed.emoji} ${e.deed.title} (−${e.deed.minutes.fa()} دقیقه)", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                androidx.compose.material3.TextButton(onClick = { viewModel.undoBad(child.id, e) }) { Text("بخشیدم ↩") }
            }
        }
        if (open) {
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
                viewModel.badCatalog.forEach { d ->
                    androidx.compose.material3.AssistChip(onClick = { confirm = d }, label = { Text("${d.emoji} ${d.title} −${d.minutes.fa()}") })
                }
            }
        }
    }
    confirm?.let { d ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("${d.emoji} ${d.title}") },
            text = { Text("${d.minutes.fa()} دقیقه از وقت امروز ${child.name} کم شود؟ کودک این را در «کارهای من» می‌بیند، همراه با راه جبران: «${d.fix}»") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { viewModel.recordBad(child.id, d); confirm = null; open = false }) { Text("ثبت") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirm = null }) { Text("انصراف") } },
        )
    }
}
