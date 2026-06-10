package com.cinnamon.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Medical word-building element: prefix, root/combining form, or suffix. */
@Entity(
    tableName = "morphemes",
    indices = [Index(value = ["form"], unique = true), Index(value = ["kind"])]
)
data class Morpheme(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val form: String,
    val kind: String,                // prefix | root | suffix
    val meaning: String,
    val origin: String,
    val examplesRaw: String
) {
    val examples: List<String> get() = examplesRaw.splitPiped()
}

/** Ward abbreviation with how-it's-actually-said context. */
@Entity(
    tableName = "abbreviations",
    indices = [Index(value = ["short"], unique = true)]
)
data class Abbreviation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val short: String,
    val expansion: String,
    val context: String
)

/** Communication frame: rounds, patient talk, colleague talk, writing, idiom. */
@Entity(
    tableName = "phrases",
    indices = [Index(value = ["phrase"], unique = true), Index(value = ["category"])]
)
data class PhraseEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phrase: String,
    val category: String,
    val meaning: String,
    val example: String,
    val register: String             // formal | neutral | casual
)

/** Easily-confused pair with a memorable disambiguation. */
@Entity(
    tableName = "confusables",
    indices = [Index(value = ["a", "b"], unique = true)]
)
data class Confusable(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val a: String,
    val b: String,
    val howToTell: String,
    val exampleA: String,
    val exampleB: String
)

/** Graded sentence for the unscramble game. */
@Entity(
    tableName = "sentences",
    indices = [Index(value = ["text"], unique = true)]
)
data class PracticeSentence(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val domain: String,              // clinical | general
    val level: String                // B2+ | C1 | C2
)
