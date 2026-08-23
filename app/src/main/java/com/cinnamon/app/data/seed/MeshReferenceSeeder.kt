package com.cinnamon.app.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.local.MeshReferenceEntry
import com.cinnamon.app.data.prefs.ProgressStore
import com.cinnamon.app.data.startup.StartupPerformanceTrace
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.Locale

/**
 * Installs the versioned NLM MeSH reference atlas without touching the authored
 * lexicon deck or any learner-owned SRS/bookmark state.
 */
object MeshReferenceSeeder {
    private const val TAG = "MeshReferenceSeeder"
    const val SEED_VERSION = 1
    private const val DATASET_ID = "cinnamon.mesh.reference"
    private const val DATASET_VERSION = "2026.1"
    private const val MANIFEST_PATH = "mesh/manifest.json"
    private const val EXPECTED_DESCRIPTOR_COUNT = 5742
    private const val MAX_DESCRIPTORS = 6500
    private const val MAX_PAYLOAD_BYTES = 4_000_000L
    private const val MAX_SCOPE_NOTE_CHARS = 1200
    private const val MAX_SYNONYMS_PER_DESCRIPTOR = 64
    private const val MAX_TERM_CHARS = 96
    private const val MAX_TREE_NUMBERS_PER_DESCRIPTOR = 16
    private const val REQUIRED_ATTRIBUTION =
        "Courtesy of the U.S. National Library of Medicine"
    private const val SOURCE_ARCHIVE_SHA256 =
        "9fe35b3170652376a592daf69e91a80d6c693ecaf9c571ceb701d04204cb357d"
    private const val SOURCE_URL =
        "https://nlmpubs.nlm.nih.gov/projects/mesh/MESH_FILES/xmlmesh/desc2026.gz"
    private const val TERMS_URL =
        "https://www.nlm.nih.gov/databases/download/terms_and_conditions.html"
    private val allowedCategoryRoots = setOf("A", "C", "E", "F", "G", "N")
    private val expectedCategoryMembershipCounts = mapOf(
        "A" to 697,
        "C" to 1853,
        "E" to 1534,
        "F" to 493,
        "G" to 1167,
        "N" to 632
    )
    private val meshUiPattern = Regex("""D\d{6,9}""")
    private val treeNumberPattern = Regex("""[ACEFGN]\d{2}(?:\.\d{3})*""")

    private val moshi = Moshi.Builder().build()
    private val manifestAdapter = moshi.adapter(MeshReferenceManifest::class.java).failOnUnknown()
    private val recordAdapter = moshi.adapter(MeshReferenceDto::class.java).failOnUnknown()
    private val seedMutex = Mutex()
    private val _state = MutableStateFlow<MeshReferenceSeedState>(
        MeshReferenceSeedState.NotStarted
    )
    val state: StateFlow<MeshReferenceSeedState> = _state.asStateFlow()

    suspend fun seedIfNeeded(context: Context): MeshReferenceSeedResult =
        seedMutex.withLock {
            withContext(Dispatchers.IO) {
            _state.value = MeshReferenceSeedState.Validating
            try {
                val store = ProgressStore.getInstance(context)
                val database = AppDatabase.getDatabase(context)
                if (
                    store.meshReferenceSeededVersionOnce() >= SEED_VERSION &&
                    database.meshReferenceDao().countOnce() == EXPECTED_DESCRIPTOR_COUNT &&
                    database.meshReferenceDao().countForSnapshotOnce(
                        datasetVersion = DATASET_VERSION,
                        sourceYear = 2026
                    ) == EXPECTED_DESCRIPTOR_COUNT
                ) {
                    _state.value = MeshReferenceSeedState.Ready
                    return@withContext MeshReferenceSeedResult.AlreadyCurrent
                }

                val bundle = StartupPerformanceTrace.measureBlocking(
                    "mesh_reference_load_and_validate"
                ) {
                    loadAndValidate(context)
                }
                StartupPerformanceTrace.measure("mesh_reference_room_transaction") {
                    database.withTransaction {
                        // The Atlas is a replaceable, read-only snapshot. Replacing it
                        // atomically prevents removed or renamed MeSH descriptors from
                        // lingering across future annual dataset upgrades.
                        database.meshReferenceDao().deleteSnapshot()
                        database.meshReferenceDao().insertAll(bundle.entries)
                        val installedCount = database.meshReferenceDao().countOnce()
                        require(installedCount == bundle.entries.size) {
                            "MeSH reference count differs after snapshot replacement"
                        }
                    }
                }
                StartupPerformanceTrace.measure("mesh_reference_record_seed_version") {
                    store.setMeshReferenceSeededVersion(SEED_VERSION)
                }
                _state.value = MeshReferenceSeedState.Ready
                Log.i(
                    TAG,
                    "Validated ${bundle.datasetVersion}; " +
                        "${bundle.entries.size} MeSH descriptors ready"
                )
                MeshReferenceSeedResult.Installed(
                    datasetVersion = bundle.datasetVersion,
                    descriptorCount = bundle.entries.size
                )
            } catch (cancellation: CancellationException) {
                _state.value = MeshReferenceSeedState.NotStarted
                throw cancellation
            } catch (error: Exception) {
                _state.value = MeshReferenceSeedState.Failed
                throw error
            }
        }
        }

