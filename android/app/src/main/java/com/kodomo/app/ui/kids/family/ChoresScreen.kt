package com.kodomo.app.ui.kids.family

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.fa
import com.kodomo.app.data.prefs.Chore
import com.kodomo.app.data.prefs.ChoreStore
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.ui.fx.GameSfx
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.mascot.MascotSpeech
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChoresViewModel @Inject constructor(savedState: SavedStateHandle, private val store: ChoreStore) : ViewModel() {
    private val childId = savedState.childId
    val pendingIds = store.pending.map { list -> list.filter { it.childId == childId }.map { it.chore.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    val done = store.doneCount(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    fun markDone(chore: Chore) = viewModelScope.launch { store.markDone(childId, chore) }
}

/** «کمک به خانواده»: real chores for the child's age; the parent confirms them in the parent hub to give stars. */
@Composable
fun ChoresScreen(onBack: () -> Unit, viewModel: ChoresViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val pending by viewModel.pendingIds.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val age = space.child.ageYears()
    val chores = ChoreStore.catalog.filter { it.minAge <= age }
    TrackActivity(ActivityCategory.HOME, "کمک به خانواده")
    KidScreenScaffold(title = "کمک به خانواده", onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { MascotSpeech("یک کار خوب برای خانواده انجام بده، بعد دکمه را بزن. وقتی مامان یا بابا تأیید کنند، ستاره می‌گیری! 💚", MascotMood.EXCITED) }
            item { Text("🏅 کارهای تأییدشده تا امروز: ${done.fa()}", style = MaterialTheme.typography.titleMedium) }
            items(chores, key = { it.id }) { chore ->
                val waiting = chore.id in pending
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            if (waiting) Brush.horizontalGradient(listOf(Color(0xFFFFF8E1), Color(0xFFFFECB3)))
                            else Brush.horizontalGradient(listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))),
                            RoundedCornerShape(22.dp),
                        )
                        .bouncyClick {
                            if (!waiting) {
                                viewModel.markDone(chore)
                                GameSfx.sparkle()
                                space.celebrate()
                                space.voice.speak(chore.title)
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    com.kodomo.app.ui.fx.Emoji(chore.emoji, 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(chore.title, style = MaterialTheme.typography.titleMedium)
                        Text(chore.tip, style = MaterialTheme.typography.bodySmall)
                        Text(if (waiting) "⏳ منتظر تأیید مامان و بابا" else "⭐".repeat(chore.stars) + "  انجامش دادم؟ بزن!", style = MaterialTheme.typography.labelLarge, color = if (waiting) Color(0xFFE65100) else Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}
