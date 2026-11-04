package com.kodomo.app.ui.parent.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.data.apps.InstalledApp
import com.kodomo.app.data.apps.InstalledAppsRepository
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.KodomoCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AllowedAppsViewModel @Inject constructor(private val repo: InstalledAppsRepository) : ViewModel() {
    val apps = MutableStateFlow<List<InstalledApp>?>(null)
    val allowed: StateFlow<Set<String>> = repo.allowedPackages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    init {
        viewModelScope.launch { apps.value = repo.launchableApps() }
    }

    fun toggle(app: InstalledApp, on: Boolean) = viewModelScope.launch { repo.setAllowed(app.packageName, on) }
}

/** Parent chooses which installed apps (games, video players, …) the child may open from inside Kodomo. */
@Composable
fun AllowedAppsScreen(onBack: () -> Unit, viewModel: AllowedAppsViewModel = hiltViewModel()) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val allowed by viewModel.allowed.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    KidScreenScaffold(title = "برنامه‌های مجاز برای کودک", onBack = onBack) { padding ->
        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@KidScreenScaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(
                    "برنامه‌های انتخاب‌شده در بخش «برنامه‌های من» محیط کودک نمایش داده می‌شوند. زمان استفاده از آن‌ها جزو سقف روزانه حساب می‌شود و با پایان زمان، کودک به کدومو برگردانده می‌شود. در حالت قفل کامل (Device Owner) فقط همین برنامه‌ها اجازه‌ی باز شدن دارند.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    query, { query = it }, placeholder = { Text("جستجوی برنامه") }, singleLine = true,
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
            items(list.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }, key = { it.packageName }) { app ->
                KodomoCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(app.icon, contentDescription = null, modifier = Modifier.size(44.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(app.label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Switch(checked = app.packageName in allowed, onCheckedChange = { viewModel.toggle(app, it) })
                    }
                }
            }
        }
    }
}
