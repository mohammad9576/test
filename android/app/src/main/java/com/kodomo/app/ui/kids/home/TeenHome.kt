package com.kodomo.app.ui.kids.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kodomo.app.core.util.PersianCalendar
import com.kodomo.app.core.util.fa
import com.kodomo.app.ui.design.AgeBand
import com.kodomo.app.ui.design.Art
import com.kodomo.app.ui.fx.Emoji
import java.time.LocalDate
import java.time.LocalTime

/** Per-child «continue» (recently opened sections) and «saved» shelf, kept on the phone. */
object TeenShelf {
    private const val PREFS = "kodomo_teen"
    fun recent(context: Context, childId: Long): List<String> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("recent_$childId", "").orEmpty().split(',').filter { it.isNotBlank() }

    fun touch(context: Context, childId: Long, key: String) {
        val list = (listOf(key) + recent(context, childId).filter { it != key }).take(8)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("recent_$childId", list.joinToString(",")).apply()
    }
}

/** V5 minimal palettes: plain backgrounds, no scenery; children get colourful 3D icons, teens calm violet/slate. */
object V5 {
    data class Palette(val bg: Color, val surface: Color, val ink: Color, val sub: Color, val line: Color, val accent: Color, val accentSoft: Color)

    fun kid(night: Boolean) = if (night) Palette(Color(0xFF1C2126), Color(0xFF262C33), Color(0xFFF2F4F6), Color(0xFFA9B1BA), Color(0xFF353C44), Color(0xFF7EE2B0), Color(0xFF2B4A3B))
    else Palette(Color(0xFFFFFFFF), Color(0xFFF4F6F8), Color(0xFF1F2933), Color(0xFF6B7682), Color(0xFFE6E9EC), Color(0xFF1E8E5A), Color(0xFFDDF3E6))

    fun teen(night: Boolean) = if (night) Palette(Color(0xFF1D1E22), Color(0xFF2A2B31), Color(0xFFF3F2F7), Color(0xFFA7A6B3), Color(0xFF34353C), Color(0xFFC3B2FF), Color(0xFF3A3452))
    else Palette(Color(0xFFFFFFFF), Color(0xFFF3F2F8), Color(0xFF1E1E26), Color(0xFF6E6C7B), Color(0xFFE8E7EE), Color(0xFF7B5CE0), Color(0xFFEDE7FF))

    /** The four everyday doors (fixed dock) — never repeated in the grid above. */
    val dockKeys = listOf("videos", "games", "books", "study")

    fun dockLabel(key: String) = when (key) { "videos" -> "فیلم"; "games" -> "بازی"; "books" -> "کتاب"; else -> "درس" }

    fun lineIcon(key: String): ImageVector = when (key) {
        "videos" -> Icons.Outlined.PlayCircle
        "games" -> Icons.Outlined.SportsEsports
        "books" -> Icons.AutoMirrored.Outlined.MenuBook
        "study" -> Icons.Outlined.School
        "english" -> Icons.Outlined.Translate
        "livetv" -> Icons.Outlined.LiveTv
        "music" -> Icons.Outlined.MusicNote
        "discover" -> Icons.Outlined.Science
        "crafts" -> Icons.Outlined.ContentCut
        "camera" -> Icons.Outlined.PhotoCamera
        "call" -> Icons.Outlined.Call
        "planner" -> Icons.Outlined.CalendarMonth
        "chores" -> Icons.Outlined.TaskAlt
        "apps" -> Icons.Outlined.Apps
        "rewards" -> Icons.Outlined.EmojiEvents
        else -> Icons.Outlined.FavoriteBorder
    }

    /** Child-facing 3D picture for each section (Fluent 3D emoji set bundled in assets). */
    fun picture(m: HomeModule) = when (m.key) {
        "english" -> "🔤"; "music" -> "🎵"; "crafts" -> "✂️"; "camera" -> "📷"; "chores" -> "✅"; "apps" -> "🧩"
        "discover" -> "🔬"; "planner" -> "🗓️"; "rewards" -> "🏆"; "interests" -> "💚"; "livetv" -> "📺"; "call" -> "📞"
        else -> m.emoji
    }
}

@Composable
private fun Clock(): Pair<String, String> {
    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(15_000); now = LocalTime.now() } }
    val today = LocalDate.now()
    val j = PersianCalendar.toJalali(today)
    return "%d:%02d".format(now.hour, now.minute).fa() to "امروز: ${PersianCalendar.dayName(today.dayOfWeek)} ${j.day.fa()} ${PersianCalendar.monthName(j.month)}"
}

