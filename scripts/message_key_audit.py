#!/usr/bin/env python3
"""Message key auditor for AdvancedHunt.

Scans Java source for MessageManager key usages (including GUI/menu strings),
parses messages/messages_en.yml, and compares the sets.

Outputs:
- missing_in_yaml: keys referenced in code (certain literals) but absent in YAML
- unused_in_code: keys present in YAML but never referenced as certain literals
- pattern_usages: concatenated keys expressed as wildcard patterns (needs review)
- uncertain_usages: dynamic/non-literal key expressions (needs review)

This is intentionally conservative: only *string-literal* keys are treated as
certain. Anything else is shown as uncertain/pattern so you can decide.

Requirements:
- Python 3.9+
- PyYAML (pip install pyyaml)

Usage examples:
  python scripts/message_key_audit.py
  python scripts/message_key_audit.py --json report.json
  python scripts/message_key_audit.py --root . --yaml src/main/resources/messages/messages_en.yml
"""

from __future__ import annotations

import argparse
import bisect
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable, Optional


METHOD_NAMES = ("getMessage", "getMessageList")

# The MessageManager contains method declarations like getMessage(String key, ...)
# which are not real key usages and would otherwise show up as "uncertain".
IGNORE_UNCERTAIN_FILE_SUFFIXES = (
    "src/main/java/de/theredend2000/advancedhunt/managers/MessageManager.java",
)


JAVA_STRING_LITERAL_RE = re.compile(r'"(?:\\.|[^"\\])*"', re.DOTALL)


@dataclass(frozen=True)
class UsageSite:
    file: str
    line: int
    method: str
    arg_expr: str


@dataclass(frozen=True)
class CertainKeyUsage:
    key: str
    site: UsageSite


@dataclass(frozen=True)
class PatternKeyUsage:
    pattern: str
    site: UsageSite


@dataclass(frozen=True)
class UncertainKeyUsage:
    site: UsageSite


@dataclass(frozen=True)
class YamlKeyInfo:
    key: str
    value_type: str  # "scalar" or "list"


def _build_line_index(text: str) -> list[int]:
    # Returns list of indices where each line starts (0-based).
    # line_starts[i] is the index of the first char of line i+1.
    line_starts = [0]
    for match in re.finditer(r"\n", text):
        line_starts.append(match.end())
    return line_starts


def _offset_to_line(line_starts: list[int], offset: int) -> int:
    # 1-based line number
    return bisect.bisect_right(line_starts, offset)


def _strip_java_casts(expr: str) -> str:
    # Remove leading casts like (String) "foo" or ((String)) "foo".
    s = expr.strip()
    while True:
        m = re.match(r"^\(\s*[A-Za-z0-9_$.<>\[\]?]+\s*\)\s*(.+)$", s)
        if not m:
            break
        s = m.group(1).strip()
    return s


def _strip_wrapping_parens(expr: str) -> str:
    s = expr.strip()
    while s.startswith("(") and s.endswith(")"):
        inner = s[1:-1].strip()
        # Only strip if parentheses are balanced at top-level.
        if _find_matching_paren(s, 0) != len(s) - 1:
            break
        s = inner
    return s


def _parse_java_string_literal(expr: str) -> Optional[str]:
    s = _strip_wrapping_parens(_strip_java_casts(expr))
    if len(s) < 2 or not (s.startswith('"') and s.endswith('"')):
        return None

    # Validate/parse escapes minimally; keep raw value.
    # We don't need full Java escape semantics; we just want the literal content.
    content = s[1:-1]
    # Disallow unescaped quotes inside.
    i = 0
    while i < len(content):
        ch = content[i]
        if ch == "\\":
            i += 2
            continue
        if ch == '"':
            return None
        i += 1
    # Unescape common sequences for readability, but keep unknown escapes as-is.
    return bytes(content, "utf-8").decode("unicode_escape")


def _extract_java_string_literals(text: str) -> set[str]:
    out: set[str] = set()
    for m in JAVA_STRING_LITERAL_RE.finditer(text):
        # Includes the surrounding quotes.
        lit = _parse_java_string_literal(m.group(0))
        if lit is not None:
            out.add(lit)
    return out


