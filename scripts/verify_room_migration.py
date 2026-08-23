#!/usr/bin/env python3
"""Prove every supported Room migration path and durable-data invariant."""

from __future__ import annotations

import json
import re
import sqlite3
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
SCHEMA_DIR = ROOT / "app/schemas/com.cinnamon.app.data.local.AppDatabase"
SCHEMA_V2 = SCHEMA_DIR / "2.json"
SCHEMA_V3 = SCHEMA_DIR / "3.json"
SCHEMA_V4 = SCHEMA_DIR / "4.json"
SCHEMA_V5 = SCHEMA_DIR / "5.json"
SCHEMA_V6 = SCHEMA_DIR / "6.json"
MIGRATION = ROOT / "app/src/main/java/com/cinnamon/app/data/local/AppDatabaseMigrations.kt"
DAO = ROOT / "app/src/main/java/com/cinnamon/app/data/local/GamificationDao.kt"
V2_TABLES = {
    "gamification_events",
    "reward_transactions",
    "reward_summaries",
    "reward_balances",
    "quest_instances",
    "achievement_unlocks",
    "reward_presentation_receipts",
}
V3_TABLES = {
    "journey_instances",
    "journey_stage_progress",
}
V4_TABLES = {"campaign_route_choices"}
V5_TABLES = {"learning_focus_selections"}
V6_TABLES = {"mesh_reference"}
LEGACY_TABLE = "campaign_route_choices"
CURRENT_TABLE = "learning_focus_selections"
LEGACY_COLUMNS = (
    "choiceId",
    "actorId",
    "campaignDefinitionId",
    "campaignDefinitionVersion",
    "routeId",
    "journeyDefinitionId",
    "sourceEventId",
    "chosenAtEpochMillis",
)
CURRENT_COLUMNS = (
    "selectionId",
    "actorId",
    "definitionId",
    "definitionVersion",
    "optionId",
    "milestonePlanDefinitionId",
    "sourceEventId",
    "selectedAtEpochMillis",
)


def fail(message: str) -> None:
    raise SystemExit(f"Room migration verification failed: {message}")


def migration_block(source: str, name: str, next_name: str | None = None) -> str:
    marker = f"val {name}:"
    start = source.find(marker)
    if start < 0:
        fail(f"missing {name}")
    if next_name is None:
        return source[start:]
    end = source.find(f"val {next_name}:", start)
    if end < 0:
        fail(f"missing {next_name}")
    return source[start:end]


def migration_statements(source: str, name: str, next_name: str | None = None) -> list[str]:
    block = migration_block(source, name, next_name)
    statements = re.findall(
        r'db\.execSQL\(\s*"""(.*?)"""\.trimIndent\(\)\s*\)',
        block,
        flags=re.DOTALL,
    )
    if not statements:
        fail(f"no db.execSQL statements were found in {name}")
    return [statement.strip() for statement in statements]


def load_schema(path: Path, expected_version: int) -> dict:
    if not path.is_file():
        fail(f"missing exported schema: {path.relative_to(ROOT)}")
    schema = json.loads(path.read_text(encoding="utf-8"))
    actual_version = schema.get("database", {}).get("version")
    if actual_version != expected_version:
        fail(f"expected Room schema version {expected_version}, found {actual_version}")
    return schema


def build_schema(schema: dict, excluded: set[str] | None = None) -> sqlite3.Connection:
    connection = sqlite3.connect(":memory:")
    connection.execute("PRAGMA foreign_keys = OFF")
    excluded = excluded or set()
    for entity in schema["database"]["entities"]:
        table = entity["tableName"]
        if table in excluded:
            continue
        connection.execute(entity["createSql"].replace("${TABLE_NAME}", table))
        for index in entity.get("indices", []):
            connection.execute(index["createSql"].replace("${TABLE_NAME}", table))
    connection.commit()
    return connection


def apply_statements(connection: sqlite3.Connection, statements: Iterable[str]) -> None:
    for statement in statements:
        connection.execute(statement)


def migrate_transactionally(
    connection: sqlite3.Connection,
    statements: Iterable[str],
) -> None:
    connection.commit()
    connection.execute("BEGIN")
    try:
        apply_statements(connection, statements)
    except BaseException:
        connection.rollback()
        raise
    else:
        connection.commit()


def table_names(connection: sqlite3.Connection) -> set[str]:
    return {
        row[0]
        for row in connection.execute(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'"
        )
    }