/** Bottom of every home: the fixed four-door dock, then «call family» and the parent lock — separate from the dock. */
@Composable
private fun HomeFooter(p: V5.Palette, dock: List<HomeModule>, teen: Boolean, onOpen: (HomeModule) -> Unit, onCallFamily: () -> Unit, onParent: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(p.bg).navigationBarsPadding()) {
        HorizontalDivider(color = p.line)
        if (!teen) FamilyRow(p, onCallFamily, onParent)
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            dock.forEach { m ->
                Column(
                    Modifier.heightIn(min = 64.dp).width(76.dp).clip(RoundedCornerShape(16.dp)).clickable { onOpen(m) }.padding(vertical = 6.dp)
                        .semantics { contentDescription = V5.dockLabel(m.key) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(V5.lineIcon(m.key), contentDescription = null, tint = if (teen) p.accent else p.ink, modifier = Modifier.size(28.dp))
                    Text(V5.dockLabel(m.key), style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = p.ink))
                }
            }
        }
    }
}

@Composable
private fun FamilyRow(p: V5.Palette, onCallFamily: () -> Unit, onParent: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onCallFamily), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Call, contentDescription = null, tint = Color(0xFF1E8E5A), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text("تماس خانواده", style = TextStyle(fontSize = 14.sp, color = p.ink, fontWeight = FontWeight.Medium))
        }
        Box(Modifier.width(1.dp).height(24.dp).background(p.line))
        Row(Modifier.weight(1f).heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onParent), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF1E8E5A), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text("قفل والدین", style = TextStyle(fontSize = 14.sp, color = p.ink, fontWeight = FontWeight.Medium))
        }
    }
}

/**
 * Children's home (under 10), Samsung-Kids simple: big 3D pictures on an open, plain page, few choices,
 * one goal. Under 5: two columns, almost no text, robot guide; 5–7: three columns, six choices;
 * 7–10: three columns plus one «continue» row.
 */
@Composable
fun KidHomeV5(
    childId: Long,
    name: String,
    band: AgeBand,
    girl: Boolean,
    modules: List<HomeModule>,
    remainingSeconds: Long?,
    night: Boolean,
    character: com.kodomo.app.ui.design.CharacterKind,
    onParent: () -> Unit,
    onCallFamily: () -> Unit,
    onOpen: (HomeModule) -> Unit,
) {
    val context = LocalContext.current
    val p = V5.kid(night)
    val open: (HomeModule) -> Unit = { m -> TeenShelf.touch(context, childId, m.key); onOpen(m) }
    val dock = modules.filter { it.key in V5.dockKeys }.sortedBy { V5.dockKeys.indexOf(it.key) }
    val rest = modules.filter { it.key !in V5.dockKeys && it.key != "call" }
    val columns = if (band == AgeBand.UNDER_5) 2 else 3
    val limit = if (band == AgeBand.UNDER_5) 4 else 6
    var more by remember { mutableStateOf(false) }
    val shown = if (more || rest.size <= limit) rest else rest.take(limit - 1)
    val (clock, date) = Clock()
    val minutes = remainingSeconds?.let { (it / 60).toInt() }

    Column(Modifier.fillMaxSize().background(p.bg)) {
        Column(Modifier.weight(1f).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            if (band == AgeBand.UNDER_5) {
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Art("robot/wave", 72.dp) { Emoji("🤖", 64.dp) }
                    Spacer(Modifier.weight(1f))
                    if (minutes != null) TimePill(p, "زمان باقی‌مانده", "${minutes.fa()} دقیقه")
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    com.kodomo.app.ui.design.CharacterAvatar(character, 60.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("سلام $name!", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Black, color = p.ink))
                        Text(date, style = TextStyle(fontSize = 13.sp, color = p.sub))
                    }
                    Text(clock, style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, color = p.ink))
                }
                if (minutes != null) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.accentSoft).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("زمان باقی‌مانده‌ی امروز: ${minutes.fa()} دقیقه", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.ink))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            val iconSize: Dp = if (band == AgeBand.UNDER_5) 104.dp else 70.dp
            val cells = shown.map { it as Any? } + if (!more && rest.size > limit) listOf("more") else emptyList()
            cells.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    row.forEach { c ->
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (c is HomeModule) BigPicture(p, V5.picture(c), c.title, iconSize, band != AgeBand.UNDER_5 || true) { open(c) }
                            else BigPicture(p, "➕", "بیشتر", iconSize, true) { more = true }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (band == AgeBand.SEVEN_TO_10) {
                val last = TeenShelf.recent(context, childId).firstNotNullOfOrNull { k -> modules.firstOrNull { it.key == k } }
                if (last != null) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.accentSoft).clickable { open(last) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Emoji(V5.picture(last), 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("ادامه بده", style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.ink))
                            Text(last.title, style = TextStyle(fontSize = 13.sp, color = p.sub))
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null, tint = p.sub)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        HomeFooter(p, dock, teen = false, onOpen = open, onCallFamily = onCallFamily, onParent = onParent)
    }
}

