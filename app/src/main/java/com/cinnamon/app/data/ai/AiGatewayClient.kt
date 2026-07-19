package com.cinnamon.app.data.ai

import com.cinnamon.app.BuildConfig
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.CancellationException
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.Response as OkHttpResponse
import okhttp3.ResponseBody
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException
import java.net.URI
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * The Android client only talks to the product-owned gateway. Provider credentials,
 * model selection, and system prompts are intentionally server-side concerns.
 */
enum class AiConversationMode(val wireValue: String) {
    NativeCoach("native_coach"),
    StandardizedPatient("standardized_patient")
}

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class GatewayChatRequest(
    val mode: String,
    val message: String,
    val scenarioId: String? = null
)

@JsonClass(generateAdapter = true)
data class GatewayChatResponse(
    val reply: String?,
    val requestId: String? = null
)

@JsonClass(generateAdapter = true)
internal data class GatewayErrorEnvelope(
    val error: GatewayErrorPayload? = null
)

@JsonClass(generateAdapter = true)
internal data class GatewayErrorPayload(
    val code: String? = null,
    val requestId: String? = null
)

interface AiGatewayService {
    @POST("v1/ai/chat")
    suspend fun sendChat(
        @Header("Authorization") authorization: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Header("X-Request-ID") requestId: String,
        @Body request: GatewayChatRequest
    ): Response<GatewayChatResponse>
}

sealed interface AiGatewayResult {
    data class Success(
        val reply: String,
        val requestId: String
    ) : AiGatewayResult

    data object NotConfigured : AiGatewayResult
    data object NotAuthenticated : AiGatewayResult
    data class Failure(val error: AiGatewayError) : AiGatewayResult
}

/** Whether the user-authored request reached a point where delivery can be stated honestly. */
enum class AiGatewayDeliveryCertainty {
    NotSent,
    ResponseReceived,
    UnknownAfterSend
}

/** Stable product-facing failure codes. Raw gateway/provider messages never cross this boundary. */
enum class AiGatewayFailureCode(val wireValue: String) {
    InvalidRequest("invalid_request"),
    Unauthenticated("unauthenticated"),
    Forbidden("forbidden"),
    IdempotencyConflict("idempotency_conflict"),
    RateLimited("rate_limited"),
    GatewayUnavailable("gateway_unavailable"),
    UpstreamUnavailable("upstream_unavailable"),
    UnexpectedResponse("unexpected_response")
}

data class AiGatewayRetryAfter(val headerValue: String) {
    val delaySeconds: Long?
        get() = headerValue.toLongOrNull()?.takeIf { it >= 0 }
}

sealed interface AiGatewayError {
    val requestId: String
    val statusCode: Int?
    val retryAfter: AiGatewayRetryAfter?
    val deliveryCertainty: AiGatewayDeliveryCertainty

    data class HttpFailure(
        val code: AiGatewayFailureCode,
        override val requestId: String,
        override val statusCode: Int,
        override val retryAfter: AiGatewayRetryAfter?
    ) : AiGatewayError {
        override val deliveryCertainty = AiGatewayDeliveryCertainty.ResponseReceived
    }

    data class InvalidResponse(
        override val requestId: String,
        override val statusCode: Int
    ) : AiGatewayError {
        override val retryAfter: AiGatewayRetryAfter? = null
        override val deliveryCertainty = AiGatewayDeliveryCertainty.ResponseReceived
    }

    data class NetworkUnavailable(
        override val requestId: String
    ) : AiGatewayError {
        override val statusCode: Int? = null
        override val retryAfter: AiGatewayRetryAfter? = null
        override val deliveryCertainty = AiGatewayDeliveryCertainty.NotSent
    }

    data class ClientFailure(
        override val requestId: String
    ) : AiGatewayError {
        override val statusCode: Int? = null
        override val retryAfter: AiGatewayRetryAfter? = null
        override val deliveryCertainty = AiGatewayDeliveryCertainty.NotSent
    }

    /** No response headers arrived after the request began crossing the network boundary. */
    data class DeliveryUnconfirmed(
        override val requestId: String
    ) : AiGatewayError {
        override val statusCode: Int? = null
        override val retryAfter: AiGatewayRetryAfter? = null
        override val deliveryCertainty = AiGatewayDeliveryCertainty.UnknownAfterSend
    }
}

/**
 * Product authentication is intentionally not faked in the client. Until the app
 * has a real short-lived product session, live AI requests fail closed.
 */
object ProductSessionTokenProvider {
    suspend fun currentToken(): String? = null
}

