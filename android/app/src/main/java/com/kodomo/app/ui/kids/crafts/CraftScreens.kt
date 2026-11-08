package com.kodomo.app.ui.kids.crafts

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.domain.crafts.CraftGuide
import com.kodomo.app.domain.crafts.CraftGuides
import com.kodomo.app.domain.crafts.CraftKind
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.fx.Emoji
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity

private fun Int.fa() = toString().map { if (it.isDigit()) '۰' + (it - '0') else it }.joinToString("")

private val kindColors = mapOf(
    CraftKind.ORIGAMI to (Color(0xFFB3E5FC) to Color(0xFF0277BD)),
    CraftKind.RECYCLE to (Color(0xFFC8E6C9) to Color(0xFF2E7D32)),
    CraftKind.PAPER to (Color(0xFFFFE0B2) to Color(0xFFEF6C00)),
    CraftKind.COLOR to (Color(0xFFF8BBD0) to Color(0xFFC2185B)),
    CraftKind.SCIENCE to (Color(0xFFD1C4E9) to Color(0xFF5E35B1)),
)

/** «کاردستی»: learn a craft step by step first, then add your own creation to «پروژه‌های من». */
@Composable
fun CraftsScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onMyProjects: () -> Unit) {
    val space = LocalKidSpace.current
    TrackActivity(ActivityCategory.PROJECT, "کاردستی")
    val guides = remember(space.child.id) { CraftGuides.forAge(space.child.ageYears()) }
    var kind by remember { mutableStateOf<CraftKind?>(null) }
    val shown = guides.filter { kind == null || it.kind == kind }
    KidScreenScaffold(title = "کاردستی و اوریگامی", onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))))
                        .clickable(onClick = onMyProjects).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Emoji("🖼️", 56.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("پروژه‌های من", fontWeight = FontWeight.Black, fontSize = 19.sp)
                        Text("چیزی ساختی؟ عکسش را بگیر و اینجا نگه دار", fontSize = 14.sp)
                    }
                    Text("➕", fontSize = 26.sp)
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = kind == null, onClick = { kind = null }, label = { Text("همه (${guides.size.fa()})") }) }
                    items(CraftKind.entries.filter { k -> guides.any { it.kind == k } }) { k ->
                        FilterChip(selected = kind == k, onClick = { kind = k }, label = { Text("${k.emoji} ${k.label}") })
                    }
                }
            }
            items(shown, key = { it.id }) { g -> GuideCard(g) { space.guide(g.title); onOpen(g.id) } }
        }
    }
}

@Composable
private fun GuideCard(g: CraftGuide, onClick: () -> Unit) {
    val (from, to) = kindColors.getValue(g.kind)
    Row(
        Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(24.dp)).clip(RoundedCornerShape(24.dp)).background(Color.White).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(from, to))).padding(10.dp)) { Emoji(g.emoji, 56.dp) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(g.title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF263238))
            Text("${g.kind.emoji} ${g.kind.label} • ${g.steps.size.fa()} مرحله • ${g.minutes.fa()} دقیقه", fontSize = 13.sp, color = Color(0xFF546E7A))
            if (g.withAdult) Text("👨‍👩‍👧 با کمک بزرگ‌تر", fontSize = 13.sp, color = to)
        }
    }
}

@Composable
fun CraftGuideScreen(id: String, onBack: () -> Unit, onMyProjects: () -> Unit) {
    val space = LocalKidSpace.current
    val found = remember(id) { CraftGuides.byId(id) }
    if (found == null) { LaunchedEffect(Unit) { onBack() }; return }
    val g: CraftGuide = found
    TrackActivity(ActivityCategory.PROJECT, g.title, g.id)
    // page 0 = materials, then the steps, then «done»
    var page by remember { mutableIntStateOf(0) }
    val last = g.steps.size + 1
    LaunchedEffect(page) {
        when (page) {
            0 -> space.voice.speak("${g.title}. وسایل لازم: ${g.materials.joinToString("، ")}")
            last -> { space.voice.speak("آفرین! ${g.title} را ساختی!"); space.celebrate() }
            else -> space.voice.speak(g.steps[page - 1].text)
        }
    }
    val (from, to) = kindColors.getValue(g.kind)
    KidScreenScaffold(title = g.title, onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            LinearProgressIndicator(progress = { page / last.toFloat() }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)), color = to)
            Spacer(Modifier.height(16.dp))
            AnimatedContent(page, label = "step", modifier = Modifier.weight(1f)) { p ->
                Column(
                    Modifier.fillMaxSize().clip(RoundedCornerShape(32.dp)).background(Brush.verticalGradient(listOf(from.copy(alpha = 0.6f), Color.White))).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    when (p) {
                        0 -> {
                            Emoji(g.emoji, 150.dp)
                            Text("وسایل لازم", fontWeight = FontWeight.Black, fontSize = 24.sp)
                            g.materials.forEach { Text("• $it", fontSize = 19.sp, textAlign = TextAlign.Center) }
                            if (g.withAdult) Text("👨‍👩‍👧 این کاردستی را با کمک یک بزرگ‌تر انجام بده.", fontSize = 16.sp, color = to, textAlign = TextAlign.Center)
                        }
                        last -> {
                            Emoji("🎉", 150.dp)
                            Text("آفرین، ساختی!", fontWeight = FontWeight.Black, fontSize = 28.sp)
                            Text("حالا از کاردستی‌ات عکس بگیر و به «پروژه‌های من» اضافه کن تا مامان و بابا هم ببینند.", fontSize = 18.sp, textAlign = TextAlign.Center)
                        }
                        else -> {
                            val s = g.steps[p - 1]
                            Text("مرحله‌ی ${p.fa()} از ${g.steps.size.fa()}", fontSize = 16.sp, color = to, fontWeight = FontWeight.Bold)
                            Emoji(s.emoji, 150.dp)
                            Text(s.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 34.sp, color = Color(0xFF263238))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { if (page > 0) page-- }, enabled = page > 0, modifier = Modifier.weight(1f)) { Text("قبلی", fontSize = 18.sp) }
                if (page < last) Button(onClick = { page++ }, modifier = Modifier.weight(1f)) { Text(if (page == 0) "شروع ▶" else "بعدی ▶", fontSize = 18.sp) }
                else Button(onClick = onMyProjects, modifier = Modifier.weight(1f)) { Text("📸 به پروژه‌هایم", fontSize = 18.sp) }
            }
        }
    }
}