def table_contract(connection: sqlite3.Connection, table: str) -> dict:
    columns = [tuple(row[1:6]) for row in connection.execute(f"PRAGMA table_info(`{table}`)")]
    foreign_keys = sorted(
        tuple(row[2:8]) for row in connection.execute(f"PRAGMA foreign_key_list(`{table}`)")
    )
    indexes: list[tuple[str, bool, tuple[str, ...]]] = []
    for row in connection.execute(f"PRAGMA index_list(`{table}`)"):
        name = row[1]
        if name.startswith("sqlite_autoindex_"):
            continue
        index_columns = tuple(
            index_row[2]
            for index_row in connection.execute(f"PRAGMA index_info(`{name}`)")
        )
        indexes.append((name, bool(row[2]), index_columns))
    return {
        "columns": columns,
        "foreign_keys": foreign_keys,
        "indexes": sorted(indexes),
    }


def assert_table_contracts(
    expected: sqlite3.Connection,
    actual: sqlite3.Connection,
    tables: Iterable[str],
    path: str,
) -> None:
    missing = sorted(set(tables) - table_names(actual))
    if missing:
        fail(f"{path} did not create tables: {', '.join(missing)}")
    mismatches = [
        table
        for table in sorted(tables)
        if table_contract(expected, table) != table_contract(actual, table)
    ]
    if mismatches:
        fail(f"{path} schema contract differs for: {', '.join(mismatches)}")


def quoted_columns(columns: Iterable[str]) -> str:
    return ", ".join(f"`{column}`" for column in columns)


def rows(
    connection: sqlite3.Connection,
    table: str,
    columns: Iterable[str] | None = None,
) -> list[tuple]:
    selected = quoted_columns(columns) if columns else "*"
    return list(connection.execute(f"SELECT {selected} FROM `{table}` ORDER BY rowid"))


def snapshot_tables(connection: sqlite3.Connection, tables: Iterable[str]) -> dict[str, list[tuple]]:
    return {table: rows(connection, table) for table in sorted(tables)}


def insert_event(
    connection: sqlite3.Connection,
    *,
    event_id: str,
    actor_id: str,
    event_type: str,
    subject_type: str,
    subject_id: str,
    occurred_at: int,
    study_day: int,
) -> None:
    connection.execute(
        """
        INSERT INTO `gamification_events` (
            `eventId`, `actorId`, `eventType`, `subjectType`, `subjectId`,
            `occurredAtEpochMillis`, `recordedAtEpochMillis`, `studyDay`,
            `idempotencyKey`, `source`, `ruleVersion`, `metadataJson`, `replayOfEventId`
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL)
        """,
        (
            event_id,
            actor_id,
            event_type,
            subject_type,
            subject_id,
            occurred_at,
            occurred_at + 7,
            study_day,
            f"idempotency::{event_id}",
            "migration-fixture",
            1,
            '{"fixture":"v4→v5","exact":true}',
        ),
    )


