package com.cinnamon.app.domain.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConceptMasteryPromotionTest {

    @Test
    fun `only the first delayed third success promotes a concept to mastery`() {
        assertTrue(
            isConceptMasteryPromotion(
                quality = 4,
                previousReps = 2,
                updatedReps = 3,
                updatedIntervalDays = 4f
            )
        )
        assertFalse(
            isConceptMasteryPromotion(
                quality = 4,
                previousReps = 3,
                updatedReps = 4,
                updatedIntervalDays = 8f
            )
        )
    }

    @Test
    fun `again and short intervals never create delayed mastery`() {
        assertFalse(isConceptMasteryPromotion(1, 2, 3, 4f))
        assertFalse(isConceptMasteryPromotion(4, 2, 3, 3f))
    }
}
