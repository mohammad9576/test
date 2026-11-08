package com.kodomo.app.ui.parent.ratings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kodomo.app.ui.fx.Emoji
import com.kodomo.app.ui.kids.video.ContentProvider

/** Parents pick a service (Telewebion, Aparat Kids, Filimo, Filmnet, Sheyda, Namava, Taaghche, Fidibo) to add titles from. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersHubScreen(onBack: () -> Unit, onOpen: (ContentProvider) -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("افزودن کارتون و کتاب") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "برگشت") } })
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    "سرویس را باز کنید، در صورت نیاز با حساب کاربری خودتان وارد شوید و صفحه‌ی کارتون یا کتاب را باز کنید. کدومو ارزیابی آن را از kodoumo.ir می‌آورد (سن، خشونت، ترس، نکات والدین) و فقط با تأیید شما برای کودک نمایش داده می‌شود؛ کودک هم فقط همان صفحه را می‌بیند.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item { Text("🎬 فیلم و کارتون", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(ContentProvider.videos) { p -> ProviderRow(p, onOpen) }
            item { Text("📚 کتاب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(ContentProvider.books) { p -> ProviderRow(p, onOpen) }
            item {
                Text("ℹ️ اشتراک و خرید هر سرویس به عهده‌ی خانواده است؛ کدومو فقط صفحه‌ی تأییدشده را قفل‌شده به کودک نشان می‌دهد.", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun ProviderRow(p: ContentProvider, onOpen: (ContentProvider) -> Unit) {
    Card(onClick = { onOpen(p) }, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Emoji(p.emoji, 40.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(p.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(android.net.Uri.parse(p.startUrl).host.orEmpty().removePrefix("www.") + if (p.needsAccount) " • نیاز به حساب کاربری" else " • رایگان", style = MaterialTheme.typography.bodySmall)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}
