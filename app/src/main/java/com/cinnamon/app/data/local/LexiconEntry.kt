package com.cinnamon.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One dictionary headword. The lexicon itself is the spaced-repetition deck:
 * every entry carries its own SM-2 scheduling state.
 */
@Entity(
    tableName = "lexicon",
    indices = [
        Index(value = ["term"], unique = true),
        Index(value = ["domain"]),
        Index(value = ["topic"]),
        Index(value = ["level"]),
        Index(value = ["dueAt"])
    ]
)
data class LexiconEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val term: String,
    val ipa: String,
    val pos: String,                 // noun | verb | adjective | adverb | phrase
    val domain: String,              // clinical | general
    val topic: String,
    val level: String,               // B2+ | C1 | C2
    val definition: String,
    val plain: String,
    val etymology: String,
    val morphemesRaw: String,        // "part (meaning)|part (meaning)"
    val synonymsRaw: String,
    val antonymsRaw: String,
    val collocationsRaw: String,
    val exampleClinical: String,
    val exampleCasual: String,
    val usageNote: String,
    val abbreviation: String,
    // ── learner state ──
    val isBookmarked: Boolean = false,
    val reps: Int = 0,               // successful SM-2 repetitions in a row
    val easeFactor: Float = 2.5f,
    val intervalDays: Float = 0f,
    val dueAt: Long = 0L,            // 0 = brand new, never queued
    val timesSeen: Int = 0
) {
    val morphemes: List<String> get() = morphemesRaw.splitPiped()
    val synonyms: List<String> get() = synonymsRaw.splitPiped()
    val antonyms: List<String> get() = antonymsRaw.splitPiped()
    val collocations: List<String> get() = collocationsRaw.splitPiped()
}

internal fun String.splitPiped(): List<String> =
    split('|').map { it.trim() }.filter { it.isNotEmpty() }
