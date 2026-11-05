package com.kodomo.app.ui.parent.wellbeing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.data.prefs.Wellbeing
import com.kodomo.app.data.prefs.WellbeingStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WellbeingViewModel @Inject constructor(private val store: WellbeingStore) : ViewModel() {
    val state = store.state
    fun save(w: Wellbeing) { viewModelScope.launch { store.save(w) } }
}

private fun Int.fa() = toString().map { if (it.isDigit()) '۰' + (it - '0') else it }.joinToString("")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WellbeingScreen(onBack: () -> Unit) {
    val vm: WellbeingViewModel = hiltViewModel()
    val w by vm.state.collectAsStateWithLifecycle(Wellbeing())
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("سلامت و ایمنی کودک") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "برگشت") } },
        )
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Section("👀 استراحت چشم (قانون ۲۰-۲۰-۲۰)", "هر چند دقیقه، ۲۰ ثانیه صفحه قفل می‌شود و همراه کودک از او می‌خواهد به دور نگاه کند، پلک بزند و کش بیاید.") {
                    Choices(listOf(0 to "خاموش", 15 to "هر ۱۵ دقیقه", 20 to "هر ۲۰ دقیقه", 30 to "هر ۳۰ دقیقه"), w.eyeBreakMinutes) { vm.save(w.copy(eyeBreakMinutes = it)) }
                }
            }
            item {
                Section("🔊 محافظت از شنوایی", "صدای رسانه در محیط کودک از این حد بالاتر نمی‌رود (برای هدفون مهم است). حداکثر: ${w.maxVolumePercent.fa()}٪") {
                    Slider(
                        value = w.maxVolumePercent.toFloat(),
                        onValueChange = { vm.save(w.copy(maxVolumePercent = (it / 10).toInt() * 10)) },
                        valueRange = 30f..100f,
                        steps = 6,
                    )
                }
            }
            item {
                Section("📚 اول درس، بعد بازی", "بازی‌ها فقط بعد از این تعداد پاسخ درست در «درس و تمرین» همان روز باز می‌شوند.") {
                    Choices(listOf(0 to "خاموش", 3 to "۳ تمرین", 5 to "۵ تمرین", 10 to "۱۰ تمرین"), w.studyFirstAnswers) { vm.save(w.copy(studyFirstAnswers = it)) }
                }
            }
            item {
                Section("🗓️ آخر هفته و ساعت مدرسه", "پنجشنبه و جمعه وقت بیشتری اضافه شود؛ و از شنبه تا چهارشنبه در ساعت مدرسه محیط کودک قفل باشد (با رمز والدین می‌توانید برای امروز باز کنید).") {
                    Text("وقت اضافه‌ی آخر هفته", style = MaterialTheme.typography.labelLarge)
                    Choices(listOf(0 to "هیچ", 15 to "+۱۵ دقیقه", 30 to "+۳۰ دقیقه", 60 to "+۱ ساعت"), w.weekendExtraMinutes) { vm.save(w.copy(weekendExtraMinutes = it)) }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        androidx.compose.material3.Switch(checked = w.schoolLock, onCheckedChange = { vm.save(w.copy(schoolLock = it)) })
                        Text("  قفل در ساعت مدرسه", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (w.schoolLock) {
                        Text("از ساعت", style = MaterialTheme.typography.labelLarge)
                        Choices(listOf(7 * 60 to "۷:۰۰", 7 * 60 + 30 to "۷:۳۰", 8 * 60 to "۸:۰۰", 13 * 60 to "۱۳:۰۰"), w.schoolStartMinutes) { vm.save(w.copy(schoolStartMinutes = it)) }
                        Text("تا ساعت", style = MaterialTheme.typography.labelLarge)
                        Choices(listOf(12 * 60 to "۱۲:۰۰", 13 * 60 to "۱۳:۰۰", 14 * 60 to "۱۴:۰۰", 18 * 60 to "۱۸:۰۰"), w.schoolEndMinutes) { vm.save(w.copy(schoolEndMinutes = it)) }
                    }
                }
            }
            item {
                Section("📞 تماس اضطراری", "دکمه‌ی «زنگ به مامان/بابا» در صفحه‌ی اصلی و صفحه‌ی پایان زمان نشان داده می‌شود. شماره‌گیر تلفن ۵ دقیقه بدون قفل باز می‌ماند؛ هیچ برنامه‌ی دیگری باز نمی‌شود.") {
                    OutlinedTextField(
                        value = w.momPhone, onValueChange = { vm.save(w.copy(momPhone = it)) },
                        label = { Text("شماره‌ی مامان") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = w.dadPhone, onValueChange = { vm.save(w.copy(dadPhone = it)) },
                        label = { Text("شماره‌ی بابا") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                Section("🛡️ همیشه فعال", "") {
                    listOf(
                        "قفل محیط کودک (Kiosk) و دکمه‌ی برگشت روی برنامه‌های مجاز",
                        "عکس‌برداری از صفحه و ضبط صفحه در محیط کودک غیرفعال است",
                        "فیلم‌ها فقط پس از ارزیابی کودومو یا تأیید شما پخش می‌شوند",
                        "مرورگر فقط سایت‌های مجاز؛ تبلیغات و پنجره‌های بازشو مسدود",
                        "بدون خرید درون‌برنامه‌ای، بدون تبلیغ، بدون گفتگو با غریبه‌ها",
                        "زمان روزانه، ساعت خواب و هشدار ۵ دقیقه‌ای",
                    ).forEach { Text("✅ $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, body: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (body.isNotBlank()) Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Choices(options: List<Pair<Int, String>>, selected: Int, onPick: (Int) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (v, label) -> FilterChip(selected = v == selected, onClick = { onPick(v) }, label = { Text(label) }) }
    }
}
