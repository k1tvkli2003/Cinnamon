#!/usr/bin/env python3
"""Validate Cinnamon's deterministic NLM MeSH reference atlas."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any


DATASET_ID = "cinnamon.mesh.reference"
DATASET_VERSION = "2026.1"
EXPECTED_DESCRIPTOR_COUNT = 5742
EXPECTED_MEMBERSHIPS = {
    "A": 697,
    "C": 1853,
    "E": 1534,
    "F": 493,
    "G": 1167,
    "N": 632,
}
SOURCE_ARCHIVE_SHA256 = (
    "9fe35b3170652376a592daf69e91a80d6c693ecaf9c571ceb701d04204cb357d"
)
SOURCE_URL = "https://nlmpubs.nlm.nih.gov/projects/mesh/MESH_FILES/xmlmesh/desc2026.gz"
TERMS_URL = "https://www.nlm.nih.gov/databases/download/terms_and_conditions.html"
QUALITY_BUDGETS = {
    "maxDescriptors": 6500,
    "maxPayloadBytes": 4_000_000,
    "maxScopeNoteChars": 1200,
    "maxSynonymsPerDescriptor": 64,
    "maxTermChars": 96,
    "maxTreeNumbersPerDescriptor": 16,
}
EXPECTED_FIELDS = {
    "categoryRoots",
    "introducedYear",
    "lastUpdated",
    "meshUi",
    "scopeNote",
    "synonyms",
    "term",
    "treeNumbers",
}
ROOTS = tuple("ACEFGN")
MESH_UI = re.compile(r"D\d{6,9}\Z")
TREE_NUMBER = re.compile(r"[ACEFGN]\d{2}(?:\.\d{3})*\Z")
ISO_DATE = re.compile(r"\d{4}-\d{2}-\d{2}\Z")
CONTROL = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f]")


class ValidationError(RuntimeError):
    pass


def sha256_path(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def normalized(value: str) -> str:
    return " ".join(value.casefold().split())


def require_text(value: Any, location: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise ValidationError(f"{location}: expected non-blank text")
    if value != " ".join(value.split()):
        raise ValidationError(f"{location}: whitespace is not normalized")
    if CONTROL.search(value):
        raise ValidationError(f"{location}: contains a control character")
    return value


def validate_record(
    record: Any,
    line_number: int,
    seen_ui: set[str],
    seen_term: dict[str, str],
) -> Counter[str]:
    location = f"line {line_number}"
    if not isinstance(record, dict) or set(record) != EXPECTED_FIELDS:
        raise ValidationError(
            f"{location}: fields must be exactly {sorted(EXPECTED_FIELDS)}"
        )

    mesh_ui = require_text(record["meshUi"], f"{location}.meshUi")
    if not MESH_UI.fullmatch(mesh_ui):
        raise ValidationError(f"{location}: invalid MeSH UI {mesh_ui!r}")
    if mesh_ui in seen_ui:
        raise ValidationError(f"{location}: duplicate MeSH UI {mesh_ui}")
    seen_ui.add(mesh_ui)

    term = require_text(record["term"], f"{location}.term")
    if len(term) > QUALITY_BUDGETS["maxTermChars"]:
        raise ValidationError(f"{location}: preferred term exceeds the quality budget")
    term_key = normalized(term)
    if term_key in seen_term:
        raise ValidationError(
            f"{location}: duplicate term {term!r}; first at {seen_term[term_key]}"
        )
    seen_term[term_key] = mesh_ui

    scope_note = require_text(record["scopeNote"], f"{location}.scopeNote")
    if len(scope_note) > QUALITY_BUDGETS["maxScopeNoteChars"]:
        raise ValidationError(f"{location}: scope note exceeds the quality budget")
    introduced_year = record["introducedYear"]
    if introduced_year is not None and (
        not isinstance(introduced_year, int)
        or introduced_year < 1800
        or introduced_year > 2026
    ):
        raise ValidationError(f"{location}: invalid introducedYear")
    last_updated = record["lastUpdated"]
    if last_updated is not None and (
        not isinstance(last_updated, str) or not ISO_DATE.fullmatch(last_updated)
    ):
        raise ValidationError(f"{location}: invalid lastUpdated")

    tree_numbers = record["treeNumbers"]
    if not isinstance(tree_numbers, list) or not tree_numbers:
        raise ValidationError(f"{location}: treeNumbers must be non-empty")
    if len(tree_numbers) != len(set(tree_numbers)):
        raise ValidationError(f"{location}: duplicate tree number")
    if len(tree_numbers) > QUALITY_BUDGETS["maxTreeNumbersPerDescriptor"]:
        raise ValidationError(f"{location}: too many tree numbers")
    for tree_number in tree_numbers:
        if not isinstance(tree_number, str) or not TREE_NUMBER.fullmatch(tree_number):
            raise ValidationError(
                f"{location}: unsupported tree number {tree_number!r}"
            )
    selected_roots = {
        tree_number[0]
        for tree_number in tree_numbers
        if tree_number.count(".") + 1 <= 3
    }
    if not selected_roots.intersection(ROOTS):
        raise ValidationError(f"{location}: does not satisfy selection policy")

    category_roots = record["categoryRoots"]
    expected_roots = [
        root
        for root in ROOTS
        if any(tree_number.startswith(root) for tree_number in tree_numbers)
    ]
    if category_roots != expected_roots:
        raise ValidationError(
            f"{location}: categoryRoots mismatch; expected {expected_roots}"
        )

    synonyms = record["synonyms"]
    if not isinstance(synonyms, list):
        raise ValidationError(f"{location}: synonyms must be a list")
    if len(synonyms) > QUALITY_BUDGETS["maxSynonymsPerDescriptor"]:
        raise ValidationError(f"{location}: too many synonyms")
    seen_synonyms: set[str] = set()
    for index, synonym_value in enumerate(synonyms):
        synonym = require_text(
            synonym_value,
            f"{location}.synonyms[{index}]",
        )
        if "|" in synonym:
            raise ValidationError(f"{location}: synonym contains the storage delimiter")
        synonym_key = normalized(synonym)
        if synonym_key == term_key:
            raise ValidationError(f"{location}: preferred term repeated as synonym")
        if synonym_key in seen_synonyms:
            raise ValidationError(f"{location}: duplicate synonym {synonym!r}")
        seen_synonyms.add(synonym_key)

    return Counter(category_roots)


def validate(root: Path) -> tuple[int, Counter[str]]:
    manifest_path = root / "manifest.json"
    try:
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise ValidationError(f"Invalid manifest: {error}") from error

    if manifest.get("schemaVersion") != 1:
        raise ValidationError("manifest schemaVersion must be 1")
    if manifest.get("datasetId") != DATASET_ID:
        raise ValidationError("unexpected datasetId")
    if manifest.get("datasetVersion") != DATASET_VERSION:
        raise ValidationError("unexpected datasetVersion")
    payload = manifest.get("payload")
    if not isinstance(payload, dict):
        raise ValidationError("manifest payload is missing")
    payload_path = root / str(payload.get("file"))
    if payload.get("format") != "jsonl":
        raise ValidationError("payload format must be jsonl")
    if manifest.get("qualityBudgets") != QUALITY_BUDGETS:
        raise ValidationError("qualityBudgets differ from the accepted runtime budgets")
    if payload_path.stat().st_size != payload.get("bytes"):
        raise ValidationError("payload byte count mismatch")
    if payload_path.stat().st_size > QUALITY_BUDGETS["maxPayloadBytes"]:
        raise ValidationError("payload exceeds the byte budget")
    if sha256_path(payload_path) != payload.get("sha256"):
        raise ValidationError("payload SHA-256 mismatch")

    source = manifest.get("source")
    if not isinstance(source, dict):
        raise ValidationError("manifest source is missing")
    if source.get("productionYear") != 2026:
        raise ValidationError("source productionYear must be 2026")
    if source.get("endorsement") is not False:
        raise ValidationError("NLM endorsement must explicitly be false")
    if source.get("attribution") != "Courtesy of the U.S. National Library of Medicine":
        raise ValidationError("required NLM attribution is missing")
    if source.get("sourceArchiveSha256") != SOURCE_ARCHIVE_SHA256:
        raise ValidationError("source archive SHA-256 differs from the approved snapshot")
    if source.get("downloadUrl") != SOURCE_URL:
        raise ValidationError("source download URL differs from the approved NLM endpoint")
    if source.get("termsUrl") != TERMS_URL:
        raise ValidationError("source terms URL differs from the approved NLM endpoint")
    require_text(source.get("stalenessDisclosure"), "manifest.source.stalenessDisclosure")
    accessed_date = source.get("accessedDate")
    if not isinstance(accessed_date, str) or not ISO_DATE.fullmatch(accessed_date):
        raise ValidationError("manifest.source.accessedDate must be an ISO date")

    seen_ui: set[str] = set()
    seen_term: dict[str, str] = {}
    memberships: Counter[str] = Counter()
    with payload_path.open("r", encoding="utf-8", newline="") as stream:
        for line_number, line in enumerate(stream, start=1):
            if not line.endswith("\n"):
                raise ValidationError(f"line {line_number}: missing LF terminator")
            try:
                record = json.loads(line)
            except json.JSONDecodeError as error:
                raise ValidationError(
                    f"line {line_number}: invalid JSON ({error})"
                ) from error
            memberships.update(
                validate_record(record, line_number, seen_ui, seen_term)
            )

    expected_count = payload.get("count")
    if expected_count != EXPECTED_DESCRIPTOR_COUNT:
        raise ValidationError(
            "payload count differs from the approved dataset snapshot"
        )
    if expected_count > QUALITY_BUDGETS["maxDescriptors"]:
        raise ValidationError("payload exceeds the descriptor budget")
    if len(seen_ui) != expected_count:
        raise ValidationError(
            f"payload count mismatch: manifest={expected_count}, actual={len(seen_ui)}"
        )
    policy = manifest.get("selectionPolicy")
    if not isinstance(policy, dict):
        raise ValidationError("selectionPolicy is missing")
    if policy.get("maximumTreeDepth") != 3:
        raise ValidationError("maximumTreeDepth must be 3")
    expected_memberships = policy.get("categoryMembershipCounts")
    if expected_memberships != EXPECTED_MEMBERSHIPS:
        raise ValidationError(
            "category membership differs from the approved dataset snapshot"
        )
    actual_memberships = {root: memberships[root] for root in ROOTS}
    if expected_memberships != actual_memberships:
        raise ValidationError(
            "category membership mismatch: "
            f"manifest={expected_memberships}, actual={actual_memberships}"
        )
    return len(seen_ui), memberships


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "root",
        nargs="?",
        type=Path,
        default=Path("app/src/main/assets/mesh"),
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        count, memberships = validate(args.root.resolve())
    except (OSError, ValidationError) as error:
        print(f"MeSH reference validation failed: {error}")
        return 1
    summary = ", ".join(f"{root}={memberships[root]}" for root in ROOTS)
    print(
        f"MeSH reference validation passed: descriptors={count}; {summary}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
