package com.kodomo.app.ui.kids.video

import com.kodomo.app.domain.engine.VoicePhrases
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodomo.app.core.util.fa
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoAvatar
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.KodomoPrimaryButton
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.mascot.MascotSpeech
import com.kodomo.app.ui.theme.KodomoColors

/** «باران، این انیمیشن را دوست داشتی؟» – child feedback + peer opinion. */
@Composable
fun FeedbackScreen(contentId: String, onDone: () -> Unit, viewModel: FeedbackViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val item by viewModel.item.collectAsStateWithLifecycle()
    val opinion by viewModel.opinion.collectAsStateWithLifecycle()
    var liked by remember { mutableStateOf<Boolean?>(null) }
    var comment by remember { mutableStateOf("") }
    var thanks by remember { mutableStateOf<String?>(null) }

    KidScreenScaffold(title = "بازخورد محتوا", onBack = onDone) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KodomoAvatar(space.child.avatar, size = 72.dp)
            Text(
                "${space.child.name}، «${item?.title.orEmpty()}» را دوست داشتی؟",
                style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                FeedbackChoice("❤️", "دوست داشتم", selected = liked == true, color = KodomoColors.Green, modifier = Modifier.weight(1f)) {
                    liked = true
                    space.guide("دوست داشتم")
                }
                FeedbackChoice("👎", "دوست نداشتم", selected = liked == false, color = KodomoColors.Peach, modifier = Modifier.weight(1f)) {
                    liked = false
                    space.guide("دوست نداشتم")
                }
            }
            if (space.spec.showCardLabels && space.child.ageYears() >= 5) {
                OutlinedTextField(
                    value = comment, onValueChange = { comment = it.take(280) },
                    placeholder = { Text("نظرت را درباره‌ی این انیمیشن بگو...") },
                    modifier = Modifier.fillMaxWidth(), minLines = 3, shape = RoundedCornerShape(18.dp),
                )
            }
            KodomoPrimaryButton(
                text = "ارسال نظر",
                enabled = liked != null,
                onClick = {
                    viewModel.submit(liked == true, comment) { medal ->
                        thanks = if (medal) VoicePhrases.FEEDBACK_MEDAL else VoicePhrases.FEEDBACK_THANKS
                        space.voice.speak(thanks.orEmpty())
                    }
                },
            )
            thanks?.let {
                MascotSpeech(it, MascotMood.EXCITED)
                KodomoPrimaryButton("بازگشت", onClick = onDone)
            }
            KodomoCard(Modifier.fillMaxWidth()) {
                Text("نظر همسالان تو", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text("${opinion.likePercent.fa()}٪ از بچه‌ها این را دوست داشتند", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { opinion.likePercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    color = KodomoColors.Green,
                    trackColor = KodomoColors.Peach.copy(alpha = 0.3f),
                )
                opinion.recentComments.forEach { c ->
                    Spacer(Modifier.height(8.dp))
                    Text("💬 $c", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun FeedbackChoice(emoji: String, label: String, selected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .height(150.dp)
            .background(if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface, RoundedCornerShape(26.dp))
            .border(if (selected) 3.dp else 1.dp, if (selected) color else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(26.dp))
            .bouncyClick(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(emoji, fontSize = 56.sp)
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
