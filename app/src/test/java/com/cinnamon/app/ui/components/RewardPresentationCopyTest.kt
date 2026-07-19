package com.cinnamon.app.ui.components

import com.cinnamon.app.data.gamification.PendingRewardPresentation
import org.junit.Assert.assertEquals
import org.junit.Test

class RewardPresentationCopyTest {

    @Test
    fun `positive ledger receipt states the settled XP amount`() {
        val copy = rewardPresentationCopy(12L)

        assertEquals("Progress recorded", copy.title)
        assertEquals("+12 XP added after this verified activity.", copy.detail)
        assertEquals("Dismiss reward notice", copy.dismissLabel)
    }

    @Test
    fun `zero value receipt never implies an XP award`() {
        val copy = rewardPresentationCopy(0L)

        assertEquals("Practice recorded", copy.title)
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

        assertEquals("Quest reward secured", copy.title)
        assertEquals("+10 XP added to your balance from this completed checkpoint.", copy.detail)
        assertEquals("Dismiss quest reward", copy.dismissLabel)
    }

    @Test
    fun `milestone receipt says unlock persists after dismissal`() {
        val copy = rewardPresentationCopy(
            PendingRewardPresentation(
                receiptId = "achievement-receipt",
                presentationFamily = "presentation.achievement.level_up",
                xpAwarded = 20L,
                tier = "milestone"
            )
        )

        assertEquals("Milestone unlocked", copy.title)
        assertEquals(
            "+20 XP awarded from your learning progress. This milestone stays unlocked.",
            copy.detail
        )
        assertEquals("Dismiss milestone notice", copy.dismissLabel)
    }

    @Test
    fun `journey receipt celebrates a settled chapter without making dismissal a claim`() {
        val copy = rewardPresentationCopy(
            PendingRewardPresentation(
                receiptId = "journey-receipt",
                presentationFamily = "presentation.journey.stage_complete",
                xpAwarded = 20L,
                tier = "milestone"
            )
        )

        assertEquals("Journey chapter secured", copy.title)
        assertEquals("+20 XP added. This chapter is now secured on your saved route.", copy.detail)
        assertEquals("Dismiss journey milestone", copy.dismissLabel)
    }
}
