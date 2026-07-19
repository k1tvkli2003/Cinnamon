package com.cinnamon.app.data.seed

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.cinnamon.app.data.local.Abbreviation
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.local.Confusable
import com.cinnamon.app.data.local.LexiconEntry
import com.cinnamon.app.data.local.Morpheme
import com.cinnamon.app.data.local.PhraseEntry
import com.cinnamon.app.data.local.PracticeSentence
import com.cinnamon.app.data.prefs.ProgressStore
import com.cinnamon.app.data.startup.StartupPerformanceTrace
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Installs the bundled lexicon as one validated unit. Every asset is read,
 * checksummed, parsed, and count-checked before the Room transaction begins.
 */
object LexiconSeeder {
    private const val TAG = "LexiconSeeder"
    const val SEED_VERSION = 2
    private const val DATASET_ID = "cinnamon.lexicon.core"
    private const val MANIFEST_PATH = "lexicon/manifest.json"

    private val moshi: Moshi = Moshi.Builder().build()
    private val _state = MutableStateFlow<LexiconSeedState>(LexiconSeedState.NotStarted)
    val state: StateFlow<LexiconSeedState> = _state.asStateFlow()

    suspend fun seedIfNeeded(context: Context): SeedResult = withContext(Dispatchers.IO) {
        _state.value = LexiconSeedState.Validating
        try {
            val store = ProgressStore.getInstance(context)
            val seededVersion = StartupPerformanceTrace.measure("lexicon_seed_version") {
                store.seededVersionOnce()
            }
            if (seededVersion >= SEED_VERSION) {
                _state.value = LexiconSeedState.Ready
                return@withContext SeedResult.AlreadyCurrent
            }

            val bundle = StartupPerformanceTrace.measureBlocking("lexicon_load_and_validate") {
                loadAndValidate(context)
            }
            val db = AppDatabase.getDatabase(context)
            StartupPerformanceTrace.measure("lexicon_room_transaction") {
                db.withTransaction {
                    db.lexiconDao().insertAll(bundle.entries)
                    db.learnDao().insertMorphemes(bundle.morphemes)
                    db.learnDao().insertAbbreviations(bundle.abbreviations)
                    db.learnDao().insertPhrases(bundle.phrases)
                    db.learnDao().insertConfusables(bundle.confusables)
                    db.learnDao().insertSentences(bundle.sentences)
                }
            }

            // DataStore is deliberately updated only after the Room commit. If the
            // process dies between these steps, the next run safely repeats IGNORE inserts.
            StartupPerformanceTrace.measure("lexicon_record_seed_version") {
                store.setSeededVersion(SEED_VERSION)
            }
            _state.value = LexiconSeedState.Ready
            Log.i(TAG, "Validated dataset ${bundle.datasetVersion}; ${bundle.entries.size} lexicon entries ready")
            SeedResult.Installed(
                datasetVersion = bundle.datasetVersion,
                lexiconEntries = bundle.entries.size,
                supportingItems = bundle.morphemes.size + bundle.abbreviations.size +
                    bundle.phrases.size + bundle.confusables.size + bundle.sentences.size
            )
        } catch (cancellation: CancellationException) {
            _state.value = LexiconSeedState.NotStarted
            throw cancellation
        } catch (error: Exception) {
            _state.value = LexiconSeedState.Failed
            throw error
        }
    }

