package com.kodomo.app.ui.kids.games

import com.kodomo.app.domain.engine.VoicePhrases
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.engine.AchievementCatalog
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.domain.model.MotionGame
import com.kodomo.app.domain.repository.ActivityRepository
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoPrimaryButton
import com.kodomo.app.ui.components.rememberCameraPermission
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.games.art.ColoringGame
import com.kodomo.app.ui.kids.games.brain.ColorSequenceGame
import com.kodomo.app.ui.kids.games.brain.FlappyRobotGame
import com.kodomo.app.ui.kids.games.brain.FruitCatchGame
import com.kodomo.app.ui.kids.games.brain.MathBalloonsGame
import com.kodomo.app.ui.kids.games.brain.MemoryCardsGame
import com.kodomo.app.ui.kids.games.brain.SlidePuzzleGame
import com.kodomo.app.ui.kids.games.brain.WhackAMoleGame
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodomo.app.ui.kids.games.arcade.GoalkeeperGame
import com.kodomo.app.ui.kids.games.arcade.PenaltyGame
import com.kodomo.app.ui.kids.games.arcade.PingPongGame
import com.kodomo.app.ui.kids.games.art.FreeDrawGame
import com.kodomo.app.ui.kids.games.pose.FreezeDanceGame
import com.kodomo.app.ui.kids.games.pose.JumpPopGame
import com.kodomo.app.ui.kids.games.pose.JumpingJacksGame
import com.kodomo.app.ui.kids.games.pose.PoseMatchGame
import com.kodomo.app.ui.kids.games.pose.StarCatchGame
import com.kodomo.app.ui.kids.childId
import com.kodomo.app.ui.mascot.KodomoMascot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MotionGameViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val familyRepository: FamilyRepository,
    private val rewardRepository: RewardRepository,
    private val activityRepository: ActivityRepository,
    private val progress: com.kodomo.app.data.prefs.GameProgressStore,
) : ViewModel() {
    private val childId = savedState.childId
    private val gameName: String = savedState.get<String>(com.kodomo.app.ui.navigation.Routes.ARG_ID).orEmpty()

    /** Highest unlocked level of this game for this child (null while loading). */
    val unlocked = kotlinx.coroutines.flow.MutableStateFlow<Int?>(null)

    init {
        viewModelScope.launch { unlocked.value = progress.level(childId, gameName) }
    }

    data class Result(val score: Int, val stars: Int, val passed: Boolean, val playedLevel: Int, val unlockedLevel: Int, val medal: Boolean)

    /** Stars from the score, level progression, and the «بدن فعال» medal after five completed games. */
    fun onGameFinished(score: Int, passed: Boolean, playedLevel: Int, onResult: (Result) -> Unit) = viewModelScope.launch {
        val stars = ((score / 5).coerceIn(1, 4) + if (passed) 1 else 0).coerceAtMost(5)
        familyRepository.addStars(childId, stars)
        // Passing the highest unlocked level unlocks the next one; replaying an easier level just counts the play.
        val current = progress.level(childId, gameName)
        val newLevel = if (playedLevel >= current) progress.record(childId, gameName, score, passed) else {
            progress.record(childId, gameName, score, false)
        }
        unlocked.value = newLevel
        val played = activityRepository.countCompleted(childId, ActivityCategory.GAME, minSeconds = 20)
        val medal = played >= 5 && rewardRepository.award(childId, AchievementCatalog.ACTIVE_BODY)
        onResult(Result(score, stars, passed, playedLevel, newLevel, medal))
    }
}