    private fun loadAndValidate(context: Context): MeshReferenceSeedBundle {
        val assets = context.assets
        val manifest = assets.open(MANIFEST_PATH).bufferedReader(Charsets.UTF_8).use {
            requireNotNull(manifestAdapter.fromJson(it.readText())) {
                "MeSH reference manifest is empty"
            }
        }
        require(manifest.schemaVersion == 1) {
            "Unsupported MeSH manifest schema ${manifest.schemaVersion}"
        }
        require(manifest.datasetId == DATASET_ID) {
            "Unexpected MeSH dataset ID ${manifest.datasetId}"
        }
        require(manifest.datasetVersion == DATASET_VERSION) {
            "Unexpected MeSH dataset version ${manifest.datasetVersion}"
        }
        require(manifest.generator.version == 1) {
            "Unsupported MeSH generator version ${manifest.generator.version}"
        }
        require(manifest.payload.format == "jsonl") {
            "MeSH payload format must be jsonl"
        }
        require(manifest.payload.file == "mesh-reference-2026.jsonl") {
            "Unexpected MeSH payload file ${manifest.payload.file}"
        }
        require(manifest.payload.count > 0 && manifest.payload.bytes > 0) {
            "MeSH payload manifest is empty"
        }
        require(manifest.payload.count == EXPECTED_DESCRIPTOR_COUNT) {
            "MeSH descriptor count differs from the approved snapshot"
        }
        require(
            manifest.qualityBudgets == MeshQualityBudgets(
                maxDescriptors = MAX_DESCRIPTORS,
                maxPayloadBytes = MAX_PAYLOAD_BYTES,
                maxScopeNoteChars = MAX_SCOPE_NOTE_CHARS,
                maxSynonymsPerDescriptor = MAX_SYNONYMS_PER_DESCRIPTOR,
                maxTermChars = MAX_TERM_CHARS,
                maxTreeNumbersPerDescriptor = MAX_TREE_NUMBERS_PER_DESCRIPTOR
            )
        ) {
            "MeSH quality budgets differ from the accepted runtime contract"
        }
        require(manifest.payload.count <= MAX_DESCRIPTORS) {
            "MeSH descriptor count exceeds the runtime budget"
        }
        require(manifest.payload.bytes <= MAX_PAYLOAD_BYTES) {
            "MeSH payload exceeds the runtime byte budget"
        }
        require(manifest.source.productionYear == 2026) {
            "MeSH source year must be 2026"
        }
        require(manifest.source.attribution == REQUIRED_ATTRIBUTION) {
            "Required NLM attribution is missing"
        }
        require(manifest.source.sourceArchiveSha256 == SOURCE_ARCHIVE_SHA256) {
            "MeSH source archive differs from the approved NLM snapshot"
        }
        require(manifest.source.downloadUrl == SOURCE_URL) {
            "MeSH source URL differs from the approved NLM endpoint"
        }
        require(manifest.source.termsUrl == TERMS_URL) {
            "MeSH terms URL differs from the approved NLM endpoint"
        }
        require(!manifest.source.endorsement) {
            "NLM endorsement must remain false"
        }
        require(manifest.selectionPolicy.maximumTreeDepth == 3) {
            "Unsupported MeSH selection depth"
        }
        require(
            manifest.selectionPolicy.categoryRoots.map { it.code }.toSet() ==
                allowedCategoryRoots
        ) {
            "MeSH category roots differ from the accepted reference policy"
        }
        require(
            manifest.selectionPolicy.categoryMembershipCounts ==
                expectedCategoryMembershipCounts
        ) {
            "MeSH category coverage differs from the approved snapshot"
        }

        val payloadBytes = assets.open("mesh/${manifest.payload.file}").use { it.readBytes() }
        require(payloadBytes.size.toLong() == manifest.payload.bytes) {
            "MeSH reference payload byte count differs from the manifest"
        }
        require(payloadBytes.sha256() == manifest.payload.sha256.lowercase()) {
            "MeSH reference payload failed SHA-256 validation"
        }

        val seenUi = mutableSetOf<String>()
        val seenTerms = mutableSetOf<String>()
        val categoryMembershipCounts = allowedCategoryRoots.associateWith { 0 }.toMutableMap()
        val entries = payloadBytes.decodeToString()
            .split('\n')
            .filter(String::isNotBlank)
            .mapIndexed { index, line ->
                val dto = requireNotNull(recordAdapter.fromJson(line)) {
                    "MeSH payload line ${index + 1} is empty"
                }
                dto.validate(index + 1, seenUi, seenTerms)
                dto.categoryRoots.forEach { root ->
                    categoryMembershipCounts[root] =
                        requireNotNull(categoryMembershipCounts[root]) + 1
                }
                dto.toEntity()
            }
        require(entries.size == manifest.payload.count) {
            "MeSH descriptor count differs from the manifest"
        }
        require(categoryMembershipCounts == manifest.selectionPolicy.categoryMembershipCounts) {
            "MeSH category membership counts differ from the manifest"
        }

        return MeshReferenceSeedBundle(
            datasetVersion = manifest.datasetVersion,
            entries = entries
        )
    }

