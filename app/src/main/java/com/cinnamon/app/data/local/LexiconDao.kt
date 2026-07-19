package com.cinnamon.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LexiconDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(entries: List<LexiconEntry>)

    @Update
    suspend fun update(entry: LexiconEntry)

    @Query(
        """
        SELECT * FROM lexicon
        WHERE (:domain = 'all' OR domain = :domain)
          AND (:level = 'all' OR level = :level)
          AND (:query = '' OR term LIKE '%' || :query || '%'
               OR plain LIKE '%' || :query || '%'
               OR definition LIKE '%' || :query || '%'
               OR topic LIKE '%' || :query || '%'
               OR synonymsRaw LIKE '%' || :query || '%')
        ORDER BY term COLLATE NOCASE ASC
        """
    )
    fun search(query: String, domain: String, level: String): Flow<List<LexiconEntry>>

    @Query("SELECT * FROM lexicon WHERE id = :id")
    fun entryById(id: Long): Flow<LexiconEntry?>

    @Query("SELECT * FROM lexicon WHERE id = :id")
    suspend fun entryByIdOnce(id: Long): LexiconEntry?

    @Query("SELECT * FROM lexicon WHERE isBookmarked = 1 ORDER BY term COLLATE NOCASE ASC")
    fun bookmarked(): Flow<List<LexiconEntry>>

    @Query("SELECT * FROM lexicon WHERE topic = :topic AND id != :excludeId ORDER BY RANDOM() LIMIT :limit")
    suspend fun relatedByTopic(topic: String, excludeId: Long, limit: Int): List<LexiconEntry>

    @Query("SELECT COUNT(*) FROM lexicon")
    fun totalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM lexicon")
    suspend fun totalCountOnce(): Int

    @Query("SELECT COUNT(*) FROM lexicon WHERE intervalDays >= 21")
    fun masteredCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM lexicon WHERE dueAt > 0 AND dueAt <= :now")
    fun dueCount(now: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM lexicon WHERE dueAt > 0 AND dueAt <= :now")
    suspend fun dueCountOnce(now: Long): Int

    @Query("SELECT * FROM lexicon WHERE dueAt > 0 AND dueAt <= :now ORDER BY dueAt ASC LIMIT :limit")
    suspend fun dueEntries(now: Long, limit: Int): List<LexiconEntry>

    @Query("SELECT * FROM lexicon WHERE dueAt = 0 ORDER BY RANDOM() LIMIT :limit")
    suspend fun freshEntries(limit: Int): List<LexiconEntry>

    @Query("SELECT * FROM lexicon WHERE (:domain = 'all' OR domain = :domain) AND plain != '' ORDER BY RANDOM() LIMIT :limit")
    suspend fun randomForGame(domain: String, limit: Int): List<LexiconEntry>

    @Query(
        """
        SELECT * FROM lexicon
        WHERE domain = 'clinical'
          AND exampleClinical != ''
          AND LOWER(exampleClinical) LIKE '%' || LOWER(term) || '%'
        ORDER BY RANDOM() LIMIT :limit
        """
    )
    suspend fun clozeCandidates(limit: Int): List<LexiconEntry>

    @Query("SELECT * FROM lexicon WHERE topic = :topic AND term != :term ORDER BY RANDOM() LIMIT :limit")
    suspend fun distractors(topic: String, term: String, limit: Int): List<LexiconEntry>

    @Query("SELECT * FROM lexicon ORDER BY id ASC LIMIT 1 OFFSET :offset")
    fun entryAtOffset(offset: Int): Flow<LexiconEntry?>

    @Query("SELECT DISTINCT topic FROM lexicon WHERE domain = :domain ORDER BY topic ASC")
    fun topicsForDomain(domain: String): Flow<List<String>>
}
