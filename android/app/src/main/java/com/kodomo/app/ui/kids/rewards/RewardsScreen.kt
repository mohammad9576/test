package com.kodomo.app.ui.kids.rewards

import com.kodomo.app.domain.engine.VoicePhrases
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.engine.FamilyRewardCatalog
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.EarnedAchievement
import com.kodomo.app.domain.model.FamilyReward
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.domain.model.RewardRedemption
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.SectionTitle
import com.kodomo.app.ui.components.StarBadge
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.mascot.MascotSpeech
import com.kodomo.app.ui.theme.KodomoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RewardsViewModel @Inject constructor(savedState: SavedStateHandle, private val rewardRepository: RewardRepository) : ViewModel() {
    private val childId = savedState.childId
    val achievements: StateFlow<List<EarnedAchievement>> = rewardRepository.observeAchievements(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val redemptions: StateFlow<List<RewardRedemption>> = rewardRepository.observeChildRedemptions(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun redeem(reward: FamilyReward, onResult: (Boolean) -> Unit) = viewModelScope.launch { onResult(rewardRepository.redeem(childId, reward.id)) }
}

/** Medals of honour + experience-based family rewards bought with stars (fulfilled by parents in the hub). */
@Composable
fun RewardsScreen(onBack: () -> Unit, viewModel: RewardsViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val redemptions by viewModel.redemptions.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf<String?>(null) }
    TrackActivity(ActivityCategory.HOME, "پاداش‌ها")

    KidScreenScaffold(title = "پاداش و دستاوردها", onBack = onBack, actions = { StarBadge(space.child.stars, Modifier.padding(end = 12.dp)) }) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MascotSpeech(message ?: "با ستاره‌هایت می‌توانی یک تجربه‌ی خانوادگی بگیری! ⭐", if (message != null) MascotMood.EXCITED else MascotMood.HAPPY)
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("مدال‌های افتخار") }
            items(achievements, key = { it.achievement.id }) { earned ->
                val unlocked = earned.earnedAt != null
                Column(
                    Modifier
                        .aspectRatio(0.8f)
                        .background(if (unlocked) KodomoColors.Star.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                        .padding(8.dp)
                        .alpha(if (unlocked) 1f else 0.45f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(if (unlocked) earned.achievement.emoji else "🔒", fontSize = 36.sp)
                    Text(earned.achievement.title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    if (space.spec.showDescriptions) Text(earned.achievement.description, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle("جایزه‌های خانوادگی") }
            items(FamilyRewardCatalog.all, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) { reward ->
                val pending = redemptions.any { it.reward.id == reward.id && !it.fulfilled }
                KodomoCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(reward.emoji, fontSize = 36.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(reward.title, style = MaterialTheme.typography.titleMedium)
                            Text(reward.description, style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = {
                                viewModel.redeem(reward) { ok ->
                                    message = if (ok) VoicePhrases.rewardRequested(reward.title) else VoicePhrases.NOT_ENOUGH_STARS
                                    if (ok) space.celebrate()
                                    space.voice.speak(message.orEmpty())
                                }
                            },
                            enabled = !pending && space.child.stars >= reward.starCost,
                        ) { Text(if (pending) "در انتظار" else "⭐ ${reward.starCost.fa()}") }
                    }
                }
            }
        }
    }
}