@Composable
private fun TimePill(p: V5.Palette, label: String, value: String) {
    Row(Modifier.clip(RoundedCornerShape(20.dp)).background(p.surface).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End) {
            Text(label, style = TextStyle(fontSize = 11.sp, color = p.sub))
            Text(value, style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.ink))
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = p.accent, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun BigPicture(p: V5.Palette, picture: String, label: String, size: Dp, showLabel: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(22.dp)).clickable(onClick = onClick).padding(6.dp).semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Emoji(picture, size)
        if (showLabel) Text(label, style = TextStyle(fontSize = if (size > 90.dp) 18.sp else 15.sp, fontWeight = FontWeight.Bold, color = p.ink), textAlign = TextAlign.Center, maxLines = 1)
    }
}

/**
 * The 10–15 home (TEEN_FINAL): grown-up and calm — greeting with a name-initial avatar and a small real clock,
 * today's focus with progress, a four-column grid of line icons, family call and parent lock as pills, and the
 * fixed four-door bar at the bottom.
 */
@Composable
fun TeenHome(
    childId: Long,
    name: String,
    modules: List<HomeModule>,
    remainingSeconds: Long?,
    usedFraction: Float,
    night: Boolean,
    onParent: () -> Unit,
    onCallFamily: () -> Unit,
    onOpen: (HomeModule) -> Unit,
) {
    val context = LocalContext.current
    val p = V5.teen(night)
    val open: (HomeModule) -> Unit = { m -> TeenShelf.touch(context, childId, m.key); onOpen(m) }
    val dock = modules.filter { it.key in V5.dockKeys }.sortedBy { V5.dockKeys.indexOf(it.key) }
    val rest = modules.filter { it.key !in V5.dockKeys && it.key != "call" }
    val (clock, date) = Clock()
    Column(Modifier.fillMaxSize().background(p.bg)) {
        Column(Modifier.weight(1f).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(p.accentSoft), contentAlignment = Alignment.Center) {
                    Text(name.trim().take(1), style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = p.accent))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("سلام $name", style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = p.ink))
                    Text(date, style = TextStyle(fontSize = 13.sp, color = p.sub))
                }
                Text(clock, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium, color = p.ink))
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(p.accentSoft).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, tint = p.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        remainingSeconds?.let { "وقت امروز: ${(it / 60).toInt().fa()} دقیقه مانده" } ?: "امروز محدودیت زمانی ندارد",
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = p.ink),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.line)) {
                    Box(Modifier.fillMaxWidth(usedFraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.accent))
                }
            }
            Spacer(Modifier.height(18.dp))
            rest.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    row.forEach { m ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { open(m) }.padding(vertical = 4.dp).semantics { contentDescription = m.title },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.size(56.dp).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(Color(m.colors.first).copy(alpha = if (night) 0.22f else 0.45f)), contentAlignment = Alignment.Center) {
                                Icon(V5.lineIcon(m.key), contentDescription = null, tint = if (night) Color(m.colors.first) else Color(m.colors.second), modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(m.title, style = TextStyle(fontSize = 12.sp, color = p.ink), maxLines = 1, textAlign = TextAlign.Center)
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            HorizontalDivider(color = p.line)
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(p, Icons.Outlined.Call, "تماس خانواده", Modifier.weight(1f), onCallFamily)
                Pill(p, Icons.Outlined.Lock, "قفل والدین", Modifier.weight(1f), onParent)
            }
        }
        HomeFooter(p, dock, teen = true, onOpen = open, onCallFamily = onCallFamily, onParent = onParent)
    }
}

@Composable
private fun Pill(p: V5.Palette, icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(24.dp)).background(p.surface).clickable(onClick = onClick).padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = p.ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = TextStyle(fontSize = 14.sp, color = p.ink))
    }
}