    private fun loadAndValidate(context: Context): SeedBundle {
        val assets = context.assets
        val manifest = StartupPerformanceTrace.measureBlocking("lexicon_manifest") {
            val manifestBytes = assets.open(MANIFEST_PATH).use { it.readBytes() }
            requireNotNull(
                moshi.adapter(DatasetManifest::class.java).fromJson(manifestBytes.decodeToString())
            ) { "Lexicon manifest is empty" }.also {
                require(it.schemaVersion == 1) { "Unsupported lexicon manifest schema ${it.schemaVersion}" }
                require(it.datasetId == DATASET_ID) { "Unexpected lexicon dataset ID ${it.datasetId}" }
                it.requireAcceptedProvenance()
            }
        }

        val expectedFiles = setOf(
            "abbreviations.json",
            "clinical_1.json",
            "clinical_2.json",
            "confusables.json",
            "general.json",
            "morphemes.json",
            "phrases.json",
            "sentences.json"
        )
        require(manifest.files.map { it.name }.toSet() == expectedFiles) {
            "Lexicon manifest file set is incomplete or contains untracked files"
        }
        require(manifest.files.map { it.name }.distinct().size == manifest.files.size) {
            "Lexicon manifest contains duplicate file names"
        }

        fun verifiedText(name: String, collection: String): String =
            StartupPerformanceTrace.measureBlocking("lexicon_asset_${name.removeSuffix(".json")}") {
                val spec = manifest.files.singleOrNull { it.name == name }
                    ?: error("Missing manifest entry for $name")
                require(spec.collection == collection) { "$name has the wrong collection type" }
                val bytes = assets.open("lexicon/$name").use { it.readBytes() }
                require(bytes.sha256() == spec.sha256.lowercase()) { "$name failed SHA-256 validation" }
                bytes.decodeToString()
            }

        fun expectedCount(name: String): Int =
            manifest.files.single { it.name == name }.count.also { require(it > 0) }

        val entryDtos = StartupPerformanceTrace.measureBlocking("lexicon_parse_entries") {
            val entryAdapter = moshi.adapter(EntryFile::class.java)
            listOf("clinical_1.json", "clinical_2.json", "general.json").flatMap { name ->
                val entries = requireNotNull(entryAdapter.fromJson(verifiedText(name, "entries"))) {
                    "$name did not contain an entries document"
                }.entries
                require(entries.size == expectedCount(name)) { "$name count does not match manifest" }
                entries
            }
        }

        val supportingDtos = StartupPerformanceTrace.measureBlocking("lexicon_parse_supporting") {
            val morphemes = requireNotNull(
                moshi.adapter(MorphemeFile::class.java)
                    .fromJson(verifiedText("morphemes.json", "morphemes"))
            ).morphemes.also { require(it.size == expectedCount("morphemes.json")) }

            val abbreviations = requireNotNull(
                moshi.adapter(AbbreviationFile::class.java)
                    .fromJson(verifiedText("abbreviations.json", "abbreviations"))
            ).abbreviations.also { require(it.size == expectedCount("abbreviations.json")) }

            val phrases = requireNotNull(
                moshi.adapter(PhraseFile::class.java)
                    .fromJson(verifiedText("phrases.json", "phrases"))
            ).phrases.also { require(it.size == expectedCount("phrases.json")) }

            val confusables = requireNotNull(
                moshi.adapter(ConfusableFile::class.java)
                    .fromJson(verifiedText("confusables.json", "confusables"))
            ).confusables.also { require(it.size == expectedCount("confusables.json")) }

            val sentences = requireNotNull(
                moshi.adapter(SentenceFile::class.java)
                    .fromJson(verifiedText("sentences.json", "sentences"))
            ).sentences.also { require(it.size == expectedCount("sentences.json")) }

            SupportingDtos(morphemes, abbreviations, phrases, confusables, sentences)
        }

        return StartupPerformanceTrace.measureBlocking("lexicon_map_entities") {
            SeedBundle(
                datasetVersion = manifest.datasetVersion,
                entries = entryDtos.map { dto ->
                    LexiconEntry(
                        term = dto.term.trim(),
                        ipa = dto.ipa,
                        pos = dto.pos,
                        domain = dto.domain,
                        topic = dto.topic,
                        level = dto.level,
                        definition = dto.definition,
                        plain = dto.plain,
                        etymology = dto.etymology,
                        morphemesRaw = dto.morphemes.joinToString("|"),
                        synonymsRaw = dto.synonyms.joinToString("|"),
                        antonymsRaw = dto.antonyms.joinToString("|"),
                        collocationsRaw = dto.collocations.joinToString("|"),
                        exampleClinical = dto.exampleClinical,
                        exampleCasual = dto.exampleCasual,
                        usageNote = dto.usageNote,
                        abbreviation = dto.abbreviation
                    )
                },
                morphemes = supportingDtos.morphemes.map {
                    Morpheme(
                        form = it.form.trim(),
                        kind = it.kind,
                        meaning = it.meaning,
                        origin = it.origin,
                        examplesRaw = it.examples.joinToString("|")
                    )
                },
                abbreviations = supportingDtos.abbreviations.map {
                    Abbreviation(short = it.short.trim(), expansion = it.expansion, context = it.context)
                },
                phrases = supportingDtos.phrases.map {
                    PhraseEntry(
                        phrase = it.phrase.trim(),
                        category = it.category,
                        meaning = it.meaning,
                        example = it.example,
                        register = it.register
                    )
                },
                confusables = supportingDtos.confusables.map {
                    Confusable(
                        a = it.a.trim(),
                        b = it.b.trim(),
                        howToTell = it.howToTell,
                        exampleA = it.exampleA,
                        exampleB = it.exampleB
                    )
                },
                sentences = supportingDtos.sentences.map {
                    PracticeSentence(text = it.text.trim(), domain = it.domain, level = it.level)
                }
            )
        }
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(decodeToString().replace("\r\n", "\n").encodeToByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }
}

sealed interface SeedResult {
    data object AlreadyCurrent : SeedResult
    data class Installed(
        val datasetVersion: String,
        val lexiconEntries: Int,
        val supportingItems: Int
    ) : SeedResult
}

sealed interface LexiconSeedState {
    data object NotStarted : LexiconSeedState
    data object Validating : LexiconSeedState
    data object Ready : LexiconSeedState
    data object Failed : LexiconSeedState
}

private data class SeedBundle(
    val datasetVersion: String,
    val entries: List<LexiconEntry>,
    val morphemes: List<Morpheme>,
    val abbreviations: List<Abbreviation>,
    val phrases: List<PhraseEntry>,
    val confusables: List<Confusable>,
    val sentences: List<PracticeSentence>
)

private data class SupportingDtos(
    val morphemes: List<MorphemeDto>,
    val abbreviations: List<AbbreviationDto>,
    val phrases: List<PhraseDto>,
    val confusables: List<ConfusableDto>,
    val sentences: List<SentenceDto>
)
