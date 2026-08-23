package com.cinnamon.app.data.seed

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MeshReferenceManifest(
    val schemaVersion: Int,
    val datasetId: String,
    val datasetVersion: String,
    val generator: MeshGeneratorManifest,
    val payload: MeshPayloadManifest,
    val qualityBudgets: MeshQualityBudgets,
    val selectionPolicy: MeshSelectionPolicy,
    val source: MeshSourceManifest
)

@JsonClass(generateAdapter = true)
data class MeshGeneratorManifest(
    val name: String,
    val version: Int
)

@JsonClass(generateAdapter = true)
data class MeshPayloadManifest(
    val bytes: Long,
    val count: Int,
    val file: String,
    val format: String,
    val sha256: String
)

@JsonClass(generateAdapter = true)
data class MeshQualityBudgets(
    val maxDescriptors: Int,
    val maxPayloadBytes: Long,
    val maxScopeNoteChars: Int,
    val maxSynonymsPerDescriptor: Int,
    val maxTermChars: Int,
    val maxTreeNumbersPerDescriptor: Int
)

@JsonClass(generateAdapter = true)
data class MeshSelectionPolicy(
    val categoryMembershipCounts: Map<String, Int>,
    val categoryRoots: List<MeshCategoryManifest>,
    val maximumTreeDepth: Int,
    val rule: String
)

@JsonClass(generateAdapter = true)
data class MeshCategoryManifest(
    val code: String,
    val label: String
)

@JsonClass(generateAdapter = true)
data class MeshSourceManifest(
    val accessedDate: String,
    val attribution: String,
    val downloadUrl: String,
    val endorsement: Boolean,
    val name: String,
    val productionYear: Int,
    val sourceArchiveSha256: String,
    val stalenessDisclosure: String,
    val termsUrl: String
)

@JsonClass(generateAdapter = true)
data class MeshReferenceDto(
    val categoryRoots: List<String>,
    val introducedYear: Int?,
    val lastUpdated: String?,
    val meshUi: String,
    val scopeNote: String,
    val synonyms: List<String>,
    val term: String,
    val treeNumbers: List<String>
)
