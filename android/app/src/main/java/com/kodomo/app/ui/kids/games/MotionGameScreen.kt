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
) : ViewModel() {
    private val childId = savedState.childId

    /** Awards stars proportional to the score, and the «بدن فعال» medal after five completed motion games. */
    fun onGameFinished(score: Int, onResult: (stars: Int, medal: Boolean) -> Unit) = viewModelScope.launch {
        val stars = (score / 5).coerceIn(1, 5)
        familyRepository.addStars(childId, stars)
        val played = activityRepository.countCompleted(childId, ActivityCategory.GAME, minSeconds = 20)
        val medal = played >= 5 && rewardRepository.award(childId, AchievementCatalog.ACTIVE_BODY)
        onResult(stars, medal)
    }
}

@Composable
fun MotionGameScreen(game: MotionGame, onBack: () -> Unit, viewModel: MotionGameViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    var finished by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var round by remember { mutableStateOf(0) }
    var medal by remember { mutableStateOf(false) }
    TrackActivity(if (game.kind == com.kodomo.app.domain.model.GameKind.ART) ActivityCategory.ART else ActivityCategory.GAME, game.title, game.name)

    val onFinish: (Int) -> Unit = { score ->
        viewModel.onGameFinished(score) { stars, gotMedal ->
            finished = score to stars
            medal = gotMedal
            space.voice.speak(VoicePhrases.GAME_DONE)
        }
    }

    KidScreenScaffold(title = game.title, onBack = onBack) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val result = finished
            if (result != null) com.kodomo.app.ui.fx.ConfettiBurst(result)
            if (result != null) {
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    LaunchedEffect(result) { com.kodomo.app.ui.fx.Sfx.success() }
                    KodomoMascot(MascotMood.EXCITED, size = 180.dp)
                    Text("امتیاز: ${result.first.fa()}", style = MaterialTheme.typography.headlineMedium)
                    Text("⭐".repeat(result.second), style = MaterialTheme.typography.displaySmall)
                    if (medal) Text("🏅 مدال «بدن فعال» را گرفتی!", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    KodomoPrimaryButton("دوباره بازی کنیم!", onClick = {
                        finished = null
                        round++
                    })
                }
            } else {
                val cameraOk = if (game.needsCamera) rememberCameraPermission() else true
                when {
                    !cameraOk -> Text(
                        "برای این بازی اجازه‌ی دوربین لازم است. از پدر یا مادرت کمک بگیر.",
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        textAlign = TextAlign.Center,
                    )
                    else -> androidx.compose.runtime.key(round) {
                        when (game) {
                            MotionGame.TILT_RESCUE -> TiltRescueGame(onFinish)
                            MotionGame.SHAKE_CLEAN -> ShakeCleanGame(onFinish)
                            MotionGame.PING_PONG -> PingPongGame(onFinish)
                            MotionGame.PONG_BODY -> PingPongGame(onFinish, useCamera = true)
                            MotionGame.PENALTY -> PenaltyGame(onFinish)
                            MotionGame.GOALKEEPER -> GoalkeeperGame(onFinish)
                            MotionGame.JUMP_POP -> JumpPopGame(onFinish)
                            MotionGame.STAR_CATCH -> StarCatchGame(onFinish)
                            MotionGame.POSE_MATCH -> PoseMatchGame(onFinish)
                            MotionGame.FREEZE_DANCE -> FreezeDanceGame(onFinish)
                            MotionGame.JUMPING_JACKS -> JumpingJacksGame(onFinish)
                            MotionGame.FREE_DRAW -> FreeDrawGame(onFinish)
                            MotionGame.COLORING -> ColoringGame(onFinish)
                            MotionGame.SIMON_SAYS -> SimonSaysGame(onFinish)
                            MotionGame.COLOR_HUNT -> ColorHuntGame(onFinish)
                        }
                    }
                }
            }
        }
    }
}
