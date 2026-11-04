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
) : ViewModel() {
    val childId = savedState.childId
    val dialogue = MascotDialogueEngine()
    val time: StateFlow<ScreenTimeManager.State> = screenTime.state
    val achievements: StateFlow<List<EarnedAchievement>> =
        rewardRepository.observeAchievements(childId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleDarkMode(child: Child) = viewModelScope.launch { familyRepository.updateChild(child.copy(darkMode = !child.darkMode)) }
}

private data class HomeModule(
    val title: String,
    val subtitle: String,
    val emoji: String,
    val colors: Pair<Long, Long>,
    val route: String,
)

private fun modulesFor(child: Child): List<HomeModule> {
    val id = child.id
    val age = child.ageYears()
    val group = child.ageGroup()
    val all = buildList {
        add(HomeModule("فیلم و انیمیشن", "محتوای امن و رده‌بندی‌شده", "🎬", 0xFFBFE3FF to 0xFF5AA9E6, Routes.kidVideos(id)))
        add(HomeModule("بازی حرکتی", "بازی با بدن و حسگرها", "🤸", 0xFFFFD1DC to 0xFFF28AA6, Routes.kidGames(id)))
        add(HomeModule("کتاب", "صوتی، با صدای والد، خودم می‌خوانم", "📚", 0xFFFFE6A7 to 0xFFFFB648, Routes.kidBooks(id)))
        add(HomeModule("موسیقی", "آهنگ‌های شاد و آرام", "🎵", 0xFFE2D6FF to 0xFF9B7FE6, Routes.kidMusic(id)))
        if (group != AgeGroup.TODDLER) {
            add(HomeModule(if (group == AgeGroup.PRE_TEEN) "تفکر و یادگیری" else "آموزش", "علوم، سواد رسانه‌ای، کاردستی", "🔬", 0xFFB9E4CB to 0xFF5FB98A, Routes.kidLearning(id)))
        }
        if (age >= AgeGroup.SCHOOL_MODULE_MIN_AGE) {
            add(HomeModule("مدرسه من", "برنامه، تکالیف، امتحان‌ها", "🏫", 0xFFD6E4F5 to 0xFF4C6A92, Routes.kidSchool(id)))
        }
        add(HomeModule("کشف محتوا", "چیزهای تازه و امن", "🧭", 0xFFFFD9C2 to 0xFFF59E6C, Routes.kidExplore(id)))
        if (group != AgeGroup.TODDLER) {
            add(HomeModule("مرورگر امن من", "جستجو در دانشنامه‌ی کودک", "🌍", 0xFFC6F0EA to 0xFF2BAEA0, Routes.kidBrowser(id)))
            add(HomeModule("پروژه‌های من", "کاردستی ۵ مرحله‌ای", "✂️", 0xFFF3DCC8 to 0xFFB57A4A, Routes.kidProjects(id)))
        }
        add(HomeModule("پاداش‌ها", "ستاره، مدال و جایزه‌ی خانوادگی", "🏆", 0xFFFFF1C7 to 0xFFFFC83D, Routes.kidRewards(id)))
        if (group != AgeGroup.TODDLER) add(HomeModule("علایق من", "انتخاب موضوع‌های مورد علاقه", "💚", 0xFFDDF7E8 to 0xFF3FA977, Routes.kidInterests(id)))
    }
    return all
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
    val modules = remember(child) { modulesFor(child) }

    BackHandler(enabled = true) { }
    TrackActivity(ActivityCategory.HOME, "خانه")
    LaunchedEffect(child.id) { space.voice.speak(greeting.text) }

    val bounce = rememberInfiniteTransition(label = "avatar")
    val avatarScale by bounce.animateFloat(1f, if (spec.gamified) 1.06f else 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "s")

    LazyVerticalGrid(
        columns = GridCells.Fixed(spec.gridColumns),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KodomoAvatar(child.avatar, size = 52.dp, modifier = Modifier.graphicsLayer { scaleX = avatarScale; scaleY = avatarScale })
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("سلام ${child.name}!", style = MaterialTheme.typography.titleLarge)
                    if (spec.gamified || spec.group == AgeGroup.PRE_TEEN) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StarBadge(child.stars)
                            val medals = achievements.count { it.earnedAt != null }
                            Text("🏅 ${medals.fa()}", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                if (time.child?.id == childId) TimeLeftChip(time.remainingSeconds, time.progress)
                if (spec.darkThemeAllowed) {
                    IconButton(onClick = { viewModel.toggleDarkMode(child) }) {
                        Icon(if (child.darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, contentDescription = "تغییر تم")
                    }
                }
                Box(
                    Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .combinedClickable(onClick = {}, onLongClick = space.requestParentExit),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = "ورود والدین (نگه دارید)", modifier = Modifier.size(18.dp))
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            MascotSpeech(
                text = greeting.text,
                mood = greeting.mood,
                mascotSize = if (spec.group == AgeGroup.TODDLER) 130.dp else 104.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(spec.cornerRadiusDp.dp))
                    .padding(10.dp),
            )
        }
        items(modules, key = { it.route }) { module ->
            ModuleTile(
                title = module.title,
                emoji = module.emoji,
                colors = module.colors,
                subtitle = module.subtitle,
                onClick = {
                    space.guide(module.title)
                    onOpen(module.route)
                },
            )
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
    }
}
