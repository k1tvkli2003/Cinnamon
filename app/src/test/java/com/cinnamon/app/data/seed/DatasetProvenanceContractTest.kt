package com.cinnamon.app.data.seed

import com.squareup.moshi.Moshi
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DatasetProvenanceContractTest {

    @Test
    fun bundledManifest_hasAcceptedProvenanceAtTheRuntimeBoundary() {
        val manifest = requireNotNull(
            Moshi.Builder()
                .build()
                .adapter(DatasetManifest::class.java)
                .fromJson(assetText("lexicon/manifest.json"))
        )

        assertEquals("project-bundled-curated", manifest.provenance.origin)
        assertEquals("editorial-review-required-before-release", manifest.provenance.reviewStatus)
        manifest.requireAcceptedProvenance()
    }

    @Test
    fun provenance_rejectsUnrecognizedOrigin() {
        val invalid = manifestWith(
            DatasetProvenance(
                origin = "unverified-external-source",
                reviewStatus = "editorial-approved"
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            invalid.requireAcceptedProvenance()
        }
    }

    @Test
    fun provenance_rejectsUnrecognizedReviewStatus() {
        val invalid = manifestWith(
            DatasetProvenance(
                origin = "project-bundled-curated",
                reviewStatus = "unchecked"
            )
        )

        assertThrows(IllegalArgumentException::class.java) {
            invalid.requireAcceptedProvenance()
        }
    }

    private fun manifestWith(provenance: DatasetProvenance) = DatasetManifest(
        schemaVersion = 1,
        datasetId = "cinnamon.lexicon.core",
        datasetVersion = "test",
        provenance = provenance,
        files = emptyList()
    )

    private fun assetText(path: String): String {
        val candidates = listOf(
            File("src/main/assets/$path"),
            File("app/src/main/assets/$path")
        )
        val file = candidates.firstOrNull(File::isFile)
            ?: error("Unable to locate test asset $path from ${File(".").absolutePath}")
        return file.readText(Charsets.UTF_8)
    }
}
