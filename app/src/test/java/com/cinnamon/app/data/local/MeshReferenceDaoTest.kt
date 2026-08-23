package com.cinnamon.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MeshReferenceDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: MeshReferenceDao

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.meshReferenceDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `search ranks exact and prefix terms before scope matches`() = runBlocking {
        dao.insertAll(
            listOf(
                descriptor(
                    meshUi = "D000005",
                    term = "Abdomen",
                    roots = "A",
                    scope = "That portion of the body between the thorax and pelvis."
                ),
                descriptor(
                    meshUi = "D000007",
                    term = "Abdominal Injuries",
                    roots = "C",
                    scope = "General injuries involving organs in the abdominal cavity."
                ),
                descriptor(
                    meshUi = "D999999",
                    term = "Body Regions",
                    roots = "A|G",
                    scope = "A broad reference that mentions the abdomen."
                )
            )
        )

        val results = dao.search(
            normalizedQuery = "abdomen",
            categoryRoot = "all",
            limit = 10
        ).first()

        assertEquals(
            listOf("Abdomen", "Body Regions"),
            results.map { it.term }
        )
    }

    @Test
    fun `category membership and stable MeSH identity stay queryable`() = runBlocking {
        val anatomy = descriptor(
            meshUi = "D000005",
            term = "Abdomen",
            roots = "A",
            scope = "That portion of the body between the thorax and pelvis."
        )
        val disease = descriptor(
            meshUi = "D000007",
            term = "Abdominal Injuries",
            roots = "C",
            scope = "General injuries involving organs in the abdominal cavity."
        )
        dao.insertAll(listOf(anatomy, disease))

        assertEquals(2, dao.countOnce())
        assertEquals(2, dao.countForSnapshotOnce("2026.1", 2026))
        assertEquals(0, dao.countForSnapshotOnce("2025.1", 2025))
        assertEquals(anatomy, dao.byMeshUi("D000005").first())
        assertEquals(
            listOf("Abdomen"),
            dao.search("", "A", 10).first().map { it.term }
        )
        assertEquals(1, dao.countForCategory("C").first())
    }

    @Test
    fun `snapshot replacement removes stale references before authoritative insert`() =
        runBlocking {
            val stale = descriptor(
                meshUi = "D999999",
                term = "Stale Atlas Row",
                roots = "A",
                scope = "A synthetic row from an older snapshot."
            )
            val current = descriptor(
                meshUi = "D000005",
                term = "Abdomen",
                roots = "A",
                scope = "That portion of the body between the thorax and pelvis."
            )
            dao.insertAll(listOf(stale))

            database.withTransaction {
                dao.deleteSnapshot()
                dao.insertAll(listOf(current))
            }

            assertEquals(1, dao.countOnce())
            assertEquals(null, dao.byMeshUi(stale.meshUi).first())
            assertEquals(current, dao.byMeshUi(current.meshUi).first())
        }

    private fun descriptor(
        meshUi: String,
        term: String,
        roots: String,
        scope: String
    ): MeshReferenceEntry =
        MeshReferenceEntry(
            meshUi = meshUi,
            term = term,
            normalizedTerm = term.lowercase(),
            scopeNote = scope,
            synonymsRaw = "",
            treeNumbersRaw = "${roots.first()}01",
            categoryRootsRaw = roots,
            introducedYear = 1966,
            lastUpdated = "2026-01-01",
            sourceYear = 2026,
            datasetVersion = "2026.1"
        )
}
