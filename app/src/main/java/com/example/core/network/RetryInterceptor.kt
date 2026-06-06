package com.example.core.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null
        var tryCount = 0

        while (tryCount < maxRetries && (response == null || !response.isSuccessful)) {
            try {
                if (tryCount > 0) {
                    Log.d("RetryInterceptor", "Retrying request... Attempt ${tryCount + 1}")
                    Thread.sleep(1000L * tryCount) // Exponential backoff scaling
                }
                response?.close() // close previous failed response
                response = chain.proceed(request)
            } catch (e: Exception) {
                exception = e as? IOException ?: IOException(e)
            }
            tryCount++
        }

        if (response == null) {
            throw exception ?: IOException("Unknown network error")
        }
        return response
    }
}