    private fun MeshReferenceDto.validate(
        lineNumber: Int,
        seenUi: MutableSet<String>,
        seenTerms: MutableSet<String>
    ) {
        require(meshUiPattern.matches(meshUi)) {
            "Invalid MeSH UI at line $lineNumber"
        }
        require(seenUi.add(meshUi)) {
            "Duplicate MeSH UI at line $lineNumber"
        }
        require(term.isNotBlank() && term == term.normalizedWhitespace()) {
            "Invalid MeSH term at line $lineNumber"
        }
        require(term.length <= MAX_TERM_CHARS) {
            "MeSH term exceeds the quality budget at line $lineNumber"
        }
        require(seenTerms.add(term.normalizedIdentity())) {
            "Duplicate MeSH preferred term at line $lineNumber"
        }
        require(scopeNote.isNotBlank() && scopeNote == scopeNote.normalizedWhitespace()) {
            "Invalid MeSH scope note at line $lineNumber"
        }
        require(scopeNote.length <= MAX_SCOPE_NOTE_CHARS) {
            "MeSH scope note exceeds the quality budget at line $lineNumber"
        }
        require(categoryRoots.isNotEmpty() && categoryRoots.all(allowedCategoryRoots::contains)) {
            "Invalid MeSH category roots at line $lineNumber"
        }
        require(categoryRoots.distinct() == categoryRoots) {
            "Duplicate MeSH category root at line $lineNumber"
        }
        require(treeNumbers.isNotEmpty() && treeNumbers.all(treeNumberPattern::matches)) {
            "Invalid MeSH tree number at line $lineNumber"
        }
        require(treeNumbers.size <= MAX_TREE_NUMBERS_PER_DESCRIPTOR) {
            "Too many MeSH tree numbers at line $lineNumber"
        }
        require(
            treeNumbers.any {
                it.substringBefore('.').isNotBlank() &&
                    it.count { character -> character == '.' } + 1 <= 3
            }
        ) {
            "MeSH descriptor does not satisfy the selection depth at line $lineNumber"
        }
        require(synonyms.map { it.normalizedIdentity() }.distinct().size == synonyms.size) {
            "Duplicate MeSH synonym at line $lineNumber"
        }
        require(synonyms.size <= MAX_SYNONYMS_PER_DESCRIPTOR) {
            "Too many MeSH synonyms at line $lineNumber"
        }
        require(synonyms.none { "|" in it }) {
            "MeSH synonym contains the storage delimiter at line $lineNumber"
        }
        require(synonyms.none { it.normalizedIdentity() == term.normalizedIdentity() }) {
            "Preferred MeSH term repeated as a synonym at line $lineNumber"
        }
    }

    private fun MeshReferenceDto.toEntity(): MeshReferenceEntry =
        MeshReferenceEntry(
            meshUi = meshUi,
            term = term,
            normalizedTerm = term.normalizedIdentity(),
            scopeNote = scopeNote,
            synonymsRaw = synonyms.joinToString("|"),
            treeNumbersRaw = treeNumbers.joinToString("|"),
            categoryRootsRaw = categoryRoots.joinToString("|"),
            introducedYear = introducedYear,
            lastUpdated = lastUpdated,
            sourceYear = 2026,
            datasetVersion = DATASET_VERSION
        )

    private fun String.normalizedWhitespace(): String =
        trim().split(Regex("""\s+""")).joinToString(" ")

    private fun String.normalizedIdentity(): String =
        normalizedWhitespace().lowercase(Locale.ROOT)

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte -> "%02x".format(byte) }
}

sealed interface MeshReferenceSeedResult {
    data object AlreadyCurrent : MeshReferenceSeedResult
    data class Installed(
        val datasetVersion: String,
        val descriptorCount: Int
    ) : MeshReferenceSeedResult
}

sealed interface MeshReferenceSeedState {
    data object NotStarted : MeshReferenceSeedState
    data object Validating : MeshReferenceSeedState
    data object Ready : MeshReferenceSeedState
    data object Failed : MeshReferenceSeedState
}

private data class MeshReferenceSeedBundle(
    val datasetVersion: String,
    val entries: List<MeshReferenceEntry>
)
