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
    raw_parts: tuple[str, ...]
    site: UsageSite


@dataclass(frozen=True)
class ExpandedKeyUsage:
    key: str
    pattern: str
    site: UsageSite
    symbols: dict[str, list[str]]


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


JAVA_IDENTIFIER_RE = re.compile(r"^[A-Za-z_$][A-Za-z0-9_$]*$")


def _expr_to_key_or_pattern(expr: str) -> tuple[str, Any]:
    """Returns (kind, value) where kind is 'certain'|'pattern'|'uncertain'.

    For kind == 'pattern', value is a dict with:
        - pattern: str  (with '*' placeholders)
        - parts: list[str] (top-level '+' parts, raw)
    """
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
        return "pattern", {"pattern": pattern, "parts": parts}

    # Technically possible (no placeholders) but we would have matched literal above.
    return "uncertain", expr.strip()


def _extract_method_calls(
    text: str, rel_file: str
) -> tuple[list[CertainKeyUsage], list[PatternKeyUsage], list[UncertainKeyUsage]]:
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
            patterns.append(PatternKeyUsage(pattern=value["pattern"], raw_parts=tuple(value["parts"]), site=site))
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


def _write_text_lines(path: Path, lines: Iterable[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    content = "\n".join(lines)
    if content and not content.endswith("\n"):
        content += "\n"
    path.write_text(content, encoding="utf-8")


def _pattern_to_regex(pattern: str) -> re.Pattern[str]:
    # Only '*' wildcards are supported.
    # Example: gui.rewards.*.name -> ^gui\.rewards\..*\.name$
    escaped = re.escape(pattern)
    escaped = escaped.replace(r"\*", r".*")
    return re.compile(r"^" + escaped + r"$")


def _extract_symbol_from_expr(expr: str) -> Optional[str]:
    s = _strip_wrapping_parens(_strip_java_casts(expr)).strip()
    if s.startswith("this."):
        s = s[len("this."):]
    # Only accept simple identifiers; method calls / field chains are too dynamic.
    if JAVA_IDENTIFIER_RE.match(s):
        return s
    return None


def _infer_literal_assignments_near_site(text: str, site_line: int, symbol: str) -> dict[str, list[int]]:
    # Heuristic, local dataflow:
    # Scan a window around the usage for assignments like:
    #   String symbol = "foo";
    #   symbol = "foo";
    #   symbol = cond ? "a" : "b";
    # Also follow 1-level aliases like `symbol = other;` when other is literal.
    lines = text.splitlines()
    start = max(0, site_line - 1 - 220)
    end = min(len(lines), site_line - 1 + 80)
    window = "\n".join(lines[start:end])

    literal_by_line: dict[str, list[int]] = {}

    # Simple assignment forms
    assign_re = re.compile(r"(?:^|[^A-Za-z0-9_$])(?:String\s+)?" + re.escape(symbol) + r"\s*=\s*(\"(?:\\.|[^\"\\])*\")")
    for m in assign_re.finditer(window):
        lit = _parse_java_string_literal(m.group(1))
        if lit is None:
            continue
        # approximate line number (within file)
        before = window[: m.start()]
        line_in_window = before.count("\n")
        file_line = start + 1 + line_in_window
        literal_by_line.setdefault(lit, []).append(file_line)

    # Ternary assignment: symbol = cond ? "a" : "b"
    ternary_re = re.compile(
        r"(?:^|[^A-Za-z0-9_$])" + re.escape(symbol) + r"\s*=\s*[^;?]*\?\s*(\"(?:\\.|[^\"\\])*\")\s*:\s*(\"(?:\\.|[^\"\\])*\")"
    )
    for m in ternary_re.finditer(window):
        for group_index in (1, 2):
            lit = _parse_java_string_literal(m.group(group_index))
            if lit is None:
                continue
            before = window[: m.start()]
            line_in_window = before.count("\n")
            file_line = start + 1 + line_in_window
            literal_by_line.setdefault(lit, []).append(file_line)

    # 1-level alias: symbol = other;
    alias_re = re.compile(r"(?:^|[^A-Za-z0-9_$])" + re.escape(symbol) + r"\s*=\s*([A-Za-z_$][A-Za-z0-9_$]*)\s*;")
    aliases = {m.group(1) for m in alias_re.finditer(window)}
    for other in aliases:
        other_assign_re = re.compile(r"(?:^|[^A-Za-z0-9_$])(?:String\s+)?" + re.escape(other) + r"\s*=\s*(\"(?:\\.|[^\"\\])*\")")
        for m in other_assign_re.finditer(window):
            lit = _parse_java_string_literal(m.group(1))
            if lit is None:
                continue
            before = window[: m.start()]
            line_in_window = before.count("\n")
            file_line = start + 1 + line_in_window
            literal_by_line.setdefault(lit, []).append(file_line)

    return literal_by_line


def _infer_key_values_from_key_classes(text: str) -> dict[str, set[str]]:
    # Looks for inner classes that contain `final String key;` and then collects
    # string literals passed as the first ctor arg in `new <Class>("...")`.
    key_classes: list[str] = []
    for m in re.finditer(r"\bclass\s+([A-Za-z_$][A-Za-z0-9_$]*)\b", text):
        name = m.group(1)
        snippet = text[m.end() : m.end() + 2500]
        if re.search(r"\bfinal\s+String\s+key\s*;", snippet):
            key_classes.append(name)

    values_by_class: dict[str, set[str]] = {}
    for cls in key_classes:
        ctor_re = re.compile(r"\bnew\s+" + re.escape(cls) + r"\s*\(\s*(\"(?:\\.|[^\"\\])*\")")
        for m in ctor_re.finditer(text):
            lit = _parse_java_string_literal(m.group(1))
            if lit is None:
                continue
            values_by_class.setdefault(cls, set()).add(lit)
    return values_by_class


def _expand_pattern_usage(
    usage: PatternKeyUsage,
    java_text: str,
    yaml_keys: set[str],
    max_expansions: int = 500,
) -> tuple[list[ExpandedKeyUsage], list[UsageSite]]:
    # Returns (expanded_usages, resolution_sites)
    # - expanded_usages: concrete keys we could infer
    # - resolution_sites: where the inferred values came from (line-level, approximate)
    raw_parts = list(usage.raw_parts)
    segments: list[tuple[str, str]] = []  # (kind, value) kind=lit|expr
    for part in raw_parts:
        lit = _parse_java_string_literal(part)
        if lit is not None:
            segments.append(("lit", lit))
        else:
            segments.append(("expr", part.strip()))

    # Determine which expr segments are resolvable symbols.
    symbols_in_order: list[Optional[str]] = []
    for kind, val in segments:
        if kind == "expr":
            symbols_in_order.append(_extract_symbol_from_expr(val))
        else:
            symbols_in_order.append(None)

    # Collect possible values for each symbol.
    symbol_values: dict[str, list[str]] = {}
    resolution_sites: list[UsageSite] = []

    # Special: infer ctor first-arg keys for `key` fields in this file.
    key_values_by_class = _infer_key_values_from_key_classes(java_text)
    combined_key_values: set[str] = set()
    for vals in key_values_by_class.values():
        combined_key_values |= vals

    for sym in sorted({s for s in symbols_in_order if s is not None}):
        inferred: set[str] = set()
        inferred_lines: list[int] = []
        if sym == "key" and combined_key_values:
            inferred |= combined_key_values

        nearby = _infer_literal_assignments_near_site(java_text, usage.site.line, sym)
        for val, lines in nearby.items():
            inferred.add(val)
            inferred_lines.extend(lines)

        if inferred:
            symbol_values[sym] = sorted(inferred)
            # record approximate source sites for debugging
            for line in sorted(set(inferred_lines))[:20]:
                resolution_sites.append(UsageSite(usage.site.file, line, "<inferred>", f"{sym}=<literal>"))

    # If we have no resolvable symbols, stop.
    unresolved_expr = [
        seg
        for (seg, sym) in zip(segments, symbols_in_order)
        if seg[0] == "expr" and (sym is None or sym not in symbol_values)
    ]
    if unresolved_expr:
        return [], []

    # Expand.
    expansions: list[tuple[str, dict[str, list[str]]]] = [("", {})]
    for (kind, val), sym in zip(segments, symbols_in_order):
        if kind == "lit":
            expansions = [(prefix + val, used) for (prefix, used) in expansions]
            continue

        assert sym is not None
        vals = symbol_values.get(sym)
        if not vals:
            return [], []

        next_expansions: list[tuple[str, dict[str, list[str]]]] = []
        for prefix, used in expansions:
            for v in vals:
                used2 = dict(used)
                used2.setdefault(sym, [])
                if v not in used2[sym]:
                    used2[sym] = [*used2[sym], v]
                next_expansions.append((prefix + v, used2))
                if len(next_expansions) >= max_expansions:
                    break
            if len(next_expansions) >= max_expansions:
                break
        expansions = next_expansions

    out: list[ExpandedKeyUsage] = []
    for key, used in expansions:
        out.append(ExpandedKeyUsage(key=key, pattern=usage.pattern, site=usage.site, symbols=used))
    return out, resolution_sites


def _yaml_stubs_for_missing_keys(missing: list[str], expected_types: dict[str, str]) -> list[str]:
    # Emits a simple YAML snippet. We intentionally do NOT attempt to round-trip
    # and edit the main messages file to avoid destroying comments/formatting.
    lines: list[str] = []
    for key in missing:
        expected = expected_types.get(key, "scalar")
        if expected == "list":
            lines.append(f"{key}:")
            lines.append('  - "TODO"')
        else:
            lines.append(f'{key}: "TODO"')
    return lines


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

    parser.add_argument(
        "--write-missing-stubs",
        type=Path,
        default=None,
        help="Write a YAML snippet with stub values for missing certain keys",
    )
    parser.add_argument(
        "--write-prune-candidates",
        type=Path,
        default=None,
        help=(
            "Write newline-separated prune candidates (safe set: keys not used as certain keys, "
            "not matched by wildcard patterns, and not found anywhere in code)",
        ),
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

    java_text_by_file: dict[str, str] = {}

    # For classifying “unused in message lookups” YAML keys:
    # - exact matches found in ANY Java string literal
    # - fallback substring matches found anywhere in Java source
    all_string_literals: set[str] = set()
    all_code_text_parts: list[str] = []

    java_files = list(_iter_java_files(java_root))
    for file_path in java_files:
        rel_file = file_path.relative_to(root).as_posix()
        text = _read_text(file_path)
        java_text_by_file[rel_file] = text
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

    # 2.5) Resolve templates into likely concrete keys when possible.
    expanded_key_usages: list[ExpandedKeyUsage] = []
    expansion_resolution_sites: list[UsageSite] = []
    for p in all_patterns:
        java_text = java_text_by_file.get(p.site.file)
        if not java_text:
            continue
        expanded, sources = _expand_pattern_usage(p, java_text, yaml_keys)
        if expanded:
            expanded_key_usages.extend(expanded)
            expansion_resolution_sites.extend(sources)

    expanded_keys = {u.key for u in expanded_key_usages}
    expanded_keys_present_in_yaml = {k for k in expanded_keys if k in yaml_keys}
    expanded_keys_missing_in_yaml = sorted([k for k in expanded_keys if k not in yaml_keys])

    # 3) Compare
    missing_in_yaml = sorted([k for k in certain_keys.keys() if k not in yaml_keys])
    # "Unused" here means: not referenced as a *certain literal* key in getMessage/getMessageList.
    unused_in_message_lookups = sorted([k for k in yaml_keys if k not in certain_keys])

    # Treat wildcard patterns (string concat) as "likely used" for pruning decisions.
    # Example: switchKey + ".name" => "*.name" which is broad, but safe.
    pattern_regexes = [_pattern_to_regex(p.pattern) for p in all_patterns]
    yaml_keys_matched_by_patterns: set[str] = set()
    if pattern_regexes:
        for key in yaml_keys:
            if any(r.match(key) for r in pattern_regexes):
                yaml_keys_matched_by_patterns.add(key)

    # For pruning, treat keys concretely used by expansions as used.
    used_for_pruning = set(certain_keys.keys()) | yaml_keys_matched_by_patterns | expanded_keys_present_in_yaml
    unused_after_patterns = sorted([k for k in yaml_keys if k not in used_for_pruning])

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

    # Safe prune candidates: not used as certain, not matched by patterns, and not found anywhere.
    prune_candidates = sorted([k for k in unused_after_patterns if k not in all_string_literals and k not in code_blob])

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
        if expected == "scalar" and actual == "list":
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
    print(f"Expanded template keys:      {len(expanded_key_usages)}")
    print(f"Expanded missing in YAML:    {len(expanded_keys_missing_in_yaml)}")
    print(f"Uncertain/dynamic usages:    {len(all_uncertain)}")
    print(f"YAML keys (flattened):       {len(yaml_keys)}")
    print(f"Missing in YAML (certain):   {len(missing_in_yaml)}")
    print(f"Unused in message lookups:   {len(unused_in_message_lookups)}")
    print(f"Matched by patterns:         {len(yaml_keys_matched_by_patterns)}")
    print(f"Unused after patterns:       {len(unused_after_patterns)}")
    print(f"- found as string literal:   {len(unused_but_found_as_string_literal)}")
    print(f"- found in code text:        {len(unused_but_found_in_code_text)}")
    print(f"- not found anywhere:        {len(unused_not_found_anywhere)}")
    print(f"Prune candidates (safe):     {len(prune_candidates)}")
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

    if expanded_keys_missing_in_yaml:
        print("Likely missing in YAML (expanded from patterns)")
        print("--------------------------------------------")
        max_show = 250
        for key in expanded_keys_missing_in_yaml[:max_show]:
            # show first site that produced it
            site = next((u.site for u in expanded_key_usages if u.key == key), None)
            if site is None:
                print(f"- {key}")
            else:
                print(f"- {key}  ({site.file}:{site.line})")
        if len(expanded_keys_missing_in_yaml) > max_show:
            print(f"... and {len(expanded_keys_missing_in_yaml) - max_show} more")
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
        print("Warnings: message key type mismatches")
        print("------------------------------------")
        for w in type_mismatches:
            site = w["example_site"]
            print(
                f"- {w['key']}  (expected {w['expected']}, YAML {w['actual']})  "
                f"({site['file']}:{site['line']})"
            )
        print()

    if prune_candidates:
        print("Prune candidates (safe set)")
        print("--------------------------")
        max_show = 250
        for key in prune_candidates[:max_show]:
            print(f"- {key}")
        if len(prune_candidates) > max_show:
            print(f"... and {len(prune_candidates) - max_show} more")
        print()

    if all_patterns:
        print("Pattern key usages (needs review)")
        print("--------------------------------")
        for p in all_patterns:
            s = p.site
            print(f"- {p.pattern}  ({s.file}:{s.line})")
            print(f"  expr: {s.arg_expr}")
        print()

    if expanded_key_usages:
        print("Expanded pattern usages (inferred keys)")
        print("--------------------------------------")
        max_show = 250
        shown = 0
        for u in expanded_key_usages:
            if shown >= max_show:
                break
            marker = "OK" if u.key in yaml_keys else "MISSING"
            print(f"- [{marker}] {u.key}  ({u.site.file}:{u.site.line})")
            shown += 1
        if len(expanded_key_usages) > max_show:
            print(f"... and {len(expanded_key_usages) - max_show} more")
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
        expected_types_for_missing: dict[str, str] = {}
        for key, sites in certain_keys.items():
            expected_types_for_missing[key] = "list" if any(s.method == "getMessageList" for s in sites) else "scalar"

        report = {
            "root": str(root),
            "java_root": str(java_root),
            "yaml_file": str(yaml_path),
            "summary": {
                "java_files_scanned": len(java_files),
                "total_usages": len(all_certain) + len(all_patterns) + len(all_uncertain),
                "unique_certain_keys": len(certain_keys),
                "pattern_usages": len(all_patterns),
                "expanded_pattern_usages": len(expanded_key_usages),
                "expanded_missing_in_yaml": len(expanded_keys_missing_in_yaml),
                "uncertain_usages": len(all_uncertain),
                "yaml_keys": len(yaml_keys),
                "missing_in_yaml": len(missing_in_yaml),
                "unused_in_message_lookups": len(unused_in_message_lookups),
                "matched_by_patterns": len(yaml_keys_matched_by_patterns),
                "unused_after_patterns": len(unused_after_patterns),
                "unused_found_as_string_literal": len(unused_but_found_as_string_literal),
                "unused_found_in_code_text": len(unused_but_found_in_code_text),
                "unused_not_found_anywhere": len(unused_not_found_anywhere),
                "prune_candidates": len(prune_candidates),
            },
            "missing_in_yaml": [
                {
                    "key": k,
                    "example_site": {
                        "file": certain_keys[k][0].file,
                        "line": certain_keys[k][0].line,
                        "method": certain_keys[k][0].method,
                    },
                    "expected_value_type": expected_types_for_missing.get(k, "scalar"),
                }
                for k in missing_in_yaml
            ],
            "unused_in_message_lookups": unused_in_message_lookups,
            "matched_by_patterns": sorted(yaml_keys_matched_by_patterns),
            "expanded_keys_present_in_yaml": sorted(expanded_keys_present_in_yaml),
            "expanded_keys_missing_in_yaml": expanded_keys_missing_in_yaml,
            "unused_after_patterns": unused_after_patterns,
            "unused_found_as_string_literal": unused_but_found_as_string_literal,
            "unused_found_in_code_text": unused_but_found_in_code_text,
            "unused_not_found_anywhere": unused_not_found_anywhere,
            "prune_candidates": prune_candidates,
            "pattern_usages": [
                {
                    "pattern": p.pattern,
                    "site": {
                        "file": p.site.file,
                        "line": p.site.line,
                        "method": p.site.method,
                    },
                    "expr": p.site.arg_expr,
                    "raw_parts": list(p.raw_parts),
                }
                for p in all_patterns
            ],
            "expanded_pattern_usages": [
                {
                    "key": u.key,
                    "pattern": u.pattern,
                    "site": {
                        "file": u.site.file,
                        "line": u.site.line,
                        "method": u.site.method,
                    },
                    "symbols": u.symbols,
                }
                for u in expanded_key_usages
            ],
            "expansion_resolution_sites": [
                {
                    "file": s.file,
                    "line": s.line,
                    "method": s.method,
                    "expr": s.arg_expr,
                }
                for s in expansion_resolution_sites
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

    if args.write_missing_stubs is not None and missing_in_yaml:
        expected_types_for_missing: dict[str, str] = {}
        for key, sites in certain_keys.items():
            expected_types_for_missing[key] = "list" if any(s.method == "getMessageList" for s in sites) else "scalar"
        stubs = _yaml_stubs_for_missing_keys(missing_in_yaml, expected_types_for_missing)
        _write_text_lines(args.write_missing_stubs, stubs)
        print(f"Wrote missing-key YAML stubs: {args.write_missing_stubs}")

    if args.write_prune_candidates is not None and prune_candidates:
        _write_text_lines(args.write_prune_candidates, prune_candidates)
        print(f"Wrote prune-candidate list: {args.write_prune_candidates}")

    # Non-zero exit if there are missing certain keys.
    return 1 if missing_in_yaml else 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
