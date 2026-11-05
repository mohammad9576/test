package com.kodomo.app.ui.parent.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.data.prefs.SafeSite
import com.kodomo.app.data.prefs.SafeSitesStore
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import com.kodomo.app.ui.components.SectionTitle
import com.kodomo.app.ui.theme.KodomoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SafeSitesViewModel @Inject constructor(private val store: SafeSitesStore) : ViewModel() {
    val sites = store.sites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val requests = store.requests.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun add(site: SafeSite) = viewModelScope.launch { store.add(site) }
    fun remove(host: String) = viewModelScope.launch { store.remove(host) }
    fun dismiss(host: String) = viewModelScope.launch { store.dismissRequest(host) }
}

/** Parent: websites the kids browser may open, plus the child's «may I open…» requests. */
@Composable
fun SafeSitesScreen(onBack: () -> Unit, viewModel: SafeSitesViewModel = hiltViewModel()) {
    val sites by viewModel.sites.collectAsStateWithLifecycle()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    var host by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }

    KidScreenScaffold(title = "سایت‌های مجاز مرورگر", onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(
                    "مرورگر کودک فقط صفحه‌های کدومو و سایت‌هایی را که اینجا تأیید کنید باز می‌کند. در هر سایت، رفتن به سایت‌های دیگر و تبلیغات مسدود است. پیش از تأیید، خودتان سایت را بررسی کنید.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (requests.isNotEmpty()) {
                item { SectionTitle("درخواست‌های فرزند") }
                items(requests, key = { "r_$it" }) { r ->
                    KodomoCard(Modifier.fillMaxWidth()) {
                        Text("🙋 «$r»", style = MaterialTheme.typography.titleMedium)
                        Row {
                            TextButton(onClick = { viewModel.add(SafeSite(r, r, "🌐")) }) { Text("تأیید") }
                            TextButton(onClick = { viewModel.dismiss(r) }) { Text("رد", color = KodomoColors.Danger) }
                        }
                    }
                }
            }
            item { SectionTitle("افزودن سایت") }
            item {
                KodomoCard(Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = host, onValueChange = { host = it }, label = { Text("آدرس (مثلاً scratch.mit.edu)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("نام نمایشی") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            viewModel.add(SafeSite(host, title.ifBlank { SafeSite.normalizeHost(host) }, "🌐"))
                            host = ""; title = ""
                        },
                        enabled = SafeSite.normalizeHost(host).contains('.'),
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("افزودن") }
                }
            }
            val approvedHosts = sites.map { it.host }
            val suggestions = SafeSitesStore.suggestions.filter { SafeSite.normalizeHost(it.host) !in approvedHosts }
            if (suggestions.isNotEmpty()) {
                item { SectionTitle("پیشنهادها (پس از بررسی شما)") }
                items(suggestions, key = { "s_" + it.host }) { s ->
                    KodomoCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.emoji, fontSize = 28.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(s.title, style = MaterialTheme.typography.titleSmall)
                                Text(s.host, style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { viewModel.add(s) }) { Text("تأیید") }
                        }
                    }
                }
            }
            item { SectionTitle("سایت‌های تأییدشده (${sites.size})") }
            items(sites, key = { "a_" + it.host }) { s ->
                KodomoCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(s.emoji, fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(s.title, style = MaterialTheme.typography.titleSmall)
                            Text(s.host, style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(onClick = { viewModel.remove(s.host) }) { Text("حذف", color = KodomoColors.Danger) }
                    }
                }
            }
        }
    }
}
