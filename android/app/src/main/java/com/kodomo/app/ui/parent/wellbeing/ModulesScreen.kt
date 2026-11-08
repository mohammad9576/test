package com.kodomo.app.ui.parent.wellbeing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.data.prefs.ModulesStore
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.ui.fx.Emoji
import com.kodomo.app.ui.kids.home.HomeModules
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModulesViewModel @Inject constructor(family: FamilyRepository, private val store: ModulesStore) : ViewModel() {
    val children = family.observeChildren()
    fun visible(childId: Long) = store.visible(childId)
    fun set(childId: Long, keys: Set<String>) { viewModelScope.launch { store.setVisible(childId, keys) } }
}

/** Parents choose, per child, which sections appear in the kids home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModulesScreen(onBack: () -> Unit) {
    val vm: ModulesViewModel = hiltViewModel()
    val children by vm.children.collectAsStateWithLifecycle(emptyList())
    var selected by remember { mutableStateOf<Child?>(null) }
    LaunchedEffect(children) { if (selected == null || children.none { it.id == selected?.id }) selected = children.firstOrNull() }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("بخش‌های محیط کودک") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "برگشت") } },
        )
    }) { pad ->
        val child = selected
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("هر بخشی را که نمی‌خواهید فرزندتان ببیند خاموش کنید. تغییرات فوراً در صفحه‌ی اصلی کودک اعمال می‌شود.", style = MaterialTheme.typography.bodyMedium)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(children, key = { it.id }) { c -> FilterChip(selected = c.id == child?.id, onClick = { selected = c }, label = { Text(c.name) }) }
                }
            }
            if (child != null) {
                val all = HomeModules.forChild(child)
                item(key = "list_${child.id}") {
                    val chosen by remember(child.id) { vm.visible(child.id) }.collectAsStateWithLifecycle(null)
                    val keys = chosen ?: all.map { it.key }.filter { it !in HomeModules.hiddenByDefault }.toSet()
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        all.forEach { m ->
                            val on = m.key in keys
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (on) 0.9f else 0.4f)).padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Emoji(m.emoji, 36.dp)
                                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                    Text(m.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(m.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(checked = on, onCheckedChange = { vm.set(child.id, if (it) keys + m.key else keys - m.key) })
                            }
                        }
                    }
                }
            }
        }
    }
}
