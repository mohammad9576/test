package com.kodomo.app.ui.components

import androidx.compose.foundation.background
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
    var error by remember { mutableStateOf<String?>(null) }
    var lockSeconds by remember { mutableIntStateOf(gateThrottle.remainingLockSeconds().toInt()) }
    val scope = rememberCoroutineScope()

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
                Spacer(Modifier.height(14.dp))
                PinDots(length = pin.length)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        challenge.questionPersian,
                        style = MaterialTheme.typography.titleLarge.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr),
                        color = KodomoColors.GreenDark,
                    )
                    OutlinedTextField(
                        value = answer,
                        onValueChange = { answer = it.take(4) },
                        singleLine = true,
                        modifier = Modifier.size(width = 110.dp, height = 58.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("پاسخ") },
                    )
                }
                Spacer(Modifier.height(8.dp))
                when {
                    lockSeconds > 0 -> Text("به دلیل تلاش‌های ناموفق، ${lockSeconds.fa()} ثانیه صبر کنید.", color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
                }
                Spacer(Modifier.height(8.dp))
                PersianKeypad(
                    enabled = lockSeconds == 0,
                    onDigit = { d -> if (pin.length < 4) pin += d },
                    onBackspace = { pin = pin.dropLast(1) },
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
