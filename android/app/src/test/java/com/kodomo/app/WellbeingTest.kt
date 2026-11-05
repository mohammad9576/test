package com.kodomo.app

import com.kodomo.app.data.prefs.Wellbeing
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WellbeingTest {
    private val w = Wellbeing(schoolLock = true, schoolStartMinutes = 8 * 60, schoolEndMinutes = 13 * 60)

    @Test fun locksOnSchoolDaysOnly() {
        // 2026-11-07 is a Saturday (school day in Iran), 2026-11-06 a Friday.
        assertTrue(w.inSchoolHours(LocalDateTime.of(2026, 11, 7, 9, 0)))
        assertFalse(w.inSchoolHours(LocalDateTime.of(2026, 11, 7, 14, 0)))
        assertFalse(w.inSchoolHours(LocalDateTime.of(2026, 11, 6, 9, 0)))
        assertFalse(w.copy(schoolLock = false).inSchoolHours(LocalDateTime.of(2026, 11, 7, 9, 0)))
    }
}
