package com.kodomo.app.ui.parent.exit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kodomo.app.kiosk.LocalKioskController

/**
 * One simple question instead of hidden launcher settings: «where do you want to go?».
 *  - back to the child's space, or
 *  - leave Kodomo completely: the phone's normal home screen comes back, and Kodomo stops being the Home app
 *    (no settings page, no launcher chooser). Kodomo becomes Home again the next time a child's space is opened.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExitChoiceSheet(onDismiss: () -> Unit, onBackToKids: (() -> Unit)?) {
    val kiosk = LocalKioskController.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("کجا می‌روید؟", style = MaterialTheme.typography.headlineSmall)
            if (onBackToKids != null) {
                ExitOption(
                    icon = "🧸", title = "برگشت به محیط کودک",
                    subtitle = "گوشی را دوباره به کودک بدهید؛ همه‌ی قفل‌ها و زمان‌بندی فعال می‌ماند.",
                    color = Color(0xFF2E7D32),
                ) { onDismiss(); onBackToKids() }
            }
            ExitOption(
                icon = "📱", title = "خروج از کدومو؛ گوشی معمولی",
                subtitle = "صفحه‌ی اصلی همیشگی گوشی باز می‌شود و کدومو دیگر لانچر نیست. هر وقت آیکون کدومو را باز کنید و کودکی را انتخاب کنید، دوباره فعال می‌شود.",
                color = Color(0xFF1565C0),
            ) { onDismiss(); kiosk.exitToPhone() }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("ماندن در پنل والدین") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExitOption(icon: String, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = color.copy(alpha = 0.10f),
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(16.dp), color = color) {
                Text(icon, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = color)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
