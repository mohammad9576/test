package com.kodomo.app.ui.kids.music

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.ui.fx.Emoji
import com.kodomo.app.ui.kids.LocalKidSpace

/** Offline songs that are always there, even before parents add any music. */
@Composable
fun MusicBoxCard(onStart: () -> Unit) {
    val playing by MusicBox.playing.collectAsState()
    var sound by remember { mutableStateOf(BoxSound.MUSIC_BOX) }
    DisposableEffect(Unit) { onDispose { MusicBox.stop() } }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFCE4EC))))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Emoji("🎶", 40.dp)
            Column(Modifier.padding(start = 8.dp)) {
                Text("جعبه‌ی موسیقی", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text("آهنگ‌های جهانی کودکانه، بدون اینترنت", style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(BoxSound.entries) { s -> FilterChip(selected = s == sound, onClick = { sound = s; MusicBox.stop() }, label = { Text(s.label) }) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SleepButton("🌙 لالایی ۱۰ دقیقه", Modifier.weight(1f)) { onStart(); MusicBox.play(BoxSong.entries.filter { it.lullaby }, BoxSound.FLUTE, 10) }
            SleepButton("🌙 لالایی ۲۰ دقیقه", Modifier.weight(1f)) { onStart(); MusicBox.play(BoxSong.entries.filter { it.lullaby }, BoxSound.FLUTE, 20) }
        }
        BoxSong.entries.forEach { song ->
            val on = playing == song
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (on) Color.White else Color.White.copy(alpha = 0.55f))
                    .clickable { if (on) MusicBox.stop() else { onStart(); MusicBox.play(listOf(song), sound) } }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Emoji(song.emoji, 30.dp)
                Text(song.title, Modifier.weight(1f).padding(horizontal = 10.dp), style = MaterialTheme.typography.titleSmall)
                if (on) Equalizer(Modifier.size(width = 36.dp, height = 24.dp))
                Text(if (on) "⏹" else "▶️", fontSize = 20.sp)
            }
        }
    }
}

@Composable
private fun SleepButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF283A5B)).clickable(onClick = onClick).padding(10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge) }
}

private val barColors = listOf(0xFFE53935, 0xFFFB8C00, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFF3949AB, 0xFF8E24AA, 0xFFD81B60).map { Color(it) }
private val barNotes = listOf(60, 62, 64, 65, 67, 69, 71, 72)
private val barNames = listOf("دو", "ر", "می", "فا", "سل", "لا", "سی", "دو")

/** A playable xylophone; pick a song and the next bar to hit lights up. */
@Composable
fun XylophoneCard(onStart: () -> Unit) {
    val space = LocalKidSpace.current
    val songs = remember { BoxSong.entries.filter { s -> s.tones.all { it.midi == null || it.midi in barNotes } } }
    var guide by remember { mutableStateOf<BoxSong?>(null) }
    var step by remember { mutableIntStateOf(0) }
    val notes = guide?.tones?.mapNotNull { it.midi }.orEmpty()
    val next = notes.getOrNull(step)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFFE8EAF6)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Emoji("🎹", 40.dp)
            Column(Modifier.padding(start = 8.dp)) {
                Text("زایلوفون من", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                Text(if (guide == null) "آزاد بزن، یا یک آهنگ انتخاب کن تا یادت بدهم" else "میله‌ای که می‌درخشد را بزن: ${step} از ${notes.size}", style = MaterialTheme.typography.bodySmall)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item { FilterChip(selected = guide == null, onClick = { guide = null; step = 0 }, label = { Text("آزاد") }) }
            items(songs) { s -> FilterChip(selected = guide == s, onClick = { guide = s; step = 0; onStart() }, label = { Text(s.title) }) }
        }
        Row(Modifier.fillMaxWidth().height(190.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.Bottom) {
            barNotes.forEachIndexed { i, midi ->
                val lit = next == midi
                val scale by animateFloatAsState(if (lit) 1.06f else 1f, label = "bar")
                Box(
                    Modifier.weight(1f).fillMaxHeight(1f - i * 0.06f).scale(scale).clip(RoundedCornerShape(12.dp))
                        .background(if (lit) Color.White else barColors[i])
                        .clickable {
                            onStart()
                            MusicBox.tap(midi)
                            if (guide != null && midi == next) {
                                step++
                                if (step >= notes.size) { space.celebrate(); step = 0 }
                            }
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Text(barNames[i], color = if (lit) barColors[i] else Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
        }
    }
}
