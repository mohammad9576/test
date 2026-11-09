package com.kodomo.app

import com.kodomo.app.domain.learn.Curriculum
import com.kodomo.app.domain.learn.English
import com.kodomo.app.domain.learn.Grade
import com.kodomo.app.ui.kids.study.englishExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CurriculumTest {
    @Test
    fun everyUnitGeneratesValidExercises() {
        val r = Random(11)
        Curriculum.units.forEach { unit ->
            assertTrue("${unit.id} has no lesson", unit.lesson.isNotEmpty())
            repeat(40) {
                val e = unit.make(r)
                assertTrue("${unit.id}: answer out of range", e.answer in e.options.indices)
                assertTrue("${unit.id}: needs at least 2 options", e.options.size >= 2)
                assertEquals("${unit.id}: duplicate options ${e.options}", e.options.size, e.options.distinct().size)
            }
        }
    }

    @Test
    fun everyGradeHasMathAndAtLeastOneOtherSubject() {
        Grade.entries.forEach { g ->
            val subjects = Curriculum.bySubject(g).keys
            assertTrue("$g has no maths", com.kodomo.app.domain.learn.Subject.MATH in subjects)
            assertTrue("$g has too few subjects", subjects.size >= 2)
        }
        assertEquals(Grade.PRE, Grade.forAge(5))
        assertEquals(Grade.G1, Grade.forAge(6))
        assertEquals(Grade.G6, Grade.forAge(11))
    }

    @Test
    fun englishExercisesAreValid() {
        val r = Random(5)
        English.units.forEach { u ->
            for (mode in 1..3) repeat(20) {
                val e = englishExercise(u.words, mode, r)
                assertTrue(e.answer in e.options.indices)
                assertEquals(e.options.size, e.options.distinct().size)
            }
        }
    }
}

class CurriculumIdsTest {
    @org.junit.Test fun unitIdsAreUnique() {
        val ids = com.kodomo.app.domain.learn.Curriculum.units.map { it.id }
        org.junit.Assert.assertEquals("duplicate unit ids: ${ids.groupBy { it }.filter { it.value.size > 1 }.keys}", ids.size, ids.toSet().size)
    }
}
