package com.kodomo.app.ui.kids.games

import com.kodomo.app.domain.engine.VoicePhrases
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.mascot.KodomoMascot
import com.kodomo.app.ui.theme.KodomoColors
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Voice-driven Simon Says. The mascot speaks a command; the accelerometer (phone held in hand) measures whether the
 * child moved. Moving is correct only when the command started with «سایمون می‌گه».
 */
@Composable
fun SimonSaysGame(level: Int, onFinish: (Int, Boolean) -> Unit) {
    val space = LocalKidSpace.current
    val commands = VoicePhrases.simonCommands
    var round by remember { mutableIntStateOf(0) }
    var correct by remember { mutableIntStateOf(0) }
    var prompt by remember { mutableStateOf(VoicePhrases.SIMON_READY) }
    var feedback by remember { mutableStateOf("") }
    var windowProgress by remember { mutableFloatStateOf(0f) }
    var energy by remember { mutableFloatStateOf(0f) }
    val rounds = 7 + level

    AccelerometerEffect { x, y, z -> energy += abs(sqrt(x * x + y * y + z * z) - 9.81f) }

    LaunchedEffect(Unit) {
        space.voice.speak(prompt)
        delay(3_000)
        while (round < rounds) {
            val simon = Random.nextFloat() < 0.65f
            val command = commands.random()
            prompt = if (simon) VoicePhrases.simon(command) else command
            feedback = ""
            space.voice.speak(prompt)
            delay(1_200)
            energy = 0f
            val windowMs = (4_300L - level * 220L).coerceAtLeast(2_000L)
            val start = System.currentTimeMillis()
            while (System.currentTimeMillis() - start < windowMs) {
                windowProgress = (System.currentTimeMillis() - start) / windowMs.toFloat()
                delay(50)
            }
            windowProgress = 0f
            val moved = energy > 120f
            val ok = moved == simon
            if (ok) correct++
            feedback = when {
                ok && simon -> VoicePhrases.SIMON_GOOD
                ok -> VoicePhrases.SIMON_NOT_FOOLED
                simon -> VoicePhrases.SIMON_SHOULD_MOVE
                else -> VoicePhrases.SIMON_FOOLED
            }
            space.voice.speak(feedback)
            round++
            delay(1_800)
        }
        onFinish(correct * 5, correct >= (rounds * 0.7f).toInt())
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("دور ${(round + 1).coerceAtMost(rounds).fa()} از ${rounds.fa()} • درست: ${correct.fa()}", style = MaterialTheme.typography.titleMedium)
        KodomoMascot(if (feedback.isEmpty()) MascotMood.THINKING else MascotMood.EXCITED, size = 170.dp)
        Text(
            prompt,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(22.dp))
                .padding(20.dp),
        )
        LinearProgressIndicator(progress = { windowProgress }, modifier = Modifier.fillMaxWidth().height(10.dp), color = KodomoColors.Sun)
        Text(feedback, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
    }
}
