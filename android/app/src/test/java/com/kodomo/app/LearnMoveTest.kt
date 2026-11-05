package com.kodomo.app

import com.kodomo.app.ui.kids.games.adventure.AdventureThemes
import com.kodomo.app.ui.kids.games.adventure.Mission
import com.kodomo.app.ui.kids.games.adventure.bodyQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LearnMoveTest {
    @Test
    fun bodyQuestionsAlwaysHaveThreeAnswersAndAValidCorrectIndex() {
        val r = Random(3)
        for (age in 3..14) for (level in 1..10) repeat(20) {
            val q = bodyQuestion(age, level, r)
            assertEquals(3, q.answers.size)
            assertTrue(q.correct in 0..2)
            assertEquals(3, q.answers.distinct().size)
        }
    }

    @Test
    fun everyAdventureUsesTenDifferentMissionsEndingWithTheBoss() {
        AdventureThemes.all.forEach { theme ->
            assertEquals(10, theme.stages.size)
            assertEquals(Mission.BOSS, theme.stages.last().mission)
            assertEquals(Mission.CATCH, theme.stages.first().mission)
        }
    }
}
