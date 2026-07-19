package com.cinnamon.app.data.seed

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DatasetManifest(
    val schemaVersion: Int,
    val datasetId: String,
    val datasetVersion: String,
    val provenance: DatasetProvenance,
    val files: List<DatasetFileSpec>
)

@JsonClass(generateAdapter = true)
data class DatasetProvenance(
    val origin: String,
    val reviewStatus: String
)

@JsonClass(generateAdapter = true)
data class DatasetFileSpec(
    val name: String,
    val collection: String,
    val count: Int,
    val sha256: String
)

internal fun DatasetManifest.requireAcceptedProvenance() {
    require(provenance.origin == "project-bundled-curated") {
        "Unexpected lexicon provenance origin ${provenance.origin}"
    }
    require(
        provenance.reviewStatus in setOf(
            "editorial-review-required-before-release",
            "editorial-approved"
        )
    ) {
        "Unsupported lexicon review status ${provenance.reviewStatus}"
    }
}

@JsonClass(generateAdapter = true)
data class EntryFile(val entries: List<EntryDto>)

@JsonClass(generateAdapter = true)
data class EntryDto(
    val term: String,
    val ipa: String = "",
    val pos: String = "",
    val domain: String = "general",
    val topic: String = "",
    val level: String = "C1",
    val definition: String = "",
    val plain: String = "",
    val etymology: String = "",
    val morphemes: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val collocations: List<String> = emptyList(),
    val exampleClinical: String = "",
    val exampleCasual: String = "",
    val usageNote: String = "",
    val abbreviation: String = ""
)

@JsonClass(generateAdapter = true)
data class MorphemeFile(val morphemes: List<MorphemeDto>)

@JsonClass(generateAdapter = true)
data class MorphemeDto(
    val form: String,
    val kind: String = "root",
    val meaning: String = "",
    val origin: String = "",
    val examples: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AbbreviationFile(val abbreviations: List<AbbreviationDto>)

@JsonClass(generateAdapter = true)
data class AbbreviationDto(
    val short: String,
    val expansion: String = "",
    val context: String = ""
)

@JsonClass(generateAdapter = true)
data class PhraseFile(val phrases: List<PhraseDto>)

@JsonClass(generateAdapter = true)
data class PhraseDto(
    val phrase: String,
    val category: String = "Natural Conversation",
    val meaning: String = "",
    val example: String = "",
    val register: String = "neutral"
)

@JsonClass(generateAdapter = true)
data class ConfusableFile(val confusables: List<ConfusableDto>)

@JsonClass(generateAdapter = true)
data class ConfusableDto(
    val a: String,
    val b: String,
    val howToTell: String = "",
    val exampleA: String = "",
    val exampleB: String = ""
)

@JsonClass(generateAdapter = true)
data class SentenceFile(val sentences: List<SentenceDto>)

@JsonClass(generateAdapter = true)
data class SentenceDto(
    val text: String,
    val domain: String = "general",
    val level: String = "C1"
)
