package com.cinnamon.app.data.ai

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

class AiGatewayRequestContractTest {

    @Test
    fun `conversation modes have stable allowlisted wire values`() {
        assertEquals("native_coach", AiConversationMode.NativeCoach.wireValue)
        assertEquals("standardized_patient", AiConversationMode.StandardizedPatient.wireValue)
    }

    @Test
    fun `serialized request contains only product contract fields`() {
        val request = GatewayChatRequest(
            mode = AiConversationMode.StandardizedPatient.wireValue,
            message = "How long have you had this symptom?",
            scenarioId = "history-taking-basics"
        )
        val json = Moshi.Builder()
            .build()
            .adapter(GatewayChatRequest::class.java)
            .toJson(request)

        val serializedKeys = Regex("\\\"([^\\\"]+)\\\"\\s*:")
            .findAll(json)
            .map { match -> match.groupValues[1] }
            .toSet()

        assertEquals(setOf("mode", "message", "scenarioId"), serializedKeys)

        val forbiddenBoundaryTerms = listOf(
            "apiKey",
            "authorization",
            "model",
            "provider",
            "systemPrompt",
            "token"
        )
        forbiddenBoundaryTerms.forEach { term ->
            assertFalse("Provider concern leaked into gateway JSON: $term", json.contains(term, ignoreCase = true))
        }
    }

    @Test
    fun `gateway call requires product auth idempotency and request correlation headers`() {
        val method = AiGatewayService::class.java.declaredMethods.single { candidate ->
            candidate.name == "sendChat" && candidate.getAnnotation(POST::class.java) != null
        }

        val post = requireNotNull(method.getAnnotation(POST::class.java))
        assertEquals("v1/ai/chat", post.value)

        val declaredHeaders = method.parameterAnnotations
            .flatMap { annotations -> annotations.filterIsInstance<Header>() }
            .map { header -> header.value }
            .toSet()

        assertEquals(
            setOf("Authorization", "Idempotency-Key", "X-Request-ID"),
            declaredHeaders
        )
        assertEquals(
            1,
            method.parameterAnnotations.count { annotations ->
                annotations.filterIsInstance<Body>().isNotEmpty()
            }
        )
        assertNotNull(method.parameterAnnotations.firstOrNull { annotations ->
            annotations.filterIsInstance<Header>().any { it.value == "Idempotency-Key" }
        })
    }
}
