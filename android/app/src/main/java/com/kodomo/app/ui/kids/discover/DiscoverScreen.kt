package com.kodomo.app.ui.kids.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.fx.Emoji
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity

/** «تفکر و یادگیری»: one door to life-skill lessons, fun facts and the kids encyclopedia (with approved sites). */
@Composable
fun DiscoverScreen(onBack: () -> Unit, onLessons: () -> Unit, onFacts: () -> Unit, onEncyclopedia: () -> Unit) {
    val space = LocalKidSpace.current
    TrackActivity(ActivityCategory.LEARNING, "تفکر و یادگیری")
    KidScreenScaffold(title = "تفکر و یادگیری", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Door("🧠", "مهارت‌های زندگی", "ایمنی، احساسات، علوم و آزمایش، سواد رسانه‌ای", Color(0xFF7ED7A6), Color(0xFF2E9E62)) { space.guide("مهارت‌های زندگی"); onLessons() }
            Door("💡", "دانستنی‌های شگفت‌انگیز", "هر روز چیزهای تازه درباره‌ی حیوانات، فضا و بدن ما", Color(0xFFFFC98A), Color(0xFFF08A24)) { space.guide("دانستنی‌ها"); onFacts() }
            Door("🌍", "دانشنامه و سایت‌های امن", "جستجو در دانشنامه‌ی کودک و سایت‌هایی که مامان و بابا تأیید کرده‌اند", Color(0xFF8FD3F4), Color(0xFF2B8FD6)) { space.guide("دانشنامه"); onEncyclopedia() }
        }
    }
}

@Composable
private fun Door(emoji: String, title: String, hint: String, from: Color, to: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(28.dp), spotColor = to).clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(from, to))).clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Emoji(emoji, 84.dp)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(hint, color = Color.White.copy(alpha = 0.95f), fontSize = 15.sp)
        }
    }
}
