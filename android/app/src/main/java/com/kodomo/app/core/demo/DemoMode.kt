package com.kodomo.app.core.demo

import com.kodomo.app.data.prefs.SessionPreferences
import com.kodomo.app.domain.model.AvatarKind
import com.kodomo.app.domain.model.ChildDraft
import com.kodomo.app.domain.model.Gender
import com.kodomo.app.domain.model.Interest
import com.kodomo.app.domain.repository.FamilyRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug-only «demo mode» used by the automated emulator smoke test (tools/smoke_test.sh): a launch intent with
 * `demo_route` creates a sample family and child of `demo_age` years and opens that screen directly. Ignored in
 * release builds.
 */
object DemoMode {
    @Volatile var route: String? = null
    @Volatile var age: Int = 7
    /** Games end their level by themselves after a few seconds (tests the end-of-level path). */
    @Volatile var autoFinish: Boolean = false
}

@Singleton
class DemoSeeder @Inject constructor(
    private val family: FamilyRepository,
    private val session: SessionPreferences,
) {
    /** Makes sure the demo family exists and returns [route] with `{child}` replaced by the demo child's id. */
    suspend fun prepare(route: String, age: Int): String {
        if (!session.current().onboardingCompleted) {
            family.createFamily("خانواده‌ی نمونه", "بابا", "مامان", "1234")
            session.setOnboardingCompleted(true)
        }
        val name = "کودک $age ساله"
        val existing = family.observeChildren().first().firstOrNull { it.name == name }
        val id = existing?.id ?: family.addChild(
            ChildDraft(
                name = name,
                birthDate = LocalDate.now().minusYears(age.toLong()).minusMonths(2),
                gender = if (age % 2 == 0) Gender.GIRL else Gender.BOY,
                avatar = if (age % 2 == 0) AvatarKind.GIRL_BOW else AvatarKind.BOY_SHORT,
                interests = setOf(Interest.SPORT, Interest.ANIMALS, Interest.SCIENCE, Interest.BOOKS),
            ),
            dailyLimitMinutes = 180,
        )
        session.setActiveChild(id)
        return route.replace("{child}", id.toString())
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DemoEntryPoint { fun demoSeeder(): DemoSeeder }
