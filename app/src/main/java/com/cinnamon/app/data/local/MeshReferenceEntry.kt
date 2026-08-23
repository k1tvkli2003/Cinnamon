package com.cinnamon.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One stable NLM MeSH descriptor in Cinnamon's read-only reference atlas.
 *
 * The MeSH UI is the canonical identity. This table deliberately contains no
 * learner-owned state; the authored [LexiconEntry] deck remains the authority
 * for review scheduling, bookmarks and learning evidence.
 */
@Entity(
    tableName = "mesh_reference",
    indices = [
        Index(value = ["normalizedTerm"], unique = true),
        Index(value = ["term"]),
        Index(value = ["sourceYear"])
    ]
)
data class MeshReferenceEntry(
    @PrimaryKey val meshUi: String,
    val term: String,
    val normalizedTerm: String,
    val scopeNote: String,
    val synonymsRaw: String,
    val treeNumbersRaw: String,
    val categoryRootsRaw: String,
    val introducedYear: Int?,
    val lastUpdated: String?,
    val sourceYear: Int,
    val datasetVersion: String
) {
    val synonyms: List<String> get() = synonymsRaw.splitPiped()
    val treeNumbers: List<String> get() = treeNumbersRaw.splitPiped()
    val categoryRoots: List<String> get() = categoryRootsRaw.splitPiped()
}

