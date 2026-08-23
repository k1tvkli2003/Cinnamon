package com.cinnamon.app.ui.screens.home

import com.cinnamon.app.data.seed.LexiconSeedState
import com.cinnamon.app.data.startup.AppStartupStage
import com.cinnamon.app.data.startup.AppStartupState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupCheckpointPresentationTest {

    @Test
    fun `every startup failure has distinct actionable recovery copy`() {
        val presentations = AppStartupStage.entries.map { stage ->
            startupCheckpointPresentation(
                startupState = AppStartupState.Failed(stage),
                seedState = LexiconSeedState.Failed
            )
        }

        assertEquals(AppStartupStage.entries.size, presentations.map { it.title }.distinct().size)
        assertEquals(AppStartupStage.entries.size, presentations.map { it.actionLabel }.distinct().size)
        presentations.forEach { presentation ->
            assertTrue(presentation.isFailure)
            assertEquals("RECOVERY CHECKPOINT", presentation.eyebrow)
            assertNotNull(presentation.actionLabel)
            assertTrue(presentation.protectionNote.contains("XP"))
            assertTrue(presentation.body.length > 60)
        }
    }

    @Test
    fun `seed-only failure is reported as lexicon recovery`() {
        val presentation = startupCheckpointPresentation(
            startupState = AppStartupState.Preparing,
            seedState = LexiconSeedState.Failed
        )

        assertEquals("Your language toolkit needs a recheck", presentation.title)
        assertEquals("Check the toolkit again", presentation.actionLabel)
        assertTrue(presentation.isFailure)
    }

    @Test
    fun `preparing state stays non actionable and truthful`() {
        val presentation = startupCheckpointPresentation(
            startupState = AppStartupState.Preparing,
            seedState = LexiconSeedState.Validating
        )

        assertFalse(presentation.isFailure)
        assertNull(presentation.actionLabel)
        assertEquals("Checking your language toolkit", presentation.title)
        assertEquals("Preparing Cinnamon", presentation.stateDescription)
    }

}
