package com.cinnamon.app.data.ai

import com.cinnamon.app.BuildConfig
import com.squareup.moshi.JsonClass
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import java.io.IOException
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ChatMessage(
    val role: String,
    val content: String
)

@JsonClass(generateAdapter = true)
data class ChatRequest(
    val model: String = "gpt-4o-mini", // Use AvalAI's supported models, you can test with gpt-4o or gemini
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f
)

@JsonClass(generateAdapter = true)
data class ChatResponse(
    val id: String?,
    val choices: List<Choice>?
)

@JsonClass(generateAdapter = true)
data class Choice(
    val message: ChatMessage
)

interface OpenAIApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") authHeader: String,
        @Body request: ChatRequest
    ): ChatResponse
}

class RetryAndErrorHandlingInterceptor(
    private val maxRetries: Int = 3,
    private val onErrorOccurred: (String) -> Unit
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null
        var tryCount = 0

        while (tryCount < maxRetries) {
            try {
                tryCount++
                response = chain.proceed(request)
                if (response.isSuccessful) {
                    return response
                }
                
                // Handle non-successful status (e.g., 429 rate limit or 5xx server issues)
                if (response.code == 429) {
                    onErrorOccurred("Rate limit exceeded. Automatic exponential delay activated...")
                    Thread.sleep(1000L * tryCount) // Exponential delay backoff
                } else if (response.code >= 500) {
                    onErrorOccurred("Hospital network or server transient error (${response.code}). Retrying...")
                    Thread.sleep(500L * tryCount)
                } else {
                    // Client issues (e.g., 401 or 403)
                    onErrorOccurred("API Connection Error: Code ${response.code}")
                    return response
                }
            } catch (e: IOException) {
                exception = e
                onErrorOccurred("Hospital basement connection backup. Retry attempt $tryCount/$maxRetries...")
                if (tryCount >= maxRetries) {
                    throw e
                }
                Thread.sleep(1000L)
            }
        }
        return response ?: throw exception ?: IOException("Request was unsuccessful with no direct response exception")
    }
}

object AvalaiRetrofitClient {
    private const val BASE_URL = "https://api.avalai.ir/"
    
    // Broadcast callback for networking issues
    var networkErrorCallback: ((String) -> Unit)? = null
    
    private val client by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .addInterceptor(RetryAndErrorHandlingInterceptor(maxRetries = 3) { message ->
                networkErrorCallback?.invoke(message)
            })
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val apiService: OpenAIApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(OpenAIApiService::class.java)
    }
}
