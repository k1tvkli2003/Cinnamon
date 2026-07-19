package com.cinnamon.app.data.startup

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.gamification.GamificationRepository
import com.cinnamon.app.domain.gamification.FoundationCampaignCatalog
import com.cinnamon.app.domain.gamification.FoundationJourneyCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies the full on-device startup pipeline, not only the catalog parser. */
@RunWith(AndroidJUnit4::class)
class AppStartupCoordinatorAndroidTest {
    @Test
    fun bundledStartup_reachesReady() {
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext

            val result = AppStartupCoordinator.prepare(context)

            assertEquals(AppStartupState.Ready, result)
            assertEquals(AppStartupState.Ready, AppStartupCoordinator.state.value)
            val database = AppDatabase.getDatabase(context)
            val journeys = database.gamificationDao()
                .observeJourneyInstances(GamificationRepository.LOCAL_ACTOR_ID)
                .first()
            val stages = database.gamificationDao()
                .observeJourneyStages(GamificationRepository.LOCAL_ACTOR_ID)
                .first()
            val campaignChoices = database.gamificationDao()
                .observeCampaignRouteChoices(GamificationRepository.LOCAL_ACTOR_ID)
                .first()
            assertEquals(4, database.openHelper.readableDatabase.version)
            val foundation = journeys.single { instance ->
                instance.definitionId == FoundationJourneyCatalog.definition.id &&
                    instance.definitionVersion == FoundationJourneyCatalog.definition.version
            }
            assertEquals(
                4,
                stages.count { stage -> stage.journeyInstanceId == foundation.journeyInstanceId }
            )
            val currentCampaignChoices = campaignChoices.filter { choice ->
                choice.campaignDefinitionId == FoundationCampaignCatalog.definition.id &&
                    choice.campaignDefinitionVersion == FoundationCampaignCatalog.definition.version
            }
            assertTrue(currentCampaignChoices.size <= 1)
            currentCampaignChoices.singleOrNull()?.let { choice ->
                val route = requireNotNull(FoundationCampaignCatalog.definition.route(choice.routeId))
                assertEquals(route.journey.id, choice.journeyDefinitionId)
                val routeInstance = journeys.single { instance ->
                    instance.definitionId == route.journey.id &&
                        instance.definitionVersion == route.journey.version
                }
                assertEquals(
                    route.journey.stages.size,
                    stages.count { stage -> stage.journeyInstanceId == routeInstance.journeyInstanceId }
                )
            }
        }
    }
}