@Composable
fun MotionGameScreen(game: MotionGame, onBack: () -> Unit, viewModel: MotionGameViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val unlocked by viewModel.unlocked.collectAsStateWithLifecycle()
    var result by remember { mutableStateOf<MotionGameViewModel.Result?>(null) }
    var round by remember { mutableStateOf(0) }
    var level by remember { mutableStateOf<Int?>(null) }
    val leveled = game.kind.leveled
    TrackActivity(if (game.kind == com.kodomo.app.domain.model.GameKind.ART) ActivityCategory.ART else ActivityCategory.GAME, game.title, game.name)
    LaunchedEffect(unlocked) { if (level == null && unlocked != null) level = unlocked }

    val onFinish: (Int, Boolean) -> Unit = { score, passed ->
        viewModel.onGameFinished(score, passed, level ?: 1) { r ->
            result = r
            space.voice.speak(if (r.passed) VoicePhrases.LEVEL_UP else VoicePhrases.TRY_AGAIN)
        }
    }

    KidScreenScaffold(
        title = game.title,
        onBack = onBack,
        actions = {
            if (leveled && level != null) {
                Text(
                    "مرحله ${level!!.fa()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .background(com.kodomo.app.ui.theme.KodomoColors.Green, androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val r = result
            val lvl = level
            if (r != null) {
                com.kodomo.app.ui.fx.ConfettiBurst(r)
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    LaunchedEffect(r) { com.kodomo.app.ui.fx.Sfx.success() }
                    KodomoMascot(if (r.passed) MascotMood.EXCITED else MascotMood.HAPPY, size = 170.dp)
                    if (leveled) {
                        Text(
                            if (r.passed && r.unlockedLevel > r.playedLevel) "🎉 مرحله ${r.unlockedLevel.fa()} باز شد!" else if (r.passed) "🏆 مرحله‌ی ${r.playedLevel.fa()} را بردی!" else "نزدیک بود! یک بار دیگر تلاش کن 💪",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Text("امتیاز: ${r.score.fa()}", style = MaterialTheme.typography.titleLarge)
                    Text("⭐".repeat(r.stars), style = MaterialTheme.typography.displaySmall)
                    if (r.medal) Text("🏅 مدال «بدن فعال» را گرفتی!", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    if (leveled && r.passed && r.unlockedLevel > r.playedLevel) {
                        KodomoPrimaryButton("مرحله‌ی بعد ▶", onClick = {
                            level = r.unlockedLevel
                            result = null
                            round++
                        })
                    }
                    androidx.compose.material3.OutlinedButton(onClick = {
                        result = null
                        round++
                    }, modifier = Modifier.fillMaxWidth()) { Text(if (r.passed) "دوباره همین مرحله" else "دوباره تلاش کن") }
                    if (leveled && r.unlockedLevel > 1) {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(r.unlockedLevel) { i ->
                                androidx.compose.material3.FilterChip(
                                    selected = level == i + 1,
                                    onClick = { level = i + 1; result = null; round++ },
                                    label = { Text((i + 1).fa()) },
                                )
                            }
                        }
                    }
                }
            } else if (lvl == null) {
                androidx.compose.material3.CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                val cameraOk = if (game.needsCamera) rememberCameraPermission() else true
                when {
                    !cameraOk -> Text(
                        "برای این بازی اجازه‌ی دوربین لازم است. از پدر یا مادرت کمک بگیر.",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        textAlign = TextAlign.Center,
                    )
                    else -> androidx.compose.runtime.key(round, lvl) {
                        when (game) {
                            MotionGame.TILT_RESCUE -> TiltRescueGame(lvl, onFinish)
                            MotionGame.SHAKE_CLEAN -> ShakeCleanGame(lvl, onFinish)
                            MotionGame.PING_PONG -> PingPongGame(lvl, onFinish)
                            MotionGame.PONG_BODY -> PingPongGame(lvl, onFinish, useCamera = true)
                            MotionGame.PENALTY -> PenaltyGame(lvl, onFinish)
                            MotionGame.GOALKEEPER -> GoalkeeperGame(lvl, onFinish)
                            MotionGame.JUMP_POP -> JumpPopGame(lvl, onFinish)
                            MotionGame.STAR_CATCH -> StarCatchGame(lvl, onFinish)
                            MotionGame.POSE_MATCH -> PoseMatchGame(lvl, onFinish)
                            MotionGame.FREEZE_DANCE -> FreezeDanceGame(lvl, onFinish)
                            MotionGame.JUMPING_JACKS -> JumpingJacksGame(lvl, onFinish)
                            MotionGame.FREE_DRAW -> FreeDrawGame(onFinish)
                            MotionGame.COLORING -> ColoringGame(onFinish)
                            MotionGame.SIMON_SAYS -> SimonSaysGame(lvl, onFinish)
                            MotionGame.COLOR_HUNT -> ColorHuntGame(lvl, onFinish)
                            MotionGame.MEMORY_CARDS -> MemoryCardsGame(lvl, onFinish)
                            MotionGame.WHACK_A_MOLE -> WhackAMoleGame(lvl, onFinish)
                            MotionGame.FRUIT_CATCH -> FruitCatchGame(lvl, onFinish)
                            MotionGame.FLAPPY_ROBOT -> FlappyRobotGame(lvl, onFinish)
                            MotionGame.SLIDE_PUZZLE -> SlidePuzzleGame(lvl, onFinish)
                            MotionGame.COLOR_SEQUENCE -> ColorSequenceGame(lvl, onFinish)
                            MotionGame.MATH_BALLOONS -> MathBalloonsGame(lvl, onFinish)
                            MotionGame.HAND_SLICE -> com.kodomo.app.ui.kids.games.pose.HandSliceGame(lvl, onFinish)
                            MotionGame.SHAPE_WALL -> com.kodomo.app.ui.kids.games.pose.ShapeWallGame(lvl, onFinish)
                            MotionGame.TAP_RACE -> com.kodomo.app.ui.kids.games.party.TapRaceGame(onFinish)
                            MotionGame.REFLEX_DUEL -> com.kodomo.app.ui.kids.games.party.ReflexDuelGame(onFinish)
                            MotionGame.AIR_HOCKEY -> com.kodomo.app.ui.kids.games.party.AirHockeyGame(onFinish)
                            MotionGame.TUG_OF_WAR -> com.kodomo.app.ui.kids.games.party.TugOfWarGame(onFinish)
                            MotionGame.SNAP_MATCH -> com.kodomo.app.ui.kids.games.party.SnapMatchGame(onFinish)
                            MotionGame.CAR_RACE -> com.kodomo.app.ui.kids.games.theme.CarRaceGame(lvl, onFinish)
                            MotionGame.SPACE_GUARD -> com.kodomo.app.ui.kids.games.theme.SpaceGuardGame(lvl, onFinish)
                            MotionGame.BAKERY -> com.kodomo.app.ui.kids.games.theme.BakeryGame(lvl, onFinish)
                            MotionGame.STYLIST -> com.kodomo.app.ui.kids.games.theme.StylistGame(lvl, onFinish)
                        }
                    }
                }
            }
        }
    }
}