def _find_matching_paren(text: str, open_paren_index: int) -> int:
    if open_paren_index < 0 or open_paren_index >= len(text) or text[open_paren_index] != "(":
        raise ValueError("open_paren_index must point to '('")

    depth = 0
    in_string = False
    in_char = False
    escaped = False

    for i in range(open_paren_index, len(text)):
        ch = text[i]

        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            continue

        if in_char:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == "'":
                in_char = False
            continue

        if ch == '"':
            in_string = True
            continue
        if ch == "'":
            in_char = True
            continue

        if ch == "(":
            depth += 1
            continue
        if ch == ")":
            depth -= 1
            if depth == 0:
                return i
            continue

    return -1


def _split_top_level(text: str, separator: str) -> list[str]:
    # Splits on separator at top-level only (not inside (), [], {}, strings).
    parts: list[str] = []
    current: list[str] = []

    depth_paren = 0
    depth_brack = 0
    depth_brace = 0
    in_string = False
    in_char = False
    escaped = False

    for ch in text:
        if in_string:
            current.append(ch)
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            continue

        if in_char:
            current.append(ch)
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == "'":
                in_char = False
            continue

        if ch == '"':
            in_string = True
            current.append(ch)
            continue
        if ch == "'":
            in_char = True
            current.append(ch)
            continue

        if ch == "(":
            depth_paren += 1
            current.append(ch)
            continue
        if ch == ")":
            depth_paren = max(0, depth_paren - 1)
            current.append(ch)
            continue
        if ch == "[":
            depth_brack += 1
            current.append(ch)
            continue
        if ch == "]":
            depth_brack = max(0, depth_brack - 1)
            current.append(ch)
            continue
        if ch == "{":
            depth_brace += 1
            current.append(ch)
            continue
        if ch == "}":
            depth_brace = max(0, depth_brace - 1)
            current.append(ch)
            continue

        if (
            ch == separator
            and depth_paren == 0
            and depth_brack == 0
            and depth_brace == 0
            and not in_string
            and not in_char
        ):
            parts.append("".join(current).strip())
            current = []
            continue

        current.append(ch)

    parts.append("".join(current).strip())
    return [p for p in parts if p != ""]


def _expr_to_key_or_pattern(expr: str) -> tuple[str, str]:
    """Returns (kind, value) where kind is 'certain'|'pattern'|'uncertain'."""
    literal = _parse_java_string_literal(expr)
    if literal is not None:
        return "certain", literal

    # Handle top-level concatenation: a + b + c
    parts = _split_top_level(expr, "+")
    if len(parts) <= 1:
        return "uncertain", expr.strip()

    pattern_parts: list[str] = []
    saw_placeholder = False
    saw_literal = False

    for part in parts:
        lit = _parse_java_string_literal(part)
        if lit is not None:
            saw_literal = True
            pattern_parts.append(lit)
        else:
            saw_placeholder = True
            # Collapse multiple unknown segments into one '*'
            if not pattern_parts or pattern_parts[-1] != "*":
                pattern_parts.append("*")

    if not saw_literal:
        return "uncertain", expr.strip()

    if saw_placeholder:
        # Merge adjacent literals around '*' naturally.
        pattern = "".join(pattern_parts)
        pattern = re.sub(r"\*+", "*", pattern)
        return "pattern", pattern

    # Technically possible (no placeholders) but we would have matched literal above.
    return "uncertain", expr.strip()


