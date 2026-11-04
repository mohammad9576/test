package com.kodomo.app

import android.app.Application
import com.kodomo.app.data.seed.CatalogSeeder
import com.kodomo.app.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class KodomoApp : Application() {

    @Inject lateinit var catalogSeeder: CatalogSeeder

    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        appScope.launch { catalogSeeder.seedIfEmpty() }
    }
}