def seed_populated_v4(connection: sqlite3.Connection) -> list[tuple]:
    selected_at = 1_700_000_000_123
    fixtures = [
        (
            "campaign_choice::local-user::v1",
            "local-user",
            "campaign.foundation-route-choice",
            1,
            "route.precision-trail",
            "journey.route.precision-trail",
            "event::selection::precision",
            selected_at,
        ),
        (
            "campaign_choice::learner-β::v1",
            "learner-β",
            "campaign.foundation-route-choice",
            1,
            "route.momentum-circuit",
            "journey.route.momentum-circuit",
            "event::selection::recall-β",
            selected_at + 50,
        ),
    ]
    for fixture in fixtures:
        insert_event(
            connection,
            event_id=fixture[6],
            actor_id=fixture[1],
            event_type="campaign_route_selected",
            subject_type="campaign_route_choice",
            subject_id=fixture[4],
            occurred_at=fixture[7],
            study_day=19_675,
        )
        connection.execute(
            """
            INSERT INTO `reward_summaries` VALUES (?, ?, 1, 0, '[]', '[]', '[]', '[]',
                'micro', '{"xpAwarded":0}', ?)
            """,
            (fixture[6], fixture[1], fixture[7]),
        )
        connection.execute(
            f"INSERT INTO `{LEGACY_TABLE}` ({quoted_columns(LEGACY_COLUMNS)}) "
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            fixture,
        )

    evidence_event = "event::milestone::precision"
    evidence_at = selected_at + 1_000
    insert_event(
        connection,
        event_id=evidence_event,
        actor_id="local-user",
        event_type="practice_session_completed",
        subject_type="vocabulary_match",
        subject_id="session::α",
        occurred_at=evidence_at,
        study_day=19_676,
    )
    connection.execute(
        """
        INSERT INTO `reward_transactions` VALUES (
            'transaction::milestone::precision', ?, 'local-user', 'grant', 'xp', 25,
            'journey.journey.route.precision-trail.stage.switch-the-rules', 1,
            'journey_stage_completed', ?, 'idempotency::transaction::precision', '{}'
        )
        """,
        (evidence_event, evidence_at),
    )
    connection.execute(
        """
        INSERT INTO `reward_summaries` VALUES (?, 'local-user', 1, 25,
            '[{"currency":"xp","amount":25}]', '[]', '[]', '[]', 'milestone',
            '{"xpAwarded":25}', ?)
        """,
        (evidence_event, evidence_at),
    )
    connection.execute(
        """
        INSERT INTO `reward_balances` VALUES (
            'local-user', 'xp', 25, ?, 'transaction::milestone::precision'
        )
        """,
        (evidence_at,),
    )
    connection.execute(
        """
        INSERT INTO `reward_presentation_receipts` VALUES (
            'receipt::milestone::precision', 'local-user', ?,
            'transaction::milestone::precision', 'presentation.journey.stage',
            'idempotency::receipt::precision', 30, 'milestone', 'pending',
            '{"xpAwarded":25}', ?, ?, NULL, NULL, NULL, 1
        )
        """,
        (evidence_event, evidence_at, evidence_at),
    )
    connection.execute(
        """
        INSERT INTO `journey_instances` VALUES (
            'journey_instance::precision', 'local-user', 'journey.route.precision-trail', 1,
            'active', 2, ?, ?, NULL, NULL
        )
        """,
        (selected_at, evidence_at),
    )
    connection.execute(
        """
        INSERT INTO `journey_stage_progress` VALUES (
            'stage_progress::precision::1', 'journey_instance::precision', 'local-user',
            'stage.switch-the-rules', 1, 'distinct_practice_content_kinds', 'completed',
            3, 3, 25, ?, ?, ?, ?
        )
        """,
        (selected_at, evidence_at, evidence_event, evidence_at),
    )
    connection.commit()
    return fixtures


def assert_exact_v4_mapping(
    connection: sqlite3.Connection,
    legacy_rows: list[tuple],
) -> None:
    migrated_rows = rows(connection, CURRENT_TABLE, CURRENT_COLUMNS)
    if migrated_rows != legacy_rows:
        fail(
            "v4->v5 did not preserve selection ID/actor/definition/version/option/plan/"
            "source-event/timestamp values exactly"
        )


def assert_foreign_keys_clean(connection: sqlite3.Connection, path: str) -> None:
    violations = list(connection.execute("PRAGMA foreign_key_check"))
    if violations:
        fail(f"{path} left foreign-key violations: {violations}")


def assert_integrity_clean(connection: sqlite3.Connection, path: str) -> None:
    result = [row[0] for row in connection.execute("PRAGMA integrity_check")]
    if result != ["ok"]:
        fail(f"{path} failed SQLite integrity_check: {result}")


def verify_empty_and_populated_v4(
    schema_v4: dict,
    schema_v5: dict,
    statements_v4_v5: list[str],
) -> None:
    expected_v5 = build_schema(schema_v5)

    empty = build_schema(schema_v4)
    empty.execute("PRAGMA foreign_keys = ON")
    migrate_transactionally(empty, statements_v4_v5)
    assert_table_contracts(expected_v5, empty, V5_TABLES, "empty v4->v5")
    if LEGACY_TABLE in table_names(empty):
        fail("empty v4->v5 retained the legacy table")
    if rows(empty, CURRENT_TABLE):
        fail("empty v4->v5 created a phantom selection")
    assert_foreign_keys_clean(empty, "empty v4->v5")
    assert_integrity_clean(empty, "empty v4->v5")

    populated = build_schema(schema_v4)
    legacy_rows = seed_populated_v4(populated)
    unaffected_tables = {
        entity["tableName"]
        for entity in schema_v4["database"]["entities"]
        if entity["tableName"] != LEGACY_TABLE
    }
    before = snapshot_tables(populated, unaffected_tables)
    populated.execute("PRAGMA foreign_keys = ON")
    migrate_transactionally(populated, statements_v4_v5)
    assert_table_contracts(expected_v5, populated, V5_TABLES, "populated v4->v5")
    if LEGACY_TABLE in table_names(populated):
        fail("populated v4->v5 retained the legacy table")
    assert_exact_v4_mapping(populated, legacy_rows)
    after = snapshot_tables(populated, unaffected_tables)
    if before != after:
        changed = sorted(table for table in before if before[table] != after[table])
        fail(f"v4->v5 rewrote unrelated durable rows: {', '.join(changed)}")
    assert_foreign_keys_clean(populated, "populated v4->v5")
    assert_integrity_clean(populated, "populated v4->v5")


