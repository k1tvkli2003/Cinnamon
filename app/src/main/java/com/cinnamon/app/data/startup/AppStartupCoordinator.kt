package com.cinnamon.app.data.startup

import android.content.Context
import android.util.Log
import com.cinnamon.app.data.gamification.GamificationCatalogRepository
import com.cinnamon.app.data.gamification.GamificationRepository
import com.cinnamon.app.data.prefs.ProgressStore
import com.cinnamon.app.data.seed.LexiconSeeder
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * One visible, retryable startup contract for every prerequisite needed before
 * the learner can use content. A failed catalog must never look like an
 * endlessly loading lexicon.
 */
object AppStartupCoordinator {
    private const val TAG = "AppStartup"

    private val mutex = Mutex()
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
                                stage = AppStartupStage.ProgressImport
                                val gamificationRepository = GamificationRepository.getInstance(context)
                                val legacySnapshot = StartupPerformanceTrace.measure("progress_snapshot") {
                                    if (gamificationRepository.isLegacyProgressImported()) {
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

                        AppStartupState.Ready.also { _state.value = it }
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
}

sealed interface AppStartupState {
    data object NotStarted : AppStartupState
    data object Preparing : AppStartupState
    data object Ready : AppStartupState
    data class Failed(val stage: AppStartupStage) : AppStartupState
}

enum class AppStartupStage {
    GamificationCatalog,
    CatalogReconciliation,
    JourneyReconciliation,
    ProgressImport,
    Lexicon
}
