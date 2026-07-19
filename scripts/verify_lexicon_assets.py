#!/usr/bin/env python3
"""Validate Cinnamon's bundled lexicon as a versioned, reproducible dataset."""

from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path
from typing import Any


REQUIRED_FIELDS: dict[str, tuple[str, ...]] = {
    "entries": (
        "term",
        "ipa",
        "pos",
        "domain",
        "topic",
        "level",
        "definition",
        "plain",
        "exampleClinical",
        "exampleCasual",
        "usageNote",
    ),
    "morphemes": ("form", "kind", "meaning", "origin", "examples"),
    "abbreviations": ("short", "expansion", "context"),
    "phrases": ("phrase", "category", "meaning", "example", "register"),
    "confusables": ("a", "b", "howToTell", "exampleA", "exampleB"),
    "sentences": ("text", "domain", "level"),
}

ALLOWED_VALUES: dict[str, dict[str, set[str]]] = {
    "entries": {
        "domain": {"clinical", "general"},
        "level": {"B2+", "C1", "C2"},
        "pos": {"adjective", "adverb", "noun", "phrase", "verb"},
    },
    "morphemes": {"kind": {"prefix", "root", "suffix"}},
    "phrases": {
        "register": {"casual", "formal", "neutral"},
        "category": {
            "Academic Writing",
            "Colleague Talk",
            "Natural Conversation",
            "Patient Communication",
            "Rounds & Presenting",
        },
    },
    "sentences": {
        "domain": {"clinical", "general"},
        "level": {"C1", "C2"},
    },
}

ALLOWED_PROVENANCE_ORIGINS = {"project-bundled-curated"}
ALLOWED_REVIEW_STATUSES = {
    "editorial-review-required-before-release",
    "editorial-approved",
}


class DatasetValidationError(RuntimeError):
    pass


def normalized(value: Any) -> str:
    return " ".join(str(value).strip().casefold().split())


def read_json(path: Path) -> Any:
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeError, json.JSONDecodeError) as exc:
        raise DatasetValidationError(f"{path.name}: invalid UTF-8 JSON ({exc})") from exc


def require_non_empty(item: dict[str, Any], field: str, location: str) -> None:
    if field not in item:
        raise DatasetValidationError(f"{location}: missing required field '{field}'")
    value = item[field]
    if value is None or value == "" or value == []:
        raise DatasetValidationError(f"{location}: required field '{field}' is empty")
    if isinstance(value, str) and not value.strip():
        raise DatasetValidationError(f"{location}: required field '{field}' is blank")


def identity_for(collection: str, item: dict[str, Any]) -> str:
    if collection == "entries":
        return normalized(item["term"])
    if collection == "morphemes":
        return f"{normalized(item['kind'])}:{normalized(item['form'])}"
    if collection == "abbreviations":
        return normalized(item["short"])
    if collection == "phrases":
        return normalized(item["phrase"])
    if collection == "confusables":
        return "|".join(sorted((normalized(item["a"]), normalized(item["b"]))))
    if collection == "sentences":
        return normalized(item["text"])
    raise DatasetValidationError(f"Unsupported collection '{collection}'")


def validate_item(collection: str, item: Any, location: str) -> dict[str, Any]:
    if not isinstance(item, dict):
        raise DatasetValidationError(f"{location}: expected an object")
    for field in REQUIRED_FIELDS[collection]:
        require_non_empty(item, field, location)
    for field, allowed in ALLOWED_VALUES.get(collection, {}).items():
        value = item.get(field)
        if value not in allowed:
            raise DatasetValidationError(
                f"{location}: unsupported {field}={value!r}; allowed={sorted(allowed)}"
            )
    return item


