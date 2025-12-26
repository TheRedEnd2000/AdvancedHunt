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
    # Defaults (root=., java=src/main/java, yaml=src/main/resources/messages/messages_en.yml)
    python scripts/message_key_audit.py

    # Explicit roots/files + JSON report
    python scripts/message_key_audit.py \
        --root . \
        --java src/main/java \
        --yaml src/main/resources/messages/messages_en.yml \
        --json report.json

    # Write helper outputs (stubs for missing keys + safe prune candidates)
    python scripts/message_key_audit.py \
        --write-missing-stubs build/missing_message_keys.yml \
        --write-prune-candidates build/prune_candidates.txt
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


def _mask_java_comments(text: str) -> str:
    """Return a same-length string with Java comments replaced by spaces.

    This prevents comment contents from confusing our simple parser
    (parenthesis matching, top-level splitting, etc.) while preserving
    offsets/line numbers.

    Notes:
    - Newlines are preserved.
    - Comment markers inside string/char literals are not treated as comments.
    """

    out = list(text)
    in_string = False
    in_char = False
    escaped = False
    in_line_comment = False
    in_block_comment = False

    i = 0
    while i < len(out):
        ch = out[i]

        if in_line_comment:
            if ch == "\n":
                in_line_comment = False
                i += 1
                continue
            out[i] = " "
            i += 1
            continue

        if in_block_comment:
            if ch == "*" and i + 1 < len(out) and out[i + 1] == "/":
                out[i] = " "
                out[i + 1] = " "
                in_block_comment = False
                i += 2
                continue
            if ch != "\n":
                out[i] = " "
            i += 1
            continue

        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            i += 1
            continue

        if in_char:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == "'":
                in_char = False
            i += 1
            continue

        if ch == '"':
            in_string = True
            i += 1
            continue

        if ch == "'":
            in_char = True
            i += 1
            continue

        # Comment starts.
        if ch == "/" and i + 1 < len(out):
            nxt = out[i + 1]
            if nxt == "/":
                out[i] = " "
                out[i + 1] = " "
                in_line_comment = True
                i += 2
                continue
            if nxt == "*":
                out[i] = " "
                out[i + 1] = " "
                in_block_comment = True
                i += 2
                continue

        i += 1

    return "".join(out)


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
class ResolvedUncertainPatternUsage:
    pattern: str
    source_expr: str
    assignment_expr: Optional[str]
    site: UsageSite


@dataclass(frozen=True)
class ExpandedKeyUsage:
    key: str
    pattern: str
    site: UsageSite
    symbols: dict[str, list[str]]


@dataclass(frozen=True)
class EnhancedPatternUsage:
    pattern: str
    original_pattern: str
    site: UsageSite
    inferred_from_symbols: dict[str, str]


@dataclass(frozen=True)
class UncertainKeyUsage:
    site: UsageSite


@dataclass(frozen=True)
class UncertainUsageContext:
    class_name: Optional[str]
    method_signature: Optional[str]
    snippet: Optional[str]
    symbol: Optional[str]
    last_assignment_expr: Optional[str]
    inferred_pattern: Optional[str]
    propagated_literals: tuple[str, ...]


@dataclass(frozen=True)
class PatternMatcherSource:
    pattern: str
    source_kind: str  # 'concat'|'enhanced_concat'|'literal_pattern'|'inferred_dynamic'
    site: Optional[UsageSite]
    original_pattern: Optional[str]


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


def _unescape_java_string_content(content: str) -> Optional[str]:
    """Best-effort Java string unescaper.

    Returns None if the literal is malformed enough that we can't safely parse it.

    We intentionally do not try to be a full Java lexer; this aims to be:
    - correct for common escapes (\\n, \\t, \\", \\\\, \\uXXXX, octal)
    - conservative for unknown/invalid escapes (kept as-is)
    """

    out: list[str] = []
    i = 0
    while i < len(content):
        ch = content[i]

        if ch != "\\":
            out.append(ch)
            i += 1
            continue

        if i + 1 >= len(content):
            # Trailing backslash is not a valid Java string escape.
            return None

        esc = content[i + 1]

        simple = {
            "b": "\b",
            "t": "\t",
            "n": "\n",
            "f": "\f",
            "r": "\r",
            '"': '"',
            "'": "'",
            "\\": "\\",
        }
        if esc in simple:
            out.append(simple[esc])
            i += 2
            continue

        if esc == "u":
            # Java allows multiple 'u' characters: \\uuuu0041
            j = i + 2
            while j < len(content) and content[j] == "u":
                j += 1
            if j + 4 > len(content):
                return None
            hex_part = content[j : j + 4]
            if not re.fullmatch(r"[0-9a-fA-F]{4}", hex_part):
                return None
            out.append(chr(int(hex_part, 16)))
            i = j + 4
            continue

        if esc in "01234567":
            # Octal escape: up to 3 octal digits.
            j = i + 1
            digits = [content[j]]
            j += 1
            for _ in range(2):
                if j >= len(content) or content[j] not in "01234567":
                    break
                digits.append(content[j])
                j += 1
            out.append(chr(int("".join(digits), 8)))
            i = j
            continue

        # Unknown escape; keep as-is to avoid mutating keys.
        out.append("\\" + esc)
        i += 2

    return "".join(out)


def _parse_java_string_literal(expr: str) -> Optional[str]:
    s = _strip_wrapping_parens(_strip_java_casts(expr))
    if len(s) < 2 or not (s.startswith('"') and s.endswith('"')):
        return None

    # We don't need full Java escape semantics; we just want the literal content
    # without accidentally mutating it via Python-specific escape handling.
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

    unescaped = _unescape_java_string_content(content)
    return unescaped


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


def _find_matching_brace(text: str, open_brace_index: int) -> int:
    if open_brace_index < 0 or open_brace_index >= len(text) or text[open_brace_index] != "{":
        raise ValueError("open_brace_index must point to '{'")

    depth = 0
    in_string = False
    in_char = False
    escaped = False

    for i in range(open_brace_index, len(text)):
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

        if ch == "{":
            depth += 1
            continue
        if ch == "}":
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


