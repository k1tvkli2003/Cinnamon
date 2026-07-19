package com.cinnamon.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.cinnamon.app.data.startup.AppStartupCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class CinnamonApp : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startupLaunchRequested = AtomicBoolean(false)

    override fun onCreate() {
        super.onCreate()
    }

    /**
     * Starts the expensive, retryable data contract from an application-owned
     * scope that outlives an Activity recreation. MainActivity triggers this
     * after its truthful loading frame has been submitted so catalog parsing
     * cannot starve time-to-first-draw. [AtomicBoolean] prevents duplicate work
     * when an Activity is recreated.
     */
    fun launchStartupPreparation() {
        if (!startupLaunchRequested.compareAndSet(false, true)) return
        applicationScope.launch {
            AppStartupCoordinator.prepare(this@CinnamonApp)
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
