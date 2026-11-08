package com.kodomo.app.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.kodomo.app.core.util.fa
import com.kodomo.app.data.prefs.SessionPreferences
import com.kodomo.app.domain.model.AvatarKind
import com.kodomo.app.domain.model.Child
import com.kodomo.app.domain.model.Family
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.ui.components.KodomoAvatar
import com.kodomo.app.ui.components.ParentGateDialog
import com.kodomo.app.ui.components.bouncyClick
import com.kodomo.app.ui.theme.KodomoColors
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfilePickerViewModel @Inject constructor(
    private val familyRepository: FamilyRepository,
    private val session: SessionPreferences,
) : ViewModel() {
    val family: StateFlow<Family?> = familyRepository.observeFamily().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val children: StateFlow<List<Child>> = familyRepository.observeChildren().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun verifyPin(pin: String) = familyRepository.verifyPin(pin)

    fun rememberActive(childId: Long?) = viewModelScope.launch { session.setActiveChild(childId) }
}

/** «چه کسی از کدومو استفاده می‌کند؟» – each family member enters their own space. */
@Composable
fun ProfilePickerScreen(
    onChild: (Long) -> Unit,
    onParentVerified: () -> Unit,
    onSetup: () -> Unit = {},
    viewModel: ProfilePickerViewModel = hiltViewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var unguarded by remember { mutableStateOf<Long?>(null) }
    val family by viewModel.family.collectAsStateWithLifecycle()
    val children by viewModel.children.collectAsStateWithLifecycle()
    var gateOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.rememberActive(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(KodomoColors.Mint, KodomoColors.Cream)))
            .statusBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text("کدومو", style = MaterialTheme.typography.headlineMedium, color = KodomoColors.Green)
        Text("همراه امن خانواده‌ها", style = MaterialTheme.typography.labelLarge, color = KodomoColors.InkSoft)
        Spacer(Modifier.height(20.dp))
        Text("چه کسی از کدومو استفاده می‌کند؟", style = MaterialTheme.typography.headlineSmall)
        family?.let { Text(it.familyName, style = MaterialTheme.typography.bodyMedium, color = KodomoColors.InkSoft) }
        Spacer(Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            family?.let { f ->
                if (f.fatherName.isNotBlank()) item { ProfileCard(f.fatherName, "پدر", AvatarKind.FATHER, com.kodomo.app.ui.components.AvatarPhotos.FATHER) { gateOpen = true } }
                if (f.motherName.isNotBlank()) item { ProfileCard(f.motherName, "مادر", AvatarKind.MOTHER, com.kodomo.app.ui.components.AvatarPhotos.MOTHER) { gateOpen = true } }
            }
            items(children, key = { it.id }) { child ->
                ProfileCard(child.name, "${child.ageYears().fa()} ساله", child.avatar, child.id) {
                    viewModel.rememberActive(child.id)
                    // Without the app guard a child could open Settings and stop or uninstall Kodomo: warn first.
                    val guarded = com.kodomo.app.kiosk.KioskPermissions.hasUsageAccess(context) && com.kodomo.app.kiosk.KioskPermissions.canDrawOverlays(context)
                    if (guarded || com.kodomo.app.core.demo.DemoMode.route != null) onChild(child.id) else unguarded = child.id
                }
            }
            if (children.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Text("هنوز پروفایل کودکی ثبت نشده است. از پنل والدین فرزند اضافه کنید.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
    }

    unguarded?.let { id ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { unguarded = null },
            icon = { Text("⚠️", style = MaterialTheme.typography.displaySmall) },
            title = { Text("نگهبان کدومو هنوز روشن نیست") },
            text = {
                Text(
                    "بدون «دسترسی استفاده» و «نمایش روی برنامه‌ها»، کودک می‌تواند از نوار بالای گوشی وارد تنظیمات شود و کدومو را ببندد یا حذف کند. " +
                        "لطفاً اول آماده‌سازی گوشی را کامل کنید (فقط چند دقیقه، یک بار).",
                )
            },
            confirmButton = { androidx.compose.material3.Button(onClick = { unguarded = null; onSetup() }) { Text("تکمیل آماده‌سازی") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { unguarded = null; onChild(id) }) { Text("فعلاً بدون محافظت") } },
        )
    }

    if (gateOpen) {
        ParentGateDialog(
            verifyPin = viewModel::verifyPin,
            onDismiss = { gateOpen = false },
            onVerified = {
                gateOpen = false
                onParentVerified()
            },
        )
    }
}

@Composable
private fun ProfileCard(name: String, role: String, avatar: AvatarKind, photoId: Long? = null, onClick: () -> Unit) {
    Column(
        Modifier
            .shadow(3.dp, RoundedCornerShape(26.dp))
            .background(Color.White, RoundedCornerShape(26.dp))
            .bouncyClick(onClick)
            .padding(vertical = 18.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        KodomoAvatar(avatar, size = 84.dp, photoId = photoId)
        Spacer(Modifier.height(8.dp))
        Text(name, style = MaterialTheme.typography.titleMedium)
        Text(role, style = MaterialTheme.typography.labelMedium, color = KodomoColors.InkSoft)
    }
}
