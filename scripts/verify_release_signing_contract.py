#!/usr/bin/env python3
"""Keep release signing fail-closed without needing access to a keystore."""

from __future__ import annotations

import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
BUILD_FILE = ROOT / "app/build.gradle.kts"
REQUIRED = ("KEYSTORE_PATH", "STORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD")
FORBIDDEN = ("my-upload-key.jks", "keyAlias = \"upload\"", "?: \"${rootDir}")


def main() -> int:
    source = BUILD_FILE.read_text(encoding="utf-8")
    missing = [name for name in REQUIRED if name not in source]
    forbidden = [token for token in FORBIDDEN if token in source]
    if missing or forbidden:
        details = []
        if missing:
            details.append(f"missing required environment keys: {', '.join(missing)}")
        if forbidden:
            details.append(f"unsafe signing fallback: {', '.join(forbidden)}")
        raise SystemExit("Release signing contract failed: " + "; ".join(details))
    print("Release signing contract verified: release tasks require explicit environment-backed credentials.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
