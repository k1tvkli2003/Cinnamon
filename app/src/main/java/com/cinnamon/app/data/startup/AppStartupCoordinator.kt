package com.cinnamon.app.data.startup

import android.content.Context
import android.util.Log
import com.cinnamon.app.data.gamification.GamificationCatalogRepository
import com.cinnamon.app.data.gamification.GamificationRepository
import com.cinnamon.app.data.prefs.ProgressStore
import com.cinnamon.app.data.seed.LexiconSeeder
import com.cinnamon.app.data.seed.MeshReferenceSeeder
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One visible, retryable startup contract for every prerequisite needed before
 * the learner can use content. A failed catalog must never look like an
 * endlessly loading lexicon.
 */
object AppStartupCoordinator {
    private const val TAG = "AppStartup"
    private const val REFERENCE_ATLAS_WARMUP_DELAY_MILLIS = 1_500L

    private val mutex = Mutex()
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val atlasWarmupRequested = AtomicBoolean(false)
    private val _state = MutableStateFlow<AppStartupState>(AppStartupState.NotStarted)
    val state: StateFlow<AppStartupState> = _state.asStateFlow()
    private val _catalogCopy = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Validated presentation labels published by the single startup owner. */
    val catalogCopy: StateFlow<Map<String, String>> = _catalogCopy.asStateFlow()
    private val _catalogBundle = MutableStateFlow<GamificationCatalogBundle?>(null)
    /** The same validated catalog bundle used by startup, exposed for read-only UI projections. */
    val catalogBundle: StateFlow<GamificationCatalogBundle?> = _catalogBundle.asStateFlow()

    suspend fun prepare(context: Context): AppStartupState =
        StartupPerformanceTrace.measure("prepare") {
            withContext(Dispatchers.IO) {
                mutex.withLock {
                    _state.value = AppStartupState.Preparing
                    _catalogCopy.value = emptyMap()
                    _catalogBundle.value = null
                    var stage = AppStartupStage.GamificationCatalog
                    try {
                        val catalog = supervisorScope {
                            // Catalog parsing shares no state with the local progress and lexicon
                            // pipeline. It can overlap those CPU/asset reads, while the two Room
                            // write paths below remain intentionally serial.
                            val catalogDeferred = async(Dispatchers.Default) {
                                StartupPerformanceTrace.measureBlocking("catalog") {
                                    GamificationCatalogRepository.getInstance(context).bundle
                                }
                            }
                            try {
                                // The first Room query opens the database and runs any pending
                                // schema migration. Keep that failure distinct from importing the
                                // legacy DataStore snapshot so recovery guidance is truthful.
                                stage = AppStartupStage.DatabaseMigration
                                val gamificationRepository = GamificationRepository.getInstance(context)
                                val legacyProgressImported = StartupPerformanceTrace.measure("database_open") {
                                    gamificationRepository.isLegacyProgressImported()
                                }
                                stage = AppStartupStage.ProgressImport
                                val legacySnapshot = StartupPerformanceTrace.measure("progress_snapshot") {
                                    if (legacyProgressImported) {
                                        null
                                    } else {
                                        ProgressStore.getInstance(context).snapshot.first()
                                    }
                                }
                                StartupPerformanceTrace.measure("progress_import") {
                                    legacySnapshot?.let { snapshot ->
                                        gamificationRepository.importLegacyProgress(snapshot)
                                    }
                                }

                                stage = AppStartupStage.Lexicon
                                StartupPerformanceTrace.measure("lexicon_seed") {
                                    LexiconSeeder.seedIfNeeded(context)
                                }

                                stage = AppStartupStage.GamificationCatalog
                                catalogDeferred.await()
                            } catch (error: Throwable) {
                                catalogDeferred.cancel()
                                throw error
                            }
                        }
                        stage = AppStartupStage.CatalogReconciliation
                        StartupPerformanceTrace.measure("catalog_reconciliation") {
                            GamificationRepository.getInstance(context).reconcileCatalog(catalog)
                        }
                        stage = AppStartupStage.JourneyReconciliation
                        StartupPerformanceTrace.measure("journey_reconciliation") {
                            GamificationRepository.getInstance(context).reconcileJourney()
                        }

                        // Publish one coherent ready snapshot only after catalog-backed state has
                        // been reconciled; UI never observes thresholds without their durable rows.
                        _catalogBundle.value = catalog
                        _catalogCopy.value = catalog.copy.strings

                        AppStartupState.Ready.also {
                            _state.value = it
                            scheduleReferenceAtlasWarmup(context.applicationContext)
                        }
                    } catch (cancellation: CancellationException) {
                        _state.value = AppStartupState.NotStarted
                        _catalogCopy.value = emptyMap()
                        _catalogBundle.value = null
                        throw cancellation
                    } catch (error: Exception) {
                        // Startup errors can originate in imported local progress. Keep the
                        // diagnostic category without serializing exception data to logcat.
                        Log.e(TAG, "Startup prerequisite failed at $stage (${error.javaClass.simpleName})")
                        _catalogCopy.value = emptyMap()
                        _catalogBundle.value = null
                        AppStartupState.Failed(stage).also { _state.value = it }
                    }
                }
            }
        }

    /**
     * The broad reference atlas is useful but is not a prerequisite for Home,
     * review, Questboard or the authored lexicon. Warm it after critical
     * readiness so first-run learners are never held behind a multi-second
     * optional import.
     */
    private fun scheduleReferenceAtlasWarmup(context: Context) {
        if (!atlasWarmupRequested.compareAndSet(false, true)) return
        backgroundScope.launch {
            delay(REFERENCE_ATLAS_WARMUP_DELAY_MILLIS)
            try {
                StartupPerformanceTrace.measure("mesh_reference_background_seed") {
                    MeshReferenceSeeder.seedIfNeeded(context)
                }
            } catch (cancellation: CancellationException) {
                atlasWarmupRequested.set(false)
                throw cancellation
            } catch (error: Exception) {
                atlasWarmupRequested.set(false)
                Log.e(
                    TAG,
                    "Optional reference atlas warmup failed " +
                        "(${error.javaClass.simpleName})"
                )
            }
        }
    }
}

sealed interface AppStartupState {
    data object NotStarted : AppStartupState
    data object Preparing : AppStartupState
    data object Ready : AppStartupState
    data class Failed(val stage: AppStartupStage) : AppStartupState
}

enum class AppStartupStage {
    GamificationCatalog,
    DatabaseMigration,
    CatalogReconciliation,
    JourneyReconciliation,
    ProgressImport,
    Lexicon
}
