package com.cinnamon.app.data.seed

import android.content.Context
import android.util.Log
import com.cinnamon.app.data.local.Abbreviation
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.local.Confusable
import com.cinnamon.app.data.local.LexiconEntry
import com.cinnamon.app.data.local.Morpheme
import com.cinnamon.app.data.local.PhraseEntry
import com.cinnamon.app.data.local.PracticeSentence
import com.cinnamon.app.data.prefs.ProgressStore
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Seeds the Room database from the bundled JSON lexicon on first launch
 * (or whenever SEED_VERSION is bumped after a dataset update).
 */
object LexiconSeeder {

    private const val TAG = "LexiconSeeder"
    const val SEED_VERSION = 1

    private val moshi: Moshi = Moshi.Builder().build()

    suspend fun seedIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        val store = ProgressStore.getInstance(context)
        if (store.seededVersionOnce() >= SEED_VERSION) return@withContext

        val db = AppDatabase.getDatabase(context)
        val assets = context.assets

        fun read(name: String): String? = try {
            assets.open("lexicon/$name").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "Missing dataset file lexicon/$name", e)
            null
        }

        // Dictionary entries (multiple files share one schema)
        val entryAdapter = moshi.adapter(EntryFile::class.java)
        val entryDtos = listOf("clinical_1.json", "clinical_2.json", "general.json")
            .mapNotNull { read(it) }
            .flatMap { json ->
                runCatching { entryAdapter.fromJson(json)?.entries.orEmpty() }
                    .onFailure { Log.e(TAG, "Bad entry file", it) }
                    .getOrDefault(emptyList())
            }
        if (entryDtos.isNotEmpty()) {
            db.lexiconDao().insertAll(
                entryDtos.map { dto ->
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
                }
            )
        }

        read("morphemes.json")?.let { json ->
            runCatching { moshi.adapter(MorphemeFile::class.java).fromJson(json)?.morphemes.orEmpty() }
                .onFailure { Log.e(TAG, "Bad morphemes file", it) }
                .getOrDefault(emptyList())
                .takeIf { it.isNotEmpty() }
                ?.let { dtos ->
                    db.learnDao().insertMorphemes(
                        dtos.map {
                            Morpheme(
                                form = it.form.trim(),
                                kind = it.kind,
                                meaning = it.meaning,
                                origin = it.origin,
                                examplesRaw = it.examples.joinToString("|")
                            )
                        }
                    )
                }
        }

        read("abbreviations.json")?.let { json ->
            runCatching { moshi.adapter(AbbreviationFile::class.java).fromJson(json)?.abbreviations.orEmpty() }
                .onFailure { Log.e(TAG, "Bad abbreviations file", it) }
                .getOrDefault(emptyList())
                .takeIf { it.isNotEmpty() }
                ?.let { dtos ->
                    db.learnDao().insertAbbreviations(
                        dtos.map { Abbreviation(short = it.short.trim(), expansion = it.expansion, context = it.context) }
                    )
                }
        }

        read("phrases.json")?.let { json ->
            runCatching { moshi.adapter(PhraseFile::class.java).fromJson(json)?.phrases.orEmpty() }
                .onFailure { Log.e(TAG, "Bad phrases file", it) }
                .getOrDefault(emptyList())
                .takeIf { it.isNotEmpty() }
                ?.let { dtos ->
                    db.learnDao().insertPhrases(
                        dtos.map {
                            PhraseEntry(
                                phrase = it.phrase.trim(),
                                category = it.category,
                                meaning = it.meaning,
                                example = it.example,
                                register = it.register
                            )
                        }
                    )
                }
        }

        read("confusables.json")?.let { json ->
            runCatching { moshi.adapter(ConfusableFile::class.java).fromJson(json)?.confusables.orEmpty() }
                .onFailure { Log.e(TAG, "Bad confusables file", it) }
                .getOrDefault(emptyList())
                .takeIf { it.isNotEmpty() }
                ?.let { dtos ->
                    db.learnDao().insertConfusables(
                        dtos.map {
                            Confusable(
                                a = it.a.trim(),
                                b = it.b.trim(),
                                howToTell = it.howToTell,
                                exampleA = it.exampleA,
                                exampleB = it.exampleB
                            )
                        }
                    )
                }
        }

        read("sentences.json")?.let { json ->
            runCatching { moshi.adapter(SentenceFile::class.java).fromJson(json)?.sentences.orEmpty() }
                .onFailure { Log.e(TAG, "Bad sentences file", it) }
                .getOrDefault(emptyList())
                .takeIf { it.isNotEmpty() }
                ?.let { dtos ->
                    db.learnDao().insertSentences(
                        dtos.map { PracticeSentence(text = it.text.trim(), domain = it.domain, level = it.level) }
                    )
                }
        }

        store.setSeededVersion(SEED_VERSION)
        Log.i(TAG, "Lexicon seeded: ${entryDtos.size} entries")
    }
}