def _split_top_level_ternary(expr: str) -> Optional[tuple[str, str, str]]:
    # Splits a Java ternary expression at top-level into (cond, true_expr, false_expr).
    # Returns None if no top-level ternary could be found.
    expr = _strip_wrapping_parens(_strip_java_casts(expr)).strip()
    depth_paren = 0
    depth_brack = 0
    depth_brace = 0
    in_string = False
    in_char = False
    escaped = False

    q_index = None
    # Find first top-level '?'
    for i, ch in enumerate(expr):
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
            depth_paren += 1
            continue
        if ch == ")":
            depth_paren = max(0, depth_paren - 1)
            continue
        if ch == "[":
            depth_brack += 1
            continue
        if ch == "]":
            depth_brack = max(0, depth_brack - 1)
            continue
        if ch == "{":
            depth_brace += 1
            continue
        if ch == "}":
            depth_brace = max(0, depth_brace - 1)
            continue

        if ch == "?" and depth_paren == 0 and depth_brack == 0 and depth_brace == 0:
            q_index = i
            break

    if q_index is None:
        return None

    # Find matching ':' for this '?' (handle nested ternaries at top-level).
    in_string = False
    in_char = False
    escaped = False
    depth_paren = depth_brack = depth_brace = 0
    # We already found the first '?', so depth starts at 1.
    ternary_depth = 1
    colon_index = None

    for i in range(q_index + 1, len(expr)):
        ch = expr[i]

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
            depth_paren += 1
            continue
        if ch == ")":
            depth_paren = max(0, depth_paren - 1)
            continue
        if ch == "[":
            depth_brack += 1
            continue
        if ch == "]":
            depth_brack = max(0, depth_brack - 1)
            continue
        if ch == "{":
            depth_brace += 1
            continue
        if ch == "}":
            depth_brace = max(0, depth_brace - 1)
            continue

        if depth_paren != 0 or depth_brack != 0 or depth_brace != 0:
            continue

        if ch == "?":
            ternary_depth += 1
            continue

        if ch == ":":
            # ':' closes one ternary level.
            if ternary_depth == 1:
                colon_index = i
                break
            ternary_depth = max(0, ternary_depth - 1)
            continue

    if colon_index is None:
        return None

    cond = expr[:q_index].strip()
    true_expr = expr[q_index + 1 : colon_index].strip()
    false_expr = expr[colon_index + 1 :].strip()
    return cond, true_expr, false_expr


def _ternary_literal_branches(expr: str) -> Optional[tuple[str, str]]:
    parsed = _split_top_level_ternary(expr)
    if parsed is None:
        return None
    _cond, t_expr, f_expr = parsed
    t_lit = _parse_java_string_literal(t_expr)
    f_lit = _parse_java_string_literal(f_expr)
    if t_lit is None or f_lit is None:
        return None
    return t_lit, f_lit


JAVA_IDENTIFIER_RE = re.compile(r"^[A-Za-z_$][A-Za-z0-9_$]*$")


def _expr_to_key_or_pattern(expr: str) -> tuple[str, Any]:
    """Returns (kind, value) where kind is 'certain'|'pattern'|'uncertain'.

    For kind == 'pattern', value is a dict with:
        - pattern: str  (with '*' placeholders)
        - parts: list[str] (top-level '+' parts, raw)
    """
    literal = _parse_java_string_literal(expr)
    if literal is not None:
        # If code intentionally uses wildcard/regex-like literal keys, treat them as
        # patterns so they protect matching YAML keys from being pruned.
        if _literal_key_is_pattern(literal):
            return "pattern", {"pattern": literal, "parts": [expr.strip()]}
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
    text: str, rel_file: str, *, parse_text: Optional[str] = None
) -> tuple[list[CertainKeyUsage], list[PatternKeyUsage], list[UncertainKeyUsage]]:
    certain: list[CertainKeyUsage] = []
    patterns: list[PatternKeyUsage] = []
    uncertain: list[UncertainKeyUsage] = []

    parse_text = text if parse_text is None else parse_text
    line_starts = _build_line_index(parse_text)

    # Find method occurrences and parse the argument list.
    method_re = re.compile(r"\b(" + "|".join(map(re.escape, METHOD_NAMES)) + r")\s*\(")

    for match in method_re.finditer(parse_text):
        method = match.group(1)
        open_paren_index = match.end() - 1
        close_paren_index = _find_matching_paren(parse_text, open_paren_index)
        if close_paren_index == -1:
            # Unbalanced; skip but still report as uncertain.
            site = UsageSite(
                rel_file,
                _offset_to_line(line_starts, match.start()),
                method,
                "<unbalanced parentheses>",
            )
            uncertain.append(UncertainKeyUsage(site))
            continue

        arg_list = parse_text[open_paren_index + 1 : close_paren_index]
        args = _split_top_level(arg_list, ",")
        if not args:
            continue

        arg_expr = args[0]
        site = UsageSite(rel_file, _offset_to_line(line_starts, match.start()), method, arg_expr.strip())

        # Support "inline if" (ternary) used directly in getMessage/getMessageList first arg.
        ternary = _ternary_literal_branches(arg_expr)
        if ternary is not None:
            t_key, f_key = ternary
            certain.append(CertainKeyUsage(key=t_key, site=site))
            certain.append(CertainKeyUsage(key=f_key, site=site))
            continue

        kind, value = _expr_to_key_or_pattern(arg_expr)

        if kind == "certain":
            certain.append(CertainKeyUsage(key=value, site=site))
        elif kind == "pattern":
            patterns.append(PatternKeyUsage(pattern=value["pattern"], raw_parts=tuple(value["parts"]), site=site))
        else:
            uncertain.append(UncertainKeyUsage(site))

    return certain, patterns, uncertain


def _safe_rel_file(file_path: Path, root: Path, java_root: Path) -> str:
    resolved = file_path.resolve()
    for base in (root.resolve(), java_root.resolve()):
        try:
            return resolved.relative_to(base).as_posix()
        except ValueError:
            continue
    return resolved.as_posix()


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


def _looks_like_regex_pattern(literal: str) -> bool:
    # Heuristic: treat only explicitly regex-y strings as regex.
    # Normal message keys contain '.' but should not be treated as regex.
    return (
        ".*" in literal
        or literal.startswith("^")
        or literal.endswith("$")
        or "[" in literal
        or "]" in literal
        or "(" in literal
        or ")" in literal
        or "|" in literal
        or "\\" in literal
    )


