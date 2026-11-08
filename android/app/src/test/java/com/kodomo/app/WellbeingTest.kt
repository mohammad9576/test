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

class WindowsTest {
    @Test fun windowsLimitUse() {
        val w = Wellbeing(windows = listOf(13 * 60..13 * 60 + 15, 14 * 60 + 30..15 * 60))
        assertTrue(w.inWindow(java.time.LocalTime.of(13, 5)))
        assertFalse(w.inWindow(java.time.LocalTime.of(13, 20)))
        assertTrue(w.inWindow(java.time.LocalTime.of(14, 45)))
        org.junit.Assert.assertEquals(14 * 60 + 30, w.nextWindow(java.time.LocalTime.of(13, 20)))
        org.junit.Assert.assertEquals(w.windows, Wellbeing.decode(Wellbeing.encode(w.windows)))
        org.junit.Assert.assertEquals(13 * 60 + 15, Wellbeing.parseHhmm("13:15"))
    }
}
