package com.cinnamon.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeshReferenceDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(entries: List<MeshReferenceEntry>): List<Long>

    /**
     * Removes only the replaceable read-only atlas snapshot. This table never
     * owns SRS, bookmarks or any other learner state.
     */
    @Query("DELETE FROM mesh_reference")
    suspend fun deleteSnapshot()

    @Query("SELECT COUNT(*) FROM mesh_reference")
    suspend fun countOnce(): Int

    @Query(
        """
        SELECT COUNT(*) FROM mesh_reference
        WHERE datasetVersion = :datasetVersion AND sourceYear = :sourceYear
        """
    )
    suspend fun countForSnapshotOnce(datasetVersion: String, sourceYear: Int): Int

    @Query("SELECT * FROM mesh_reference WHERE meshUi = :meshUi")
    fun byMeshUi(meshUi: String): Flow<MeshReferenceEntry?>

    @Query(
        """
        SELECT * FROM mesh_reference
        WHERE (
            :categoryRoot = 'all'
            OR instr('|' || categoryRootsRaw || '|', '|' || :categoryRoot || '|') > 0
        )
        AND (
            :normalizedQuery = ''
            OR normalizedTerm LIKE '%' || :normalizedQuery || '%'
            OR LOWER(synonymsRaw) LIKE '%' || :normalizedQuery || '%'
            OR LOWER(scopeNote) LIKE '%' || :normalizedQuery || '%'
        )
        ORDER BY
            CASE
                WHEN normalizedTerm = :normalizedQuery THEN 0
                WHEN normalizedTerm LIKE :normalizedQuery || '%' THEN 1
                ELSE 2
            END,
            term COLLATE NOCASE ASC
        LIMIT :limit
        """
    )
    fun search(
        normalizedQuery: String,
        categoryRoot: String,
        limit: Int
    ): Flow<List<MeshReferenceEntry>>

    @Query(
        """
        SELECT COUNT(*) FROM mesh_reference
        WHERE instr('|' || categoryRootsRaw || '|', '|' || :categoryRoot || '|') > 0
        """
    )
    fun countForCategory(categoryRoot: String): Flow<Int>
}
