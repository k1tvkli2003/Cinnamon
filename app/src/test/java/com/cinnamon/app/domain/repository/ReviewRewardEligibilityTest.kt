package com.cinnamon.app.domain.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewRewardEligibilityTest {

    @Test
    fun `again is a retry and never successful recall evidence`() {
        assertFalse(isSuccessfulReviewQuality(1))
    }

    @Test
    fun `hard good and easy are successful recall evidence`() {
        assertTrue(isSuccessfulReviewQuality(3))
        assertTrue(isSuccessfulReviewQuality(4))
        assertTrue(isSuccessfulReviewQuality(5))
    }
}
