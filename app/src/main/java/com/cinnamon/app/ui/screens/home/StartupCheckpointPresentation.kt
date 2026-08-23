package com.cinnamon.app.ui.screens.home

import com.cinnamon.app.data.seed.LexiconSeedState
import com.cinnamon.app.data.startup.AppStartupStage
import com.cinnamon.app.data.startup.AppStartupState

/**
 * Stable, testable product language for the startup gate. Keeping this outside
 * the composable prevents a new startup stage from silently falling back to a
 * vague error message.
 */
internal data class StartupCheckpointPresentation(
    val eyebrow: String,
    val title: String,
    val body: String,
    val protectionNote: String,
    val actionLabel: String?,
    val stateDescription: String,
    val isFailure: Boolean
)

internal fun startupCheckpointPresentation(
    startupState: AppStartupState,
    seedState: LexiconSeedState
): StartupCheckpointPresentation {
    val failedStage = (startupState as? AppStartupState.Failed)?.stage
        ?: AppStartupStage.Lexicon.takeIf { seedState == LexiconSeedState.Failed }

    return if (failedStage != null) {
        failedStartupCheckpoint(failedStage)
    } else {
        preparingStartupCheckpoint(seedState)
    }
}

private fun preparingStartupCheckpoint(seedState: LexiconSeedState): StartupCheckpointPresentation {
    val validatingLexicon = seedState == LexiconSeedState.Validating
    return StartupCheckpointPresentation(
        eyebrow = "LOCAL READINESS CHECK",
        title = if (validatingLexicon) {
            "Checking your language toolkit"
        } else {
            "Warming up your learning room"
        },
        body = if (validatingLexicon) {
            "Cinnamon is verifying the built-in words and practice material before opening today’s sessions."
        } else {
            "Your saved progress, Questboard and local learning record are being prepared together."
        },
        protectionNote = "Learning opens only after every local check passes.",
        actionLabel = null,
        stateDescription = "Preparing Cinnamon",
        isFailure = false
    )
}

private fun failedStartupCheckpoint(stage: AppStartupStage): StartupCheckpointPresentation {
    val copy = when (stage) {
        AppStartupStage.GamificationCatalog -> RecoveryCopy(
            title = "Your learning map paused safely",
            body = "The built-in Quest and milestone catalog did not pass verification, so Cinnamon stopped before rebuilding today’s plan.",
            action = "Verify the map again"
        )
        AppStartupStage.DatabaseMigration -> RecoveryCopy(
            title = "Your progress vault stayed locked",
            body = "The local learning record could not be opened or upgraded safely. Cinnamon stopped instead of replacing it.",
            action = "Retry safe opening"
        )
        AppStartupStage.CatalogReconciliation -> RecoveryCopy(
            title = "Today’s Questboard is holding its place",
            body = "Quests and milestones could not be reconciled with the verified catalog, so no new assignment was published.",
            action = "Prepare the Questboard again"
        )
        AppStartupStage.JourneyReconciliation -> RecoveryCopy(
            title = "Foundation Plan needs one more check",
            body = "Cinnamon could not safely align the Foundation milestones with your saved evidence, so the plan stayed paused.",
            action = "Check the plan again"
        )
        AppStartupStage.ProgressImport -> RecoveryCopy(
            title = "Your earlier progress is still waiting safely",
            body = "The previous local record could not be imported completely. The original snapshot was left in place for another attempt.",
            action = "Retry progress recovery"
        )
        AppStartupStage.Lexicon -> RecoveryCopy(
            title = "Your language toolkit needs a recheck",
            body = "A bundled word or practice file did not finish validation, so Cinnamon kept the learning activities closed.",
            action = "Check the toolkit again"
        )
    }

    return StartupCheckpointPresentation(
        eyebrow = "RECOVERY CHECKPOINT",
        title = copy.title,
        body = copy.body,
        protectionNote = "Your XP, review history and saved progress are still intact.",
        actionLabel = copy.action,
        stateDescription = "Setup recovery required",
        isFailure = true
    )
}

private data class RecoveryCopy(
    val title: String,
    val body: String,
    val action: String
)
