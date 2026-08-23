package com.cinnamon.app.domain.repository

import android.content.Context
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.local.MeshReferenceEntry
import kotlinx.coroutines.flow.Flow
import java.util.Locale

class MeshReferenceRepository private constructor(context: Context) {
    private val dao = AppDatabase.getDatabase(context.applicationContext).meshReferenceDao()

    fun search(
        query: String,
        categoryRoot: String = "all",
        limit: Int = 100
    ): Flow<List<MeshReferenceEntry>> =
        dao.search(
            normalizedQuery = query.trim().lowercase(Locale.ROOT),
            categoryRoot = categoryRoot.takeIf { it in CATEGORY_ROOTS } ?: "all",
            limit = limit.coerceIn(1, 250)
        )

    fun byMeshUi(meshUi: String): Flow<MeshReferenceEntry?> =
        dao.byMeshUi(meshUi.trim())

    fun countForCategory(categoryRoot: String): Flow<Int> =
        dao.countForCategory(categoryRoot)

    companion object {
        val CATEGORY_ROOTS: Set<String> = setOf("A", "C", "E", "F", "G", "N")

        @Volatile
        private var INSTANCE: MeshReferenceRepository? = null

        fun getInstance(context: Context): MeshReferenceRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: MeshReferenceRepository(context).also { INSTANCE = it }
            }
    }
}