object AiGatewayEndpoint {
    fun normalize(rawUrl: String): String? {
        val candidate = rawUrl.trim()
        if (candidate.isEmpty()) return null

        return runCatching {
            val uri = URI(candidate)
            val port = uri.port
            val authority = uri.rawAuthority ?: return@runCatching null
            if (!uri.scheme.equals("https", ignoreCase = true) ||
                uri.host.isNullOrBlank() ||
                authority.isBlank() ||
                authority.endsWith(":") ||
                uri.userInfo != null ||
                uri.rawQuery != null ||
                uri.rawFragment != null ||
                (port != -1 && port !in 1..65535)
            ) return@runCatching null

            val normalizedPath = uri.normalize().rawPath.orEmpty().trimEnd('/')
            "https://$authority${if (normalizedPath.isEmpty()) "/" else "$normalizedPath/"}"
        }.getOrNull()
    }
}

object AiGatewayClient {
    private fun createService(
        baseUrl: String,
        callTracker: AiGatewayCallTracker
    ): AiGatewayService {
        val client = OkHttpClient.Builder()
            .eventListener(callTracker)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AiGatewayService::class.java)
    }

    suspend fun send(
        mode: AiConversationMode,
        message: String,
        scenarioId: String?,
        idempotencyKey: String
    ): AiGatewayResult {
        val baseUrl = AiGatewayEndpoint.normalize(BuildConfig.AI_GATEWAY_BASE_URL)
            ?: return AiGatewayResult.NotConfigured
        val token = ProductSessionTokenProvider.currentToken()?.trim().orEmpty()
        if (token.isEmpty()) return AiGatewayResult.NotAuthenticated

        val requestId = UUID.randomUUID().toString()
        val callTracker = AiGatewayCallTracker()

        return try {
            val response = createService(baseUrl, callTracker).sendChat(
                authorization = "Bearer $token",
                idempotencyKey = idempotencyKey,
                requestId = requestId,
                request = GatewayChatRequest(
                    mode = mode.wireValue,
                    message = message,
                    scenarioId = scenarioId
                )
            )
            response.toGatewayResult(requestId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            AiGatewayResult.Failure(
                AiGatewayFailureMapper.fromInterruptedExchange(
                    clientRequestId = requestId,
                    snapshot = callTracker.snapshot(),
                    failureBeforeSend = AiGatewayError.NetworkUnavailable(requestId)
                )
            )
        } catch (_: RuntimeException) {
            AiGatewayResult.Failure(
                AiGatewayFailureMapper.fromInterruptedExchange(
                    clientRequestId = requestId,
                    snapshot = callTracker.snapshot(),
                    failureBeforeSend = AiGatewayError.ClientFailure(requestId)
                )
            )
        }
    }
}

internal data class AiGatewayCallSnapshot(
    val requestStarted: Boolean,
    val responseStatusCode: Int?,
    val responseRequestId: String?,
    val retryAfterHeader: String?
)

internal class AiGatewayCallTracker : EventListener() {
    @Volatile
    private var requestStarted = false

    @Volatile
    private var responseStatusCode: Int? = null

    @Volatile
    private var responseRequestId: String? = null

    @Volatile
    private var retryAfterHeader: String? = null

    override fun requestHeadersEnd(call: Call, request: okhttp3.Request) {
        requestStarted = true
    }

    override fun requestBodyStart(call: Call) {
        requestStarted = true
    }

    override fun responseHeadersEnd(call: Call, response: OkHttpResponse) {
        responseStatusCode = response.code
        responseRequestId = response.header(REQUEST_ID_HEADER).normalizedMetadata()
        retryAfterHeader = response.header(RETRY_AFTER_HEADER).normalizedMetadata()
    }

    fun snapshot(): AiGatewayCallSnapshot = AiGatewayCallSnapshot(
        requestStarted = requestStarted,
        responseStatusCode = responseStatusCode,
        responseRequestId = responseRequestId,
        retryAfterHeader = retryAfterHeader
    )
}

internal object AiGatewayFailureMapper {
    fun fromHttpResponse(
        statusCode: Int,
        gatewayCode: String?,
        requestId: String,
        retryAfterHeader: String?
    ): AiGatewayError.HttpFailure {
        val code = when (statusCode) {
            400, 422 -> AiGatewayFailureCode.InvalidRequest
            401 -> AiGatewayFailureCode.Unauthenticated
            403 -> AiGatewayFailureCode.Forbidden
            409 -> AiGatewayFailureCode.IdempotencyConflict
            429 -> AiGatewayFailureCode.RateLimited
            in 500..599 -> when {
                gatewayCode == AiGatewayFailureCode.UpstreamUnavailable.wireValue -> {
                    AiGatewayFailureCode.UpstreamUnavailable
                }
                gatewayCode == AiGatewayFailureCode.GatewayUnavailable.wireValue -> {
                    AiGatewayFailureCode.GatewayUnavailable
                }
                statusCode == 502 || statusCode == 504 -> AiGatewayFailureCode.UpstreamUnavailable
                else -> AiGatewayFailureCode.GatewayUnavailable
            }
            else -> AiGatewayFailureCode.UnexpectedResponse
        }

        return AiGatewayError.HttpFailure(
            code = code,
            requestId = requestId,
            statusCode = statusCode,
            retryAfter = retryAfterHeader.normalizedMetadata()?.let(::AiGatewayRetryAfter)
        )
    }

