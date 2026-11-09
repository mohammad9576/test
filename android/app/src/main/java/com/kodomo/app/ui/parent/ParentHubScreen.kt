package com.kodomo.app.ui.parent

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kodomo.app.domain.model.Child
import com.kodomo.app.ui.components.KodomoAvatar
import com.kodomo.app.ui.parent.community.CommunityTab
import com.kodomo.app.ui.parent.dashboard.DashboardTab
import com.kodomo.app.ui.parent.family.FamilyTab
import com.kodomo.app.ui.parent.ratings.RatingsTab
import com.kodomo.app.ui.parent.reports.ReportsTab

private enum class HubTab(val label: String, val icon: ImageVector) {
    DASHBOARD("داشبورد", Icons.Rounded.Dashboard),
    REPORTS("گزارش", Icons.Rounded.BarChart),
    RATINGS("ارزیابی", Icons.Rounded.VerifiedUser),
    COMMUNITY("جامعه", Icons.Rounded.Forum),
    FAMILY("خانواده", Icons.Rounded.FamilyRestroom),
}

@Composable
fun ParentHubScreen(
    onExit: () -> Unit,
    onOpenContent: (String) -> Unit,
    onOpenArticle: (String) -> Unit,
    onOpenTopic: (Long) -> Unit,
    onEditChild: (Long) -> Unit,
    onAddChild: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenCloud: () -> Unit,
    onOpenAllowedApps: () -> Unit,
    onOpenTelewebion: () -> Unit,
    onOpenSites: () -> Unit,
    onOpenWellbeing: () -> Unit = {},
    onOpenModules: () -> Unit = {},
    onOpenContacts: () -> Unit = {},
    onOpenWatch: () -> Unit = {},
    viewModel: ParentHubViewModel = hiltViewModel(),
) {
    var tab by rememberSaveable { mutableTabState() }
    val family by viewModel.family.collectAsStateWithLifecycle()
    BackHandler(onBack = onExit)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(family?.let { "پنل والدین • ${it.familyName}" } ?: "پنل والدین", style = MaterialTheme.typography.titleLarge)
                },
                actions = {
                    IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "خروج از پنل والدین") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                HubTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t.ordinal,
                        onClick = { tab = t.ordinal },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
    ) { padding ->
        val modifier = Modifier.fillMaxSize().padding(padding)
        when (HubTab.entries[tab]) {
            HubTab.DASHBOARD -> DashboardTab(viewModel, modifier)
            HubTab.REPORTS -> ReportsTab(viewModel, modifier)
            HubTab.RATINGS -> RatingsTab(viewModel, onOpenContent, onOpenTelewebion, modifier, onOpenWatch)
            HubTab.COMMUNITY -> CommunityTab(viewModel, onOpenArticle, onOpenTopic, modifier)
            HubTab.FAMILY -> FamilyTab(viewModel, onEditChild, onAddChild, onOpenGallery, onOpenCloud, onOpenAllowedApps, onOpenSites, modifier, onOpenWellbeing = onOpenWellbeing, onOpenModules = onOpenModules, onOpenContacts = onOpenContacts)
        }
    }
}

private fun mutableTabState() = androidx.compose.runtime.mutableIntStateOf(0)

/** Horizontal child switcher shown at the top of per-child parent tabs. */
@Composable
fun ChildSwitcher(children: List<Child>, selected: Child?, onSelect: (Long) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
        items(children, key = { it.id }) { child ->
            FilterChip(
                selected = child.id == selected?.id,
                onClick = { onSelect(child.id) },
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KodomoAvatar(child.avatar, size = 24.dp, photoId = child.id)
                        Spacer(Modifier.width(6.dp))
                        Text(child.name)
                    }
                },
            )
        }
    }
}
