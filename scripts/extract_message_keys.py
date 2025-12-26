#!/usr/bin/env python3
"""Extract message keys from a messages YAML file.

This is intentionally small and dependency-free.

It emits *leaf* keys only:
- keys whose value is on the same line (scalar)
- keys whose value is a list (e.g. `help:` followed by `- ...` lines)

It does not emit section keys (mapping-only nodes).

Examples:
    python scripts/extract_message_keys.py src/main/resources/messages/messages_en.yml

Output:
- By default prints one key per line, sorted, unique.
- Use --json to print a JSON array.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Optional


# Keep conservative: match the common Java identifier-like YAML keys used by this project.
YAML_KEY_RE = re.compile(r"^(?P<key>[A-Za-z0-9_\-]+)\s*:(?:\s+.*)?$")


def _read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8", errors="replace")

def extract_message_keys_from_yaml(text: str) -> set[str]:
    """Lightweight indentation-based YAML key flattener.

    Assumptions (fits this project's messages/*.yml):
    - indentation uses whitespace; we treat tabs as 2 spaces
    - mapping keys are simple identifiers (letters/numbers/_/-)
    - list items start with '-'

    Returns only *leaf* keys (scalar or list), not pure section keys.
    """

    lines = text.splitlines()
    keys: set[str] = set()

    stack: list[str] = []
    indents: list[int] = []

    def next_significant_line(start_index: int) -> Optional[tuple[int, str, int]]:
        for j in range(start_index, len(lines)):
            raw = lines[j].rstrip("\r\n")
            stripped = raw.strip()
            if not stripped or stripped.startswith("#"):
                continue
            raw_expanded = raw.expandtabs(2)
            indent_j = len(raw_expanded) - len(raw_expanded.lstrip(" "))
            return j, stripped, indent_j
        return None

    for i, raw_line in enumerate(lines):
        line = raw_line.rstrip("\r\n").expandtabs(2)
        stripped = line.strip()

        if not stripped or stripped.startswith("#"):
            continue
        if stripped.startswith("-"):
            continue

        indent = len(line) - len(line.lstrip(" "))

        match = YAML_KEY_RE.match(stripped)
        if not match:
            continue

        key = match.group("key")

        # Extract whether this key is a scalar leaf on the same line.
        # Example: prefix: "..."  -> leaf
        # Example: command:        -> could be section or list/scalar on following lines
        colon_index = stripped.find(":")
        value_part = stripped[colon_index + 1 :].strip() if colon_index >= 0 else ""

        # Pop to the correct parent indentation level.
        while indents and indent <= indents[-1]:
            indents.pop()
            stack.pop()

        stack.append(key)
        indents.append(indent)

        full_key = ".".join(stack)

        if value_part:
            keys.add(full_key)
            continue

        nxt = next_significant_line(i + 1)
        if nxt is None:
            continue

        _, nxt_stripped, nxt_indent = nxt
        if nxt_indent <= indent:
            # No children; ignore.
            continue

        if nxt_stripped.startswith("-"):
            # List value.
            keys.add(full_key)
            continue

        # Otherwise it's a nested mapping section; do not add.

    return keys


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Extract message keys from a messages YAML file")
    parser.add_argument("file", help="Path to a .yml/.yaml messages file")
    parser.add_argument("--json", action="store_true", help="Print as JSON array")

    args = parser.parse_args(argv)

    path = Path(args.file)
    if not path.exists() or not path.is_file():
        print(f"File not found: {path}", file=sys.stderr)
        return 2

    if path.suffix.lower() not in (".yml", ".yaml"):
        print("This simplified script only supports .yml/.yaml messages files.", file=sys.stderr)
        return 2

    text = _read_text(path)
    out_keys = sorted(extract_message_keys_from_yaml(text))

    if args.json:
        print(json.dumps(out_keys, indent=2, ensure_ascii=False))
    else:
        for k in out_keys:
            print(k)

    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
