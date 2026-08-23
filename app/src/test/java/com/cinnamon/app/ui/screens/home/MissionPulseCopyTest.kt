package com.cinnamon.app.ui.screens.home

import com.cinnamon.app.viewmodel.Quest
import org.junit.Assert.assertEquals
import org.junit.Test

class MissionPulseCopyTest {
    @Test
    fun `unassigned quest explains exactly how many words are missing`() {
        assertEquals(
            MissionPulseCopy(
                title = "Ready your daily quest",
                detail = "Add 3 more words to begin your three-review mission."
            ),
            missionPulseCopy(quest = null, dueNow = 0)
        )
        assertEquals(
            "Add 1 more word to begin your three-review mission.",
            missionPulseCopy(quest = null, dueNow = 2).detail
        )
    }

    @Test
    fun `unassigned but primed quest describes verified review evidence`() {
        assertEquals(
            "Complete three distinct due reviews to finish today’s checkpoint.",
            missionPulseCopy(quest = null, dueNow = 3).detail
        )
    }

    @Test
    fun `active quest preserves catalog title and ledger detail`() {
        val quest = quest(
            title = "Review relay",
            detail = "Two distinct reviews remain.",
            progress = 1
        )

        assertEquals(
            MissionPulseCopy(
                title = "Review relay",
                detail = "Two distinct reviews remain."
            ),
            missionPulseCopy(quest = quest, dueNow = 2)
        )
    }

    @Test
    fun `completed quest prompts a one-time claim from verified evidence`() {
        val copy = missionPulseCopy(
            quest = quest(progress = 3, completed = true, claimable = true),
            dueNow = 0
        )

        assertEquals("Daily quest complete", copy.title)
        assertEquals(
            "Three distinct reviews completed. Claim your one-time daily reward.",
            copy.detail
        )
    }

    @Test
    fun `claimed quest keeps the settled state visible`() {
        val copy = missionPulseCopy(
            quest = quest(progress = 3, completed = true, claimed = true),
            dueNow = 0
        )

        assertEquals("Quest cleared", copy.title)
        assertEquals("Reward earned. This daily quest stays complete.", copy.detail)
    }

    private fun quest(
        title: String = "Review relay",
        detail: String = "Review three distinct due words.",
        progress: Int = 0,
        completed: Boolean = false,
        claimable: Boolean = false,
        claimed: Boolean = false
    ) = Quest(
        id = "quest.daily.review_due_3",
        title = title,
        progress = progress,
        target = 3,
        completed = completed,
        detail = detail,
        instanceId = "quest.daily.review_due_3:2026-07-18",
        claimable = claimable,
        claimed = claimed,
        rewardXp = 35
    )
}
