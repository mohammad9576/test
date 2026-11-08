package com.kodomo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kodomo.app.core.util.PersianDigits
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.engine.ParentGateChallenge
import com.kodomo.app.domain.engine.ParentGateThrottle
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.mascot.KodomoMascot
import com.kodomo.app.ui.theme.KodomoColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val gateThrottle = ParentGateThrottle()

/**
 * Parent Gate: 4-digit PIN on a Persian keypad + a random arithmetic challenge (accepts Persian or Latin digits).
 * [verifyPin] is supplied by the caller (FamilyRepository.verifyPin) so the dialog stays stateless about storage.
 */
@Composable
fun ParentGateDialog(
    title: String = "ورود والدین",
    verifyPin: suspend (String) -> Boolean,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
) {
    var challenge by remember { mutableStateOf(ParentGateChallenge.random()) }
    var pin by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var answering by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lockSeconds by remember { mutableIntStateOf(gateThrottle.remainingLockSeconds().toInt()) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val biometric = remember { com.kodomo.app.core.security.BiometricGate.isEnabled(context) && com.kodomo.app.core.security.BiometricGate.isAvailable(context) }
    fun askBiometric() = com.kodomo.app.core.security.BiometricGate.authenticate(context, title, onSuccess = { gateThrottle.registerSuccess(); onVerified() })
    // Offer the fingerprint / face dialog straight away; the PIN stays as the fallback.
    LaunchedEffect(biometric) { if (biometric && !gateThrottle.isLocked()) askBiometric() }

    LaunchedEffect(lockSeconds) {
        if (lockSeconds > 0) {
            delay(1_000)
            lockSeconds = gateThrottle.remainingLockSeconds().toInt()
        }
    }

    fun submit() {
        if (gateThrottle.isLocked()) return
        scope.launch {
            val pinOk = verifyPin(pin)
            val mathOk = challenge.isCorrect(answer)
            if (pinOk && mathOk) {
                gateThrottle.registerSuccess()
                onVerified()
            } else {
                gateThrottle.registerFailure()
                error = when {
                    !pinOk -> "رمز والدین درست نیست."
                    else -> "پاسخ سؤال ریاضی درست نیست."
                }
                pin = ""
                answer = ""
                answering = false
                challenge = ParentGateChallenge.random()
                lockSeconds = gateThrottle.remainingLockSeconds().toInt()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.padding(20.dp).fillMaxWidth()) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                KodomoMascot(MascotMood.THINKING, size = 72.dp)
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text("این بخش فقط برای پدر و مادر است", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (biometric) {
                    TextButton(onClick = { askBiometric() }) { Text("👆 ورود با اثر انگشت یا چهره", style = MaterialTheme.typography.titleSmall) }
                }
                Spacer(Modifier.height(14.dp))
                // Both the PIN and the maths answer are typed on Kodomo's own keypad (never the phone keyboard).
                Box(
                    Modifier.background(if (!answering) KodomoColors.Mint else Color.Transparent, RoundedCornerShape(16.dp))
                        .bouncyClick { answering = false }.padding(horizontal = 14.dp, vertical = 8.dp),
                ) { PinDots(length = pin.length) }
                Text("رمز والدین", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    challenge.questionPersian,
                    style = MaterialTheme.typography.titleLarge.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr),
                    color = KodomoColors.GreenDark,
                )
                Box(
                    Modifier.size(width = 140.dp, height = 52.dp)
                        .background(if (answering) KodomoColors.Mint else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                        .border(2.dp, if (answering) KodomoColors.Green else Color.Transparent, RoundedCornerShape(14.dp))
                        .bouncyClick { answering = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (answer.isEmpty()) "پاسخ" else PersianDigits.toPersian(answer), style = MaterialTheme.typography.titleLarge,
                        color = if (answer.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(8.dp))
                when {
                    lockSeconds > 0 -> Text("به دلیل تلاش‌های ناموفق، ${lockSeconds.fa()} ثانیه صبر کنید.", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                PersianKeypad(
                    enabled = lockSeconds == 0,
                    onDigit = { d ->
                        if (answering) { if (answer.length < 4) answer += d }
                        else { if (pin.length < 4) pin += d; if (pin.length == 4) answering = true }
                    },
                    onBackspace = { if (answering) { if (answer.isEmpty()) answering = false else answer = answer.dropLast(1) } else pin = pin.dropLast(1) },
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    KodomoPrimaryButton(
                        text = "تأیید",
                        onClick = ::submit,
                        enabled = pin.length == 4 && PersianDigits.digitsOnly(answer).isNotEmpty() && lockSeconds == 0,
                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun PinDots(length: Int, total: Int = 4) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.size(18.dp).background(if (i < length) KodomoColors.Green else MaterialTheme.colorScheme.outlineVariant, CircleShape),
            )
        }
    }
}

/** Large Persian numeric keypad (۱..۹, ۰) emitting Latin digits for storage/verification. */
@Composable
fun PersianKeypad(onDigit: (Char) -> Unit, onBackspace: () -> Unit, enabled: Boolean = true) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d -> KeypadKey(PersianDigits.toPersian(d.toString()), enabled) { onDigit(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Spacer(Modifier.size(64.dp))
            KeypadKey("۰", enabled) { onDigit('0') }
            Box(
                Modifier.size(64.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape).bouncyClick { if (enabled) onBackspace() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "پاک کردن") }
        }
    }
}

@Composable
private fun KeypadKey(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(64.dp)
            .aspectRatio(1f)
            .background(if (enabled) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .bouncyClick { if (enabled) onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
    }
}
