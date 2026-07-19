#!/usr/bin/env python3
"""Validate Cinnamon's focused Baseline Profile source and packaged artifacts."""

from __future__ import annotations

import argparse
from pathlib import Path
import re
import sys
from zipfile import BadZipFile, ZipFile


MIN_RULES = 100
MAX_RULES = 10_000
APP_RULE = re.compile(r"^[HSP]*Lcom/cinnamon/app/")
PACKAGED_PROFILE_PATHS = (
    "assets/dexopt/baseline.prof",
    "assets/dexopt/baseline.profm",
)


def validate_source(profile_path: Path) -> list[str]:
    if not profile_path.is_file():
        raise ValueError(f"Baseline Profile source is missing: {profile_path}")

    rules = [
        line.strip()
        for line in profile_path.read_text(encoding="utf-8-sig").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    ]
    if not MIN_RULES <= len(rules) <= MAX_RULES:
        raise ValueError(
            f"Expected {MIN_RULES}..{MAX_RULES} focused rules, found {len(rules)}"
        )

    duplicates = len(rules) - len(set(rules))
    if duplicates:
        raise ValueError(f"Baseline Profile contains {duplicates} duplicate rules")

    foreign_rules = [rule for rule in rules if not APP_RULE.match(rule)]
    if foreign_rules:
        preview = "\n".join(foreign_rules[:5])
        raise ValueError(
            "Baseline Profile must contain only Cinnamon-owned rules; "
            f"found {len(foreign_rules)} foreign rules:\n{preview}"
        )

    return rules


def validate_apk(apk_path: Path) -> dict[str, int]:
    if not apk_path.is_file():
        raise ValueError(f"Benchmark APK is missing: {apk_path}")

    try:
        with ZipFile(apk_path) as apk:
            entries = set(apk.namelist())
            missing = [path for path in PACKAGED_PROFILE_PATHS if path not in entries]
            if missing:
                raise ValueError(
                    "Benchmark APK does not package the compiled Baseline Profile: "
                    + ", ".join(missing)
                )
            sizes = {path: apk.getinfo(path).file_size for path in PACKAGED_PROFILE_PATHS}
    except BadZipFile as exc:
        raise ValueError(f"Benchmark APK is not a valid ZIP archive: {apk_path}") from exc

    undersized = {path: size for path, size in sizes.items() if size < 64}
    if undersized:
        raise ValueError(f"Packaged Baseline Profile entries are unexpectedly small: {undersized}")
    return sizes


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("profile", type=Path, help="Path to baseline-prof.txt")
    parser.add_argument(
        "--apk",
        type=Path,
        help="Optional benchmark APK whose compiled profile entries must be present",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        rules = validate_source(args.profile)
        print(f"Baseline Profile source verified: {len(rules)} Cinnamon-owned rules")
        if args.apk:
            sizes = validate_apk(args.apk)
            size_summary = ", ".join(f"{path}={size} bytes" for path, size in sizes.items())
            print(f"Benchmark APK profile verified: {size_summary}")
    except ValueError as exc:
        print(f"Baseline Profile verification failed: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
