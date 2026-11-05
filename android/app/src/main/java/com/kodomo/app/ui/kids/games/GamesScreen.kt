package com.kodomo.app.ui.kids.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.kids.childId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.domain.model.GameKind
import com.kodomo.app.domain.model.MotionGame
import com.kodomo.app.ui.components.EmojiCover
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.SectionTitle
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.mascot.MascotSpeech

@dagger.hilt.android.lifecycle.HiltViewModel
class GamesViewModel @javax.inject.Inject constructor(
    savedState: androidx.lifecycle.SavedStateHandle,
    progress: com.kodomo.app.data.prefs.GameProgressStore,
) : androidx.lifecycle.ViewModel() {
    val levels = progress.observeLevels(savedState.childId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}

private val gameColors = listOf(
    0xFFFFE6A7L to 0xFFFFB648L, 0xFFBFE3FFL to 0xFF5AA9E6L, 0xFFFFD1DCL to 0xFFF28AA6L,
    0xFFB9E4CBL to 0xFF5FB98AL, 0xFFE2D6FFL to 0xFF9B7FE6L,
)

@Composable
fun GamesScreen(onBack: () -> Unit, onPlay: (MotionGame) -> Unit, viewModel: GamesViewModel = androidx.hilt.navigation.compose.hiltViewModel()) {
    val levels by viewModel.levels.collectAsStateWithLifecycle()
    val space = LocalKidSpace.current
    TrackActivity(ActivityCategory.GAME, "فهرست بازی‌ها")
    KidScreenScaffold(title = "بازی‌ها", onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MascotSpeech("بلند شو! بیشتر این بازی‌ها با حرکت بدنت کار می‌کنند؛ بازی‌های فکری و نقاشی هم داریم. هر بار که برنده شوی، مرحله‌ی سخت‌تر باز می‌شود 🤸🧠🎨", MascotMood.EXCITED) }
            GameKind.entries.forEach { kind ->
            item(key = "title_$kind") { SectionTitle(kind.label) }
            items(MotionGame.entries.filter { it.kind == kind }, key = { it.name }) { game ->
                KodomoCard(onClick = {
                    space.guide(game.title)
                    onPlay(game)
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val colors = gameColors[game.ordinal % gameColors.size]
                        EmojiCover(game.emoji, colors.first, colors.second, Modifier.size(84.dp), emojiSize = 44)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(game.title, style = MaterialTheme.typography.titleMedium)
                            if (space.spec.showCardLabels) Text(game.description, style = MaterialTheme.typography.bodySmall)
                            if (game.kind != GameKind.ART) {
                                val lvl = levels[game.name] ?: 1
                                Text(
                                    "🏅 مرحله ${lvl.fa()} از ${com.kodomo.app.data.prefs.GameProgressStore.MAX_LEVEL.fa()}  " + "★".repeat(((lvl - 1) * 5 / com.kodomo.app.data.prefs.GameProgressStore.MAX_LEVEL).coerceAtLeast(0)),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = com.kodomo.app.ui.theme.KodomoColors.Green,
                                )
                            }
                            if (game.needsCamera) Text("📷 با دوربین", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            }
        }
    }
}
