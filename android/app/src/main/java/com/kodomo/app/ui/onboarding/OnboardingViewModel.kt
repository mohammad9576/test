package com.kodomo.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kodomo.app.data.prefs.SessionPreferences
import com.kodomo.app.domain.model.ChildDraft
import com.kodomo.app.domain.repository.FamilyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OnboardingStep { WELCOME, FAMILY, PIN, CHILD_COUNT, CHILD_FORM, SCREEN_TIME, DONE }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val familyName: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val pin: String = "",
    val pinConfirm: String = "",
    val pinError: String? = null,
    val childCount: Int = 1,
    val drafts: List<ChildDraft> = listOf(ChildDraft()),
    val childIndex: Int = 0,
    val dailyLimitMinutes: Int = 30,
    val saving: Boolean = false,
) {
    val totalSteps: Int get() = 5 + childCount
    val stepNumber: Int get() = when (step) {
        OnboardingStep.WELCOME -> 1
        OnboardingStep.FAMILY -> 2
        OnboardingStep.PIN -> 3
        OnboardingStep.CHILD_COUNT -> 4
        OnboardingStep.CHILD_FORM -> 5 + childIndex
        OnboardingStep.SCREEN_TIME -> 5 + childCount
        OnboardingStep.DONE -> totalSteps
    }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val session: SessionPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun update(transform: (OnboardingState) -> OnboardingState) = _state.update(transform)

    fun setChildCount(count: Int) = _state.update { s ->
        val drafts = List(count) { i -> s.drafts.getOrNull(i) ?: ChildDraft() }
        s.copy(childCount = count, drafts = drafts, childIndex = 0)
    }

    fun updateDraft(draft: ChildDraft) = _state.update { s ->
        s.copy(drafts = s.drafts.toMutableList().also { it[s.childIndex] = draft })
    }

    fun next() = _state.update { s ->
        when (s.step) {
            OnboardingStep.WELCOME -> s.copy(step = OnboardingStep.FAMILY)
            OnboardingStep.FAMILY -> s.copy(step = OnboardingStep.PIN)
            OnboardingStep.PIN -> when {
                s.pin.length != 4 -> s.copy(pinError = "رمز باید ۴ رقم باشد.")
                s.pin != s.pinConfirm -> s.copy(pinError = "تکرار رمز با رمز یکسان نیست.", pinConfirm = "")
                s.pin.toSet().size == 1 || s.pin in listOf("1234", "4321", "0123") -> s.copy(pinError = "رمز ساده است؛ یک رمز امن‌تر انتخاب کنید.", pin = "", pinConfirm = "")
                else -> s.copy(step = OnboardingStep.CHILD_COUNT, pinError = null)
            }
            OnboardingStep.CHILD_COUNT -> s.copy(step = OnboardingStep.CHILD_FORM, childIndex = 0)
            OnboardingStep.CHILD_FORM ->
                if (s.childIndex < s.childCount - 1) s.copy(childIndex = s.childIndex + 1) else s.copy(step = OnboardingStep.SCREEN_TIME)
            OnboardingStep.SCREEN_TIME -> s
            OnboardingStep.DONE -> s
        }
    }

    fun back() = _state.update { s ->
        when (s.step) {
            OnboardingStep.WELCOME -> s
            OnboardingStep.FAMILY -> s.copy(step = OnboardingStep.WELCOME)
            OnboardingStep.PIN -> s.copy(step = OnboardingStep.FAMILY)
            OnboardingStep.CHILD_COUNT -> s.copy(step = OnboardingStep.PIN)
            OnboardingStep.CHILD_FORM -> if (s.childIndex > 0) s.copy(childIndex = s.childIndex - 1) else s.copy(step = OnboardingStep.CHILD_COUNT)
            OnboardingStep.SCREEN_TIME -> s.copy(step = OnboardingStep.CHILD_FORM, childIndex = s.childCount - 1)
            OnboardingStep.DONE -> s
        }
    }

    fun finish(onDone: () -> Unit) {
        val s = _state.value
        if (s.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            familyRepository.createFamily(s.familyName, s.fatherName, s.motherName, s.pin)
            s.drafts.filter { it.isValid }.forEach { familyRepository.addChild(it, s.dailyLimitMinutes) }
            session.setOnboardingCompleted(true)
            _state.update { it.copy(step = OnboardingStep.DONE, saving = false) }
            onDone()
        }
    }
}
