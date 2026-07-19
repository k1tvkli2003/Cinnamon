package com.cinnamon.app.ui.screens.home

import com.cinnamon.app.viewmodel.Quest

/**
 * Pure, testable copy projection for the Home mission card. The UI never
 * invents completion: every completed/claimed message comes from persisted
 * quest state projected by [Quest].
 */
internal data class MissionPulseCopy(
    val title: String,
    val detail: String
)

internal fun missionPulseCopy(
    quest: Quest?,
    dueNow: Int
): MissionPulseCopy {
    val remainingToPrime = (3 - dueNow.coerceAtLeast(0)).coerceAtLeast(0)
    val title = when {
        quest?.claimed == true -> "Quest cleared"
        quest?.claimable == true -> "Daily quest complete"
        quest != null -> quest.title
        else -> "Ready your daily quest"
    }
    val detail = when {
        quest?.claimed == true -> {
            "Reward secured. This daily milestone stays complete."
        }

        quest?.claimable == true -> {
            "Three verified reviews recorded. Claim your one-time daily reward."
        }

        quest != null -> quest.detail
        remainingToPrime > 0 -> {
            "Add $remainingToPrime more ${if (remainingToPrime == 1) "word" else "words"} to begin your three-review mission."
        }

        else -> "Complete three distinct due reviews to finish today’s checkpoint."
    }
    return MissionPulseCopy(title = title, detail = detail)
}
