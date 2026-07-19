package com.cinnamon.app.ui.screens.games

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioLanguageSprintTest {

    @Test
    fun authoredPromptsAcceptTheirDocumentedReferenceAnswers() {
        assertTrue(matchesScenarioLanguageSprintPrompt("aortic dissection", scenarioLanguageSprintPrompts[0]))
        assertTrue(matchesScenarioLanguageSprintPrompt("When did the pain begin?", scenarioLanguageSprintPrompts[1]))
        assertTrue(matchesScenarioLanguageSprintPrompt("I will ask a few questions first.", scenarioLanguageSprintPrompts[2]))
    }

    @Test
    fun blankAnswerNeverCompletesASprintStep() {
        scenarioLanguageSprintPrompts.forEach { prompt ->
            assertFalse(matchesScenarioLanguageSprintPrompt("   ", prompt))
        }
    }

    @Test
    fun transcriptCluesAcceptTheirDocumentedReferenceAnswers() {
        assertTrue(matchesTranscriptEscapeAnswer("wheezing", transcriptEscapeClues[0]))
        assertTrue(matchesTranscriptEscapeAnswer("shortness of breath", transcriptEscapeClues[1]))
        assertTrue(matchesTranscriptEscapeAnswer("I will ask one more question", transcriptEscapeClues[2]))
    }
}