def validate_provenance(manifest: dict[str, Any]) -> None:
    provenance = manifest.get("provenance")
    if not isinstance(provenance, dict):
        raise DatasetValidationError("manifest.json: provenance must be an object")

    origin = provenance.get("origin")
    review_status = provenance.get("reviewStatus")
    if origin not in ALLOWED_PROVENANCE_ORIGINS:
        raise DatasetValidationError(
            "manifest.json: unsupported provenance origin "
            f"{origin!r}; allowed={sorted(ALLOWED_PROVENANCE_ORIGINS)}"
        )
    if review_status not in ALLOWED_REVIEW_STATUSES:
        raise DatasetValidationError(
            "manifest.json: unsupported provenance reviewStatus "
            f"{review_status!r}; allowed={sorted(ALLOWED_REVIEW_STATUSES)}"
        )


def validate(root: Path) -> dict[str, int]:
    manifest_path = root / "manifest.json"
    manifest = read_json(manifest_path)
    if not isinstance(manifest, dict) or manifest.get("schemaVersion") != 1:
        raise DatasetValidationError("manifest.json: schemaVersion must be 1")
    if manifest.get("datasetId") != "cinnamon.lexicon.core":
        raise DatasetValidationError("manifest.json: unexpected datasetId")
    validate_provenance(manifest)
    file_specs = manifest.get("files")
    if not isinstance(file_specs, list) or not file_specs:
        raise DatasetValidationError("manifest.json: files must be a non-empty list")

    expected_names = {spec.get("name") for spec in file_specs if isinstance(spec, dict)}
    actual_names = {path.name for path in root.glob("*.json") if path.name != "manifest.json"}
    if expected_names != actual_names:
        raise DatasetValidationError(
            f"manifest/file mismatch: missing={sorted(expected_names - actual_names)}, "
            f"untracked={sorted(actual_names - expected_names)}"
        )

    seen: dict[str, dict[str, str]] = {collection: {} for collection in REQUIRED_FIELDS}
    totals: dict[str, int] = {collection: 0 for collection in REQUIRED_FIELDS}

    for spec in file_specs:
        if not isinstance(spec, dict):
            raise DatasetValidationError("manifest.json: every file entry must be an object")
        name = spec.get("name")
        collection = spec.get("collection")
        expected_count = spec.get("count")
        expected_hash = spec.get("sha256")
        if collection not in REQUIRED_FIELDS:
            raise DatasetValidationError(f"manifest.json: unsupported collection {collection!r}")
        if not isinstance(name, str) or Path(name).name != name:
            raise DatasetValidationError(f"manifest.json: invalid file name {name!r}")
        path = root / name
        # Git may materialize CRLF on Windows and LF in Linux CI. The dataset
        # identity is content-sensitive but line-ending-neutral.
        payload = path.read_bytes().replace(b"\r\n", b"\n")
        actual_hash = hashlib.sha256(payload).hexdigest()
        if actual_hash != expected_hash:
            raise DatasetValidationError(f"{name}: SHA-256 mismatch; update only after editorial review")

        document = read_json(path)
        if not isinstance(document, dict) or set(document) != {collection}:
            raise DatasetValidationError(f"{name}: root object must contain only '{collection}'")
        items = document[collection]
        if not isinstance(items, list):
            raise DatasetValidationError(f"{name}: '{collection}' must be a list")
        if len(items) != expected_count:
            raise DatasetValidationError(
                f"{name}: expected {expected_count} items from manifest, found {len(items)}"
            )

        for index, raw_item in enumerate(items):
            location = f"{name}:{index}"
            item = validate_item(collection, raw_item, location)
            identity = identity_for(collection, item)
            if not identity:
                raise DatasetValidationError(f"{location}: empty normalized identity")
            previous = seen[collection].get(identity)
            if previous is not None:
                raise DatasetValidationError(
                    f"{location}: duplicate identity also present at {previous}"
                )
            seen[collection][identity] = location
        totals[collection] += len(items)

    return totals


def main(argv: list[str]) -> int:
    root = Path(argv[1] if len(argv) > 1 else "app/src/main/assets/lexicon").resolve()
    try:
        totals = validate(root)
    except (DatasetValidationError, OSError) as exc:
        print(f"Lexicon dataset validation failed: {exc}", file=sys.stderr)
        return 1
    summary = ", ".join(f"{name}={count}" for name, count in sorted(totals.items()))
    print(f"Lexicon dataset validation passed: {summary}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
