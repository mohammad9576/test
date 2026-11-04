package com.kodomo.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kodomo.app.core.util.PersianCalendar
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.AgeGroup
import com.kodomo.app.domain.model.AvatarKind
import com.kodomo.app.domain.model.ChildDraft
import com.kodomo.app.domain.model.Gender
import com.kodomo.app.domain.model.Interest
import java.time.LocalDate

/** Shared child profile form (onboarding + parent hub): name, birth date, gender, avatar, interests, talents, abilities. */
@Composable
fun ChildProfileForm(draft: ChildDraft, onChange: (ChildDraft) -> Unit, modifier: Modifier = Modifier) {
    var showDatePicker by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = draft.name, onValueChange = { onChange(draft.copy(name = it.take(30))) },
            label = { Text("نام فرزند") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
            Spacer(Modifier.height(0.dp))
            val birth = draft.birthDate
            Text(
                if (birth == null) "  تاریخ تولد را انتخاب کنید"
                else "  ${PersianCalendar.format(birth)} • ${AgeGroup.ageInYears(birth).fa()} ساله (${AgeGroup.fromAge(AgeGroup.ageInYears(birth)).label})",
            )
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            Gender.entries.forEachIndexed { index, gender ->
                SegmentedButton(
                    selected = draft.gender == gender,
                    onClick = {
                        val avatar = when {
                            gender == Gender.GIRL && draft.avatar in listOf(AvatarKind.BOY_CURLY, AvatarKind.BOY_SHORT) -> AvatarKind.GIRL_BOW
                            gender == Gender.BOY && draft.avatar in listOf(AvatarKind.GIRL_BOW, AvatarKind.GIRL_BRAIDS) -> AvatarKind.BOY_SHORT
                            else -> draft.avatar
                        }
                        onChange(draft.copy(gender = gender, avatar = avatar))
                    },
                    shape = SegmentedButtonDefaults.itemShape(index, Gender.entries.size),
                ) { Text(gender.label) }
            }
        }
        Text("آواتار", style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(AvatarKind.forChildren) { kind ->
                KodomoAvatar(kind, size = 60.dp, selected = draft.avatar == kind, modifier = Modifier.bouncyClick { onChange(draft.copy(avatar = kind)) })
            }
        }
        Text("علاقه‌مندی‌ها", style = MaterialTheme.typography.titleSmall)
        ChipSelector(
            options = Interest.entries,
            selected = draft.interests,
            label = { "${it.emoji} ${it.label}" },
            onToggle = { interest ->
                onChange(draft.copy(interests = if (interest in draft.interests) draft.interests - interest else draft.interests + interest))
            },
        )
        OutlinedTextField(
            value = draft.talents, onValueChange = { onChange(draft.copy(talents = it.take(200))) },
            label = { Text("استعدادها (مثلاً نقاشی، حافظه‌ی قوی)") }, modifier = Modifier.fillMaxWidth(), minLines = 2,
        )
        OutlinedTextField(
            value = draft.abilities, onValueChange = { onChange(draft.copy(abilities = it.take(200))) },
            label = { Text("توانمندی‌ها (مثلاً خواندن، شنا)") }, modifier = Modifier.fillMaxWidth(), minLines = 2,
        )
    }
    if (showDatePicker) {
        val today = LocalDate.now()
        val thisYear = PersianCalendar.toJalali(today).year
        PersianDatePickerDialog(
            initial = draft.birthDate ?: today.minusYears(5),
            minYear = thisYear - 18,
            maxYear = thisYear,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                showDatePicker = false
                onChange(draft.copy(birthDate = if (it.isAfter(today)) today else it))
            },
        )
    }
}

@Composable
fun AgeGroupHint(birth: LocalDate?) {
    if (birth == null) return
    val age = AgeGroup.ageInYears(birth)
    Row { Text("رابط کاربری: ${AgeGroup.fromAge(age).label}", style = MaterialTheme.typography.labelMedium) }
}
