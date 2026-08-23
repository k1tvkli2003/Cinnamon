#!/usr/bin/env python3
"""Build Cinnamon's deterministic NLM MeSH reference atlas.

The generated atlas is a reference corpus, not a replacement for Cinnamon's
deeply-authored learning entries. Source descriptors remain attributable to
NLM and are selected by a documented MeSH tree policy.
"""

from __future__ import annotations

import argparse
import gzip
import hashlib
import json
import os
import re
import tempfile
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path
from typing import Any, Iterable


GENERATOR_VERSION = 1
DATASET_ID = "cinnamon.mesh.reference"
DATASET_VERSION = "2026.1"
SOURCE_YEAR = 2026
SOURCE_URL = "https://nlmpubs.nlm.nih.gov/projects/mesh/MESH_FILES/xmlmesh/desc2026.gz"
TERMS_URL = "https://www.nlm.nih.gov/databases/download/terms_and_conditions.html"
SOURCE_ARCHIVE_SHA256 = (
    "9fe35b3170652376a592daf69e91a80d6c693ecaf9c571ceb701d04204cb357d"
)
ROOT_LABELS = {
    "A": "Anatomy",
    "C": "Diseases",
    "E": "Analytical, Diagnostic and Therapeutic Techniques and Equipment",
    "F": "Psychiatry and Psychology",
    "G": "Phenomena and Processes",
    "N": "Health Care",
}
SELECTED_ROOTS = tuple(ROOT_LABELS)
MAX_TREE_DEPTH = 3
QUALITY_BUDGETS = {
    "maxDescriptors": 6500,
    "maxPayloadBytes": 4_000_000,
    "maxScopeNoteChars": 1200,
    "maxSynonymsPerDescriptor": 64,
    "maxTermChars": 96,
    "maxTreeNumbersPerDescriptor": 16,
}
WHITESPACE = re.compile(r"\s+")


def normalized_text(value: str | None) -> str:
    return WHITESPACE.sub(" ", value or "").strip()


