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
        add(HomeModule("درس و تمرین", "مطابق پایه‌ی تحصیلی + جایزه‌ی بازی", "📝", 0xFFFFF59D to 0xFFF9A825, Routes.kidStudy(id)))
        add(HomeModule("انگلیسی", "Hello! از صفر با تلفظ", "🔤", 0xFFD1C4E9 to 0xFF7E57C2, Routes.kidEnglish(id)))
        add(HomeModule("شبکه پویا", "کارتون زنده، فقط همین شبکه", "📺", 0xFFFFE0B2 to 0xFFFB8C00, Routes.kidLiveTv(id)))
        add(HomeModule("بازی و نقاشی", "بازی با بدن، حسگرها و نقاشی", "🤸", 0xFFFFD1DC to 0xFFF28AA6, Routes.kidGames(id)))
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
        add(HomeModule("برنامه‌های من", "بازی‌ها و برنامه‌های مجاز", "📱", 0xFFE3DAFF to 0xFF7E57C2, Routes.kidApps(id)))
        add(HomeModule("کمک به خانواده", "کار خوب انجام بده، ستاره بگیر", "🧹", 0xFFDCEDC8 to 0xFF7CB342, Routes.kidChores(id)))
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
    // Greet once when the child enters the kids space, not every time they come back to the home screen.
    LaunchedEffect(child.id) { if (GreetingMemory.shouldGreet(child.id)) space.voice.speakLine(greeting) }

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
            BuddyChooserRow()
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
        items(modules.size, key = { modules[it].route }) { i ->
            val module = modules[i]
            ModuleTile(
                index = i,
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
        item(span = { GridItemSpan(maxLineSpan) }) {
            com.kodomo.app.ui.kids.wellbeing.CallParentsRow(com.kodomo.app.ui.kids.LocalWellbeing.current, space.callParent)
        }
        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.height(24.dp)) }
    }
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

/** «دوست من»: shows the chosen companion and lets the child swap it for another character. */
@Composable
private fun BuddyChooserRow() {
    val space = LocalKidSpace.current
    val buddy = com.kodomo.app.ui.mascot.LocalBuddy.current
    var picking by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(buddy.colors.first).copy(alpha = 0.6f), RoundedCornerShape(50))
            .bouncyClick { picking = true }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.kodomo.app.ui.fx.Emoji(buddy.emoji, 26.dp)
        Spacer(Modifier.width(8.dp))
        Text("دوست من: ${buddy.title}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
        Text("عوض کن 🔄", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