    fun fromInterruptedExchange(
        clientRequestId: String,
        snapshot: AiGatewayCallSnapshot,
        failureBeforeSend: AiGatewayError
    ): AiGatewayError {
        val responseStatus = snapshot.responseStatusCode
        val requestId = snapshot.responseRequestId ?: clientRequestId
        return when {
            responseStatus == null && !snapshot.requestStarted -> failureBeforeSend
            responseStatus == null -> AiGatewayError.DeliveryUnconfirmed(requestId)
            responseStatus in 200..299 -> AiGatewayError.InvalidResponse(requestId, responseStatus)
            else -> fromHttpResponse(
                statusCode = responseStatus,
                gatewayCode = null,
                requestId = requestId,
                retryAfterHeader = snapshot.retryAfterHeader
            )
        }
    }
}

internal fun Response<GatewayChatResponse>.toGatewayResult(
    clientRequestId: String
): AiGatewayResult {
    return try {
        val responseRequestId = headers()[REQUEST_ID_HEADER].normalizedMetadata()
        if (!isSuccessful) {
            val gatewayError = errorBody().readGatewayError()
            val requestId = gatewayError?.error?.requestId.normalizedMetadata()
                ?: responseRequestId
                ?: clientRequestId
            return AiGatewayResult.Failure(
                AiGatewayFailureMapper.fromHttpResponse(
                    statusCode = code(),
                    gatewayCode = gatewayError?.error?.code.normalizedMetadata(),
                    requestId = requestId,
                    retryAfterHeader = headers()[RETRY_AFTER_HEADER]
                )
            )
        }

        val responseBody = body()
        val reply = responseBody?.reply?.trim().orEmpty()
        val requestId = responseBody?.requestId.normalizedMetadata()
            ?: responseRequestId
            ?: clientRequestId
        if (reply.isEmpty()) {
            AiGatewayResult.Failure(
                AiGatewayError.InvalidResponse(
                    requestId = requestId,
                    statusCode = code()
                )
            )
        } else {
            AiGatewayResult.Success(reply = reply, requestId = requestId)
        }
    } finally {
        errorBody()?.close()
    }
}

private fun ResponseBody?.readGatewayError(): GatewayErrorEnvelope? {
    if (this == null) return null
    return runCatching {
        val source = source()
        source.request(MAX_ERROR_BODY_BYTES)
        val bytesToRead = minOf(source.buffer.size, MAX_ERROR_BODY_BYTES)
        val json = source.buffer.clone().readUtf8(bytesToRead)
        GATEWAY_ERROR_ADAPTER.fromJson(json)
    }.getOrNull()
}

private fun String?.normalizedMetadata(): String? = this?.trim()?.takeIf(String::isNotEmpty)

private const val REQUEST_ID_HEADER = "X-Request-ID"
private const val RETRY_AFTER_HEADER = "Retry-After"
private const val MAX_ERROR_BODY_BYTES = 16_384L
private val GATEWAY_ERROR_ADAPTER = com.squareup.moshi.Moshi.Builder()
    .build()
    .adapter(GatewayErrorEnvelope::class.java)

object OfflineGuidedPractice {
    const val notSentNotice = "Live coaching is not connected. Your message was not sent; the on-device guide is ready."
    const val deliveryUnconfirmedNotice = "Live coaching did not complete. We could not confirm delivery; the on-device guide is ready."
    const val confirmedFailureNotice = "Live coaching did not complete. Continue with this on-device guide."

    fun responseWhenNotSent(mode: AiConversationMode, scenarioId: String?): String = guide(
        mode = mode,
        scenarioId = scenarioId,
        status = "Live coaching is not connected on this device. Work through this on-device guide instead."
    )

    fun responseAfterUnconfirmedDelivery(mode: AiConversationMode, scenarioId: String?): String = guide(
        mode = mode,
        scenarioId = scenarioId,
        status = "Live coaching did not complete. Continue with this on-device guide."
    )

    fun responseAfterConfirmedFailure(mode: AiConversationMode, scenarioId: String?): String = guide(
        mode = mode,
        scenarioId = scenarioId,
        status = confirmedFailureNotice
    )

    private fun guide(
        mode: AiConversationMode,
        scenarioId: String?,
        status: String
    ): String = when (mode) {
        AiConversationMode.NativeCoach -> """
            Offline guided practice

            $status Choose one sentence to refine, then check its purpose, verb, register, and one concrete detail.
        """.trimIndent()

        AiConversationMode.StandardizedPatient -> """
            Offline guided practice

            $status This guide does not assess a patient or replace clinical supervision. For ${scenarioId ?: "this scenario"}, try one open question, one clarification, and one concise summary.
        """.trimIndent()
    }
}