def verify_empty_and_populated_v5_to_v6(
    schema_v4: dict,
    schema_v5: dict,
    schema_v6: dict,
    statements_v4_v5: list[str],
    statements_v5_v6: list[str],
) -> None:
    expected_v6 = build_schema(schema_v6)

    empty = build_schema(schema_v5)
    empty.execute("PRAGMA foreign_keys = ON")
    migrate_transactionally(empty, statements_v5_v6)
    assert_table_contracts(expected_v6, empty, V6_TABLES, "empty v5->v6")
    if rows(empty, "mesh_reference"):
        fail("empty v5->v6 created phantom MeSH reference rows")
    assert_foreign_keys_clean(empty, "empty v5->v6")
    assert_integrity_clean(empty, "empty v5->v6")

    populated = build_schema(schema_v4)
    seed_populated_v4(populated)
    populated.execute("PRAGMA foreign_keys = ON")
    migrate_transactionally(populated, statements_v4_v5)
    preserved_tables = {
        entity["tableName"] for entity in schema_v5["database"]["entities"]
    }
    before = snapshot_tables(populated, preserved_tables)
    migrate_transactionally(populated, statements_v5_v6)
    assert_table_contracts(expected_v6, populated, V6_TABLES, "populated v5->v6")
    after = snapshot_tables(populated, preserved_tables)
    if before != after:
        changed = sorted(table for table in before if before[table] != after[table])
        fail(f"v5->v6 rewrote durable rows: {', '.join(changed)}")
    if rows(populated, "mesh_reference"):
        fail("v5->v6 migration fabricated MeSH reference rows before validated seeding")
    assert_foreign_keys_clean(populated, "populated v5->v6")
    assert_integrity_clean(populated, "populated v5->v6")


def expect_rollback(
    connection: sqlite3.Connection,
    statements: list[str],
    scenario: str,
) -> None:
    legacy_before = rows(connection, LEGACY_TABLE, LEGACY_COLUMNS)
    try:
        migrate_transactionally(connection, statements)
    except sqlite3.DatabaseError:
        pass
    else:
        fail(f"{scenario} unexpectedly migrated")
    if LEGACY_TABLE not in table_names(connection):
        fail(f"{scenario} did not restore the legacy table on rollback")
    if CURRENT_TABLE in table_names(connection):
        fail(f"{scenario} left a partially migrated table after rollback")
    temporary_tables = {
        row[0]
        for row in connection.execute(
            "SELECT name FROM sqlite_temp_master WHERE type = 'table'"
        )
    }
    if "learning_focus_migration_guard" in temporary_tables:
        fail(f"{scenario} left the migration guard after rollback")
    if rows(connection, LEGACY_TABLE, LEGACY_COLUMNS) != legacy_before:
        fail(f"{scenario} changed legacy rows despite rollback")


def verify_corruption_rollbacks(schema_v4: dict, statements_v4_v5: list[str]) -> None:
    orphan = build_schema(schema_v4)
    orphan.execute(
        f"INSERT INTO `{LEGACY_TABLE}` ({quoted_columns(LEGACY_COLUMNS)}) "
        "VALUES ('orphan', 'learner', 'campaign.foundation-route-choice', 1, "
        "'route.precision-trail', 'journey.route.precision-trail', "
        "'missing-source-event', 1700000000000)"
    )
    orphan.commit()
    expect_rollback(orphan, statements_v4_v5, "orphan-source-event fixture")

    duplicate = build_schema(schema_v4)
    duplicate.execute("DROP INDEX `idx_campaign_choices_actor_definition_version`")
    for number, option in enumerate(("route.precision-trail", "route.momentum-circuit"), start=1):
        event_id = f"duplicate-event-{number}"
        insert_event(
            duplicate,
            event_id=event_id,
            actor_id="learner",
            event_type="campaign_route_selected",
            subject_type="campaign_route_choice",
            subject_id=option,
            occurred_at=1_700_000_000_000 + number,
            study_day=19_675,
        )
        duplicate.execute(
            f"INSERT INTO `{LEGACY_TABLE}` ({quoted_columns(LEGACY_COLUMNS)}) "
            "VALUES (?, 'learner', 'campaign.foundation-route-choice', 1, ?, ?, ?, ?)",
            (
                f"duplicate-choice-{number}",
                option,
                f"journey.{option}",
                event_id,
                1_700_000_000_000 + number,
            ),
        )
    duplicate.commit()
    expect_rollback(duplicate, statements_v4_v5, "duplicate-definition fixture")


