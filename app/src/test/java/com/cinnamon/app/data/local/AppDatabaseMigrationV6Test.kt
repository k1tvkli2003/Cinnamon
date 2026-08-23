package com.cinnamon.app.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/** Opens real old SQLite files through Room v6, so schema validation runs after every migration. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDatabaseMigrationV6Test {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val ownedDatabaseNames = mutableListOf<String>()

    @After
    fun cleanUp() {
        ownedDatabaseNames.forEach(context::deleteDatabase)
    }

    @Test
    fun `every supported empty schema chain opens as Room v6`() {
        (1..5).forEach { sourceVersion ->
            val name = databaseName("empty-v$sourceVersion")
            createOldDatabase(name, sourceVersion)

            val migrated = openAsV6(name)
            try {
                val sqlite = migrated.openHelper.writableDatabase
                assertEquals(6, sqlite.version)
                assertTrue(sqlite.hasTable("learning_focus_selections"))
                assertFalse(sqlite.hasTable("campaign_route_choices"))
                assertTrue(sqlite.hasTable("mesh_reference"))
                assertEquals(0, sqlite.foreignKeyViolations())
                assertEquals(listOf("ok"), sqlite.integrityCheck())
            } finally {
                migrated.close()
            }
        }
    }

    @Test
    fun `populated v4 preserves every frozen selection value and related data`() = runBlocking {
        val name = databaseName("populated-v4")
        val expected = LegacySelectionFixture(
            selectionId = "campaign_choice::learner-β::v1",
            actorId = "learner-β",
            definitionId = "campaign.foundation-route-choice",
            definitionVersion = 1,
            optionId = "route.precision-trail",
            milestonePlanDefinitionId = "journey.route.precision-trail",
            sourceEventId = "event::selection::β",
            selectedAtEpochMillis = 1_700_000_000_123L
        )
        createOldDatabase(name, sourceVersion = 4) { sqlite ->
            insertEvent(sqlite, expected.sourceEventId, expected.actorId, expected.selectedAtEpochMillis)
            sqlite.execSQL(
                """
                INSERT INTO `campaign_route_choices` (
                    `choiceId`, `actorId`, `campaignDefinitionId`,
                    `campaignDefinitionVersion`, `routeId`, `journeyDefinitionId`,
                    `sourceEventId`, `chosenAtEpochMillis`
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    expected.selectionId,
                    expected.actorId,
                    expected.definitionId,
                    expected.definitionVersion,
                    expected.optionId,
                    expected.milestonePlanDefinitionId,
                    expected.sourceEventId,
                    expected.selectedAtEpochMillis
                )
            )
            sqlite.execSQL(
                """
                INSERT INTO `reward_summaries` VALUES (
                    ?, ?, 1, 0, '[]', '[]', '[]', '[]', 'micro',
                    '{"xpAwarded":0}', ?
                )
                """.trimIndent(),
                arrayOf<Any?>(expected.sourceEventId, expected.actorId, expected.selectedAtEpochMillis)
            )
        }

        val migrated = openAsV6(name)
        try {
            val selection = migrated.gamificationDao()
                .observeLearningFocusSelections(expected.actorId)
                .first()
                .single()
            assertEquals(expected.selectionId, selection.selectionId)
            assertEquals(expected.actorId, selection.actorId)
            assertEquals(expected.definitionId, selection.definitionId)
            assertEquals(expected.definitionVersion, selection.definitionVersion)
            assertEquals(expected.optionId, selection.optionId)
            assertEquals(
                expected.milestonePlanDefinitionId,
                selection.milestonePlanDefinitionId
            )
            assertEquals(expected.sourceEventId, selection.sourceEventId)
            assertEquals(expected.selectedAtEpochMillis, selection.selectedAtEpochMillis)
            assertNotNull(migrated.gamificationDao().eventById(expected.sourceEventId))
            assertEquals(
                0L,
                migrated.gamificationDao().rewardSummaryForEvent(expected.sourceEventId)?.xpAwarded
            )
            assertEquals(0, migrated.openHelper.writableDatabase.foreignKeyViolations())
            assertEquals(
                listOf("ok"),
                migrated.openHelper.writableDatabase.integrityCheck()
            )
        } finally {
            migrated.close()
        }
    }

    @Test
    fun `orphan source event aborts and rolls migration back to intact v4`() {
        val name = databaseName("orphan-v4")
        createOldDatabase(name, sourceVersion = 4) { sqlite ->
            sqlite.execSQL("PRAGMA foreign_keys = OFF")
            sqlite.execSQL(
                """
                INSERT INTO `campaign_route_choices` VALUES (
                    'orphan-choice', 'learner', 'campaign.foundation-route-choice', 1,
                    'route.precision-trail', 'journey.route.precision-trail',
                    'missing-event', 1700000000000
                )
                """.trimIndent()
            )
        }

        val failure = runCatching { openAsV6(name) }.exceptionOrNull()
        assertNotNull(failure)
        assertRolledBackV4(name, expectedLegacyRows = 1)
    }

    @Test
    fun `duplicate authored selection aborts and rolls migration back to intact v4`() {
        val name = databaseName("duplicate-v4")
        createOldDatabase(name, sourceVersion = 4) { sqlite ->
            sqlite.execSQL("DROP INDEX `idx_campaign_choices_actor_definition_version`")
            repeat(2) { index ->
                val eventId = "duplicate-event-$index"
                val optionId = if (index == 0) {
                    "route.precision-trail"
                } else {
                    "route.momentum-circuit"
                }
                insertEvent(sqlite, eventId, "learner", 1_700_000_000_000L + index)
                sqlite.execSQL(
                    """
                    INSERT INTO `campaign_route_choices` VALUES (
                        ?, 'learner', 'campaign.foundation-route-choice', 1, ?, ?, ?, ?
                    )
                    """.trimIndent(),
                    arrayOf<Any?>(
                        "duplicate-choice-$index",
                        optionId,
                        "journey.$optionId",
                        eventId,
                        1_700_000_000_000L + index
                    )
                )
            }
        }

        val failure = runCatching { openAsV6(name) }.exceptionOrNull()
        assertNotNull(failure)
        assertRolledBackV4(name, expectedLegacyRows = 2)
    }

    private fun databaseName(scenario: String): String =
        "learning-focus-migration-$scenario-${System.nanoTime()}.db".also(ownedDatabaseNames::add)

    private fun createOldDatabase(
        name: String,
        sourceVersion: Int,
        seed: (SQLiteDatabase) -> Unit = {}
    ) {
        val schemaVersion = if (sourceVersion == 1) 2 else sourceVersion
        val excludedTables = if (sourceVersion == 1) V2_TABLES else emptySet()
        val databaseFile = context.getDatabasePath(name)
        databaseFile.parentFile?.mkdirs()
        val sqlite = SQLiteDatabase.openOrCreateDatabase(databaseFile, null)
        try {
            sqlite.execSQL("PRAGMA foreign_keys = OFF")
            val schema = JSONObject(schemaFile(schemaVersion).readText())
                .getJSONObject("database")
                .getJSONArray("entities")
            for (entityIndex in 0 until schema.length()) {
                val entity = schema.getJSONObject(entityIndex)
                val tableName = entity.getString("tableName")
                if (tableName in excludedTables) continue
                sqlite.execSQL(
                    entity.getString("createSql").replace("\${TABLE_NAME}", tableName)
                )
                val indices = entity.getJSONArray("indices")
                for (index in 0 until indices.length()) {
                    sqlite.execSQL(
                        indices.getJSONObject(index)
                            .getString("createSql")
                            .replace("\${TABLE_NAME}", tableName)
                    )
                }
            }
            seed(sqlite)
            sqlite.version = sourceVersion
        } finally {
            sqlite.close()
        }
    }

    private fun schemaFile(version: Int): File {
        val relative = "schemas/com.cinnamon.app.data.local.AppDatabase/$version.json"
        val candidates = listOf(File(relative), File("app/$relative"))
        return candidates.firstOrNull(File::isFile)
            ?: error("Missing exported Room schema $version from ${System.getProperty("user.dir")}")
    }

    private fun openAsV6(name: String): AppDatabase {
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(
                AppDatabaseMigrations.MIGRATION_1_2,
                AppDatabaseMigrations.MIGRATION_2_3,
                AppDatabaseMigrations.MIGRATION_3_4,
                AppDatabaseMigrations.MIGRATION_4_5,
                AppDatabaseMigrations.MIGRATION_5_6
            )
            .allowMainThreadQueries()
            .build()
        return try {
            database.openHelper.writableDatabase
            database
        } catch (failure: Throwable) {
            database.close()
            throw failure
        }
    }

    private fun insertEvent(
        sqlite: SQLiteDatabase,
        eventId: String,
        actorId: String,
        occurredAt: Long
    ) {
        sqlite.execSQL(
            """
            INSERT INTO `gamification_events` (
                `eventId`, `actorId`, `eventType`, `subjectType`, `subjectId`,
                `occurredAtEpochMillis`, `recordedAtEpochMillis`, `studyDay`,
                `idempotencyKey`, `source`, `ruleVersion`, `metadataJson`, `replayOfEventId`
            ) VALUES (?, ?, 'campaign_route_selected', 'campaign_route_choice',
                'route.precision-trail', ?, ?, 19675, ?, 'migration-test', 1, '{}', NULL)
            """.trimIndent(),
            arrayOf<Any?>(eventId, actorId, occurredAt, occurredAt, "idempotency::$eventId")
        )
    }

    private fun assertRolledBackV4(name: String, expectedLegacyRows: Int) {
        val sqlite = SQLiteDatabase.openDatabase(
            context.getDatabasePath(name).absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )
        try {
            assertEquals(4, sqlite.version)
            assertTrue(sqlite.hasRawTable("campaign_route_choices"))
            assertFalse(sqlite.hasRawTable("learning_focus_selections"))
            sqlite.rawQuery("SELECT COUNT(*) FROM `campaign_route_choices`", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(expectedLegacyRows, cursor.getInt(0))
            }
        } finally {
            sqlite.close()
        }
    }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.hasTable(tableName: String): Boolean =
        query(
            "SELECT EXISTS(SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?)",
            arrayOf(tableName)
        ).use { cursor -> cursor.moveToFirst() && cursor.getInt(0) == 1 }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.foreignKeyViolations(): Int =
        query("PRAGMA foreign_key_check").use { cursor -> cursor.count }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.integrityCheck(): List<String> =
        query("PRAGMA integrity_check").use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    private fun SQLiteDatabase.hasRawTable(tableName: String): Boolean =
        rawQuery(
            "SELECT EXISTS(SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?)",
            arrayOf(tableName)
        ).use { cursor -> cursor.moveToFirst() && cursor.getInt(0) == 1 }

    private data class LegacySelectionFixture(
        val selectionId: String,
        val actorId: String,
        val definitionId: String,
        val definitionVersion: Int,
        val optionId: String,
        val milestonePlanDefinitionId: String,
        val sourceEventId: String,
        val selectedAtEpochMillis: Long
    )

    private companion object {
        val V2_TABLES = setOf(
            "gamification_events",
            "reward_transactions",
            "reward_summaries",
            "reward_balances",
            "quest_instances",
            "achievement_unlocks",
            "reward_presentation_receipts"
        )
    }
}
