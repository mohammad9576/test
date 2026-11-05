package com.kodomo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.kodomo.app.domain.model.MascotMood
import com.kodomo.app.ui.mascot.KodomoMascot
import com.kodomo.app.ui.navigation.KodomoNavHost
import com.kodomo.app.ui.navigation.Routes
import com.kodomo.app.ui.theme.KodomoColors
import com.kodomo.app.ui.theme.KodomoTheme

@Composable
fun KodomoRoot(appViewModel: AppViewModel) {
    val session by appViewModel.session.collectAsStateWithLifecycle()
    KodomoTheme(darkTheme = false) {
        val demoRoute = com.kodomo.app.core.demo.DemoMode.route
        if (demoRoute != null) {
            // Automated smoke test: prepare sample data and open the requested screen directly.
            val context = androidx.compose.ui.platform.LocalContext.current
            var start by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
            androidx.compose.runtime.LaunchedEffect(demoRoute) {
                start = dagger.hilt.android.EntryPointAccessors.fromApplication(context.applicationContext, com.kodomo.app.core.demo.DemoEntryPoint::class.java)
                    .demoSeeder().prepare(demoRoute, com.kodomo.app.core.demo.DemoMode.age)
            }
            val s = start
            val nav = rememberNavController()
            if (s == null) {
                Box(Modifier.fillMaxSize().background(KodomoColors.Mint))
            } else {
                KodomoNavHost(navController = nav, startDestination = Routes.PROFILES)
                androidx.compose.runtime.LaunchedEffect(s) { if (s != Routes.PROFILES) runCatching { nav.navigate(s) } }
            }
            return@KodomoTheme
        }
        val current = session
        if (current == null) {
            Box(Modifier.fillMaxSize().background(KodomoColors.Mint), contentAlignment = Alignment.Center) {
                KodomoMascot(MascotMood.HAPPY, size = 180.dp)
            }
        } else {
            val start = remember { if (current.onboardingCompleted) Routes.PROFILES else Routes.ONBOARDING }
            KodomoNavHost(navController = rememberNavController(), startDestination = start)
        }
    }
}