def verify_evidence_anchor_contract(schema_v5: dict) -> None:
    dao_source = DAO.read_text(encoding="utf-8")
    anchored_methods = (
        "distinctReviewedSubjectCountForStudyDayAfterOnce",
        "distinctSubjectIdCountAfterOnce",
        "distinctSubjectTypeCountAfterOnce",
        "recentStudyDaysAfterOnce",
        "activeStudyDayCountAfterOnce",
    )
    for method in anchored_methods:
        method_at = dao_source.find(f"fun {method}")
        if method_at < 0:
            method_at = dao_source.find(f"suspend fun {method}")
        if method_at < 0:
            fail(f"missing anchored DAO query: {method}")
        query_window = dao_source[max(0, method_at - 1_000) : method_at]
        if (
            "occurredAtEpochMillis > :afterEpochMillis" not in query_window
            or "occurredAtEpochMillis <= :throughEpochMillis" not in query_window
        ):
            fail(f"{method} does not use the strict post-selection time window")

    connection = build_schema(schema_v5)
    anchor = 1_700_000_000_000
    insert_event(
        connection,
        event_id="event-at-anchor",
        actor_id="anchor-learner",
        event_type="review_completed",
        subject_type="lexicon_entry",
        subject_id="must-be-excluded",
        occurred_at=anchor,
        study_day=19_675,
    )
    insert_event(
        connection,
        event_id="event-after-anchor",
        actor_id="anchor-learner",
        event_type="review_completed",
        subject_type="lexicon_entry",
        subject_id="must-be-included",
        occurred_at=anchor + 1,
        study_day=19_676,
    )
    count = connection.execute(
        """
        SELECT COUNT(DISTINCT subjectId)
        FROM gamification_events
        WHERE actorId = 'anchor-learner'
          AND eventType = 'review_completed'
          AND occurredAtEpochMillis > ?
          AND occurredAtEpochMillis <= ?
        """,
        (anchor, anchor + 1),
    ).fetchone()[0]
    if count != 1:
        fail("evidence at selection millisecond was not excluded or +1ms evidence was lost")


def verify_destructive_scope(
    additive_statements: list[str],
    statements_v4_v5: list[str],
) -> None:
    destructive_pattern = re.compile(r"^\s*(DROP|DELETE|TRUNCATE|ALTER)\b", re.IGNORECASE)
    destructive = next(
        (statement for statement in additive_statements if destructive_pattern.match(statement)),
        None,
    )
    if destructive:
        command = destructive.lstrip().split(maxsplit=1)[0].upper()
        fail(f"an additive migration contains a destructive command: {command}")

    allowed_drops = {
        "drop table `learning_focus_migration_guard`",
        "drop table `campaign_route_choices`",
    }
    actual_destructive = {
        re.sub(r"\s+", " ", statement.strip()).lower()
        for statement in statements_v4_v5
        if destructive_pattern.match(statement)
    }
    if actual_destructive != allowed_drops:
        fail(
            "v4->v5 destructive scope differs from the two bounded post-proof table drops: "
            f"{sorted(actual_destructive)}"
        )
    guard_at = next(
        index
        for index, statement in enumerate(statements_v4_v5)
        if "INSERT INTO `learning_focus_migration_guard`" in statement
    )
    legacy_drop_at = next(
        index
        for index, statement in enumerate(statements_v4_v5)
        if re.sub(r"\s+", " ", statement.strip()).lower()
        == "drop table `campaign_route_choices`"
    )
    if guard_at >= legacy_drop_at:
        fail("legacy table is dropped before conservation/FK proof")


