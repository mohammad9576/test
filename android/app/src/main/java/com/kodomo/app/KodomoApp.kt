package com.kodomo.app

import android.app.Application
import android.util.Log
import com.kodomo.app.data.seed.CatalogSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KodomoApp : Application() {

    @Inject lateinit var catalogSeeder: CatalogSeeder

    private val startupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        startupScope.launch {
            runCatching { catalogSeeder.seedIfEmpty() }.onFailure { Log.e("KodomoApp", "Catalog seeding failed", it) }
        }
    }
}