def _compile_key_matcher(pattern_or_regex: str) -> Optional[re.Pattern[str]]:
    """Compile a key matcher.

    Supports:
    - wildcard patterns using '*' (escaped, then '*' -> '.*', anchored)
    - raw regex-like patterns (anchored unless already anchored)
    """

    s = pattern_or_regex.strip()
    if not s:
        return None

    if "*" in s and not _looks_like_regex_pattern(s):
        return _pattern_to_regex(s)

    if _looks_like_regex_pattern(s):
        regex = s
        if not regex.startswith("^"):
            regex = "^" + regex
        if not regex.endswith("$"):
            regex = regex + "$"
        try:
            return re.compile(regex)
        except re.error:
            return None

    return None


def _literal_key_is_pattern(literal: str) -> bool:
    # Treat explicit wildcard or regex-like literals as patterns.
    return "*" in literal or _looks_like_regex_pattern(literal)


def _java_line_snippet(text: str, line: int, *, context: int = 2) -> str:
    lines = text.splitlines()
    if not lines:
        return ""
    idx = max(0, min(len(lines) - 1, line - 1))
    start = max(0, idx - context)
    end = min(len(lines), idx + context + 1)

    out: list[str] = []
    width = len(str(end))
    for i in range(start, end):
        prefix = f"{i + 1:>{width}}| "
        out.append(prefix + lines[i])
    return "\n".join(out)


def _find_enclosing_class_name(text: str, line: int) -> Optional[str]:
    # Best-effort: last `class X` before the line.
    prefix = "\n".join(text.splitlines()[: max(0, line - 1)])
    last: Optional[str] = None
    for m in re.finditer(r"\bclass\s+([A-Za-z_$][A-Za-z0-9_$]*)\b", prefix):
        last = m.group(1)
    return last


_CONTROL_BLOCK_START_RE = re.compile(r"^\s*(if|for|while|switch|catch|synchronized|do|else|try)\b")


def _find_enclosing_method_context(text: str, line: int) -> tuple[Optional[str], Optional[str], list[str]]:
    """Return (method_name, signature, param_names) for the method enclosing line.

    Heuristic only; designed to be good enough for local helper methods.
    """

    lines = text.splitlines()
    if not lines:
        return None, None, []

    start_line = max(0, min(len(lines) - 1, line - 1))

    for i in range(start_line, max(-1, start_line - 120), -1):
        s = lines[i]
        if "(" not in s:
            continue
        if _CONTROL_BLOCK_START_RE.match(s):
            continue
        if s.strip().startswith("//"):
            continue

        sig_parts: list[str] = [s.strip()]
        j = i + 1
        while j < len(lines) and "{" not in " ".join(sig_parts) and j < i + 12:
            if lines[j].strip().startswith("@"):  # annotations typically belong above
                break
            sig_parts.append(lines[j].strip())
            j += 1
        signature = " ".join([p for p in sig_parts if p])

        if "->" in signature:
            continue

        m = re.search(r"\b([A-Za-z_$][A-Za-z0-9_$]*)\s*\(", signature)
        if not m:
            continue
        method_name = m.group(1)

        if ";" in signature and "{" not in signature:
            continue

        open_idx = signature.find("(")
        close_idx = _find_matching_paren(signature, open_idx) if open_idx != -1 else -1
        if close_idx == -1:
            continue

        params_blob = signature[open_idx + 1 : close_idx].strip()
        param_names: list[str] = []
        if params_blob:
            for p in _split_top_level(params_blob, ","):
                p2 = re.sub(r"@[A-Za-z0-9_$.]+(\([^)]*\))?\s+", "", p).strip()
                p2 = re.sub(r"\b(final|@NotNull|@Nullable)\b\s+", "", p2).strip()
                tokens = [t for t in re.split(r"\s+", p2) if t]
                if tokens:
                    name = tokens[-1]
                    if JAVA_IDENTIFIER_RE.match(name):
                        param_names.append(name)

        return method_name, signature, param_names

    return None, None, []


def _expression_to_wildcard_pattern(
    expr: str,
    *,
    java_text: str,
    site_line: int,
    max_inline_depth: int = 2,
) -> Optional[tuple[str, tuple[str, ...]]]:
    """Try to turn an expression into a '*' wildcard pattern.

    Returns (pattern, raw_parts) or None.
    """

    parts = _split_top_level(expr, "+")
    if len(parts) <= 1:
        return None

    segments: list[tuple[str, str]] = []
    for part in parts:
        lit = _parse_java_string_literal(part)
        if lit is not None:
            segments.append(("lit", lit))
        else:
            segments.append(("expr", part.strip()))

    segments = _inline_concat_symbol_segments(segments, java_text, site_line, max_depth=max_inline_depth)

    saw_literal = any(k == "lit" for k, _ in segments)
    if not saw_literal:
        return None

    out_parts: list[str] = []
    for kind, val in segments:
        if kind == "lit":
            out_parts.append(val)
        else:
            if not out_parts or out_parts[-1] != "*":
                out_parts.append("*")

    pattern = "".join(out_parts)
    pattern = re.sub(r"\*+", "*", pattern)
    return pattern, tuple(parts)


def _propagate_literal_args_from_call_sites(
    *,
    java_text: str,
    target_method_name: str,
    param_index: int,
    max_results: int = 50,
) -> list[tuple[str, int]]:
    """Find string-literal arguments passed to a given method in the same file.

    Returns list of (literal, line).
    """

    parse_text = _mask_java_comments(java_text)
    line_starts = _build_line_index(parse_text)

    call_re = re.compile(r"\b" + re.escape(target_method_name) + r"\s*\(")
    out: list[tuple[str, int]] = []
    for m in call_re.finditer(parse_text):
        open_paren_index = m.end() - 1
        close_paren_index = _find_matching_paren(parse_text, open_paren_index)
        if close_paren_index == -1:
            continue

        # Skip declarations: if the next non-space char after ')' is '{'.
        k = close_paren_index + 1
        while k < len(parse_text) and parse_text[k].isspace():
            k += 1
        if k < len(parse_text) and parse_text[k] == "{":
            continue

        arg_list = parse_text[open_paren_index + 1 : close_paren_index]
        args = _split_top_level(arg_list, ",")
        if param_index < 0 or param_index >= len(args):
            continue

        lit = _parse_java_string_literal(args[param_index])
        if lit is None:
            continue

        call_line = _offset_to_line(line_starts, m.start())
        out.append((lit, call_line))
        if len(out) >= max_results:
            break

    return out


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

    # Inline if/else assignment (common alternative to ternary):
    # if (cond) symbol = "a"; else symbol = "b";
    # Also supports braces with minimal nesting.
    if_else_re = re.compile(
        r"\bif\s*\([^\)]*\)\s*(?:\{\s*)?" + re.escape(symbol) +
        r"\s*=\s*(\"(?:\\.|[^\"\\])*\")\s*;\s*(?:\}\s*)?" +
        r"(?:else\s*(?:\{\s*)?" + re.escape(symbol) + r"\s*=\s*(\"(?:\\.|[^\"\\])*\")\s*;)?",
        re.DOTALL,
    )
    for m in if_else_re.finditer(window):
        for group_index in (1, 2):
            if m.group(group_index) is None:
                continue
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


