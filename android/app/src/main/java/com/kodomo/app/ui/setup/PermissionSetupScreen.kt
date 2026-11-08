package com.kodomo.app.ui.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kodomo.app.kiosk.FakeHomeActivity
import com.kodomo.app.kiosk.KioskPermissions
import com.kodomo.app.kiosk.LocalKioskController
import com.kodomo.app.ui.fx.Emoji

/** One permission the safe kids space needs. */
private data class Need(val emoji: String, val title: String, val why: String, val granted: Boolean, val open: () -> Unit)

private val runtimePermissions: Array<String> get() = buildList {
    add(Manifest.permission.CAMERA)
    add(Manifest.permission.RECORD_AUDIO)
    add(Manifest.permission.CALL_PHONE)
    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
}.toTypedArray()

private fun Context.has(p: String) = ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

private fun Context.ignoresBattery(): Boolean =
    (getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)

/**
 * Guided setup right after installing: Android's own permission dialogs open by themselves, one after another, and
 * each special setting (which Android only lets the user switch on in Settings) opens directly on Kodomo's page, so
 * the parent only taps «allow». Returning to the app moves on to the next one automatically.
 */
@Composable
fun PermissionSetupScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val kiosk = LocalKioskController.current
    var tick by remember { mutableIntStateOf(0) }
    var auto by remember { mutableStateOf(true) }
    val lifecycle = LocalLifecycleOwner.current
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        lifecycle.lifecycle.addObserver(obs)
        onDispose { lifecycle.lifecycle.removeObserver(obs) }
    }
    val runtime = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }
    fun start(i: Intent, fallback: Intent? = null) {
        runCatching { context.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .recoverCatching { fallback?.let { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }

    val needs = remember(tick) {
        listOf(
            Need("📷", "دوربین، میکروفون، تماس و اعلان", "برای بازی‌های حرکتی، عکاسی کودک، تماس با مخاطبان تأییدشده و خبر دادن به شما.",
                runtimePermissions.all { context.has(it) }) { runtime.launch(runtimePermissions) },
            Need("🛡️", "نگهبان برنامه‌ها (دسترسی استفاده)", "تا کدومو بفهمد کودک کدام برنامه را باز کرده و او را برگرداند.",
                KioskPermissions.hasUsageAccess(context)) {
                start(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}")), KioskPermissions.usageAccessIntent(context))
            },
            Need("🪟", "نمایش روی برنامه‌های دیگر", "برای دکمه‌ی «برگشت به کدومو» و زمان باقی‌مانده روی برنامه‌های مجاز.",
                KioskPermissions.canDrawOverlays(context)) { start(KioskPermissions.overlayIntent(context)) },
            Need("🔋", "اجرای همیشگی (بدون بهینه‌سازی باتری)", "تا گوشی نگهبان را در پس‌زمینه نبندد.",
                context.ignoresBattery()) {
                start(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")), KioskPermissions.appDetailsIntent(context))
            },
            Need("🏠", "کدومو صفحه‌ی اصلی گوشی", "تا دکمه‌ی Home کودک را از محیط امن بیرون نبرد.",
                isDefaultHome(context)) { if (!FakeHomeActivity.showHomeChooser(context)) kiosk.openDefaultHomeSettings() },
            Need("🔐", "محافظت در برابر حذف برنامه", "مدیر دستگاه: کودک نمی‌تواند کدومو را حذف کند.",
                isAdmin(context)) { kiosk.requestDeviceAdmin() },
        )
    }
    val next = needs.firstOrNull { !it.granted }
    // Android's own dialogs open by themselves the first time; each later step opens when the parent returns.
    val tried = remember { mutableSetOf<String>() }
    LaunchedEffect(tick) {
        if (tick == 0) return@LaunchedEffect
        kotlinx.coroutines.delay(600)
        if (auto && next != null && tried.add(next.title)) next.open()
    }
    val done = needs.count { it.granted }

    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Emoji("🛡️", 96.dp)
                Text("آماده‌سازی گوشی برای کودک", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("روی هر مورد بزنید و در صفحه‌ای که باز می‌شود «اجازه» یا «روشن» را انتخاب کنید. بعد به کدومو برگردید.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { done / needs.size.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            }
        }
        items(needs) { n ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                    .background(if (n.granted) Color(0xFFE8F5E9) else if (n === next) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.surface)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Emoji(n.emoji, 44.dp)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(n.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(n.why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (n.granted) Text("✅", style = MaterialTheme.typography.headlineSmall)
                else Button(onClick = { auto = false; n.open() }) { Text("اجازه") }
            }
        }
        item {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(if (next == null) "همه‌چیز آماده است ✨ ورود به کدومو" else "ادامه (بعداً از داشبورد کامل می‌کنم)")
            }
            if (next != null) TextButton(onClick = { auto = false; next.open() }, modifier = Modifier.fillMaxWidth()) { Text("بعدی: ${next.title}") }
        }
    }
}

private fun isDefaultHome(context: Context): Boolean {
    val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
    @Suppress("DEPRECATION")
    return context.packageManager.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == context.packageName
}

private fun isAdmin(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
    return dpm.isAdminActive(android.content.ComponentName(context, com.kodomo.app.kiosk.KodomoDeviceAdminReceiver::class.java))
}
