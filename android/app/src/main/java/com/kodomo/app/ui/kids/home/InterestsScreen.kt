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
import com.kodomo.app.domain.model.MotionGame as G
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.stateIn
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.navigation.Routes
import com.kodomo.app.ui.navigation.ReadingMode

/** A suggestion shown under «علایق من»: any book, video, lesson, discovery or game that matches the interests. */
data class Suggestion(val title: String, val emoji: String, val kind: String, val route: String, val interests: Set<Interest>)

/** Which Kodomo games fit which interest. */
private val gamesByInterest: Map<Interest, List<com.kodomo.app.domain.model.MotionGame>> =
    mapOf(
        Interest.SPORT to listOf(G.BOXING, G.SKI, G.SPRINT_RACE, G.BASKETBALL, G.SQUAT, G.JUMP_ROPE, G.PING_PONG, G.PENALTY, G.GOALKEEPER, G.JUMPING_JACKS, G.SHAPE_WALL),
        Interest.ART to listOf(G.FREE_DRAW, G.COLORING, G.STYLIST, G.BAKERY),
        Interest.GAMES to listOf(G.TAP_RACE, G.AIR_HOCKEY, G.MEMORY_CARDS, G.CAR_RACE, G.SNAP_MATCH),
        Interest.SCIENCE to listOf(G.MATH_BALLOONS, G.SLIDE_PUZZLE, G.COLOR_SEQUENCE, G.TILT_RESCUE),
        Interest.ASTRONOMY to listOf(G.SPACE_GUARD, G.STAR_CATCH),
        Interest.ANIMALS to listOf(G.WHACK_A_MOLE, G.FRUIT_CATCH, G.SHAKE_CLEAN),
        Interest.MUSIC to listOf(G.FREEZE_DANCE, G.SIMON_SAYS, G.COLOR_SEQUENCE),
        Interest.BOOKS to listOf(G.MEMORY_CARDS),
        Interest.MOVIES to listOf(G.HAND_SLICE),
    )

@HiltViewModel
class InterestsViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    contentRepository: com.kodomo.app.domain.repository.ContentRepository,
    savedState: androidx.lifecycle.SavedStateHandle,
) : ViewModel() {
    private val childId = savedState.childId

    /** Everything the child is allowed to see, turned into suggestions. Filtered by the live interest selection in the UI. */
    val suggestions = kotlinx.coroutines.flow.combine(
        contentRepository.observeAllowedForChild(childId, com.kodomo.app.domain.model.ContentType.BOOK),
        contentRepository.observeAllowedForChild(childId, com.kodomo.app.domain.model.ContentType.VIDEO),
        contentRepository.observeAllowedForChild(childId, com.kodomo.app.domain.model.ContentType.LEARNING),
        contentRepository.observeAllowedForChild(childId, com.kodomo.app.domain.model.ContentType.EXPLORE),
    ) { books, videos, lessons, explore ->
        books.map { Suggestion(it.title, it.emoji, "📚 کتاب", Routes.kidBook(childId, it.id, ReadingMode.AUDIO.name), it.interests) } +
            videos.filter { it.playableUrl != null }.map { Suggestion(it.title, it.emoji, "🎬 فیلم", Routes.kidVideo(childId, it.id), it.interests) } +
            lessons.map { Suggestion(it.title, it.emoji, "🔬 آموزش", Routes.kidLesson(childId, it.id), it.interests) } +
            explore.map { Suggestion(it.title, it.emoji, "🧭 کشف", Routes.kidExploreItem(childId, it.id), it.interests) } +
            gamesByInterest.flatMap { (interest, games) -> games.map { Suggestion(it.title, it.emoji, "🎮 بازی", Routes.kidGame(childId, it), setOf(interest)) } }
                .groupBy { it.route }.map { (_, list) -> list.first().copy(interests = list.flatMap { it.interests }.toSet()) }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(child: Child, interests: Set<Interest>, onDone: () -> Unit) = viewModelScope.launch {
        familyRepository.updateChild(child.copy(interests = interests))
        onDone()
    }
}

@Composable
fun InterestsScreen(onBack: () -> Unit, onOpen: (String) -> Unit = {}, viewModel: InterestsViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    var selected by remember { mutableStateOf(space.child.interests) }
    val all by viewModel.suggestions.collectAsStateWithLifecycle()
    // Mix the kinds so the list is not just books; a stable shuffle per child keeps it from jumping around.
    val picks = remember(all, selected) {
        all.filter { it.interests.intersect(selected).isNotEmpty() }
            .sortedByDescending { it.interests.intersect(selected).size }
            .groupBy { it.kind }.values
            .map { it.shuffled(kotlin.random.Random(space.child.id)) }
            .let { groups -> (0 until (groups.maxOfOrNull { it.size } ?: 0)).flatMap { i -> groups.mapNotNull { it.getOrNull(i) } } }
            .take(24)
    }
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
                    Box { com.kodomo.app.ui.fx.Emoji(interest.emoji, 46.dp) }
                    Text(interest.label, style = MaterialTheme.typography.labelLarge)
                    if (on) com.kodomo.app.ui.fx.Emoji("✅", 17.dp)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                KodomoPrimaryButton("ذخیره‌ی تغییرات", onClick = { viewModel.save(space.child, selected) {} ; space.celebrate() })
            }
            if (picks.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("✨ پیشنهادهای مخصوص تو", style = MaterialTheme.typography.titleLarge)
                }
                items(picks, span = { GridItemSpan(maxLineSpan) }) { s ->
                    androidx.compose.foundation.layout.Row(
                        Modifier
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp))
                            .bouncyClick { space.guide(s.title); onOpen(s.route) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        com.kodomo.app.ui.fx.Emoji(s.emoji, 41.dp)
                        androidx.compose.foundation.layout.Spacer(Modifier.padding(horizontal = 6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleSmall)
                            Text(s.kind + "  " + s.interests.intersect(selected).joinToString(" ") { it.emoji }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("▶", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (selected.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { Text("پیشنهادی پیدا نشد؛ علاقه‌ی دیگری را هم امتحان کن.", style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