def _infer_last_assignment_expr_near_site(text: str, site_line: int, symbol: str) -> Optional[str]:
    # Best-effort: find the last assignment to `symbol` before `site_line` within a window.
    # We only support single-statement assignments ending with ';'.
    lines = text.splitlines()
    start = max(0, site_line - 1 - 260)
    end = min(len(lines), site_line - 1 + 10)

    assign_re = re.compile(
        r"^(?:\s*(?:final\s+)?String\s+)?" + re.escape(symbol) + r"\s*=\s*(.+?)\s*;\s*$"
    )

    last_expr: Optional[str] = None
    for i in range(start, min(end, site_line - 1) + 1):
        m = assign_re.match(lines[i])
        if m:
            last_expr = m.group(1).strip()

    return last_expr


def _enhance_pattern_usage(usage: PatternKeyUsage, java_text: str) -> Optional[EnhancedPatternUsage]:
    # Build a better wildcard pattern by substituting intermediate variables that are
    # themselves concatenations (e.g. fieldKey = "a." + key + ".b."), preserving
    # their literal segments.
    segments: list[tuple[str, str]] = []  # (kind, value) kind=lit|expr
    for part in usage.raw_parts:
        lit = _parse_java_string_literal(part)
        if lit is not None:
            segments.append(("lit", lit))
        else:
            segments.append(("expr", part.strip()))

    inferred: dict[str, str] = {}
    out_parts: list[str] = []

    for kind, val in segments:
        if kind == "lit":
            out_parts.append(val)
            continue

        sym = _extract_symbol_from_expr(val)
        if sym is None:
            out_parts.append("*")
            continue

        # If the symbol is directly set to a literal nearby, we cannot represent multiple
        # values in a single pattern, so fall back to '*'.
        literal_assignments = _infer_literal_assignments_near_site(java_text, usage.site.line, sym)
        if len(literal_assignments) == 1:
            only_value = next(iter(literal_assignments.keys()))
            inferred[sym] = only_value
            out_parts.append(only_value)
            continue
        if len(literal_assignments) > 1:
            # If the symbol can take multiple literal values, we can't inline a single
            # concrete value. But we can often keep useful structure by preserving the
            # longest common prefix/suffix.
            values = sorted(literal_assignments.keys())
            # Python stdlib has os.path.commonprefix, but we keep this local and string-based.
            prefix = values[0]
            for v in values[1:]:
                # shrink prefix until it matches
                while prefix and not v.startswith(prefix):
                    prefix = prefix[:-1]

            suffix = values[0]
            for v in values[1:]:
                while suffix and not v.endswith(suffix):
                    suffix = suffix[1:]

            if prefix or suffix:
                out_parts.append(prefix + "*" + suffix)
            else:
                out_parts.append("*")
            continue

        rhs = _infer_last_assignment_expr_near_site(java_text, usage.site.line, sym)
        if rhs is None:
            out_parts.append("*")
            continue

        rhs_kind, rhs_value = _expr_to_key_or_pattern(rhs)
        if rhs_kind == "certain":
            inferred[sym] = rhs_value
            out_parts.append(rhs_value)
            continue
        if rhs_kind == "pattern":
            inferred[sym] = rhs_value["pattern"]
            out_parts.append(rhs_value["pattern"])
            continue

        out_parts.append("*")

    enhanced = "".join(out_parts)
    enhanced = re.sub(r"\*+", "*", enhanced)
    if enhanced == usage.pattern:
        return None
    return EnhancedPatternUsage(
        pattern=enhanced,
        original_pattern=usage.pattern,
        site=usage.site,
        inferred_from_symbols=inferred,
    )


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

    # Enum inference: enums that define `final String key;` and have constants like NAME("second", ...)
    for enum_name, enum_keys in _infer_enum_key_values(text).items():
        if enum_keys:
            values_by_class.setdefault(f"enum:{enum_name}", set()).update(enum_keys)

    return values_by_class


def _infer_enum_key_values(text: str) -> dict[str, set[str]]:
    out: dict[str, set[str]] = {}

    enum_re = re.compile(r"\benum\s+([A-Za-z_$][A-Za-z0-9_$]*)\s*\{")
    for m in enum_re.finditer(text):
        enum_name = m.group(1)
        open_brace = text.find("{", m.end() - 1)
        if open_brace == -1:
            continue
        close_brace = _find_matching_brace(text, open_brace)
        if close_brace == -1:
            continue

        body = text[open_brace + 1 : close_brace]
        if not re.search(r"\bfinal\s+String\s+key\s*;", body):
            continue

        # Constants are before the first ';' at top-level (paren depth 0).
        in_string = False
        in_char = False
        escaped = False
        depth_paren = 0
        constants_end = None
        for i, ch in enumerate(body):
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
                depth_paren += 1
                continue
            if ch == ")":
                depth_paren = max(0, depth_paren - 1)
                continue

            if ch == ";" and depth_paren == 0:
                constants_end = i
                break

        if constants_end is None:
            continue

        constants_blob = body[:constants_end]
        # Split constants on top-level commas.
        constants = _split_top_level(constants_blob, ",")
        keys: set[str] = set()
        for c in constants:
            cm = re.search(r"\(\s*(\"(?:\\.|[^\"\\])*\")", c)
            if not cm:
                continue
            lit = _parse_java_string_literal(cm.group(1))
            if lit is None:
                continue
            keys.add(lit)

        if keys:
            out[enum_name] = keys

    return out


