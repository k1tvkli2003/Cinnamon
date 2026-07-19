package com.cinnamon.app.data.gamification

import android.content.Context
import com.cinnamon.app.domain.gamification.GamificationCatalogBundle
import com.cinnamon.app.domain.gamification.GamificationCatalogLoader

/**
 * Asset-backed catalog boundary. UI reads names and descriptions only after the
 * versioned catalog has passed its structural validator; it never treats copy
 * as a reward authority.
 */
class GamificationCatalogRepository private constructor(context: Context) {
    private val appContext = context.applicationContext

    val bundle: GamificationCatalogBundle by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        appContext.assets.open(CATALOG_ASSET).use { catalogInput ->
            appContext.assets.open(COPY_ASSET).use { copyInput ->
                GamificationCatalogLoader().loadValidated(catalogInput, copyInput)
            }
        }
    }

    fun titleFor(key: String): String = bundle.copy.strings[key] ?: key

    fun descriptionFor(key: String): String = bundle.copy.strings[key] ?: key

    companion object {
        private const val CATALOG_ASSET = "gamification/catalog-v1.json"
        private const val COPY_ASSET = "gamification/copy-en-v1.json"

        @Volatile
        private var INSTANCE: GamificationCatalogRepository? = null

        fun getInstance(context: Context): GamificationCatalogRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: GamificationCatalogRepository(context).also { INSTANCE = it }
            }
    }
}
