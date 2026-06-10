package com.cinnamon.app.di

import com.cinnamon.app.domain.repository.ChatRepository
import com.cinnamon.app.core.network.RetryInterceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import com.cinnamon.app.data.ai.AvalaiRetrofitClient
import android.util.Log

/**
 * Dependency Injection (Hilt) Architecture Mock.
 * 
 * In a full Dagger-Hilt setup (Phase 5 plan), this would be annotated with
 * @Module and @InstallIn(SingletonComponent::class)
 */
object NetworkModule {
    
    // @Provides
    // @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        Log.d("DI", "Network Resilience Pipeline initialized.")
        return OkHttpClient.Builder()
            .addInterceptor(RetryInterceptor(maxRetries = 3)) // Network Resilience Iterceptor
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // @Provides
    // @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.avalai.ir/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
    }
}