def _inline_concat_symbol_segments(
    segments: list[tuple[str, str]],
    java_text: str,
    site_line: int,
    max_depth: int = 2,
) -> list[tuple[str, str]]:
    # Inline simple assignment expressions into the current concat segments.
    # Example: [expr fieldKey, lit every_interval] with
    #   fieldKey = "a." + key + ".b.";
    # becomes: [lit a., expr key, lit .b., lit every_interval]
    current = segments
    inlined_symbols: set[str] = set()

    for _ in range(max_depth):
        changed = False
        next_segments: list[tuple[str, str]] = []

        for kind, val in current:
            if kind != "expr":
                next_segments.append((kind, val))
                continue

            sym = _extract_symbol_from_expr(val)
            if sym is None or sym in inlined_symbols:
                next_segments.append((kind, val))
                continue

            rhs = _infer_last_assignment_expr_near_site(java_text, site_line, sym)
            if rhs is None:
                next_segments.append((kind, val))
                continue

            rhs_kind, rhs_value = _expr_to_key_or_pattern(rhs)
            if rhs_kind == "certain":
                next_segments.append(("lit", rhs_value))
                inlined_symbols.add(sym)
                changed = True
                continue

            if rhs_kind == "pattern":
                # Inline the RHS parts, preserving symbols.
                for part in rhs_value["parts"]:
                    lit = _parse_java_string_literal(part)
                    if lit is not None:
                        next_segments.append(("lit", lit))
                    else:
                        next_segments.append(("expr", part.strip()))
                inlined_symbols.add(sym)
                changed = True
                continue

            next_segments.append((kind, val))

        current = next_segments
        if not changed:
            break

    return current


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

    # Inline intermediate concat symbols (e.g. fieldKey).
    segments = _inline_concat_symbol_segments(segments, java_text, usage.site.line)

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

    # Special: infer ctor first-arg keys for `key` fields + enum constant keys in this file.
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
    """Emit a nested YAML snippet for dotted message keys.

    This is intended to be pasteable into the nested messages YAML.
    We intentionally do NOT round-trip edit the main messages file to avoid
    destroying formatting/comments.
    """

    def insert(tree: dict[str, Any], dotted_key: str, value: Any) -> None:
        parts = [p for p in dotted_key.split(".") if p]
        cur: dict[str, Any] = tree
        for part in parts[:-1]:
            existing = cur.get(part)
            if existing is None:
                nxt: dict[str, Any] = {}
                cur[part] = nxt
                cur = nxt
                continue
            if not isinstance(existing, dict):
                # Collision (scalar/list vs nested). Keep existing and stop.
                return
            cur = existing

        leaf = parts[-1] if parts else dotted_key
        if leaf not in cur:
            cur[leaf] = value
            return
        # Collision: keep first value.

    tree: dict[str, Any] = {}
    for key in missing:
        expected = expected_types.get(key, "scalar")
        val: Any
        if expected == "list":
            val = ["TODO"]
        else:
            val = "TODO"
        insert(tree, key, val)

    def render(node: Any, indent: int) -> list[str]:
        pad = " " * indent
        if isinstance(node, dict):
            lines: list[str] = []
            for k in sorted(node.keys()):
                v = node[k]
                if isinstance(v, dict):
                    lines.append(f"{pad}{k}:")
                    lines.extend(render(v, indent + 2))
                elif isinstance(v, list):
                    lines.append(f"{pad}{k}:")
                    for item in v:
                        lines.append(f"{pad}  - {json.dumps(str(item), ensure_ascii=False)}")
                else:
                    lines.append(f"{pad}{k}: {json.dumps(str(v), ensure_ascii=False)}")
            return lines

        if isinstance(node, list):
            return [f"{pad}- {json.dumps(str(x), ensure_ascii=False)}" for x in node]

        return [f"{pad}{json.dumps(str(node), ensure_ascii=False)}"]

    return render(tree, 0)


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
        text = _read_text(file_path)

        rel_file = _safe_rel_file(file_path, root, java_root)
        java_text_by_file[rel_file] = text
        all_code_text_parts.append(text)
        all_string_literals |= _extract_java_string_literals(text)

        parse_text = _mask_java_comments(text)
        certain, patterns, uncertain = _extract_method_calls(text, rel_file, parse_text=parse_text)
        all_certain.extend(certain)
        all_patterns.extend(patterns)
        all_uncertain.extend(uncertain)

    # Whitelist-ignore uncertain sites from MessageManager internals.
    all_uncertain = [
        u
        for u in all_uncertain
        if not any(u.site.file.endswith(suffix) for suffix in IGNORE_UNCERTAIN_FILE_SUFFIXES)
    ]

    # Resolve some uncertain usages where the arg is a simple identifier and we can
    # infer its literal values nearby (ternary or if/else assignments). Additionally:
    # - infer wildcard patterns from nearby concat assignments (to protect prune candidates)
    # - propagate literal args from call sites when the symbol is a method parameter
    resolved_uncertain_as_certain: list[CertainKeyUsage] = []
    resolved_uncertain_as_patterns: list[ResolvedUncertainPatternUsage] = []
    uncertain_contexts: dict[tuple[str, int, str, str], UncertainUsageContext] = {}
    remaining_uncertain: list[UncertainKeyUsage] = []
    for u in all_uncertain:
        java_text = java_text_by_file.get(u.site.file)
        if not java_text:
            remaining_uncertain.append(u)
            continue

        sym = _extract_symbol_from_expr(u.site.arg_expr)

        class_name = _find_enclosing_class_name(java_text, u.site.line)
        enclosing_method_name, enclosing_sig, enclosing_params = _find_enclosing_method_context(java_text, u.site.line)
        snippet = _java_line_snippet(java_text, u.site.line, context=2)

        inferred_literals: dict[str, list[int]] = {}
        last_assignment_expr: Optional[str] = None
        inferred_pattern: Optional[str] = None
        propagated_literals: list[str] = []

        if sym is not None:
            inferred_literals = _infer_literal_assignments_near_site(java_text, u.site.line, sym)
            last_assignment_expr = _infer_last_assignment_expr_near_site(java_text, u.site.line, sym)

            # 1) If we can infer literal values, convert to certain keys.
            if inferred_literals:
                for lit in sorted(inferred_literals.keys()):
                    resolved_uncertain_as_certain.append(CertainKeyUsage(key=lit, site=u.site))
            else:
                # 2) Try infer a wildcard pattern from the last assignment.
                if last_assignment_expr is not None:
                    inferred_pat = _expression_to_wildcard_pattern(
                        last_assignment_expr,
                        java_text=java_text,
                        site_line=u.site.line,
                    )
                    if inferred_pat is not None:
                        inferred_pattern, _raw_parts = inferred_pat
                        resolved_uncertain_as_patterns.append(
                            ResolvedUncertainPatternUsage(
                                pattern=inferred_pattern,
                                source_expr=u.site.arg_expr,
                                assignment_expr=last_assignment_expr,
                                site=u.site,
                            )
                        )

                # 3) If this symbol is a parameter, propagate literal args from callsites.
                if enclosing_method_name and enclosing_params and sym in enclosing_params:
                    param_index = enclosing_params.index(sym)
                    propagated = _propagate_literal_args_from_call_sites(
                        java_text=java_text,
                        target_method_name=enclosing_method_name,
                        param_index=param_index,
                    )
                    for lit, call_line in propagated:
                        propagated_literals.append(lit)
                        call_site = UsageSite(
                            u.site.file,
                            call_line,
                            "<propagated>",
                            f"{enclosing_method_name}(...)",
                        )
                        resolved_uncertain_as_certain.append(CertainKeyUsage(key=lit, site=call_site))

        ctx_key = (u.site.file, u.site.line, u.site.method, u.site.arg_expr)
        uncertain_contexts[ctx_key] = UncertainUsageContext(
            class_name=class_name,
            method_signature=enclosing_sig,
            snippet=snippet,
            symbol=sym,
            last_assignment_expr=last_assignment_expr,
            inferred_pattern=inferred_pattern,
            propagated_literals=tuple(sorted(set(propagated_literals))),
        )

        # Keep as uncertain only if we didn't resolve to any literal key.
        if sym is None or (not inferred_literals and not propagated_literals):
            remaining_uncertain.append(u)

    all_uncertain = remaining_uncertain
    all_certain.extend(resolved_uncertain_as_certain)

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

    # 2.25) Improve wildcard patterns by resolving intermediate variables.
    enhanced_patterns: list[EnhancedPatternUsage] = []
    all_effective_patterns: list[str] = []
    for p in all_patterns:
        java_text = java_text_by_file.get(p.site.file)
        if not java_text:
            all_effective_patterns.append(p.pattern)
            continue
        enhanced = _enhance_pattern_usage(p, java_text)
        if enhanced is not None:
            enhanced_patterns.append(enhanced)
            all_effective_patterns.append(enhanced.pattern)
        else:
            all_effective_patterns.append(p.pattern)

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

    expanded_pattern_sites: set[tuple[str, int, str]] = {
        (u.site.file, u.site.line, u.pattern)
        for u in expanded_key_usages
    }

    # 3) Compare
    missing_in_yaml = sorted([k for k in certain_keys.keys() if k not in yaml_keys])
    # "Unused" here means: not referenced as a *certain literal* key in getMessage/getMessageList.
    unused_in_message_lookups = sorted([k for k in yaml_keys if k not in certain_keys])

    # Treat wildcard/regex patterns as "likely used" for pruning decisions.
    # We use enhanced patterns when available so intermediate variables (e.g. fieldKey)
    # preserve their literal segments. Also include patterns inferred from dynamic usages.
    pattern_sources: list[PatternMatcherSource] = []

    # Patterns coming from code concatenation usages.
    enhanced_by_site: dict[tuple[str, int, str], EnhancedPatternUsage] = {
        (e.site.file, e.site.line, e.site.method): e for e in enhanced_patterns
    }
    for p in all_patterns:
        # If this pattern was enhanced, prefer the enhanced one.
        enhanced = enhanced_by_site.get((p.site.file, p.site.line, p.site.method))
        if enhanced is not None:
            pattern_sources.append(
                PatternMatcherSource(
                    pattern=enhanced.pattern,
                    source_kind="enhanced_concat",
                    site=enhanced.site,
                    original_pattern=enhanced.original_pattern,
                )
            )
            continue

        # Detect literal patterns (string literal containing '*' or regex markers).
        lit = _parse_java_string_literal(p.site.arg_expr)
        if lit is not None and _literal_key_is_pattern(lit):
            pattern_sources.append(
                PatternMatcherSource(
                    pattern=lit,
                    source_kind="literal_pattern",
                    site=p.site,
                    original_pattern=None,
                )
            )
        else:
            pattern_sources.append(
                PatternMatcherSource(
                    pattern=p.pattern,
                    source_kind="concat",
                    site=p.site,
                    original_pattern=None,
                )
            )

    # Patterns inferred from dynamic usages (identifier assigned by concat).
    for p in resolved_uncertain_as_patterns:
        pattern_sources.append(
            PatternMatcherSource(
                pattern=p.pattern,
                source_kind="inferred_dynamic",
                site=p.site,
                original_pattern=None,
            )
        )

    # De-duplicate while preserving order.
    seen_patterns: set[tuple[str, str, Optional[str], Optional[int]]] = set()
    deduped_pattern_sources: list[PatternMatcherSource] = []
    for ps in pattern_sources:
        key = (ps.pattern, ps.source_kind, ps.site.file if ps.site else None, ps.site.line if ps.site else None)
        if key in seen_patterns:
            continue
        seen_patterns.add(key)
        deduped_pattern_sources.append(ps)
    pattern_sources = deduped_pattern_sources

    compiled_pattern_sources: list[tuple[re.Pattern[str], PatternMatcherSource]] = []
    for ps in pattern_sources:
        r = _compile_key_matcher(ps.pattern)
        if r is None:
            continue
        compiled_pattern_sources.append((r, ps))

    yaml_keys_matched_by_patterns: set[str] = set()
    yaml_key_first_match: dict[str, PatternMatcherSource] = {}
    if compiled_pattern_sources:
        for key in yaml_keys:
            for r, src in compiled_pattern_sources:
                if r.match(key):
                    yaml_keys_matched_by_patterns.add(key)
                    yaml_key_first_match.setdefault(key, src)
                    break

    # For pruning, treat keys concretely used by expansions as used.
    used_for_pruning = set(certain_keys.keys()) | yaml_keys_matched_by_patterns | expanded_keys_present_in_yaml
    unused_after_patterns = sorted([k for k in yaml_keys if k not in used_for_pruning])

    # Further classify unused YAML keys by presence anywhere in code.
    # 1) Found as an exact Java string literal
    unused_but_found_as_string_literal: list[str] = []
    # 2) Not an exact literal, but found as a substring in code text
    unused_but_found_in_code_text: list[str] = []
    # 3) Not directly found, but matched by wildcard pattern(s)
    unused_but_matched_by_patterns: list[str] = []
    # 4) Not directly found, but inferred by expanded pattern usages
    unused_but_inferred_by_expansion: list[str] = []
    # 5) Not found anywhere (even via patterns/expansion)
    unused_not_found_anywhere: list[str] = []

    code_blob = "\n".join(all_code_text_parts)
    for key in unused_in_message_lookups:
        if key in all_string_literals:
            unused_but_found_as_string_literal.append(key)
        elif key in code_blob:
            unused_but_found_in_code_text.append(key)
        elif key in expanded_keys_present_in_yaml:
            unused_but_inferred_by_expansion.append(key)
        elif key in yaml_keys_matched_by_patterns:
            unused_but_matched_by_patterns.append(key)
        else:
            unused_not_found_anywhere.append(key)

    # Safe prune candidates: keep explicitly identical to the
    # "not found anywhere (even via patterns/expansion)" bucket.
    prune_candidates = sorted(unused_not_found_anywhere)

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
    if enhanced_patterns:
        print(f"Enhanced pattern usages:     {len(enhanced_patterns)}")
    print(f"Expanded template keys:      {len(expanded_key_usages)}")
    print(f"Expanded missing in YAML:    {len(expanded_keys_missing_in_yaml)}")
    if resolved_uncertain_as_certain:
        print(f"Resolved dynamic usages:     {len(resolved_uncertain_as_certain)}")
    if resolved_uncertain_as_patterns:
        print(f"Inferred dynamic patterns:   {len(resolved_uncertain_as_patterns)}")
    print(f"Uncertain/dynamic usages:    {len(all_uncertain)}")
    print(f"YAML keys (flattened):       {len(yaml_keys)}")
    print(f"Missing in YAML (certain):   {len(missing_in_yaml)}")
    print(f"Unused in message lookups:   {len(unused_in_message_lookups)}")
    print(f"Matched by patterns:         {len(yaml_keys_matched_by_patterns)}")
    print(f"Unused after patterns:       {len(unused_after_patterns)}")
    print(f"- found as string literal:   {len(unused_but_found_as_string_literal)}")
    print(f"- found in code text:        {len(unused_but_found_in_code_text)}")
    print(f"- inferred by expansion:     {len(unused_but_inferred_by_expansion)}")
    print(f"- matched by patterns:       {len(unused_but_matched_by_patterns)}")
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

    if unused_but_inferred_by_expansion:
        print("YAML keys not used in message lookups, but inferred by expanded patterns")
        print("-----------------------------------------------------------------------")
        max_show = 250
        for key in unused_but_inferred_by_expansion[:max_show]:
            print(f"- {key}")
        if len(unused_but_inferred_by_expansion) > max_show:
            print(f"... and {len(unused_but_inferred_by_expansion) - max_show} more")
        print()

    if unused_but_matched_by_patterns:
        print("YAML keys not used in message lookups, but matched by wildcard patterns")
        print("---------------------------------------------------------------------")
        if pattern_sources:
            # Make it obvious where the wildcards/regexes come from.
            kinds = sorted({p.source_kind for p in pattern_sources})
            print(f"Patterns considered for matching: {len(pattern_sources)}  (sources: {', '.join(kinds)})")

            # Group patterns to keep the section scannable.
            by_kind: dict[str, list[PatternMatcherSource]] = {}
            for ps in pattern_sources:
                by_kind.setdefault(ps.source_kind, []).append(ps)

            per_group_cap = 6
            total_shown = 0
            total_cap = 18

            for kind in kinds:
                group = by_kind.get(kind) or []
                if not group:
                    continue
                print(f"{kind}: {len(group)}")

                shown_here = 0
                for ps in group:
                    if total_shown >= total_cap or shown_here >= per_group_cap:
                        break
                    if ps.site is None:
                        print(f"- pattern: {ps.pattern}")
                    else:
                        suffix = ""
                        if ps.source_kind == "enhanced_concat" and ps.original_pattern:
                            suffix = f"  (original: {ps.original_pattern})"
                        print(f"- pattern: {ps.pattern}  ({ps.site.file}:{ps.site.line}){suffix}")
                    total_shown += 1
                    shown_here += 1

                remaining_here = len(group) - shown_here
                if remaining_here > 0 and (shown_here >= per_group_cap or total_shown >= total_cap):
                    print(f"... and {remaining_here} more {kind} pattern(s)")

                if total_shown >= total_cap:
                    remaining_total = len(pattern_sources) - total_shown
                    if remaining_total > 0:
                        print(f"... and {remaining_total} more patterns total")
                    break
        # Group keys by the first matching pattern to make this section easier to read.
        groups: dict[tuple[str, str, Optional[str], Optional[int]], list[str]] = {}
        src_by_group: dict[tuple[str, str, Optional[str], Optional[int]], PatternMatcherSource] = {}
        for key in unused_but_matched_by_patterns:
            src = yaml_key_first_match.get(key)
            gk = (
                src.pattern if src is not None else "<unknown>",
                src.source_kind if src is not None else "<unknown>",
                src.site.file if (src is not None and src.site is not None) else None,
                src.site.line if (src is not None and src.site is not None) else None,
            )
            groups.setdefault(gk, []).append(key)
            if src is not None:
                src_by_group[gk] = src

        # Sort groups by pattern then kind then site.
        sorted_groups = sorted(groups.items(), key=lambda kv: kv[0])

        max_patterns_show = 40
        max_keys_per_pattern = 30
        shown_patterns = 0
        total_keys_shown = 0
        total_keys_cap = 500

        for gk, keys in sorted_groups:
            if shown_patterns >= max_patterns_show or total_keys_shown >= total_keys_cap:
                break

            pattern, kind, file, line = gk
            keys_sorted = sorted(keys)
            src = src_by_group.get(gk)

            if file is None or line is None:
                header = f"pattern: {pattern}  [{kind}]"
            else:
                header = f"pattern: {pattern}  [{kind}]  ({file}:{line})"
            if src is not None and src.source_kind == "enhanced_concat" and src.original_pattern:
                header += f"  (original: {src.original_pattern})"
            header += f"  -> {len(keys_sorted)} key(s)"

            print(header)

            keys_to_show = keys_sorted[:max_keys_per_pattern]
            for k in keys_to_show:
                if total_keys_shown >= total_keys_cap:
                    break
                print(f"- {k}")
                total_keys_shown += 1

            if len(keys_sorted) > len(keys_to_show):
                print(f"... and {len(keys_sorted) - len(keys_to_show)} more key(s) for this pattern")

            shown_patterns += 1

        remaining_patterns = len(sorted_groups) - shown_patterns
        remaining_keys = len(unused_but_matched_by_patterns) - total_keys_shown
        if remaining_patterns > 0:
            print(f"... and {remaining_patterns} more pattern group(s)")
        if remaining_keys > 0 and total_keys_shown >= total_keys_cap:
            print(f"... and {remaining_keys} more key(s) total")
        print()

    # Note: keys that are truly not found anywhere (even via patterns/expansion)
    # are identical to the prune candidate set. We list them only once below.

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

    unexpanded_patterns = [
        p for p in all_patterns
        if (p.site.file, p.site.line, p.pattern) not in expanded_pattern_sites
    ]
    if unexpanded_patterns:
        print("Pattern key usages (unexpanded; needs review)")
        print("-------------------------------------------")
        for p in unexpanded_patterns:
            s = p.site
            print(f"- {p.pattern}  ({s.file}:{s.line})")
            print(f"  expr: {s.arg_expr}")
        print()

    if enhanced_patterns:
        print("Enhanced patterns (resolved intermediates)")
        print("----------------------------------------")
        max_show = 250
        for e in enhanced_patterns[:max_show]:
            print(f"- {e.pattern}  ({e.site.file}:{e.site.line})")
            if e.inferred_from_symbols:
                items = ", ".join(f"{k}={v}" for k, v in sorted(e.inferred_from_symbols.items()))
                print(f"  inferred: {items}")
            print(f"  original: {e.original_pattern}")
        if len(enhanced_patterns) > max_show:
            print(f"... and {len(enhanced_patterns) - max_show} more")
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
            ctx = uncertain_contexts.get((s.file, s.line, s.method, s.arg_expr))
            if ctx is not None:
                if ctx.class_name or ctx.method_signature:
                    extra = " / ".join([p for p in [ctx.class_name, ctx.method_signature] if p])
                    if extra:
                        print(f"  context: {extra}")
                if ctx.symbol:
                    print(f"  symbol: {ctx.symbol}")
                if ctx.last_assignment_expr:
                    print(f"  last_assign: {ctx.last_assignment_expr}")
                if ctx.inferred_pattern:
                    print(f"  inferred_pattern: {ctx.inferred_pattern}")
                if ctx.propagated_literals:
                    lits = ", ".join(ctx.propagated_literals[:10])
                    suffix = "" if len(ctx.propagated_literals) <= 10 else f" (+{len(ctx.propagated_literals) - 10} more)"
                    print(f"  propagated_literals: {lits}{suffix}")
                if ctx.snippet:
                    print("  snippet:\n" + "\n".join("    " + ln for ln in ctx.snippet.splitlines()))
        if len(all_uncertain) > max_show:
            print(f"... and {len(all_uncertain) - max_show} more")
        print()

    if resolved_uncertain_as_patterns:
        print("Inferred wildcard patterns (from dynamic usages)")
        print("---------------------------------------------")
        max_show = 250
        for p in resolved_uncertain_as_patterns[:max_show]:
            s = p.site
            print(f"- {p.pattern}  ({s.file}:{s.line})")
            print(f"  expr: {p.source_expr}")
            if p.assignment_expr:
                print(f"  last_assign: {p.assignment_expr}")
        if len(resolved_uncertain_as_patterns) > max_show:
            print(f"... and {len(resolved_uncertain_as_patterns) - max_show} more")
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
                "resolved_dynamic_usages": len(resolved_uncertain_as_certain),
                "inferred_dynamic_patterns": len(resolved_uncertain_as_patterns),
                "uncertain_usages": len(all_uncertain),
                "yaml_keys": len(yaml_keys),
                "missing_in_yaml": len(missing_in_yaml),
                "unused_in_message_lookups": len(unused_in_message_lookups),
                "matched_by_patterns": len(yaml_keys_matched_by_patterns),
                "unused_after_patterns": len(unused_after_patterns),
                "unused_found_as_string_literal": len(unused_but_found_as_string_literal),
                "unused_found_in_code_text": len(unused_but_found_in_code_text),
                "unused_inferred_by_expansion": len(unused_but_inferred_by_expansion),
                "unused_matched_by_patterns": len(unused_but_matched_by_patterns),
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
            "unused_inferred_by_expansion": unused_but_inferred_by_expansion,
            "unused_matched_by_patterns": unused_but_matched_by_patterns,
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
            "unexpanded_pattern_usages": [
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
                for p in unexpanded_patterns
            ],
            "enhanced_pattern_usages": [
                {
                    "pattern": e.pattern,
                    "original_pattern": e.original_pattern,
                    "site": {
                        "file": e.site.file,
                        "line": e.site.line,
                        "method": e.site.method,
                    },
                    "inferred_from_symbols": e.inferred_from_symbols,
                }
                for e in enhanced_patterns
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
            "resolved_dynamic_usages": [
                {
                    "key": u.key,
                    "site": {
                        "file": u.site.file,
                        "line": u.site.line,
                        "method": u.site.method,
                    },
                    "expr": u.site.arg_expr,
                }
                for u in resolved_uncertain_as_certain
            ],
            "inferred_dynamic_patterns": [
                {
                    "pattern": p.pattern,
                    "site": {
                        "file": p.site.file,
                        "line": p.site.line,
                        "method": p.site.method,
                    },
                    "expr": p.source_expr,
                    "last_assignment_expr": p.assignment_expr,
                }
                for p in resolved_uncertain_as_patterns
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
            "uncertain_usages": [],
            "type_mismatches": type_mismatches,
            "yaml_duplicates_or_type_collisions": sorted(set(duplicates)),
        }

        for u in all_uncertain:
            ctx = uncertain_contexts.get((u.site.file, u.site.line, u.site.method, u.site.arg_expr))
            report["uncertain_usages"].append(
                {
                    "site": {
                        "file": u.site.file,
                        "line": u.site.line,
                        "method": u.site.method,
                    },
                    "expr": u.site.arg_expr,
                    "context": (
                        {
                            "class_name": ctx.class_name,
                            "method_signature": ctx.method_signature,
                            "symbol": ctx.symbol,
                            "last_assignment_expr": ctx.last_assignment_expr,
                            "inferred_pattern": ctx.inferred_pattern,
                            "propagated_literals": list(ctx.propagated_literals),
                            "snippet": ctx.snippet,
                        }
                        if ctx is not None
                        else None
                    ),
                }
            )
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