def _extract_method_calls(text: str, rel_file: str) -> tuple[list[CertainKeyUsage], list[PatternKeyUsage], list[UncertainKeyUsage]]:
    certain: list[CertainKeyUsage] = []
    patterns: list[PatternKeyUsage] = []
    uncertain: list[UncertainKeyUsage] = []

    line_starts = _build_line_index(text)

    # Find method occurrences and parse the argument list.
    method_re = re.compile(r"\b(" + "|".join(map(re.escape, METHOD_NAMES)) + r")\s*\(")

    for match in method_re.finditer(text):
        method = match.group(1)
        open_paren_index = match.end() - 1
        close_paren_index = _find_matching_paren(text, open_paren_index)
        if close_paren_index == -1:
            # Unbalanced; skip but still report as uncertain.
            site = UsageSite(rel_file, _offset_to_line(line_starts, match.start()), method, "<unbalanced parentheses>")
            uncertain.append(UncertainKeyUsage(site))
            continue

        arg_list = text[open_paren_index + 1 : close_paren_index]
        args = _split_top_level(arg_list, ",")
        if not args:
            continue

        arg_expr = args[0]
        site = UsageSite(rel_file, _offset_to_line(line_starts, match.start()), method, arg_expr.strip())
        kind, value = _expr_to_key_or_pattern(arg_expr)

        if kind == "certain":
            certain.append(CertainKeyUsage(key=value, site=site))
        elif kind == "pattern":
            patterns.append(PatternKeyUsage(pattern=value, site=site))
        else:
            uncertain.append(UncertainKeyUsage(site))

    return certain, patterns, uncertain


def _iter_java_files(java_root: Path) -> Iterable[Path]:
    yield from java_root.rglob("*.java")


def _flatten_yaml_keys(data: Any) -> list[YamlKeyInfo]:
    out: list[YamlKeyInfo] = []

    def walk(node: Any, path: list[str]) -> None:
        if isinstance(node, dict):
            for k, v in node.items():
                # Only string keys are meaningful for Bukkit config.
                walk(v, [*path, str(k)])
            return

        if isinstance(node, list):
            out.append(YamlKeyInfo(key=".".join(path), value_type="list"))
            return

        # Scalars (str/int/bool/None/float)
        out.append(YamlKeyInfo(key=".".join(path), value_type="scalar"))

    if not isinstance(data, dict):
        return out

    walk(data, [])
    return [k for k in out if k.key != ""]


def _read_text(path: Path) -> str:
    # Java + YAML are UTF-8 in this repo; be tolerant.
    return path.read_text(encoding="utf-8", errors="replace")


def _load_yaml(path: Path) -> Any:
    try:
        import yaml  # type: ignore
    except Exception:
        raise RuntimeError(
            "PyYAML is required. Install it with: pip install pyyaml"
        )

    with path.open("r", encoding="utf-8", errors="replace") as f:
        return yaml.safe_load(f)


