package com.cinnamon.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiGatewayEndpointTest {

    @Test
    fun `normalizes an HTTPS gateway base URL`() {
        assertEquals(
            "https://gateway.cinnamon.example/v1/",
            AiGatewayEndpoint.normalize("  https://gateway.cinnamon.example/v1  ")
        )
        assertEquals(
            "https://gateway.cinnamon.example/v1/",
            AiGatewayEndpoint.normalize("https://gateway.cinnamon.example/v1/")
        )
        assertEquals(
            "https://gateway.cinnamon.example/api/chat/",
            AiGatewayEndpoint.normalize("HTTPS://gateway.cinnamon.example/api/./v1/../chat")
        )
        assertEquals(
            "https://gateway.cinnamon.example/",
            AiGatewayEndpoint.normalize("https://gateway.cinnamon.example")
        )
    }

    @Test
    fun `rejects missing non HTTPS and relative gateway URLs`() {
        val invalidUrls = listOf(
            "",
            "   ",
            "http://gateway.cinnamon.example",
            "gateway.cinnamon.example",
            "/v1/ai",
            "https:///v1/ai"
        )

        invalidUrls.forEach { url ->
            assertNull("Expected gateway URL to be rejected: '$url'", AiGatewayEndpoint.normalize(url))
        }
    }

    @Test
    fun `rejects gateway URLs containing embedded credentials`() {
        val invalidUrls = listOf(
            "https://user@gateway.cinnamon.example",
            "https://user:password@gateway.cinnamon.example"
        )

        invalidUrls.forEach { url ->
            assertNull("Expected credential-bearing URL to be rejected", AiGatewayEndpoint.normalize(url))
        }
    }

    @Test
    fun `rejects gateway URLs containing query or fragment components`() {
        val invalidUrls = listOf(
            "https://gateway.cinnamon.example/api?tenant=demo",
            "https://gateway.cinnamon.example/api#configuration"
        )

        invalidUrls.forEach { url ->
            assertNull("Expected ambiguous gateway URL to be rejected", AiGatewayEndpoint.normalize(url))
        }
    }

    @Test
    fun `accepts only valid explicit TCP ports`() {
        assertEquals(
            "https://gateway.cinnamon.example:443/",
            AiGatewayEndpoint.normalize("https://gateway.cinnamon.example:443")
        )

        val invalidUrls = listOf(
            "https://gateway.cinnamon.example:0",
            "https://gateway.cinnamon.example:65536",
            "https://gateway.cinnamon.example:",
            "https://gateway.cinnamon.example:not-a-port"
        )
        invalidUrls.forEach { url ->
            assertNull("Expected out-of-range gateway port to be rejected", AiGatewayEndpoint.normalize(url))
        }
    }
}
