package com.kodomo.app.ui.kids.call

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodomo.app.data.prefs.ApprovedContact
import com.kodomo.app.data.prefs.ContactsStore
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.kiosk.LocalKioskController
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.fx.Emoji
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.kids.wellbeing.EmergencyCall
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class KidCallViewModel @Inject constructor(store: ContactsStore) : ViewModel() {
    val contacts = store.contacts
}

private val tileColors = listOf(0xFF66BB6A to 0xFF2E7D32, 0xFF4FC3F7 to 0xFF0277BD, 0xFFFFB74D to 0xFFEF6C00, 0xFFF06292 to 0xFFC2185B, 0xFF9575CD to 0xFF512DA8, 0xFF4DB6AC to 0xFF00796B)

/** «تماس»: one big button per person the parents approved; nobody else can be called from the kids space. */
@Composable
fun KidCallScreen(onBack: () -> Unit, vm: KidCallViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val context = LocalContext.current
    val kiosk = LocalKioskController.current
    val contacts by vm.contacts.collectAsStateWithLifecycle(emptyList())
    TrackActivity(ActivityCategory.HOME, "تماس")

    fun call(c: ApprovedContact) {
        space.voice.speak("زنگ می‌زنم به ${c.name}")
        EmergencyCall.allowedUntil = System.currentTimeMillis() + 10 * 60_000
        val direct = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val intent = Intent(if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL, Uri.parse("tel:${c.phone}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!kiosk.launchExternalApp(intent)) runCatching { context.startActivity(intent) }
    }

    KidScreenScaffold(title = "تماس", onBack = onBack) { padding ->
        if (contacts.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Emoji("📞", 120.dp)
                Text("هنوز مامان و بابا کسی را برای تماس اضافه نکرده‌اند.", fontSize = 19.sp, textAlign = TextAlign.Center)
                Text("(پنل والدین ← خانواده ← مخاطبان تماس)", fontSize = 14.sp, color = Color.Gray)
            }
            return@KidScreenScaffold
        }
        LazyVerticalGrid(GridCells.Fixed(2), Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) { Text("به کی زنگ بزنیم؟ 📞", fontSize = 22.sp, fontWeight = FontWeight.Black) }
            items(contacts, key = { it.phone }) { c ->
                val (from, to) = tileColors[(c.phone.hashCode() and 0x7fffffff) % tileColors.size]
                Column(
                    Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(28.dp), spotColor = Color(to)).clip(RoundedCornerShape(28.dp))
                        .background(Brush.verticalGradient(listOf(Color(from), Color(to)))).clickable { call(c) }.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Emoji(c.relation.emoji, 84.dp)
                    Text(c.name, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2)
                    Text("📞 زنگ بزن", color = Color.White.copy(alpha = 0.95f), fontSize = 15.sp)
                }
            }
        }
    }
}
