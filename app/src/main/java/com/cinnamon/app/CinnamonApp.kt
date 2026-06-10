package com.cinnamon.app

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.cinnamon.app.core.tts.TtsSpeaker
import com.cinnamon.app.data.seed.LexiconSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CinnamonApp : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("CinnamonApp", "FATAL CRASH on thread ${thread.name}", exception)
        }

        TtsSpeaker.init(this)

        // Brew the lexicon: seed Room from the bundled JSON dataset on first launch.
        applicationScope.launch {
            try {
                LexiconSeeder.seedIfNeeded(this@CinnamonApp)
            } catch (e: Exception) {
                Log.e("CinnamonApp", "Lexicon seeding failed", e)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }
}
