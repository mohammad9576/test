package com.kodomo.app.ui.parent.family

import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.data.prefs.ApprovedContact
import com.kodomo.app.data.prefs.ContactsStore
import com.kodomo.app.data.prefs.Relation
import com.kodomo.app.ui.fx.Emoji
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContactsViewModel @Inject constructor(private val store: ContactsStore) : ViewModel() {
    val contacts = store.contacts
    fun add(c: ApprovedContact) { viewModelScope.launch { store.add(c) } }
    fun remove(phone: String) { viewModelScope.launch { store.remove(phone) } }
}

/** Parents approve the people the child may call (father, mother, grandparents, uncles, aunts…). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ContactsScreen(onBack: () -> Unit) {
    val vm: ContactsViewModel = hiltViewModel()
    val contacts by vm.contacts.collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf(Relation.MOM) }
    // Picking one number from the phone book needs no contacts permission (Android grants just that one row).
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        val uri = r.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) { phone = c.getString(0).orEmpty(); if (name.isBlank()) name = c.getString(1).orEmpty() }
            }
        }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text("مخاطبان تماس کودک") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "برگشت") } })
    }) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("کودک در بخش «تماس» فقط به همین افراد می‌تواند زنگ بزند. شماره‌گیر فقط ۱۰ دقیقه بدون قفل باز می‌ماند.", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("افزودن مخاطب", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Relation.entries.forEach { r ->
                                FilterChip(selected = relation == r, onClick = { relation = r; if (name.isBlank() || Relation.entries.any { it.label == name }) name = r.label }, label = { Text("${r.emoji} ${r.label}") })
                            }
                        }
                        OutlinedTextField(name, { name = it }, label = { Text("نامی که کودک می‌بیند (مثلاً «مامان‌بزرگ مهری»)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(phone, { phone = it }, label = { Text("شماره تلفن") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { runCatching { picker.launch(android.content.Intent(android.content.Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)) } }, modifier = Modifier.weight(1f)) { Text("📇 از دفترچه تلفن") }
                            val clean = ContactsStore.clean(phone)
                            Button(onClick = { vm.add(ApprovedContact(name.trim().ifBlank { relation.label }, clean, relation)); name = ""; phone = "" }, enabled = clean.length >= 4, modifier = Modifier.weight(1f)) { Text("✅ تأیید و افزودن") }
                        }
                    }
                }
            }
            items(contacts, key = { it.phone }) { c ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Emoji(c.relation.emoji, 40.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                            Text(c.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text("${c.relation.label} • ${c.phone}", style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { vm.remove(c.phone) }) { Text("حذف") }
                    }
                }
            }
        }
    }
}
