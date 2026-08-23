package com.cinnamon.app.data.seed

import android.os.SystemClock
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cinnamon.app.data.local.AppDatabase
import com.cinnamon.app.data.local.MeshReferenceEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeshReferenceSeederAndroidTest {

    private companion object {
        const val TAG = "MeshReferenceRuntime"
        const val FIRST_SEED_BUDGET_MILLIS = 15_000L
        const val CURRENT_CHECK_BUDGET_MILLIS = 1_000L
        const val SEARCH_BUDGET_MILLIS = 1_000L
    }

    @Test
    fun validated_reference_seed_preserves_authored_learner_state() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        LexiconSeeder.seedIfNeeded(context)
        val database = AppDatabase.getDatabase(context)
        val original = requireNotNull(database.lexiconDao().entryAtOffset(0).first())
        val learnerOwned = original.copy(
            isBookmarked = true,
            reps = 3,
            easeFactor = 2.7f,
            intervalDays = 21f,
            dueAt = 1_700_000_000_000L,
            timesSeen = 4
        )
        database.lexiconDao().update(learnerOwned)
        val staleReference = MeshReferenceEntry(
            meshUi = "D999999999",
            term = "Stale Atlas Row",
            normalizedTerm = "stale atlas row",
            scopeNote = "A synthetic stale row used only to prove snapshot reconciliation.",
            synonymsRaw = "",
            treeNumbersRaw = "A01",
            categoryRootsRaw = "A",
            introducedYear = 2025,
            lastUpdated = "2025-01-01",
            sourceYear = 2025,
            datasetVersion = "stale-test-snapshot"
        )
        database.meshReferenceDao().insertAll(listOf(staleReference))

        try {
            val firstSeedStartedAt = SystemClock.elapsedRealtime()
            val firstResult = MeshReferenceSeeder.seedIfNeeded(context)
            val firstSeedMillis = SystemClock.elapsedRealtime() - firstSeedStartedAt
            Log.i(TAG, "first_seed_ms=$firstSeedMillis")
            assertTrue(
                firstResult is MeshReferenceSeedResult.Installed ||
                    firstResult is MeshReferenceSeedResult.AlreadyCurrent
            )
            assertTrue(
                "Reference Atlas first seed exceeded ${FIRST_SEED_BUDGET_MILLIS}ms: " +
                    "${firstSeedMillis}ms",
                firstSeedMillis <= FIRST_SEED_BUDGET_MILLIS
            )
            assertEquals(5742, database.meshReferenceDao().countOnce())
            assertNotNull(database.meshReferenceDao().byMeshUi("D000005").first())
            assertEquals(
                null,
                database.meshReferenceDao().byMeshUi(staleReference.meshUi).first()
            )
            val synonymSearchStartedAt = SystemClock.elapsedRealtime()
            val synonymResults = database.meshReferenceDao().search(
                normalizedQuery = "blood pressure, high",
                categoryRoot = "C",
                limit = 10
            ).first()
            val synonymSearchMillis =
                SystemClock.elapsedRealtime() - synonymSearchStartedAt
            Log.i(TAG, "synonym_search_ms=$synonymSearchMillis")
            assertTrue(synonymResults.any { it.meshUi == "D006973" })
            assertTrue(
                "Reference Atlas synonym search exceeded ${SEARCH_BUDGET_MILLIS}ms: " +
                    "${synonymSearchMillis}ms",
                synonymSearchMillis <= SEARCH_BUDGET_MILLIS
            )
            assertTrue(
                database.meshReferenceDao().search(
                    normalizedQuery = "thorax",
                    categoryRoot = "A",
                    limit = 50
                ).first().any { it.meshUi == "D000005" }
            )
            assertTrue(
                database.meshReferenceDao().search(
                    normalizedQuery = "blood pressure, high",
                    categoryRoot = "A",
                    limit = 10
                ).first().none { it.meshUi == "D006973" }
            )

            val after = requireNotNull(database.lexiconDao().entryByIdOnce(original.id))
            assertEquals(learnerOwned, after)
            val currentCheckStartedAt = SystemClock.elapsedRealtime()
            val currentResult = MeshReferenceSeeder.seedIfNeeded(context)
            val currentCheckMillis = SystemClock.elapsedRealtime() - currentCheckStartedAt
            Log.i(TAG, "current_check_ms=$currentCheckMillis")
            assertEquals(
                MeshReferenceSeedResult.AlreadyCurrent,
                currentResult
            )
            assertTrue(
                "Reference Atlas current check exceeded ${CURRENT_CHECK_BUDGET_MILLIS}ms: " +
                    "${currentCheckMillis}ms",
                currentCheckMillis <= CURRENT_CHECK_BUDGET_MILLIS
            )
            assertEquals(5742, database.meshReferenceDao().countOnce())
        } finally {
            database.lexiconDao().update(original)
        }
    }
}
