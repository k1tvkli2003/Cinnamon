package com.cinnamon.app.data.seed

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.security.MessageDigest
import java.util.Locale

class MeshReferenceDatasetContractTest {

    private val moshi = Moshi.Builder().build()
    private val manifestAdapter =
        moshi.adapter(MeshReferenceManifest::class.java).failOnUnknown()
    private val recordAdapter =
        moshi.adapter(MeshReferenceDto::class.java).failOnUnknown()

    @Test
    fun `reference atlas is attributable deterministic and structurally complete`() {
        val manifestFile = assetFile("mesh/manifest.json")
        val manifest = requireNotNull(manifestAdapter.fromJson(manifestFile.readText()))
        val payloadFile = assetFile("mesh/${manifest.payload.file}")
        val payloadBytes = payloadFile.readBytes()

        assertEquals(1, manifest.schemaVersion)
        assertEquals("cinnamon.mesh.reference", manifest.datasetId)
        assertEquals("2026.1", manifest.datasetVersion)
        assertEquals(5742, manifest.payload.count)
        assertEquals("jsonl", manifest.payload.format)
        assertEquals(payloadBytes.size.toLong(), manifest.payload.bytes)
        assertEquals(manifest.payload.sha256, payloadBytes.sha256())
        assertEquals(2026, manifest.source.productionYear)
        assertEquals(
            "9fe35b3170652376a592daf69e91a80d6c693ecaf9c571ceb701d04204cb357d",
            manifest.source.sourceArchiveSha256
        )
        assertEquals(
            "https://nlmpubs.nlm.nih.gov/projects/mesh/MESH_FILES/xmlmesh/desc2026.gz",
            manifest.source.downloadUrl
        )
        assertEquals(
            "Courtesy of the U.S. National Library of Medicine",
            manifest.source.attribution
        )
        assertFalse(manifest.source.endorsement)
        assertTrue(manifest.source.stalenessDisclosure.contains("MeSH 2026"))
        assertEquals(6500, manifest.qualityBudgets.maxDescriptors)
        assertEquals(4_000_000L, manifest.qualityBudgets.maxPayloadBytes)
        assertTrue(manifest.payload.count <= manifest.qualityBudgets.maxDescriptors)
        assertTrue(manifest.payload.bytes <= manifest.qualityBudgets.maxPayloadBytes)

        val records = payloadBytes.decodeToString()
            .lineSequence()
            .filter(String::isNotBlank)
            .map { requireNotNull(recordAdapter.fromJson(it)) }
            .toList()
        assertEquals(manifest.payload.count, records.size)
        assertEquals(records.size, records.map { it.meshUi }.distinct().size)
        assertEquals(
            records.size,
            records.map { it.term.trim().lowercase(Locale.ROOT) }.distinct().size
        )

        val allowedRoots = setOf("A", "C", "E", "F", "G", "N")
        records.forEach { record ->
            assertTrue(record.meshUi.matches(Regex("""D\d{6,9}""")))
            assertTrue(record.term.isNotBlank())
            assertTrue(record.term.length <= manifest.qualityBudgets.maxTermChars)
            assertTrue(record.scopeNote.isNotBlank())
            assertTrue(
                record.scopeNote.length <= manifest.qualityBudgets.maxScopeNoteChars
            )
            assertTrue(
                record.synonyms.size <=
                    manifest.qualityBudgets.maxSynonymsPerDescriptor
            )
            assertTrue(
                record.treeNumbers.size <=
                    manifest.qualityBudgets.maxTreeNumbersPerDescriptor
            )
            assertFalse(record.synonyms.any { "|" in it })
            assertTrue(record.categoryRoots.isNotEmpty())
            assertTrue(record.categoryRoots.all(allowedRoots::contains))
            assertTrue(record.treeNumbers.isNotEmpty())
            assertTrue(
                record.treeNumbers.any {
                    it.first().toString() in allowedRoots &&
                        it.count { character -> character == '.' } + 1 <= 3
                }
            )
            assertFalse(
                record.synonyms.any {
                    it.trim().equals(record.term.trim(), ignoreCase = true)
                }
            )
        }

        val memberships = allowedRoots.associateWith { root ->
            records.count { root in it.categoryRoots }
        }
        assertEquals(
            mapOf(
                "A" to 697,
                "C" to 1853,
                "E" to 1534,
                "F" to 493,
                "G" to 1167,
                "N" to 632
            ),
            memberships
        )
        assertEquals(
            manifest.selectionPolicy.categoryMembershipCounts,
            memberships
        )
    }

    @Test
    fun `reference atlas expansion does not replace the authored core deck`() {
        val coreCount = listOf(
            "lexicon/clinical_1.json",
            "lexicon/clinical_2.json",
            "lexicon/general.json"
        ).sumOf { path ->
            val document = assetFile(path).readText()
            Regex("""\"term\"\s*:""").findAll(document).count()
        }

        assertEquals(243, coreCount)
        assertEquals(5742, referenceManifest().payload.count)
    }

    private fun referenceManifest(): MeshReferenceManifest =
        requireNotNull(
            manifestAdapter.fromJson(assetFile("mesh/manifest.json").readText())
        )

    private fun assetFile(path: String): File {
        val candidates = listOf(
            File("src/main/assets/$path"),
            File("app/src/main/assets/$path")
        )
        return candidates.firstOrNull(File::isFile)
            ?: error("Unable to locate test asset $path from ${File(".").absolutePath}")
    }

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte -> "%02x".format(byte) }
}
