#!/usr/bin/env python3
"""Fail closed when Android source or CI reintroduces a provider credential path.

This validator deliberately reports only filenames and rule names. It never prints a
matching line, which keeps accidental credential discovery out of CI logs.
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path


RULES: tuple[tuple[str, re.Pattern[str]], ...] = (
    ("provider credential identifier", re.compile(r"\b(?:AVALAI|GEMINI)_API_KEY\b")),
    ("direct provider host", re.compile(r"\bapi\.avalai\.ir\b", re.IGNORECASE)),
    ("provider secret BuildConfig field", re.compile(r"BuildConfig\.[A-Za-z0-9_]*(?:KEY|TOKEN|SECRET)\b")),
    ("body-level HTTP logging", re.compile(r"HttpLoggingInterceptor\.Level\.BODY")),
    ("Secrets Gradle plugin", re.compile(r"mapsplatform\.secrets-gradle-plugin")),
)
GENERATED_DIRECTORY_NAMES = frozenset({"build", ".gradle", ".idea"})
TRACKED_SIGNING_ARTIFACT = re.compile(
    r"(?:^|/)(?:debug\.keystore(?:\.base64)?|[^/]+\.(?:jks|keystore|p12|pfx))$",
    re.IGNORECASE,
)


def candidate_files(root: Path) -> list[Path]:
    candidates: list[Path] = []
    for relative in (Path("app"), Path(".github"), Path("build.gradle.kts"), Path("gradle")):
        path = root / relative
        if path.is_file():
            candidates.append(path)
        elif path.is_dir():
            candidates.extend(
                child
                for child in path.rglob("*")
                if child.is_file() and child.suffix in {".kt", ".kts", ".toml", ".yml", ".yaml", ".xml"}
                and not any(part in GENERATED_DIRECTORY_NAMES for part in child.relative_to(root).parts)
            )
    return candidates


def tracked_signing_artifacts(root: Path) -> list[tuple[Path, str]]:
    """Inspect Git's index by filename only; never read or print key material."""
    if not (root / ".git").exists():
        return []
    result = subprocess.run(
        ["git", "ls-files", "-z"],
        cwd=root,
        check=False,
        capture_output=True,
        text=False,
    )
    if result.returncode != 0:
        return [(Path(".git"), "could not inspect tracked files for signing artifacts")]
    failures: list[tuple[Path, str]] = []
    for raw in result.stdout.split(b"\0"):
        if not raw:
            continue
        relative = Path(raw.decode("utf-8", errors="surrogateescape"))
        if TRACKED_SIGNING_ARTIFACT.search(relative.as_posix()) and (root / relative).is_file():
            failures.append((relative, "tracked signing artifact"))
    return failures


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", nargs="?", type=Path, default=Path.cwd())
    args = parser.parse_args()
    root = args.root.resolve()

    failures: list[tuple[Path, str]] = []
    legacy_env = root / ".env.example"
    if legacy_env.exists():
        failures.append((legacy_env, "tracked environment-template file"))
    failures.extend(tracked_signing_artifacts(root))

    for path in candidate_files(root):
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        for rule_name, pattern in RULES:
            if pattern.search(text):
                failures.append((path, rule_name))

    if failures:
        print("Mobile secret-boundary validation failed:")
        for path, reason in sorted(set(failures)):
            print(f"- {path.relative_to(root)}: {reason}")
        return 1

    print("Mobile secret-boundary validation passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
