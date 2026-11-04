package com.kodomo.app.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodomo.app.core.util.PersianDigits
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.AvatarKind
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.components.ChildProfileForm
import com.kodomo.app.ui.components.KodomoAvatar
import com.kodomo.app.ui.components.KodomoPrimaryButton
import com.kodomo.app.ui.components.PersianKeypad
import com.kodomo.app.ui.components.PinDots
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.ui.mascot.KodomoMascot
import com.kodomo.app.ui.theme.KodomoColors

@Composable
fun OnboardingScreen(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.step != OnboardingStep.WELCOME) { viewModel.back() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(KodomoColors.Mint, KodomoColors.Cream)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            if (state.step != OnboardingStep.WELCOME) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = viewModel::back) { Text("بازگشت") }
                    Spacer(Modifier.weight(1f))
                    Text("${state.stepNumber.fa()} از ${state.totalSteps.fa()}", style = MaterialTheme.typography.labelLarge)
                }
                LinearProgressIndicator(
                    progress = { state.stepNumber / state.totalSteps.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    trackColor = Color.White,
                )
                Spacer(Modifier.height(12.dp))
            }
            AnimatedContent(
                targetState = state.step to state.childIndex,
                transitionSpec = { (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
                label = "onboarding",
            ) { (step, _) ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                    when (step) {
                        OnboardingStep.WELCOME -> WelcomeStep()
                        OnboardingStep.FAMILY -> FamilyStep(state, viewModel)
                        OnboardingStep.PIN -> PinStep(state, viewModel)
                        OnboardingStep.CHILD_COUNT -> ChildCountStep(state, viewModel)
                        OnboardingStep.CHILD_FORM -> {
                            val draft = state.drafts[state.childIndex]
                            KodomoAvatar(draft.avatar, size = 96.dp)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (draft.name.isBlank()) "فرزند ${(state.childIndex + 1).fa()}" else draft.name,
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text("تکمیل پروفایل کودک", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(12.dp))
                            ChildProfileForm(draft = draft, onChange = viewModel::updateDraft)
                        }
                        OnboardingStep.SCREEN_TIME -> ScreenTimeStep(state, viewModel)
                        OnboardingStep.DONE -> KodomoMascot(MascotMood.EXCITED, size = 200.dp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            val canContinue = when (state.step) {
                OnboardingStep.FAMILY -> state.familyName.isNotBlank() && (state.fatherName.isNotBlank() || state.motherName.isNotBlank())
                OnboardingStep.PIN -> state.pin.length == 4 && state.pinConfirm.length == 4
                OnboardingStep.CHILD_FORM -> state.drafts[state.childIndex].isValid
                OnboardingStep.SCREEN_TIME -> !state.saving && state.dailyLimitMinutes in 5..60
                else -> true
            }
            KodomoPrimaryButton(
                text = when (state.step) {
                    OnboardingStep.WELCOME -> "شروع"
                    OnboardingStep.SCREEN_TIME -> "تأیید و شروع"
                    OnboardingStep.CHILD_FORM -> "ذخیره و ادامه"
                    else -> "ادامه"
                },
                enabled = canContinue,
                onClick = { if (state.step == OnboardingStep.SCREEN_TIME) viewModel.finish(onFinished) else viewModel.next() },
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    Spacer(Modifier.height(24.dp))
    Text("کدومو", style = MaterialTheme.typography.displaySmall, color = KodomoColors.Green)
    Text("همراه امن خانواده", style = MaterialTheme.typography.titleMedium, color = KodomoColors.InkSoft)
    Spacer(Modifier.height(16.dp))
    KodomoMascot(MascotMood.EXCITED, size = 220.dp)
    Spacer(Modifier.height(16.dp))
    Text("به کدومو خوش آمدید", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(
        "کدومو زیست‌بوم دیجیتال خانواده است: محیطی امن و شاد برای کودک، و ابزاری دقیق برای والدین تا انتخاب، زمان و کیفیت مصرف رسانه را مدیریت کنند.",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FamilyStep(state: OnboardingState, vm: OnboardingViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        KodomoAvatar(AvatarKind.FATHER, size = 72.dp)
        KodomoAvatar(AvatarKind.MOTHER, size = 72.dp)
    }
    Spacer(Modifier.height(12.dp))
    Text("حساب خانواده", style = MaterialTheme.typography.headlineSmall)
    Text("هر خانواده یک حساب اصلی دارد و هر کودک پروفایل مستقل.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(state.familyName, { v -> vm.update { it.copy(familyName = v.take(40)) } }, label = { Text("نام خانوادگی (مثلاً خانواده رضایی)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(state.fatherName, { v -> vm.update { it.copy(fatherName = v.take(30)) } }, label = { Text("نام والد یا سرپرست اول (پدر)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(state.motherName, { v -> vm.update { it.copy(motherName = v.take(30)) } }, label = { Text("نام والد یا سرپرست دوم (مادر)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PinStep(state: OnboardingState, vm: OnboardingViewModel) {
    val confirming = state.pin.length == 4
    KodomoMascot(MascotMood.THINKING, size = 110.dp)
    Text(if (confirming) "تکرار رمز والدین" else "رمز ۴ رقمی والدین", style = MaterialTheme.typography.headlineSmall)
    Text(
        "این رمز برای ورود به پنل والدین و خروج از محیط امن کودک لازم است. آن را به کودک نگویید.",
        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(16.dp))
    PinDots(if (confirming) state.pinConfirm.length else state.pin.length)
    state.pinError?.let {
        Spacer(Modifier.height(8.dp))
        Text(it, color = MaterialTheme.colorScheme.error)
    }
    Spacer(Modifier.height(16.dp))
    PersianKeypad(
        onDigit = { d ->
            vm.update { s ->
                if (s.pin.length < 4) s.copy(pin = s.pin + d, pinError = null) else if (s.pinConfirm.length < 4) s.copy(pinConfirm = s.pinConfirm + d) else s
            }
        },
        onBackspace = {
            vm.update { s -> if (s.pinConfirm.isNotEmpty()) s.copy(pinConfirm = s.pinConfirm.dropLast(1)) else s.copy(pin = s.pin.dropLast(1)) }
        },
    )
}

@Composable
private fun ChildCountStep(state: OnboardingState, vm: OnboardingViewModel) {
    KodomoMascot(MascotMood.HAPPY, size = 120.dp)
    Text("فرزندان شما", style = MaterialTheme.typography.headlineSmall)
    Text("چند فرزند دارید؟", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        (1..5).forEach { n ->
            val selected = state.childCount == n
            Box(
                Modifier
                    .size(54.dp)
                    .background(if (selected) KodomoColors.Green else Color.White, CircleShape)
                    .border(1.dp, KodomoColors.Green, CircleShape)
                    .bouncyClick { vm.setChildCount(n) },
                contentAlignment = Alignment.Center,
            ) {
                Text(if (n == 5) "۵+" else n.fa(), style = MaterialTheme.typography.titleLarge, color = if (selected) Color.White else KodomoColors.Green)
            }
        }
    }
    Spacer(Modifier.height(20.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        val avatars = listOf(AvatarKind.GIRL_BOW, AvatarKind.BOY_SHORT, AvatarKind.GIRL_BRAIDS, AvatarKind.BOY_CURLY, AvatarKind.BABY)
        avatars.take(state.childCount).forEach { KodomoAvatar(it, size = 64.dp) }
    }
}

@Composable
private fun ScreenTimeStep(state: OnboardingState, vm: OnboardingViewModel) {
    Text("⏰", style = MaterialTheme.typography.displaySmall)
    Text("مدت زمان استفاده از گوشی", style = MaterialTheme.typography.headlineSmall)
    Text("هر ۵ دقیقه یک گزینه؛ حداکثر یک ساعت. بعداً از پنل والدین تا ۱۲۰ دقیقه قابل تغییر است.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        maxItemsInEachRow = 3,
    ) {
        (5..60 step 5).forEach { minutes ->
            val selected = state.dailyLimitMinutes == minutes
            Column(
                Modifier
                    .size(width = 92.dp, height = 64.dp)
                    .background(if (selected) KodomoColors.MintDeep else Color.White, RoundedCornerShape(16.dp))
                    .border(if (selected) 2.dp else 1.dp, if (selected) KodomoColors.Green else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                    .bouncyClick { vm.update { it.copy(dailyLimitMinutes = minutes) } },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(minutes.fa(), style = MaterialTheme.typography.titleLarge)
                Text("دقیقه", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = if (state.dailyLimitMinutes % 5 == 0) "" else state.dailyLimitMinutes.fa(),
        onValueChange = { v ->
            val parsed = PersianDigits.parseIntOrNull(v)
            if (parsed != null) vm.update { it.copy(dailyLimitMinutes = parsed.coerceIn(5, 60)) }
        },
        label = { Text("زمان سفارشی (حداکثر ۶۰ دقیقه)") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(8.dp))
    Text("حداکثر زمان استفاده: ${state.dailyLimitMinutes.fa()} دقیقه", style = MaterialTheme.typography.titleMedium)
}
