package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger

class MedicalApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Initialize Baseline Profiles manually if needed, 
        // global crash handlers, and R8 optimization checks would go here.
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            // Global Error Handling Pipeline
            android.util.Log.e("MedicalApp", "FATAL CRASH on thread ${thread.name}", exception)
            // Send to Crashlytics / Custom Analytics
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
            // Robust Caching Strategy
            .respectCacheHeaders(false)
            .logger(DebugLogger())
            .build()
    }
}
