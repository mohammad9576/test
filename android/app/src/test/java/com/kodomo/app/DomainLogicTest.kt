package com.kodomo.app

import com.kodomo.app.core.util.PersianCalendar
import com.kodomo.app.core.util.PersianDigits
import com.kodomo.app.domain.engine.ContentPolicy
import com.kodomo.app.domain.engine.ParentGateChallenge
import com.kodomo.app.domain.engine.ParentGateThrottle
import com.kodomo.app.domain.model.AgeGroup
import com.kodomo.app.domain.model.ContentItem
import com.kodomo.app.domain.model.ContentType
import com.kodomo.app.domain.model.Interest
import com.kodomo.app.domain.model.KodomoRating
import com.kodomo.app.domain.model.ParentalSettings
import com.kodomo.app.kiosk.ScreenTimeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import kotlin.random.Random

class DomainLogicTest {

    @Test
    fun persianDigitsRoundTrip() {
        assertEquals("1234", PersianDigits.normalize("۱۲۳۴"))
        assertEquals("1234", PersianDigits.normalize("١٢٣٤"))
        assertEquals("۲۴ + ۱۷", PersianDigits.toPersian("24 + 17"))
        assertEquals(41, PersianDigits.parseIntOrNull(" ۴۱ "))
    }

    @Test
    fun parentGateAcceptsPersianAndLatinAnswers() {
        val challenge = ParentGateChallenge(24, 17, ParentGateChallenge.Operator.PLUS)
        assertEquals("۲۴ + ۱۷ = ؟", challenge.questionPersian)
        assertTrue(challenge.isCorrect("41"))
        assertTrue(challenge.isCorrect("۴۱"))
        assertFalse(challenge.isCorrect("42"))
    }

    @Test
    fun randomChallengesAreAlwaysPositiveAndNonTrivial() {
        val random = Random(42)
        repeat(500) {
            val c = ParentGateChallenge.random(random)
            assertTrue(c.answer > 10)
        }
    }

    @Test
    fun throttleLocksAfterFiveFailures() {
        var now = 0L
        val throttle = ParentGateThrottle { now }
        repeat(4) { throttle.registerFailure() }
        assertFalse(throttle.isLocked())
        throttle.registerFailure()
        assertTrue(throttle.isLocked())
        now += 61_000
        assertFalse(throttle.isLocked())
    }

    @Test
    fun jalaliConversionMatchesKnownDates() {
        val nowruz = PersianCalendar.toJalali(LocalDate.of(2024, 3, 20))
        assertEquals(PersianCalendar.JalaliDate(1403, 1, 1), nowruz)
        assertEquals(LocalDate.of(2024, 3, 20), PersianCalendar.toGregorian(1403, 1, 1))
        val sample = LocalDate.of(2026, 11, 4)
        val j = PersianCalendar.toJalali(sample)
        assertEquals(sample, PersianCalendar.toGregorian(j.year, j.month, j.day))
    }

    @Test
    fun ageGroupBoundaries() {
        assertEquals(AgeGroup.TODDLER, AgeGroup.fromAge(2))
        assertEquals(AgeGroup.TODDLER, AgeGroup.fromAge(4))
        assertEquals(AgeGroup.PRIMARY_KID, AgeGroup.fromAge(5))
        assertEquals(AgeGroup.PRIMARY_KID, AgeGroup.fromAge(8))
        assertEquals(AgeGroup.PRE_TEEN, AgeGroup.fromAge(9))
        assertEquals(6, AgeGroup.ageInYears(LocalDate.of(2020, 1, 1), LocalDate.of(2026, 6, 1)))
    }

    @Test
    fun bedtimeWindowCrossesMidnight() {
        assertTrue(ScreenTimeManager.isBedtime(LocalTime.of(22, 0), 21 * 60, 7 * 60))
        assertTrue(ScreenTimeManager.isBedtime(LocalTime.of(6, 59), 21 * 60, 7 * 60))
        assertFalse(ScreenTimeManager.isBedtime(LocalTime.of(12, 0), 21 * 60, 7 * 60))
        assertTrue(ScreenTimeManager.isBedtime(LocalTime.of(14, 30), 14 * 60, 15 * 60))
    }

    @Test
    fun contentPolicyRespectsAgeAndThresholds() {
        val item = ContentItem(
            id = "x", type = ContentType.VIDEO, title = "t", description = "", body = "", category = "", emoji = "", colorStart = 0, colorEnd = 0,
            mediaUrl = null, durationMinutes = 1, rating = KodomoRating(6, 12, violence = 3, fear = 1, educational = 4, abnormality = 1, creativity = 3, familyValues = 3),
            positives = emptyList(), negatives = emptyList(), expertReview = "", expertName = "", peerLikes = 0, peerDislikes = 0,
            interests = setOf(Interest.ART), author = "",
        )
        val strict = ParentalSettings.defaults(1)
        assertFalse(ContentPolicy.isAllowed(item, 8, strict))
        assertTrue(ContentPolicy.isAllowed(item, 8, strict.copy(maxViolence = 3)))
        assertFalse(ContentPolicy.isAllowed(item, 5, strict.copy(maxViolence = 5)))
    }
}
