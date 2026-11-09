package com.kodomo.app.ui.kids.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.domain.model.MascotMood
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.engine.MascotDialogueEngine
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.AgeGroup
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.model.EarnedAchievement
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.kiosk.ScreenTimeManager
import com.kodomo.app.ui.components.KodomoAvatar
import com.kodomo.app.ui.components.ModuleTile
import com.kodomo.app.ui.components.StarBadge
import com.kodomo.app.ui.components.TimeLeftChip
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.mascot.MascotSpeech
import com.kodomo.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KidHomeViewModel @Inject constructor(
    savedState: androidx.lifecycle.SavedStateHandle,
    private val familyRepository: FamilyRepository,
    rewardRepository: RewardRepository,
    screenTime: ScreenTimeManager,
    modulesStore: com.kodomo.app.data.prefs.ModulesStore,
) : ViewModel() {
    val childId = savedState.childId
    val visible = modulesStore.visible(savedState.childId)
    val dialogue = MascotDialogueEngine()
    val time: StateFlow<ScreenTimeManager.State> = screenTime.state
    val achievements: StateFlow<List<EarnedAchievement>> =
        rewardRepository.observeAchievements(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleDarkMode(child: Child) = viewModelScope.launch { familyRepository.updateChild(child.copy(darkMode = !child.darkMode)) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KidHomeScreen(onOpen: (String) -> Unit, childId: Long, viewModel: KidHomeViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val child = space.child
    val spec = space.spec
    val time by viewModel.time.collectAsStateWithLifecycle()
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val greeting = remember(child.id, child.interests) { viewModel.dialogue.greeting(child) }
    val chosen by viewModel.visible.collectAsStateWithLifecycle(null)
    val modules = remember(child, chosen) {
        val all = HomeModules.forChild(child)
        val keys = chosen
        if (keys == null) all.filter { it.key !in HomeModules.hiddenByDefault } else all.filter { it.key in keys }
    }

    BackHandler(enabled = true) { }
    TrackActivity(ActivityCategory.HOME, "خانه")
    // Greet once when the child enters the kids space, not every time they come back to the home screen.
    LaunchedEffect(child.id) { if (GreetingMemory.shouldGreet(child.id)) space.voice.speakLine(greeting) }

    val bounce = rememberInfiniteTransition(label = "avatar")
    val avatarScale by bounce.animateFloat(1f, if (spec.gamified) 1.06f else 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")

    val night = child.darkMode || com.kodomo.app.ui.theme.NightMode.isNight()
    LauncherHome(
        name = child.name,
        modules = modules,
        columns = if (spec.group == AgeGroup.TODDLER) 3 else 4,
        remainingSeconds = if (time.child?.id == childId) time.remainingSeconds else null,
        progress = time.progress,
        stars = child.stars,
        medals = achievements.count { it.earnedAt != null },
        night = night,
        buddy = { m -> BuddyWidget(greeting.text, m) },
        extras = { com.kodomo.app.ui.kids.wellbeing.CallParentsRow(com.kodomo.app.ui.kids.LocalWellbeing.current, space.callParent) },
        onParentLongPress = space.requestParentExit,
        onOpen = { module -> space.guide(module.title); onOpen(module.route) },
    )
}

/** Remembers when each child was last greeted aloud, so returning from a section stays quiet. */
internal object GreetingMemory {
    private val last = mutableMapOf<Long, Long>()
    fun shouldGreet(childId: Long, quietMillis: Long = 45 * 60_000L): Boolean {
        val now = System.currentTimeMillis()
        val previous = last[childId]
        if (previous != null && now - previous < quietMillis) return false
        last[childId] = now
        return true
    }
}

/** The companion as a home-screen widget: it says hello; tapping it lets the child pick another character. */
@Composable
private fun BuddyWidget(greeting: String, modifier: Modifier = Modifier) {
    val space = LocalKidSpace.current
    val buddy = com.kodomo.app.ui.mascot.LocalBuddy.current
    var picking by remember { mutableStateOf(false) }
    Row(
        modifier.glass().bouncyClick { picking = true }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.kodomo.app.ui.mascot.BuddyMascot(buddy, MascotMood.HAPPY, size = 58.dp)
        Spacer(Modifier.width(6.dp))
        Column(Modifier.weight(1f)) {
            Text(buddy.title, style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(greeting, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
    if (picking) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("دوستت را انتخاب کن!") },
            text = {
                LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(com.kodomo.app.domain.model.Buddy.entries.size) { i ->
                        val b = com.kodomo.app.domain.model.Buddy.entries[i]
                        Column(
                            Modifier
                                .background(
                                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(b.colors.first), Color(b.colors.second))),
                                    RoundedCornerShape(18.dp),
                                )
                                .bouncyClick {
                                    space.chooseBuddy(b)
                                    space.celebrate()
                                    picking = false
                                }
                                .padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            com.kodomo.app.ui.mascot.BuddyMascot(b, MascotMood.HAPPY, size = 64.dp)
                            Text(b.title, style = MaterialTheme.typography.labelMedium, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                            Text(b.tagline, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
                        }
                    }
                }
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { picking = false }) { Text("بستن") } },
        )
    }
}
