package com.cinnamon.app.data.ai

import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response as OkHttpResponse
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AiGatewayErrorContractTest {

    @Test
    fun `required non success statuses map to stable typed failures`() {
        val cases = listOf(
            HttpCase(400, "invalid_request", AiGatewayFailureCode.InvalidRequest),
            HttpCase(401, "unauthenticated", AiGatewayFailureCode.Unauthenticated),
            HttpCase(403, "forbidden", AiGatewayFailureCode.Forbidden),
            HttpCase(409, "idempotency_conflict", AiGatewayFailureCode.IdempotencyConflict),
            HttpCase(429, "rate_limited", AiGatewayFailureCode.RateLimited),
            HttpCase(500, "gateway_unavailable", AiGatewayFailureCode.GatewayUnavailable),
            HttpCase(502, "upstream_unavailable", AiGatewayFailureCode.UpstreamUnavailable),
            HttpCase(503, "upstream_unavailable", AiGatewayFailureCode.UpstreamUnavailable)
        )

        cases.forEach { case ->
            val result = errorResponse(
                statusCode = case.statusCode,
                json = """{"error":{"code":"${case.wireCode}","requestId":"server-${case.statusCode}"}}"""
            ).toGatewayResult(clientRequestId = "client-${case.statusCode}")

            val failure = assertType<AiGatewayResult.Failure>(result).error
            val httpFailure = assertType<AiGatewayError.HttpFailure>(failure)
            assertEquals(case.expectedCode, httpFailure.code)
            assertEquals(case.statusCode, httpFailure.statusCode)
            assertEquals("server-${case.statusCode}", httpFailure.requestId)
            assertEquals(AiGatewayDeliveryCertainty.ResponseReceived, httpFailure.deliveryCertainty)
        }
    }

    @Test
    fun `HTTP status remains authoritative when error code is absent or contradictory`() {
        val unauthenticated = errorResponse(
            statusCode = 401,
            json = """{"error":{"code":"forbidden","requestId":"server-auth"}}"""
        ).toGatewayResult("client-auth")
        val gatewayFailure = errorResponse(
            statusCode = 500,
            json = """{"error":{"code":"forbidden","requestId":"server-500"}}"""
        ).toGatewayResult("client-500")
        val defaultUpstreamFailure = errorResponse(
            statusCode = 504,
            json = "{}"
        ).toGatewayResult("client-504")

        assertEquals(
            AiGatewayFailureCode.Unauthenticated,
            assertHttpFailure(unauthenticated).code
        )
        assertEquals(
            AiGatewayFailureCode.GatewayUnavailable,
            assertHttpFailure(gatewayFailure).code
        )
        assertEquals(
            AiGatewayFailureCode.UpstreamUnavailable,
            assertHttpFailure(defaultUpstreamFailure).code
        )
    }

    @Test
    fun `request correlation and retry after metadata survive error mapping`() {
        val result = errorResponse(
            statusCode = 429,
            json = """{"error":{"code":"rate_limited","requestId":"body-request"}}""",
            headers = Headers.headersOf(
                "X-Request-ID", "header-request",
                "Retry-After", "120"
            )
        ).toGatewayResult(clientRequestId = "client-request")

        val failure = assertHttpFailure(result)
        assertEquals("body-request", failure.requestId)
        assertEquals("120", failure.retryAfter?.headerValue)
        assertEquals(120L, failure.retryAfter?.delaySeconds)
    }

    @Test
    fun `request id falls back from safe header to client correlation id`() {
        val headerResult = errorResponse(
            statusCode = 403,
            json = """{"error":{"code":"forbidden"}}""",
            headers = Headers.headersOf("X-Request-ID", "header-request")
        ).toGatewayResult(clientRequestId = "client-request")
        val clientResult = errorResponse(
            statusCode = 500,
            json = "not-json"
        ).toGatewayResult(clientRequestId = "client-request")

        assertEquals("header-request", assertHttpFailure(headerResult).requestId)
        assertEquals("client-request", assertHttpFailure(clientResult).requestId)
    }

    @Test
    fun `successful response preserves server request id and trims reply`() {
        val result = Response.success(
            GatewayChatResponse(
                reply = "  A concise coaching reply.  ",
                requestId = "server-success"
            )
        ).toGatewayResult(clientRequestId = "client-success")

        val success = assertType<AiGatewayResult.Success>(result)
        assertEquals("A concise coaching reply.", success.reply)
        assertEquals("server-success", success.requestId)
    }

    @Test
    fun `empty successful payload is a confirmed invalid response not delivery uncertainty`() {
        val result = Response.success(
            GatewayChatResponse(reply = "   ", requestId = "server-empty")
        ).toGatewayResult(clientRequestId = "client-empty")

        val error = assertType<AiGatewayResult.Failure>(result).error
        val invalidResponse = assertType<AiGatewayError.InvalidResponse>(error)
        assertEquals("server-empty", invalidResponse.requestId)
        assertEquals(200, invalidResponse.statusCode)
        assertEquals(
            AiGatewayDeliveryCertainty.ResponseReceived,
            invalidResponse.deliveryCertainty
        )
    }

    @Test
    fun `delivery remains unconfirmed only after send begins without response headers`() {
        val notSent = AiGatewayFailureMapper.fromInterruptedExchange(
            clientRequestId = "client-not-sent",
            snapshot = AiGatewayCallSnapshot(
                requestStarted = false,
                responseStatusCode = null,
                responseRequestId = null,
                retryAfterHeader = null
            ),
            failureBeforeSend = AiGatewayError.NetworkUnavailable("client-not-sent")
        )
        val unknownAfterSend = AiGatewayFailureMapper.fromInterruptedExchange(
            clientRequestId = "client-unknown",
            snapshot = AiGatewayCallSnapshot(
                requestStarted = true,
                responseStatusCode = null,
                responseRequestId = null,
                retryAfterHeader = null
            ),
            failureBeforeSend = AiGatewayError.NetworkUnavailable("client-unknown")
        )

        assertType<AiGatewayError.NetworkUnavailable>(notSent)
        assertEquals(AiGatewayDeliveryCertainty.NotSent, notSent.deliveryCertainty)
        assertType<AiGatewayError.DeliveryUnconfirmed>(unknownAfterSend)
        assertEquals(
            AiGatewayDeliveryCertainty.UnknownAfterSend,
            unknownAfterSend.deliveryCertainty
        )
    }

    @Test
    fun `captured response headers turn interrupted reads into confirmed typed failures`() {
        val error = AiGatewayFailureMapper.fromInterruptedExchange(
            clientRequestId = "client-response",
            snapshot = AiGatewayCallSnapshot(
                requestStarted = true,
                responseStatusCode = 503,
                responseRequestId = "header-response",
                retryAfterHeader = "Wed, 21 Oct 2026 07:28:00 GMT"
            ),
            failureBeforeSend = AiGatewayError.NetworkUnavailable("client-response")
        )

        val failure = assertType<AiGatewayError.HttpFailure>(error)
        assertEquals(AiGatewayFailureCode.GatewayUnavailable, failure.code)
        assertEquals("header-response", failure.requestId)
        assertEquals("Wed, 21 Oct 2026 07:28:00 GMT", failure.retryAfter?.headerValue)
        assertNull(failure.retryAfter?.delaySeconds)
        assertEquals(AiGatewayDeliveryCertainty.ResponseReceived, failure.deliveryCertainty)
    }

    private fun assertHttpFailure(result: AiGatewayResult): AiGatewayError.HttpFailure {
        val error = assertType<AiGatewayResult.Failure>(result).error
        return assertType(error)
    }

    private inline fun <reified T> assertType(value: Any?): T {
        assertTrue("Expected ${T::class.java.simpleName}, got ${value?.javaClass?.simpleName}", value is T)
        return value as T
    }

    private fun errorResponse(
        statusCode: Int,
        json: String,
        headers: Headers = Headers.Builder().build()
    ): Response<GatewayChatResponse> {
        val rawResponse = OkHttpResponse.Builder()
            .request(Request.Builder().url("https://gateway.cinnamon.example/v1/ai/chat").build())
            .protocol(Protocol.HTTP_1_1)
            .code(statusCode)
            .message("Test response")
            .headers(headers)
            .build()
        return Response.error(
            json.toResponseBody("application/json".toMediaType()),
            rawResponse
        )
    }

    private data class HttpCase(
        val statusCode: Int,
        val wireCode: String,
        val expectedCode: AiGatewayFailureCode
    )
}