def _write_json(path: Path, obj: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Audit message keys used in code vs messages_en.yml")
    parser.add_argument(
        "--root",
        type=Path,
        default=Path("."),
        help="Project root (default: .)",
    )
    parser.add_argument(
        "--java",
        type=Path,
        default=None,
        help="Java source root (default: <root>/src/main/java)",
    )
    parser.add_argument(
        "--yaml",
        type=Path,
        default=None,
        help="YAML file to compare (default: <root>/src/main/resources/messages/messages_en.yml)",
    )
    parser.add_argument(
        "--json",
        type=Path,
        default=None,
        help="Write a JSON report to this path (optional)",
    )

    args = parser.parse_args(argv)

    root: Path = args.root.resolve()
    java_root = (args.java or root / "src" / "main" / "java").resolve()
    yaml_path = (args.yaml or root / "src" / "main" / "resources" / "messages" / "messages_en.yml").resolve()

    if not java_root.exists() or not java_root.is_dir():
        print(f"ERROR: Java root not found: {java_root}")
        return 2

    if not yaml_path.exists() or not yaml_path.is_file():
        print(f"ERROR: YAML file not found: {yaml_path}")
        return 2

    # 1) Scan code
    all_certain: list[CertainKeyUsage] = []
    all_patterns: list[PatternKeyUsage] = []
    all_uncertain: list[UncertainKeyUsage] = []

    # For classifying “unused in message lookups” YAML keys:
    # - exact matches found in ANY Java string literal
    # - fallback substring matches found anywhere in Java source
    all_string_literals: set[str] = set()
    all_code_text_parts: list[str] = []

    java_files = list(_iter_java_files(java_root))
    for file_path in java_files:
        rel_file = file_path.relative_to(root).as_posix()
        text = _read_text(file_path)
        all_code_text_parts.append(text)
        all_string_literals |= _extract_java_string_literals(text)
        certain, patterns, uncertain = _extract_method_calls(text, rel_file)
        all_certain.extend(certain)
        all_patterns.extend(patterns)
        all_uncertain.extend(uncertain)

    # Whitelist-ignore uncertain sites from MessageManager internals.
    all_uncertain = [
        u
        for u in all_uncertain
        if not any(u.site.file.endswith(suffix) for suffix in IGNORE_UNCERTAIN_FILE_SUFFIXES)
    ]

    certain_keys: dict[str, list[UsageSite]] = {}
    for usage in all_certain:
        certain_keys.setdefault(usage.key, []).append(usage.site)

    # 2) Parse YAML
    try:
        yaml_data = _load_yaml(yaml_path)
    except RuntimeError as e:
        print(f"ERROR: {e}")
        return 2

    yaml_keys_info = _flatten_yaml_keys(yaml_data)
    yaml_key_types: dict[str, str] = {}
    duplicates: list[str] = []
    for info in yaml_keys_info:
        if info.key in yaml_key_types and yaml_key_types[info.key] != info.value_type:
            duplicates.append(info.key)
        yaml_key_types[info.key] = info.value_type

    yaml_keys = set(yaml_key_types.keys())

    # 3) Compare
    missing_in_yaml = sorted([k for k in certain_keys.keys() if k not in yaml_keys])
    # "Unused" here means: not referenced as a *certain literal* key in getMessage/getMessageList.
    unused_in_message_lookups = sorted([k for k in yaml_keys if k not in certain_keys])

    # Further classify unused YAML keys by presence anywhere in code.
    # 1) Found as an exact Java string literal
    unused_but_found_as_string_literal: list[str] = []
    # 2) Not an exact literal, but found as a substring in code text
    unused_but_found_in_code_text: list[str] = []
    # 3) Not found anywhere in code
    unused_not_found_anywhere: list[str] = []

    code_blob = "\n".join(all_code_text_parts)
    for key in unused_in_message_lookups:
        if key in all_string_literals:
            unused_but_found_as_string_literal.append(key)
        elif key in code_blob:
            unused_but_found_in_code_text.append(key)
        else:
            unused_not_found_anywhere.append(key)

    type_mismatches: list[dict[str, Any]] = []
    for key, sites in certain_keys.items():
        expected = "list" if any(s.method == "getMessageList" for s in sites) else "scalar"
        actual = yaml_key_types.get(key)
        if actual is None:
            continue
        if expected == "list" and actual == "scalar":
            type_mismatches.append(
                {
                    "key": key,
                    "expected": expected,
                    "actual": actual,
                    "example_site": {
                        "file": sites[0].file,
                        "line": sites[0].line,
                        "method": sites[0].method,
                    },
                }
            )

    # 4) Print report
    print("Message Key Audit")
    print("=================")
    print(f"Project root: {root}")
    print(f"Java root:    {java_root}")
    print(f"YAML file:    {yaml_path}")
    print()

    print("Summary")
    print("-------")
    print(f"Java files scanned:          {len(java_files)}")
    print(f"Total key usages found:      {len(all_certain) + len(all_patterns) + len(all_uncertain)}")
    print(f"Unique certain keys (code):  {len(certain_keys)}")
    print(f"Pattern key usages:          {len(all_patterns)}")
    print(f"Uncertain/dynamic usages:    {len(all_uncertain)}")
    print(f"YAML keys (flattened):       {len(yaml_keys)}")
    print(f"Missing in YAML (certain):   {len(missing_in_yaml)}")
    print(f"Unused in message lookups:   {len(unused_in_message_lookups)}")
    print(f"- found as string literal:   {len(unused_but_found_as_string_literal)}")
    print(f"- found in code text:        {len(unused_but_found_in_code_text)}")
    print(f"- not found anywhere:        {len(unused_not_found_anywhere)}")
    if duplicates:
        print(f"YAML duplicate/type-collisions: {len(duplicates)}")
    print()

    if missing_in_yaml:
        print("Missing in YAML (certain keys)")
        print("-----------------------------")
        for key in missing_in_yaml:
            site = certain_keys[key][0]
            print(f"- {key}  ({site.file}:{site.line})")
        print()

    if unused_but_found_as_string_literal:
        print("YAML keys not used in message lookups, but found as Java string literals")
        print("-----------------------------------------------------------------------")
        max_show = 250
        for key in unused_but_found_as_string_literal[:max_show]:
            print(f"- {key}")
        if len(unused_but_found_as_string_literal) > max_show:
            print(f"... and {len(unused_but_found_as_string_literal) - max_show} more")
        print()

    if unused_but_found_in_code_text:
        print("YAML keys not used in message lookups, but found somewhere in code text")
        print("---------------------------------------------------------------------")
        max_show = 250
        for key in unused_but_found_in_code_text[:max_show]:
            print(f"- {key}")
        if len(unused_but_found_in_code_text) > max_show:
            print(f"... and {len(unused_but_found_in_code_text) - max_show} more")
        print()

    if unused_not_found_anywhere:
        print("YAML keys not found anywhere in code")
        print("----------------------------------")
        max_show = 250
        for key in unused_not_found_anywhere[:max_show]:
            print(f"- {key}")
        if len(unused_not_found_anywhere) > max_show:
            print(f"... and {len(unused_not_found_anywhere) - max_show} more")
        print()

    if type_mismatches:
        print("Warnings: getMessageList used but YAML is scalar")
        print("-----------------------------------------------")
        for w in type_mismatches:
            site = w["example_site"]
            print(f"- {w['key']}  (expected list, YAML scalar)  ({site['file']}:{site['line']})")
        print()

    if all_patterns:
        print("Pattern key usages (needs review)")
        print("--------------------------------")
        for p in all_patterns:
            s = p.site
            print(f"- {p.pattern}  ({s.file}:{s.line})")
            print(f"  expr: {s.arg_expr}")
        print()

    if all_uncertain:
        print("Uncertain/dynamic key usages (needs review)")
        print("------------------------------------------")
        max_show = 250
        for u in all_uncertain[:max_show]:
            s = u.site
            print(f"- ({s.method}) {s.file}:{s.line}")
            print(f"  expr: {s.arg_expr}")
        if len(all_uncertain) > max_show:
            print(f"... and {len(all_uncertain) - max_show} more")
        print()

    if args.json is not None:
        report = {
            "root": str(root),
            "java_root": str(java_root),
            "yaml_file": str(yaml_path),
            "summary": {
                "java_files_scanned": len(java_files),
                "total_usages": len(all_certain) + len(all_patterns) + len(all_uncertain),
                "unique_certain_keys": len(certain_keys),
                "pattern_usages": len(all_patterns),
                "uncertain_usages": len(all_uncertain),
                "yaml_keys": len(yaml_keys),
                "missing_in_yaml": len(missing_in_yaml),
                "unused_in_message_lookups": len(unused_in_message_lookups),
                "unused_found_as_string_literal": len(unused_but_found_as_string_literal),
                "unused_found_in_code_text": len(unused_but_found_in_code_text),
                "unused_not_found_anywhere": len(unused_not_found_anywhere),
            },
            "missing_in_yaml": [
                {
                    "key": k,
                    "example_site": {
                        "file": certain_keys[k][0].file,
                        "line": certain_keys[k][0].line,
                        "method": certain_keys[k][0].method,
                    },
                }
                for k in missing_in_yaml
            ],
            "unused_in_message_lookups": unused_in_message_lookups,
            "unused_found_as_string_literal": unused_but_found_as_string_literal,
            "unused_found_in_code_text": unused_but_found_in_code_text,
            "unused_not_found_anywhere": unused_not_found_anywhere,
            "pattern_usages": [
                {
                    "pattern": p.pattern,
                    "site": {
                        "file": p.site.file,
                        "line": p.site.line,
                        "method": p.site.method,
                    },
                    "expr": p.site.arg_expr,
                }
                for p in all_patterns
            ],
            "uncertain_usages": [
                {
                    "site": {
                        "file": u.site.file,
                        "line": u.site.line,
                        "method": u.site.method,
                    },
                    "expr": u.site.arg_expr,
                }
                for u in all_uncertain
            ],
            "type_mismatches": type_mismatches,
            "yaml_duplicates_or_type_collisions": sorted(set(duplicates)),
        }
        _write_json(args.json, report)
        print(f"Wrote JSON report: {args.json}")

    # Non-zero exit if there are missing certain keys.
    return 1 if missing_in_yaml else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
