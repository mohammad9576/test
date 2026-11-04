package com.kodomo.app.ui.kids.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.model.Interest
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoPrimaryButton
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.mascot.MascotSpeech
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InterestsViewModel @Inject constructor(private val familyRepository: FamilyRepository) : ViewModel() {
    fun save(child: Child, interests: Set<Interest>, onDone: () -> Unit) = viewModelScope.launch {
        familyRepository.updateChild(child.copy(interests = interests))
        onDone()
    }
}

@Composable
fun InterestsScreen(onBack: () -> Unit, viewModel: InterestsViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    var selected by remember { mutableStateOf(space.child.interests) }
    KidScreenScaffold(title = "علایق من", onBack = onBack) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MascotSpeech("علاقه‌مندی‌هات رو انتخاب کن تا پیشنهادهای مخصوص خودت رو آماده کنم!", MascotMood.EXCITED)
            }
            items(Interest.entries) { interest ->
                val on = interest in selected
                Column(
                    Modifier
                        .aspectRatio(1f)
                        .background(if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                        .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
                        .bouncyClick {
                            selected = if (on) selected - interest else selected + interest
                            space.guide(interest.label)
                        }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box { Text(interest.emoji, fontSize = 38.sp) }
                    Text(interest.label, style = MaterialTheme.typography.labelLarge)
                    if (on) Text("✅", fontSize = 14.sp)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                KodomoPrimaryButton("ذخیره‌ی تغییرات", onClick = { viewModel.save(space.child, selected, onBack) })
            }
        }
    }
}
