#!/usr/bin/env python3
"""Prove every additive Room migration path matches the exported current schema."""

from __future__ import annotations

import json
import re
import sqlite3
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SCHEMA_DIR = ROOT / "app/schemas/com.cinnamon.app.data.local.AppDatabase"
SCHEMA_V2 = SCHEMA_DIR / "2.json"
SCHEMA_V3 = SCHEMA_DIR / "3.json"
SCHEMA_V4 = SCHEMA_DIR / "4.json"
MIGRATION = ROOT / "app/src/main/java/com/cinnamon/app/data/local/AppDatabaseMigrations.kt"
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


def build_expected(schema: dict) -> sqlite3.Connection:
    connection = sqlite3.connect(":memory:")
    connection.execute("PRAGMA foreign_keys = OFF")
    for entity in schema["database"]["entities"]:
        table = entity["tableName"]
        connection.execute(entity["createSql"].replace("${TABLE_NAME}", table))
        for index in entity.get("indices", []):
            connection.execute(index["createSql"].replace("${TABLE_NAME}", table))
    return connection


def build_without_tables(schema: dict, excluded: set[str]) -> sqlite3.Connection:
    connection = sqlite3.connect(":memory:")
    connection.execute("PRAGMA foreign_keys = OFF")
    for entity in schema["database"]["entities"]:
        if entity["tableName"] in excluded:
            continue
        table = entity["tableName"]
        connection.execute(entity["createSql"].replace("${TABLE_NAME}", table))
        for index in entity.get("indices", []):
            connection.execute(index["createSql"].replace("${TABLE_NAME}", table))
    return connection


def apply_statements(connection: sqlite3.Connection, statements: list[str]) -> None:
    for statement in statements:
        connection.execute(statement)


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


def main() -> int:
    for schema_path in (SCHEMA_V2, SCHEMA_V3, SCHEMA_V4):
        if not schema_path.is_file():
            fail(f"missing exported schema: {schema_path.relative_to(ROOT)}")
    source = MIGRATION.read_text(encoding="utf-8")
    schema_v2 = json.loads(SCHEMA_V2.read_text(encoding="utf-8"))
    schema_v3 = json.loads(SCHEMA_V3.read_text(encoding="utf-8"))
    schema_v4 = json.loads(SCHEMA_V4.read_text(encoding="utf-8"))
    if schema_v2.get("database", {}).get("version") != 2:
        fail("expected Room schema version 2")
    if schema_v3.get("database", {}).get("version") != 3:
        fail("expected Room schema version 3")
    if schema_v4.get("database", {}).get("version") != 4:
        fail("expected Room schema version 4")
    statements_v1_v2 = migration_statements(source, "MIGRATION_1_2", "MIGRATION_2_3")
    statements_v2_v3 = migration_statements(source, "MIGRATION_2_3", "MIGRATION_3_4")
    statements_v3_v4 = migration_statements(source, "MIGRATION_3_4")
    statements = statements_v1_v2 + statements_v2_v3 + statements_v3_v4
    destructive = next(
        (
            statement
            for statement in statements
            if re.match(r"^\s*(DROP|DELETE|TRUNCATE|ALTER)\b", statement, flags=re.IGNORECASE)
        ),
        None,
    )
    if destructive:
        command = destructive.lstrip().split(maxsplit=1)[0].upper()
        fail(f"migration is not additive-only: found {command}")
    expected_v2 = build_expected(schema_v2)
    expected_v3 = build_expected(schema_v3)
    expected_v4 = build_expected(schema_v4)

    # Validate an existing v1 database all the way through v2 and v3.
    migrated_from_v1 = build_without_tables(schema_v2, V2_TABLES)
    apply_statements(migrated_from_v1, statements_v1_v2)
    v2_mismatches = [
        table
        for table in sorted(V2_TABLES)
        if table_contract(expected_v2, table) != table_contract(migrated_from_v1, table)
    ]
    if v2_mismatches:
        fail(f"v1->v2 schema contract differs for: {', '.join(v2_mismatches)}")
    apply_statements(migrated_from_v1, statements_v2_v3)

    # Validate the direct upgrade path from a real v2 contract as well.
    migrated_from_v2 = build_without_tables(schema_v3, V3_TABLES)
    apply_statements(migrated_from_v2, statements_v2_v3)

    actual_tables = {
        row[0]
        for row in migrated_from_v2.execute("SELECT name FROM sqlite_master WHERE type='table'")
    }
    missing = sorted(V3_TABLES - actual_tables)
    if missing:
        fail(f"v2->v3 migration did not create tables: {', '.join(missing)}")

    direct_mismatches = [
        table
        for table in sorted(V3_TABLES)
        if table_contract(expected_v3, table) != table_contract(migrated_from_v2, table)
    ]
    if direct_mismatches:
        fail(f"v2->v3 schema contract differs for: {', '.join(direct_mismatches)}")

    # Validate a direct v3->v4 upgrade before checking the complete chain.
    migrated_from_v3 = build_without_tables(schema_v4, V4_TABLES)
    apply_statements(migrated_from_v3, statements_v3_v4)
    actual_v4_tables = {
        row[0]
        for row in migrated_from_v3.execute("SELECT name FROM sqlite_master WHERE type='table'")
    }
    missing_v4 = sorted(V4_TABLES - actual_v4_tables)
    if missing_v4:
        fail(f"v3->v4 migration did not create tables: {', '.join(missing_v4)}")
    v4_mismatches = [
        table
        for table in sorted(V4_TABLES)
        if table_contract(expected_v4, table) != table_contract(migrated_from_v3, table)
    ]
    if v4_mismatches:
        fail(f"v3->v4 schema contract differs for: {', '.join(v4_mismatches)}")

    apply_statements(migrated_from_v1, statements_v3_v4)

    current_tables = [entity["tableName"] for entity in schema_v4["database"]["entities"]]
    chained_mismatches = [
        table
        for table in sorted(current_tables)
        if table_contract(expected_v4, table) != table_contract(migrated_from_v1, table)
    ]
    if chained_mismatches:
        fail(f"v1->v4 chained schema contract differs for: {', '.join(chained_mismatches)}")

    print(
        f"Room migrations verified: v1->v2 ({len(V2_TABLES)} tables), "
        f"v2->v3 ({len(V3_TABLES)} tables), v3->v4 ({len(V4_TABLES)} table), "
        f"{len(statements)} additive SQL statements, current schema v4."
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (KeyError, json.JSONDecodeError, sqlite3.DatabaseError) as error:
        fail(str(error))
