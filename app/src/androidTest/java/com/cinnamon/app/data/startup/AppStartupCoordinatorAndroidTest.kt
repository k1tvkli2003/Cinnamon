package com.cinnamon.app.data.startup

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cinnamon.app.data.gamification.GamificationRepository
import com.cinnamon.app.data.gamification.LegacyLearningFocusWireCompatibility
import com.cinnamon.app.data.gamification.LegacyFoundationJourneyWireCompatibility
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.seed.MeshReferenceSeedState
import com.cinnamon.app.data.seed.MeshReferenceSeeder
import com.cinnamon.app.domain.gamification.FoundationJourneyCatalog
import com.cinnamon.app.domain.gamification.FoundationLearningFocusCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the full on-device startup pipeline, not only the catalog parser. */
@RunWith(AndroidJUnit4::class)
class AppStartupCoordinatorAndroidTest {
    private companion object {
        const val TAG = "CinnamonStartupRuntime"
        const val CRITICAL_READY_BUDGET_MILLIS = 5_000L
    }

    @Test
    fun bundledStartup_reachesReady() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext

            val startupStartedAt = SystemClock.elapsedRealtime()
            val result = AppStartupCoordinator.prepare(context)
            val criticalReadyMillis = SystemClock.elapsedRealtime() - startupStartedAt
            Log.i(TAG, "critical_ready_ms=$criticalReadyMillis")

            assertEquals(AppStartupState.Ready, result)
            assertEquals(AppStartupState.Ready, AppStartupCoordinator.state.value)
            assertTrue(
                "Critical startup exceeded ${CRITICAL_READY_BUDGET_MILLIS}ms: " +
                    "${criticalReadyMillis}ms",
                criticalReadyMillis <= CRITICAL_READY_BUDGET_MILLIS
            )
            val database = AppDatabase.getDatabase(context)
            val journeys = database.gamificationDao()
                .observeJourneyInstances(GamificationRepository.LOCAL_ACTOR_ID)
                .first()
            val stages = database.gamificationDao()
                .observeJourneyStages(GamificationRepository.LOCAL_ACTOR_ID)
                .first()
            val focusSelections = database.gamificationDao()
                .observeLearningFocusSelections(GamificationRepository.LOCAL_ACTOR_ID)
                .first()
            assertEquals(6, database.openHelper.readableDatabase.version)
            val foundation = journeys.single { instance ->
                LegacyFoundationJourneyWireCompatibility.matchesDefinitionIdentity(
                    definition = FoundationJourneyCatalog.definition,
                    storedDefinitionId = instance.definitionId,
                    storedDefinitionVersion = instance.definitionVersion
                )
            }
            assertEquals(
                4,
                stages.count { stage -> stage.journeyInstanceId == foundation.journeyInstanceId }
            )
            val definition = FoundationLearningFocusCatalog.definition
            val currentSelections = focusSelections.filter { selection ->
                LegacyLearningFocusWireCompatibility.matchesDefinitionIdentity(
                    definition = definition,
                    storedDefinitionId = selection.definitionId,
                    storedDefinitionVersion = selection.definitionVersion
                )
            }
            assertTrue(currentSelections.size <= 1)
            currentSelections.singleOrNull()?.let { selection ->
                val option = requireNotNull(
                    LegacyLearningFocusWireCompatibility.canonicalOption(
                        definition = definition,
                        storedDefinitionId = selection.definitionId,
                        storedDefinitionVersion = selection.definitionVersion,
                        storedOptionId = selection.optionId,
                        storedMilestonePlanDefinitionId = selection.milestonePlanDefinitionId
                    )
                )
                val persistedPlan = LegacyLearningFocusWireCompatibility.milestonePlanForSelection(
                    option = option,
                    persistedMilestonePlanDefinitionId = selection.milestonePlanDefinitionId
                )
                assertEquals(persistedPlan.id, selection.milestonePlanDefinitionId)
                val focusInstance = journeys.single { instance ->
                    instance.definitionId == persistedPlan.id &&
                        instance.definitionVersion == persistedPlan.version
                }
                assertEquals(
                    persistedPlan.stages.size,
                    stages.count { stage ->
                        stage.journeyInstanceId == focusInstance.journeyInstanceId
                    }
                )
            }
            val atlasState = withTimeout(20_000L) {
                MeshReferenceSeeder.state.first { state ->
                    state == MeshReferenceSeedState.Ready ||
                        state == MeshReferenceSeedState.Failed
                }
            }
            val atlasReadyMillis = SystemClock.elapsedRealtime() - startupStartedAt
            Log.i(TAG, "atlas_ready_ms=$atlasReadyMillis")
            assertEquals(MeshReferenceSeedState.Ready, atlasState)
            assertEquals(5742, database.meshReferenceDao().countOnce())
        }
    }
}
