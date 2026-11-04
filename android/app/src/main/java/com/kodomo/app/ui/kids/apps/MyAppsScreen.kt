package com.kodomo.app.ui.kids.apps

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.data.apps.InstalledApp
import com.kodomo.app.data.apps.InstalledAppsRepository
import com.kodomo.app.domain.model.ActivityCategory
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.kiosk.ScreenTimeManager
import com.kodomo.app.ui.components.EmptyState
import com.kodomo.app.ui.components.KidScreenScaffold
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.ui.kids.LocalKidSpace
import com.kodomo.app.ui.kids.TrackActivity
import com.kodomo.app.ui.mascot.MascotSpeech
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MyAppsViewModel @Inject constructor(
    private val apps: InstalledAppsRepository,
    private val screenTime: ScreenTimeManager,
) : ViewModel() {
    val allowed: StateFlow<List<InstalledApp>?> = apps.allowedPackages
        .mapLatest { apps.allowedApps(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Starts an approved app while keeping the screen-time clock running under the «برنامه‌ها» category. */
    fun open(app: InstalledApp, onMissing: () -> Unit, start: (android.content.Intent) -> Boolean) {
        val intent = apps.launchIntent(app.packageName) ?: return onMissing()
        screenTime.enterExternalApp(app.label, app.packageName)
        if (!start(intent)) {
            screenTime.resume()
            onMissing()
        }
    }
}

@Composable
fun MyAppsScreen(onBack: () -> Unit, viewModel: MyAppsViewModel = hiltViewModel()) {
    val space = LocalKidSpace.current
    val context = LocalContext.current
    val apps by viewModel.allowed.collectAsStateWithLifecycle()
    val kiosk = com.kodomo.app.kiosk.LocalKioskController.current
    TrackActivity(ActivityCategory.APP, "برنامه‌های من")

    KidScreenScaffold(title = "برنامه‌های من", onBack = onBack) { padding ->
        val list = apps
        if (list != null && list.isEmpty()) {
            EmptyState("هنوز برنامه‌ای اضافه نشده. از مامان یا بابا بخواه از پنل والدین برنامه‌های مجاز را انتخاب کنند.", Modifier.padding(padding))
            return@KidScreenScaffold
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                MascotSpeech("این‌ها برنامه‌هایی هستند که مامان و بابا اجازه داده‌اند. زمانت همین‌جا هم حساب می‌شود ⏰", MascotMood.HAPPY)
            }
            items(list.orEmpty(), key = { it.packageName }) { app ->
                Column(
                    Modifier
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
                        .bouncyClick {
                            space.guide(app.label)
                            viewModel.open(
                                app,
                                onMissing = { Toast.makeText(context, "این برنامه باز نشد.", Toast.LENGTH_SHORT).show() },
                                start = { kiosk.launchExternalApp(it) },
                            )
                        }
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Image(app.icon, contentDescription = app.label, modifier = Modifier.size(56.dp))
                    Text(app.label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
                }
            }
        }
    }
}
