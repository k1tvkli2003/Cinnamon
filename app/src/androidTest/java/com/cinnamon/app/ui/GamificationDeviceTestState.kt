package com.cinnamon.app.ui

import android.content.Context
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.startup.AppStartupCoordinator
import com.cinnamon.app.data.startup.AppStartupState

/**
 * Resets only the disposable debug app's learner/gamification state while preserving bundled
 * content. Callers must run on an explicitly selected test device; host verifiers reject physical
 * devices unless the operator opts in.
 */
internal object GamificationDeviceTestState {

    suspend fun reset(
        context: Context,
        configureLexicon: suspend (database: AppDatabase, nowEpochMillis: Long) -> Unit = { _, _ -> }
    ): AppDatabase {
        check(AppStartupCoordinator.prepare(context) == AppStartupState.Ready) {
            "Cinnamon startup was not ready before the device-test reset"
        }
        val database = AppDatabase.getDatabase(context)
        val writableDatabase = database.openHelper.writableDatabase

        database.runInTransaction {
            // Foreign-key-safe child-to-parent order. These are all derived learner rows; the
            // bundled lexicon and content catalogs stay installed and are never deleted.
            listOf(
                "learning_focus_selections",
                "reward_presentation_receipts",
                "achievement_unlocks",
                "journey_stage_progress",
                "journey_instances",
                "quest_instances",
                "reward_balances",
                "reward_transactions",
                "reward_summaries",
                "gamification_events"
            ).forEach { table -> writableDatabase.execSQL("DELETE FROM $table") }
            writableDatabase.execSQL(
                """
                UPDATE lexicon
                SET reps = 0,
                    easeFactor = 2.5,
                    intervalDays = 0,
                    dueAt = 0,
                    timesSeen = 0
                """.trimIndent()
            )
        }

        configureLexicon(database, System.currentTimeMillis())
        check(AppStartupCoordinator.prepare(context) == AppStartupState.Ready) {
            "Cinnamon startup was not ready after the device-test reset"
        }
        return database
    }
}
