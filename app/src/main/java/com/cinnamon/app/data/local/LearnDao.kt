package com.cinnamon.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LearnDao {

    // ── Morphemes ──
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMorphemes(items: List<Morpheme>)

    @Query(
        """
        SELECT * FROM morphemes
        WHERE (:kind = 'all' OR kind = :kind)
          AND (:query = '' OR form LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%'
               OR examplesRaw LIKE '%' || :query || '%')
        ORDER BY form COLLATE NOCASE ASC
        """
    )
    fun morphemes(query: String, kind: String): Flow<List<Morpheme>>

    // ── Abbreviations ──
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAbbreviations(items: List<Abbreviation>)

    @Query(
        """
        SELECT * FROM abbreviations
        WHERE (:query = '' OR short LIKE '%' || :query || '%' OR expansion LIKE '%' || :query || '%')
        ORDER BY short COLLATE NOCASE ASC
        """
    )
    fun abbreviations(query: String): Flow<List<Abbreviation>>

    // ── Phrases ──
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPhrases(items: List<PhraseEntry>)

    @Query(
        """
        SELECT * FROM phrases
        WHERE (:category = 'all' OR category = :category)
          AND (:query = '' OR phrase LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%')
        ORDER BY phrase COLLATE NOCASE ASC
        """
    )
    fun phrases(query: String, category: String): Flow<List<PhraseEntry>>

    @Query("SELECT DISTINCT category FROM phrases ORDER BY category ASC")
    fun phraseCategories(): Flow<List<String>>

    // ── Confusables ──
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertConfusables(items: List<Confusable>)

    @Query(
        """
        SELECT * FROM confusables
        WHERE (:query = '' OR a LIKE '%' || :query || '%' OR b LIKE '%' || :query || '%')
        ORDER BY a COLLATE NOCASE ASC
        """
    )
    fun confusables(query: String): Flow<List<Confusable>>

    /** A small, varied round assembled from the seeded clinical-language corpus. */
    @Query("SELECT * FROM confusables ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomConfusables(limit: Int): List<Confusable>

    // ── Sentences ──
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSentences(items: List<PracticeSentence>)

    @Query("SELECT * FROM sentences ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomSentences(limit: Int): List<PracticeSentence>
}