def main() -> int:
    schema_v2 = load_schema(SCHEMA_V2, 2)
    schema_v3 = load_schema(SCHEMA_V3, 3)
    schema_v4 = load_schema(SCHEMA_V4, 4)
    schema_v5 = load_schema(SCHEMA_V5, 5)
    schema_v6 = load_schema(SCHEMA_V6, 6)
    source = MIGRATION.read_text(encoding="utf-8")
    statements_v1_v2 = migration_statements(source, "MIGRATION_1_2", "MIGRATION_2_3")
    statements_v2_v3 = migration_statements(source, "MIGRATION_2_3", "MIGRATION_3_4")
    statements_v3_v4 = migration_statements(source, "MIGRATION_3_4", "MIGRATION_4_5")
    statements_v4_v5 = migration_statements(source, "MIGRATION_4_5", "MIGRATION_5_6")
    statements_v5_v6 = migration_statements(source, "MIGRATION_5_6")
    additive_statements = (
        statements_v1_v2
        + statements_v2_v3
        + statements_v3_v4
        + statements_v5_v6
    )
    all_statements = additive_statements + statements_v4_v5
    verify_destructive_scope(additive_statements, statements_v4_v5)

    expected_v2 = build_schema(schema_v2)
    expected_v3 = build_schema(schema_v3)
    expected_v4 = build_schema(schema_v4)
    expected_v5 = build_schema(schema_v5)
    expected_v6 = build_schema(schema_v6)

    migrated_from_v1 = build_schema(schema_v2, V2_TABLES)
    apply_statements(migrated_from_v1, statements_v1_v2)
    assert_table_contracts(expected_v2, migrated_from_v1, V2_TABLES, "v1->v2")
    apply_statements(migrated_from_v1, statements_v2_v3)

    migrated_from_v2 = build_schema(schema_v3, V3_TABLES)
    apply_statements(migrated_from_v2, statements_v2_v3)
    assert_table_contracts(expected_v3, migrated_from_v2, V3_TABLES, "v2->v3")

    migrated_from_v3 = build_schema(schema_v4, V4_TABLES)
    apply_statements(migrated_from_v3, statements_v3_v4)
    assert_table_contracts(expected_v4, migrated_from_v3, V4_TABLES, "v3->v4")

    apply_statements(migrated_from_v1, statements_v3_v4)
    apply_statements(migrated_from_v1, statements_v4_v5)
    apply_statements(migrated_from_v1, statements_v5_v6)
    current_tables = {entity["tableName"] for entity in schema_v6["database"]["entities"]}
    assert_table_contracts(expected_v6, migrated_from_v1, current_tables, "v1->v6")
    if table_names(migrated_from_v1) != current_tables:
        unexpected = sorted(table_names(migrated_from_v1) - current_tables)
        fail(f"v1->v6 left unexpected tables: {', '.join(unexpected)}")
    assert_foreign_keys_clean(migrated_from_v1, "v1->v6")
    assert_integrity_clean(migrated_from_v1, "v1->v6")

    migrated_from_v2_to_v6 = build_schema(schema_v2)
    apply_statements(migrated_from_v2_to_v6, statements_v2_v3)
    apply_statements(migrated_from_v2_to_v6, statements_v3_v4)
    apply_statements(migrated_from_v2_to_v6, statements_v4_v5)
    apply_statements(migrated_from_v2_to_v6, statements_v5_v6)
    assert_table_contracts(expected_v6, migrated_from_v2_to_v6, current_tables, "v2->v6")

    migrated_from_v3_to_v6 = build_schema(schema_v3)
    apply_statements(migrated_from_v3_to_v6, statements_v3_v4)
    apply_statements(migrated_from_v3_to_v6, statements_v4_v5)
    apply_statements(migrated_from_v3_to_v6, statements_v5_v6)
    assert_table_contracts(expected_v6, migrated_from_v3_to_v6, current_tables, "v3->v6")

    verify_empty_and_populated_v4(schema_v4, schema_v5, statements_v4_v5)
    verify_empty_and_populated_v5_to_v6(
        schema_v4,
        schema_v5,
        schema_v6,
        statements_v4_v5,
        statements_v5_v6,
    )
    verify_corruption_rollbacks(schema_v4, statements_v4_v5)
    verify_evidence_anchor_contract(schema_v6)

    print(
        "Room migrations verified: v1/v2/v3/v4/v5->v6; empty and populated "
        "v4->v5 and v5->v6 conservation; "
        "exact frozen-value mapping; unchanged journey/reward/balance/event rows; unique/FK "
        "failure rollback; foreign-key/integrity checks; strict selection-time evidence anchor; "
        f"{len(all_statements)} SQL statements; current schema v6."
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (KeyError, StopIteration, json.JSONDecodeError, sqlite3.DatabaseError) as error:
        fail(str(error))
