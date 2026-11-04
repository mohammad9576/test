package com.kodomo.app.di

import android.content.Context
import androidx.room.Room
import com.kodomo.app.BuildConfig
import com.kodomo.app.data.local.KodomoDatabase
import com.kodomo.app.data.repository.ActivityRepositoryImpl
import com.kodomo.app.data.repository.ContentRepositoryImpl
import com.kodomo.app.data.repository.DownloadRepositoryImpl
import com.kodomo.app.data.repository.EncyclopediaRepositoryImpl
import com.kodomo.app.data.repository.FamilyRepositoryImpl
import com.kodomo.app.data.repository.ForumRepositoryImpl
import com.kodomo.app.data.repository.MemoryRepositoryImpl
import com.kodomo.app.data.repository.ProjectRepositoryImpl
import com.kodomo.app.data.repository.RewardRepositoryImpl
import com.kodomo.app.data.repository.SchoolRepositoryImpl
import com.kodomo.app.data.repository.SettingsRepositoryImpl
import com.kodomo.app.domain.repository.ActivityRepository
import com.kodomo.app.domain.repository.ContentRepository
import com.kodomo.app.domain.repository.DownloadRepository
import com.kodomo.app.domain.repository.EncyclopediaRepository
import com.kodomo.app.domain.repository.FamilyRepository
import com.kodomo.app.domain.repository.ForumRepository
import com.kodomo.app.domain.repository.MemoryRepository
import com.kodomo.app.domain.repository.ProjectRepository
import com.kodomo.app.domain.repository.RewardRepository
import com.kodomo.app.domain.repository.SchoolRepository
import com.kodomo.app.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides @Singleton @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KodomoDatabase =
        Room.databaseBuilder(context, KodomoDatabase::class.java, KodomoDatabase.NAME)
            .addMigrations(KodomoDatabase.MIGRATION_1_2)
            .build()

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds abstract fun family(impl: FamilyRepositoryImpl): FamilyRepository
    @Binds abstract fun settings(impl: SettingsRepositoryImpl): SettingsRepository
    @Binds abstract fun content(impl: ContentRepositoryImpl): ContentRepository
    @Binds abstract fun downloads(impl: DownloadRepositoryImpl): DownloadRepository
    @Binds abstract fun activity(impl: ActivityRepositoryImpl): ActivityRepository
    @Binds abstract fun school(impl: SchoolRepositoryImpl): SchoolRepository
    @Binds abstract fun projects(impl: ProjectRepositoryImpl): ProjectRepository
    @Binds abstract fun rewards(impl: RewardRepositoryImpl): RewardRepository
    @Binds abstract fun encyclopedia(impl: EncyclopediaRepositoryImpl): EncyclopediaRepository
    @Binds abstract fun forum(impl: ForumRepositoryImpl): ForumRepository
    @Binds abstract fun memories(impl: MemoryRepositoryImpl): MemoryRepository
}