def sha256_path(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def tree_depth(tree_number: str) -> int:
    return tree_number.count(".") + 1


def selected_tree(tree_number: str) -> bool:
    return (
        bool(tree_number)
        and tree_number[0] in SELECTED_ROOTS
        and tree_depth(tree_number) <= MAX_TREE_DEPTH
    )


def unique_preserving_order(values: Iterable[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for value in values:
        key = value.casefold()
        if value and key not in seen:
            seen.add(key)
            result.append(value)
    return result


def preferred_scope(record: ET.Element) -> str:
    for concept in record.findall("./ConceptList/Concept"):
        if concept.get("PreferredConceptYN") == "Y":
            scope = normalized_text(concept.findtext("./ScopeNote"))
            if scope:
                return scope
    return normalized_text(record.findtext(".//ScopeNote"))


def synonyms(record: ET.Element, preferred_term: str) -> list[str]:
    values: list[str] = []
    for term in record.findall(".//ConceptList/Concept/TermList/Term"):
        if term.get("IsPermutedTermYN") == "Y":
            continue
        value = normalized_text(term.findtext("./String"))
        if value and value.casefold() != preferred_term.casefold():
            values.append(value)
    return unique_preserving_order(values)


def parse_record(record: ET.Element) -> dict[str, Any] | None:
    mesh_ui = normalized_text(record.findtext("./DescriptorUI"))
    term = normalized_text(record.findtext("./DescriptorName/String"))
    scope_note = preferred_scope(record)
    tree_numbers = unique_preserving_order(
        tree_number
        for tree_number in (
            normalized_text(node.text)
            for node in record.findall("./TreeNumberList/TreeNumber")
        )
        if tree_number[:1] in SELECTED_ROOTS
    )
    if not any(selected_tree(tree_number) for tree_number in tree_numbers):
        return None
    if not mesh_ui or not term or not scope_note:
        return None

    category_roots = [
        root
        for root in SELECTED_ROOTS
        if any(tree_number.startswith(root) for tree_number in tree_numbers)
    ]
    introduced = record.find("./DateIntroduced")
    introduced_year = normalized_text(
        introduced.findtext("./Year") if introduced is not None else None
    )
    last_updated = record.find("./LastUpdated")
    updated_parts = []
    if last_updated is not None:
        for child in ("Year", "Month", "Day"):
            value = normalized_text(last_updated.findtext(f"./{child}"))
            if value:
                updated_parts.append(value.zfill(2) if child != "Year" else value.zfill(4))

    return {
        "categoryRoots": category_roots,
        "introducedYear": int(introduced_year) if introduced_year.isdigit() else None,
        "lastUpdated": "-".join(updated_parts) if len(updated_parts) == 3 else None,
        "meshUi": mesh_ui,
        "scopeNote": scope_note,
        "synonyms": synonyms(record, term),
        "term": term,
        "treeNumbers": tree_numbers,
    }


def load_records(source: Path) -> list[dict[str, Any]]:
    records: list[dict[str, Any]] = []
    seen_ui: set[str] = set()
    seen_term: dict[str, str] = {}

    with gzip.open(source, "rb") as stream:
        for _, element in ET.iterparse(stream, events=("end",)):
            if element.tag != "DescriptorRecord":
                continue
            parsed = parse_record(element)
            element.clear()
            if parsed is None:
                continue
            mesh_ui = parsed["meshUi"]
            term_key = parsed["term"].casefold()
            if mesh_ui in seen_ui:
                raise ValueError(f"Duplicate MeSH UI in source selection: {mesh_ui}")
            if term_key in seen_term:
                raise ValueError(
                    f"Duplicate preferred descriptor term {parsed['term']!r}: "
                    f"{seen_term[term_key]} and {mesh_ui}"
                )
            seen_ui.add(mesh_ui)
            seen_term[term_key] = mesh_ui
            records.append(parsed)

    records.sort(key=lambda record: record["meshUi"])
    return records


def json_line(record: dict[str, Any]) -> str:
    return json.dumps(
        record,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    )


def write_atomic_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temp_name = tempfile.mkstemp(
        prefix=f".{path.name}.",
        suffix=".tmp",
        dir=path.parent,
        text=True,
    )
    try:
        with os.fdopen(descriptor, "w", encoding="utf-8", newline="\n") as stream:
            stream.write(content)
        os.replace(temp_name, path)
    except Exception:
        try:
            os.unlink(temp_name)
        except FileNotFoundError:
            pass
        raise


def build_manifest(
    source: Path,
    payload: Path,
    records: list[dict[str, Any]],
    accessed_date: str,
) -> dict[str, Any]:
    membership = Counter(
        root for record in records for root in record["categoryRoots"]
    )
    return {
        "datasetId": DATASET_ID,
        "datasetVersion": DATASET_VERSION,
        "generator": {
            "name": "scripts/build_mesh_reference.py",
            "version": GENERATOR_VERSION,
        },
        "payload": {
            "bytes": payload.stat().st_size,
            "count": len(records),
            "file": payload.name,
            "format": "jsonl",
            "sha256": sha256_path(payload),
        },
        "qualityBudgets": QUALITY_BUDGETS,
        "schemaVersion": 1,
        "selectionPolicy": {
            "categoryMembershipCounts": {
                root: membership[root] for root in SELECTED_ROOTS
            },
            "categoryRoots": [
                {"code": root, "label": ROOT_LABELS[root]}
                for root in SELECTED_ROOTS
            ],
            "maximumTreeDepth": MAX_TREE_DEPTH,
            "rule": (
                "Include a descriptor when at least one A/C/E/F/G/N MeSH "
                "tree number has depth <= 3 and the descriptor has a scope note."
            ),
        },
        "source": {
            "accessedDate": accessed_date,
            "attribution": "Courtesy of the U.S. National Library of Medicine",
            "downloadUrl": SOURCE_URL,
            "endorsement": False,
            "name": "Medical Subject Headings (MeSH)",
            "productionYear": SOURCE_YEAR,
            "sourceArchiveSha256": sha256_path(source),
            "stalenessDisclosure": (
                "This bundled reference reflects NLM MeSH 2026 and may not "
                "reflect later NLM updates."
            ),
            "termsUrl": TERMS_URL,
        },
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--source",
        type=Path,
        default=Path(".codex-tmp/desc2026.gz"),
    )
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(
            "app/src/main/assets/mesh/mesh-reference-2026.jsonl"
        ),
    )
    parser.add_argument(
        "--manifest",
        type=Path,
        default=Path("app/src/main/assets/mesh/manifest.json"),
    )
    parser.add_argument("--accessed-date", default="2026-07-23")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if not args.source.is_file():
        raise SystemExit(f"Source archive not found: {args.source}")
    source_sha256 = sha256_path(args.source)
    if source_sha256 != SOURCE_ARCHIVE_SHA256:
        raise SystemExit(
            "Source archive SHA-256 differs from the approved NLM MeSH 2026 "
            f"snapshot: expected={SOURCE_ARCHIVE_SHA256}, actual={source_sha256}"
        )

    records = load_records(args.source)
    payload_content = "".join(f"{json_line(record)}\n" for record in records)
    write_atomic_text(args.output, payload_content)

    manifest = build_manifest(
        source=args.source,
        payload=args.output,
        records=records,
        accessed_date=args.accessed_date,
    )
    manifest_content = json.dumps(
        manifest,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"
    write_atomic_text(args.manifest, manifest_content)

    print(
        f"Built {DATASET_ID}@{DATASET_VERSION}: "
        f"{len(records)} descriptors, {args.output.stat().st_size} bytes"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
