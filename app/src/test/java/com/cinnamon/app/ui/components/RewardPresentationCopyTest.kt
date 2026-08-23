package com.cinnamon.app.ui.components

import com.cinnamon.app.data.gamification.PendingRewardPresentation
import org.junit.Assert.assertEquals
import org.junit.Test

class RewardPresentationCopyTest {

    @Test
    fun `positive receipt states the earned XP amount`() {
        val copy = rewardPresentationCopy(12L)

        assertEquals("Progress saved", copy.title)
        assertEquals("+12 XP earned from this completed activity.", copy.detail)
        assertEquals("Dismiss reward notice", copy.dismissLabel)
    }

    @Test
    fun `zero value receipt never implies an XP award`() {
        val copy = rewardPresentationCopy(0L)

        assertEquals("Practice saved", copy.title)
        assertEquals("Your completed activity is saved.", copy.detail)
        assertEquals("Dismiss reward notice", copy.dismissLabel)
    }

    @Test
    fun `quest receipt names the one completed checkpoint reward`() {
        val copy = rewardPresentationCopy(
            PendingRewardPresentation(
                receiptId = "quest-receipt",
                presentationFamily = "presentation.quest.complete",
                xpAwarded = 10L,
                tier = "standard"
            )
        )

        assertEquals("Quest complete", copy.title)
        assertEquals("+10 XP earned from this checkpoint.", copy.detail)
        assertEquals("Dismiss quest reward", copy.dismissLabel)
    }

    @Test
    fun `achievement receipt says unlock persists after dismissal`() {
        val copy = rewardPresentationCopy(
            PendingRewardPresentation(
                receiptId = "achievement-receipt",
                presentationFamily = "presentation.achievement.level_up",
                xpAwarded = 20L,
                tier = "milestone"
            )
        )

        assertEquals("Achievement unlocked", copy.title)
        assertEquals(
            "+20 XP earned. This achievement stays in your collection.",
            copy.detail
        )
        assertEquals("Dismiss achievement notice", copy.dismissLabel)
    }

    @Test
    fun `foundation receipt celebrates a completed milestone without making dismissal a claim`() {
        val copy = rewardPresentationCopy(
            PendingRewardPresentation(
                receiptId = "journey-receipt",
                presentationFamily = "presentation.journey.stage_complete",
                xpAwarded = 20L,
                tier = "milestone"
            )
        )

        assertEquals("Milestone complete", copy.title)
        assertEquals("+20 XP earned. This Foundation milestone is now complete.", copy.detail)
        assertEquals("Dismiss milestone reward", copy.dismissLabel)
    }
}
