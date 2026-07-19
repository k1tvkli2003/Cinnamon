package com.cinnamon.app.ui.screens.practice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClozeRewardEligibilityTest {

    @Test
    fun `a completed cloze run with no correct answers is not reward evidence`() {
        assertFalse(isRewardEligibleClozeRun(correctCount = 0, totalRounds = 10))
        assertFalse(isRewardEligibleClozeRun(correctCount = 2, totalRounds = 10))
    }

    @Test
    fun `three or more verified answers can be counted as meaningful practice`() {
        assertTrue(isRewardEligibleClozeRun(correctCount = 3, totalRounds = 10))
        assertTrue(isRewardEligibleClozeRun(correctCount = 10, totalRounds = 10))
    }
}
