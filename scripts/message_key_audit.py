#!/usr/bin/env python3
"""Message key auditor for AdvancedHunt (no wildcard output).

This script compares message keys defined in messages/messages_en.yml (or another
messages_*.yml) with usages in the Java source.

Key principles:
- Only *string literal* keys (e.g. getMessage("foo.bar")) are treated as concrete.
- Any dynamic expression (ternary, concatenation, variable, method call, etc.) is
  reported as a *dynamic usage site* and never turned into a printed '*' pattern.
- Output can be grouped into "GUI" vs "Player" based on code signals.

Grouping heuristics (best-effort):
- If the call explicitly passes boolean applyPrefix=false -> GUI.
- If the literal key starts with "gui." -> GUI.
- Otherwise -> Player.

Notes:
- This does not attempt to prove whether a message is ultimately sent to chat or
  shown in an inventory; it uses the MessageManager API conventions.
- "Unused" means: present in YAML but not referenced by any *literal* usage.

Ignore config:
- Optional YAML/JSON file (default: scripts/message_key_audit_ignores.yml)
- Supported keys:
  - ignore_missing: [list of keys or matchers]
  - ignore_unused:  [list of keys or matchers]
  - ignore_dynamic_sites: [list of file matchers]
- Backwards compatible with the previous key:
  - expanded_missing_in_yaml is treated as ignore_missing
- Matchers:
  - exact key/file
  - wildcard '*' (glob-like)
  - regex (starts with '^')

Exit codes:
- 0: ok
- 1: missing or unused found (unless --no-fail)
- 2: usage/config error
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable, Optional

METHOD_NAMES = ("getMessage", "getMessageList")


###############################################################################
# YAML key extraction (dependency-free; matches project style)
###############################################################################


YAML_KEY_RE = re.compile(r"^(?P<key>(?:[A-Za-z0-9_\-]+|\"[^\"]+\"|'[^']+'))\s*:(?:\s+.*)?$")


def _normalize_yaml_key_segment(key: str) -> str:
    key = key.strip()
    if len(key) >= 2 and ((key[0] == '"' and key[-1] == '"') or (key[0] == "'" and key[-1] == "'")):
        return key[1:-1]
    return key


def _read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        return path.read_text(encoding="utf-8", errors="replace")


def _next_significant_yaml_line(lines: list[str], start_index: int) -> Optional[tuple[int, str, int]]:
    for j in range(start_index, len(lines)):
        raw = lines[j].rstrip("\r\n")
        stripped = raw.strip()
        if not stripped or stripped.startswith("#"):
            continue
        raw_expanded = raw.expandtabs(2)
        indent = len(raw_expanded) - len(raw_expanded.lstrip(" "))
        return j, stripped, indent
    return None


def extract_message_leaf_keys_from_yaml(text: str) -> set[str]:
    """Extract leaf message keys from a messages YAML.

    Returns only keys whose value is a scalar on the same line or a list.
    Does not return section-only keys.
    """

    lines = text.splitlines()
    keys: set[str] = set()

    stack: list[str] = []
    indents: list[int] = []

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

        key = _normalize_yaml_key_segment(match.group("key"))
        colon_index = stripped.find(":")
        value_part = stripped[colon_index + 1 :].strip() if colon_index >= 0 else ""

        while indents and indent <= indents[-1]:
            indents.pop()
            stack.pop()

        stack.append(key)
        indents.append(indent)
        full_key = ".".join(stack)

        if value_part:
            keys.add(full_key)
            continue

        nxt = _next_significant_yaml_line(lines, i + 1)
        if nxt is None:
            continue

        _, nxt_stripped, nxt_indent = nxt
        if nxt_indent <= indent:
            continue
        if nxt_stripped.startswith("-"):
            keys.add(full_key)

    return keys


def extract_message_leaf_key_types_from_yaml(text: str) -> dict[str, str]:
    """Extract leaf message key types from a messages YAML.

    Returns a mapping of full key -> type, where type is:
    - 'scalar' for keys whose value is a scalar on the same line
    - 'list' for keys whose value is a YAML list (dash items)

    Only includes leaf keys (same selection criteria as extract_message_leaf_keys_from_yaml).
    """

    lines = text.splitlines()
    types: dict[str, str] = {}

    stack: list[str] = []
    indents: list[int] = []

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

        key = _normalize_yaml_key_segment(match.group("key"))
        colon_index = stripped.find(":")
        value_part = stripped[colon_index + 1 :].strip() if colon_index >= 0 else ""

        while indents and indent <= indents[-1]:
            indents.pop()
            stack.pop()

        stack.append(key)
        indents.append(indent)
        full_key = ".".join(stack)

        if value_part:
            types[full_key] = "scalar"
            continue

        nxt = _next_significant_yaml_line(lines, i + 1)
        if nxt is None:
            continue

        _, nxt_stripped, nxt_indent = nxt
        if nxt_indent <= indent:
            continue
        if nxt_stripped.startswith("-"):
            types[full_key] = "list"

    return types


###############################################################################
# Ignore config
###############################################################################


@dataclass(frozen=True)
class IgnoreConfig:
    ignore_missing: tuple[str, ...] = ()
    ignore_unused: tuple[str, ...] = ()
    ignore_dynamic_sites: tuple[str, ...] = ()


def _load_yaml_best_effort(path: Path) -> Any:
    # Optional dependency: PyYAML. If unavailable, we degrade to "no ignores".
    try:
        import yaml  # type: ignore
    except Exception:
        return None

    try:
        return yaml.safe_load(_read_text(path))
    except Exception:
        return None


def _load_ignore_config(path: Path) -> IgnoreConfig:
    if not path.exists() or not path.is_file():
        return IgnoreConfig()

    if path.suffix.lower() == ".json":
        try:
            data = json.loads(_read_text(path))
        except Exception:
            return IgnoreConfig()
    else:
        data = _load_yaml_best_effort(path)
        if data is None:
            # Fall back to line-based parsing.
            entries = [
                ln.strip()
                for ln in _read_text(path).splitlines()
                if ln.strip() and not ln.strip().startswith("#")
            ]
            return IgnoreConfig(ignore_missing=tuple(entries))

    if not isinstance(data, dict):
        return IgnoreConfig()

    def to_tuple(value: Any) -> tuple[str, ...]:
        if value is None:
            return ()
        if isinstance(value, str):
            return (value,)
        if isinstance(value, list):
            return tuple(str(x) for x in value if str(x).strip())
        return ()

    # Back-compat: old key name.
    legacy = to_tuple(data.get("expanded_missing_in_yaml"))
    ignore_missing = to_tuple(data.get("ignore_missing")) or legacy

    return IgnoreConfig(
        ignore_missing=ignore_missing,
        ignore_unused=to_tuple(data.get("ignore_unused")),
        ignore_dynamic_sites=to_tuple(data.get("ignore_dynamic_sites")),
    )


def _compile_matchers(entries: Iterable[str]) -> tuple[set[str], list[re.Pattern[str]]]:
    exact: set[str] = set()
    patterns: list[re.Pattern[str]] = []

    for raw in entries:
        s = str(raw).strip()
        if not s or s.startswith("#"):
            continue

        if s.startswith("^"):
            try:
                patterns.append(re.compile(s))
            except re.error:
                exact.add(s)
            continue

        if "*" in s:
            # glob-like matcher; match entire string
            regex = "^" + re.escape(s).replace("\\*", ".*") + "$"
            patterns.append(re.compile(regex))
            continue

        exact.add(s)

    return exact, patterns


def _matches(value: str, *, exact: set[str], patterns: list[re.Pattern[str]]) -> bool:
    if value in exact:
        return True
    return any(p.match(value) for p in patterns)


###############################################################################
# Java scanning (simple parser)
###############################################################################


JAVA_STRING_LITERAL_RE = re.compile(r"^\"(?:\\.|[^\"\\])*\"$", re.DOTALL)


def _build_line_starts(text: str) -> list[int]:
    starts = [0]
    for m in re.finditer(r"\n", text):
        starts.append(m.end())
    return starts


def _line_of_offset(line_starts: list[int], offset: int) -> int:
    # 1-based line number
    lo = 0
    hi = len(line_starts)
    while lo + 1 < hi:
        mid = (lo + hi) // 2
        if line_starts[mid] <= offset:
            lo = mid
        else:
            hi = mid
    return lo + 1


def _mask_java_comments(text: str) -> str:
    """Return a same-length string with Java comments replaced by spaces."""
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


def _parse_java_string_literal(expr: str) -> Optional[str]:
    s = expr.strip()
    if not JAVA_STRING_LITERAL_RE.match(s):
        return None
    inner = s[1:-1]
    try:
        # Good enough for message keys: handles \\ \\" \n \t and \uXXXX.
        return bytes(inner, "utf-8").decode("unicode_escape")
    except Exception:
        # Fall back to a very conservative unescape.
        return inner.replace("\\\"", '"').replace("\\\\", "\\")


def _split_top_level_args(arg_text: str) -> list[str]:
    args: list[str] = []
    buf: list[str] = []
    in_string = False
    in_char = False
    escaped = False
    depth_paren = 0
    depth_brack = 0
    depth_brace = 0

    for ch in arg_text:
        if in_string:
            buf.append(ch)
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            continue

        if in_char:
            buf.append(ch)
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == "'":
                in_char = False
            continue

        if ch == '"':
            in_string = True
            buf.append(ch)
            continue
        if ch == "'":
            in_char = True
            buf.append(ch)
            continue

        if ch == "(":
            depth_paren += 1
            buf.append(ch)
            continue
        if ch == ")":
            if depth_paren > 0:
                depth_paren -= 1
            buf.append(ch)
            continue
        if ch == "[":
            depth_brack += 1
            buf.append(ch)
            continue
        if ch == "]":
            if depth_brack > 0:
                depth_brack -= 1
            buf.append(ch)
            continue
        if ch == "{":
            depth_brace += 1
            buf.append(ch)
            continue
        if ch == "}":
            if depth_brace > 0:
                depth_brace -= 1
            buf.append(ch)
            continue

        if ch == "," and depth_paren == 0 and depth_brack == 0 and depth_brace == 0:
            args.append("".join(buf).strip())
            buf.clear()
            continue

        buf.append(ch)

    tail = "".join(buf).strip()
    if tail:
        args.append(tail)
    return args


def _extract_call_args(masked_text: str, open_paren_index: int) -> Optional[tuple[str, int]]:
    """Extract raw args text and closing paren index for a call starting at '('"""
    assert masked_text[open_paren_index] == "("
    in_string = False
    in_char = False
    escaped = False
    depth = 0
    i = open_paren_index
    i += 1
    start = i

    while i < len(masked_text):
        ch = masked_text[i]

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

        if ch == "(":
            depth += 1
            i += 1
            continue
        if ch == ")":
            if depth == 0:
                return masked_text[start:i], i
            depth -= 1
            i += 1
            continue

        i += 1

    return None


@dataclass(frozen=True)
class UsageSite:
    file: str
    line: int
    method: str
    key_expr: str
    apply_prefix: Optional[bool]
    delivery: str = "unknown"  # 'gui'|'player'|'unknown'


@dataclass(frozen=True)
class LiteralKeyUsage:
    key: str
    site: UsageSite
    group: str  # 'gui'|'player'


@dataclass(frozen=True)
class DynamicKeyUsage:
    site: UsageSite
    group_hint: str  # 'gui'|'player'
    safe_pattern: Optional[str] = None
    resolved_literals: tuple[str, ...] = ()


JAVA_IDENTIFIER_RE = re.compile(r"^[A-Za-z_$][A-Za-z0-9_$]*$")


def _infer_group_for_literal(key: str, apply_prefix: Optional[bool]) -> str:
    if apply_prefix is False:
        return "gui"
    if key.startswith("gui."):
        return "gui"
    return "player"


_PLAYER_DELIVERY_RE = re.compile(
    r"\b(sendMessage|sendActionBar|sendTitle|sendSubtitle|sendRawMessage)\s*\(",
    flags=re.IGNORECASE,
)


def _statement_sends_to_player(stmt: str) -> bool:
    # Conservative: only flag as player-delivery when a send* call appears *before*
    # the getMessage/getMessageList call in the same gathered statement fragment.
    # This avoids false positives where sendMessage appears later (e.g., inside a
    # different lambda argument of the same outer method call).
    idx_get = -1
    for token in ("getMessage(", "getMessageList("):
        j = stmt.find(token)
        if j != -1 and (idx_get == -1 or j < idx_get):
            idx_get = j
    if idx_get == -1:
        return False

    m_send = _PLAYER_DELIVERY_RE.search(stmt)
    if not m_send:
        return False
    return m_send.start() < idx_get


def _try_extract_assigned_identifier(stmt: str) -> Optional[str]:
    # Match: <id> = ...;
    m = re.match(r"\s*(?P<id>[A-Za-z_$][A-Za-z0-9_$]*)\s*=", stmt)
    if not m:
        return None
    return m.group("id")


def _variable_sent_to_player_soon(masked_lines: list[str], start_line_1_based: int, var_name: str, *, max_lookahead: int = 30) -> bool:
    # Look ahead a small window for sendMessage(varName) before it's reassigned.
    start = max(0, start_line_1_based)  # next line index
    end = min(len(masked_lines), start + max_lookahead)
    assign_re = re.compile(r"\b" + re.escape(var_name) + r"\b\s*=")
    send_re = re.compile(r"\b(sendMessage|sendActionBar|sendTitle|sendSubtitle|sendRawMessage)\s*\(.*\b" + re.escape(var_name) + r"\b", flags=re.IGNORECASE)

    for i in range(start, end):
        line = masked_lines[i]
        if var_name not in line and "send" not in line:
            continue
        if assign_re.search(line):
            return False
        if send_re.search(line):
            return True
    return False


def _infer_group_for_dynamic(file_rel: str, apply_prefix: Optional[bool]) -> str:
    if apply_prefix is False:
        return "gui"
    # Dynamic keys in this project are mostly GUI templates under menus.
    if "/menu/" in file_rel.replace("\\", "/"):
        return "gui"
    return "player"


def _strip_wrapping_parens(expr: str) -> str:
    s = expr.strip()
    # Strip a single layer of wrapping parentheses, repeatedly.
    while s.startswith("(") and s.endswith(")"):
        inner = s[1:-1].strip()
        if not inner:
            return s
        # Heuristic: only strip if parentheses appear balanced at top-level.
        # (We don't need full parsing here; we just avoid stripping '(a) + b'.)
        depth = 0
        in_string = False
        in_char = False
        escaped = False
        ok = True
        for ch in inner:
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
            elif ch == ")":
                depth -= 1
                if depth < 0:
                    ok = False
                    break
        if not ok or depth != 0:
            return s
        s = inner
    return s


def _split_top_level_plus(expr: str) -> Optional[list[str]]:
    """Split a Java expression on top-level '+' operators.

    Returns None if there is no top-level '+' split.
    """
    s = expr.strip()
    parts: list[str] = []
    buf: list[str] = []
    in_string = False
    in_char = False
    escaped = False
    depth_paren = 0
    depth_brack = 0
    depth_brace = 0
    saw_plus = False

    for ch in s:
        if in_string:
            buf.append(ch)
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            continue
        if in_char:
            buf.append(ch)
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == "'":
                in_char = False
            continue

        if ch == '"':
            in_string = True
            buf.append(ch)
            continue
        if ch == "'":
            in_char = True
            buf.append(ch)
            continue

        if ch == "(":
            depth_paren += 1
            buf.append(ch)
            continue
        if ch == ")":
            if depth_paren > 0:
                depth_paren -= 1
            buf.append(ch)
            continue
        if ch == "[":
            depth_brack += 1
            buf.append(ch)
            continue
        if ch == "]":
            if depth_brack > 0:
                depth_brack -= 1
            buf.append(ch)
            continue
        if ch == "{":
            depth_brace += 1
            buf.append(ch)
            continue
        if ch == "}":
            if depth_brace > 0:
                depth_brace -= 1
            buf.append(ch)
            continue

        if ch == "+" and depth_paren == 0 and depth_brack == 0 and depth_brace == 0:
            saw_plus = True
            parts.append("".join(buf).strip())
            buf.clear()
            continue

        buf.append(ch)

    tail = "".join(buf).strip()
    if tail:
        parts.append(tail)
    if not saw_plus:
        return None
    return [p for p in parts if p]


def _try_resolve_simple_ternary_literals(expr: str) -> Optional[tuple[str, str]]:
    """Resolve `cond ? "a" : "b"` where both branches are string literals."""
    s = _strip_wrapping_parens(expr)

    in_string = False
    in_char = False
    escaped = False
    depth_paren = 0
    depth_brack = 0
    depth_brace = 0

    q_index: Optional[int] = None
    colon_index: Optional[int] = None

    for i, ch in enumerate(s):
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
            if depth_paren > 0:
                depth_paren -= 1
            continue
        if ch == "[":
            depth_brack += 1
            continue
        if ch == "]":
            if depth_brack > 0:
                depth_brack -= 1
            continue
        if ch == "{":
            depth_brace += 1
            continue
        if ch == "}":
            if depth_brace > 0:
                depth_brace -= 1
            continue

        if depth_paren == 0 and depth_brack == 0 and depth_brace == 0:
            if ch == "?" and q_index is None:
                q_index = i
                continue
            if ch == ":" and q_index is not None and colon_index is None:
                colon_index = i
                continue

    if q_index is None or colon_index is None or colon_index <= q_index:
        return None

    true_expr = s[q_index + 1 : colon_index].strip()
    false_expr = s[colon_index + 1 :].strip()
    lit_true = _parse_java_string_literal(true_expr)
    lit_false = _parse_java_string_literal(false_expr)
    if lit_true is None or lit_false is None:
        return None
    return lit_true, lit_false


def _derive_safe_pattern_from_concat(expr: str) -> Optional[str]:
    """Derive a *safe* pattern from a string concatenation expression.

    Allowed examples:
    - x.*
    - x.*.x

    Disallowed examples:
    - *.x
    - *
    - any pattern with more than one '*'
    """
    parts = _split_top_level_plus(_strip_wrapping_parens(expr))
    if not parts:
        return None

    wildcard_count = 0
    built: list[str] = []

    for part in parts:
        lit = _parse_java_string_literal(part)
        if lit is not None:
            built.append(lit)
            continue
        wildcard_count += 1
        if wildcard_count > 1:
            return None
        built.append("*")

    pattern = "".join(built)
    if not pattern or pattern == "*":
        return None
    if pattern.startswith("*"):
        # Never allow patterns like '*.x'
        return None
    return pattern


def _extract_simple_identifier(expr: str) -> Optional[str]:
    s = _strip_wrapping_parens(expr).strip()
    if JAVA_IDENTIFIER_RE.match(s):
        return s
    return None


def _parse_identifier_plus_literal(expr: str) -> Optional[tuple[str, str]]:
    """Parse `symbol + "suffix"` where suffix is a string literal."""
    parts = _split_top_level_plus(_strip_wrapping_parens(expr))
    if not parts or len(parts) != 2:
        return None
    left = _extract_simple_identifier(parts[0])
    right_lit = _parse_java_string_literal(parts[1])
    if left is None or right_lit is None:
        return None
    return left, right_lit


def _parse_literal_plus_identifier(expr: str) -> Optional[tuple[str, str]]:
    """Parse `"prefix" + symbol` where prefix is a string literal."""
    parts = _split_top_level_plus(_strip_wrapping_parens(expr))
    if not parts or len(parts) != 2:
        return None
    left_lit = _parse_java_string_literal(parts[0])
    right = _extract_simple_identifier(parts[1])
    if left_lit is None or right is None:
        return None
    return left_lit, right


def _gather_statement_from(lines: list[str], start_index: int, *, max_lines: int = 8) -> Optional[str]:
    """Join lines until a top-level ';' is found (best-effort)."""
    buf: list[str] = []
    in_string = False
    in_char = False
    escaped = False
    depth_paren = 0
    depth_brack = 0
    depth_brace = 0

    for j in range(start_index, min(len(lines), start_index + max_lines)):
        line = lines[j]
        for ch in line:
            buf.append(ch)
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
                if depth_paren > 0:
                    depth_paren -= 1
                continue
            if ch == "[":
                depth_brack += 1
                continue
            if ch == "]":
                if depth_brack > 0:
                    depth_brack -= 1
                continue
            if ch == "{":
                depth_brace += 1
                continue
            if ch == "}":
                if depth_brace > 0:
                    depth_brace -= 1
                continue

            if ch == ";" and depth_paren == 0 and depth_brack == 0 and depth_brace == 0:
                return "".join(buf)
        buf.append("\n")
    return None


def _find_recent_assignment_rhs(masked_lines: list[str], symbol: str, *, upto_line_1_based: int, max_lookback: int = 120) -> Optional[str]:
    """Find a recent assignment to `symbol` above the given line (1-based)."""
    # Search backwards for a line that mentions `symbol` and '='.
    start = max(0, upto_line_1_based - 2)  # previous line index
    end = max(-1, start - max_lookback)
    assign_hint = re.compile(r"\b" + re.escape(symbol) + r"\b\s*=")

    for i in range(start, end, -1):
        line = masked_lines[i]
        if symbol not in line:
            continue
        if not assign_hint.search(line):
            continue

        stmt = _gather_statement_from(masked_lines, i)
        if stmt is None:
            continue
        # Extract RHS of the assignment (best effort).
        m = re.search(r"\b" + re.escape(symbol) + r"\b\s*=\s*(.+?)\s*;", stmt, flags=re.DOTALL)
        if not m:
            continue
        return m.group(1).strip()

    return None


def _collect_recent_literal_assignments(masked_lines: list[str], symbol: str, *, upto_line_1_based: int, max_lookback: int = 240) -> tuple[str, ...]:
    """Collect nearby assignments `symbol = "...";` above the given line (1-based).

    This is intentionally conservative: it only returns string-literal RHS values.
    Useful for patterns like:
      String key;
      switch (...) { case A: key = "x"; ... case B: key = "y"; ... }
      getMessage(key)
    """

    start = max(0, upto_line_1_based - 2)
    end = max(-1, start - max_lookback)
    assign_hint = re.compile(r"\b" + re.escape(symbol) + r"\b\s*=")
    decl_hint = re.compile(r"\bString\b[^;\n]*\b" + re.escape(symbol) + r"\b")

    out: list[str] = []
    seen: set[str] = set()

    for i in range(start, end, -1):
        line = masked_lines[i]

        # Stop if we reached a likely declaration (limits overreach across scopes).
        if decl_hint.search(line):
            break

        if symbol not in line:
            continue
        if not assign_hint.search(line):
            continue

        stmt = _gather_statement_from(masked_lines, i)
        if stmt is None:
            continue
        m = re.search(r"\b" + re.escape(symbol) + r"\b\s*=\s*(.+?)\s*;", stmt, flags=re.DOTALL)
        if not m:
            continue
        rhs = m.group(1).strip()

        lit = _parse_java_string_literal(rhs)
        if lit is None:
            continue
        if lit in seen:
            continue
        seen.add(lit)
        out.append(lit)

        # Don't collect an unbounded amount.
        if len(out) >= 32:
            break

    return tuple(out)


def _resolve_dynamic_key_expr(masked_lines: list[str], key_expr: str, *, line_1_based: int) -> tuple[tuple[str, ...], Optional[str]]:
    """Try to resolve a dynamic key expression into literals or a safe pattern.

    Returns (resolved_literals, safe_pattern). At most one of them will be non-empty.
    """
    expr = _strip_wrapping_parens(key_expr)

    # Direct symbol: try resolve from recent assignment.
    sym = _extract_simple_identifier(expr)
    if sym is not None:
        rhs = _find_recent_assignment_rhs(masked_lines, sym, upto_line_1_based=line_1_based)
        if rhs is not None:
            tern = _try_resolve_simple_ternary_literals(rhs)
            if tern is not None:
                return (tern[0], tern[1]), None
            pat = _derive_safe_pattern_from_concat(rhs)
            if pat is not None:
                return (), pat

        # If we couldn't resolve a single RHS, try collecting multiple literal assignments.
        lits = _collect_recent_literal_assignments(masked_lines, sym, upto_line_1_based=line_1_based)
        if lits:
            return lits, None
        return (), None

    # symbol + "suffix"
    sym_suffix = _parse_identifier_plus_literal(expr)
    if sym_suffix is not None:
        sym2, suffix = sym_suffix
        rhs = _find_recent_assignment_rhs(masked_lines, sym2, upto_line_1_based=line_1_based)
        if rhs is not None:
            tern = _try_resolve_simple_ternary_literals(rhs)
            if tern is not None:
                return (tern[0] + suffix, tern[1] + suffix), None
            pat = _derive_safe_pattern_from_concat(rhs)
            if pat is not None:
                return (), pat + suffix
        # If unknown, would be '*.suffix' which we disallow.
        return (), None

    # "prefix" + no-arg method call (where the method returns known string literals)
    prefix_call = _parse_literal_plus_noarg_call(expr)
    if prefix_call is not None:
        prefix, method_name = prefix_call
        method_values = _extract_noarg_string_method_return_literals(masked_lines).get(method_name)
        if method_values:
            return tuple(prefix + v for v in method_values), None
        # Even if unknown, prefix + '*' is safe (never starts with '*').
        if prefix:
            candidate = prefix + "*"
            if not candidate.startswith("*"):
                return (), candidate
        return (), None

    # "prefix" + symbol
    prefix_sym = _parse_literal_plus_identifier(expr)
    if prefix_sym is not None:
        prefix, sym3 = prefix_sym
        rhs = _find_recent_assignment_rhs(masked_lines, sym3, upto_line_1_based=line_1_based)
        if rhs is not None:
            tern = _try_resolve_simple_ternary_literals(rhs)
            if tern is not None:
                return (prefix + tern[0], prefix + tern[1]), None
            pat = _derive_safe_pattern_from_concat(rhs)
            if pat is not None:
                # prefix + (something with 0/1 wildcard)
                return (), prefix + pat
        # If unknown, we can still safely pattern-match because prefix is known and pattern won't start with '*'.
        if prefix:
            candidate = prefix + "*"
            if not candidate.startswith("*"):
                return (), candidate
        return (), None

    return (), None


def _parse_literal_plus_noarg_call(expr: str) -> Optional[tuple[str, str]]:
    """Parse: "prefix" + methodName()"""
    m = re.match(r"^\s*(?P<lit>\"(?:\\.|[^\"\\])*\")\s*\+\s*(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*\(\s*\)\s*$", expr)
    if not m:
        return None
    lit = _parse_java_string_literal(m.group("lit"))
    if lit is None:
        return None
    return lit, m.group("name")


def _strip_java_string_literals(text: str) -> str:
    return re.sub(r'"(?:\\.|[^"\\])*"', '""', text)


def _extract_noarg_string_method_return_literals(masked_lines: list[str]) -> dict[str, tuple[str, ...]]:
    """Extract simple no-arg String methods that return literal strings.

    Supported forms:
    - return "x";
    - return cond ? "a" : "b";

    This is intentionally conservative and file-local.
    """
    out: dict[str, tuple[str, ...]] = {}
    i = 0
    sig_re = re.compile(r"^\s*(?:public|protected|private)?\s*(?:final\s+)?String\s+(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*\(\s*\)\s*\{\s*$")
    while i < len(masked_lines):
        m = sig_re.match(masked_lines[i])
        if not m:
            i += 1
            continue
        name = m.group("name")
        brace = 1
        body_lines: list[str] = []
        i += 1
        while i < len(masked_lines) and brace > 0:
            line = masked_lines[i]
            stripped = _strip_java_string_literals(line)
            brace += stripped.count("{")
            brace -= stripped.count("}")
            body_lines.append(line)
            i += 1

        body = "\n".join(body_lines)
        m_ret = re.search(r"\breturn\s+(?P<expr>.+?);", body, flags=re.DOTALL)
        if not m_ret:
            continue
        expr = " ".join(m_ret.group("expr").split())

        # return "literal";
        lit = _parse_java_string_literal(expr)
        if lit is not None:
            out[name] = (lit,)
            continue

        # return cond ? "a" : "b";
        tern = _try_resolve_simple_ternary_literals(expr)
        if tern is not None:
            out[name] = tern
            continue

    return out


def scan_java_for_message_usages(java_root: Path, *, root: Path) -> tuple[list[LiteralKeyUsage], list[DynamicKeyUsage]]:
    literals: list[LiteralKeyUsage] = []
    dynamics: list[DynamicKeyUsage] = []

    java_files = sorted(p for p in java_root.rglob("*.java") if p.is_file())
    call_re = re.compile(r"\b(?P<method>getMessage|getMessageList)\s*\(")

    # Precompute known keys returned by CronEditPolicy.cronTypeMessageKey().
    cron_type_keys = tuple(sorted(_extract_cron_edit_policy_type_keys(java_root)))

    const_array_re = re.compile(
        r"\bString\s*\[\]\s*(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*=\s*\{(?P<body>.*?)\}\s*;",
        flags=re.DOTALL,
    )
    array_index_expr_re = re.compile(r"^(?P<name>[A-Za-z_][A-Za-z0-9_]*)\s*\[.+\]$")

    for path in java_files:
        # Do not treat MessageManager method declarations as usage sites.
        if path.name == "MessageManager.java" and "advancedhunt/managers" in path.as_posix():
            continue
        text = _read_text(path)
        masked = _mask_java_comments(text)
        masked_lines = masked.splitlines()
        line_starts = _build_line_starts(text)
        rel = path.relative_to(root).as_posix()

        # Special-case: helper method buildCollectionActionItem(baseKey, ...) is called with a literal baseKey,
        # but the underlying lookups are baseKey + ".name" / ".lore" / ".lore_disabled".
        # Expand those call sites into concrete key usages so they aren't lost as disallowed "*.suffix" dynamics.
        expanded_collection_action_base_keys: list[tuple[str, int]] = []
        for m in re.finditer(r"\bbuildCollectionActionItem\s*\(\s*(\"(?:\\.|[^\"\\])*\")\s*,", masked):
            lit = _parse_java_string_literal(m.group(1))
            if lit is None:
                continue
            call_line = _line_of_offset(line_starts, m.start())
            expanded_collection_action_base_keys.append((lit, call_line))
            for suffix in (".name", ".lore", ".lore_disabled"):
                # Preserve the expected YAML type by tagging the synthetic usage as getMessage (scalar)
                # or getMessageList (list), matching buildCollectionActionItem's implementation.
                expected_method = "getMessage" if suffix == ".name" else "getMessageList"
                site = UsageSite(
                    file=rel,
                    line=call_line,
                    method=expected_method,
                    key_expr=m.group(1) + " + \"" + suffix + "\"",
                    apply_prefix=False,
                    delivery="gui",
                )
                literals.append(LiteralKeyUsage(key=lit + suffix, site=site, group="gui"))

        # Special-case: applyPreset(cronExpr, "some.key") results in getMessage(presetNameKey)
        # inside applyPreset(...). Expand those call sites into concrete key usages.
        expanded_apply_preset_keys: list[tuple[str, int]] = []
        for m in re.finditer(r"\bapplyPreset\s*\(\s*[^,]+,\s*(\"(?:\\.|[^\"\\])*\")\s*\)", masked):
            lit = _parse_java_string_literal(m.group(1))
            if lit is None:
                continue
            call_line = _line_of_offset(line_starts, m.start())
            expanded_apply_preset_keys.append((lit, call_line))
            # This key's value is used in a player feedback message.
            site = UsageSite(file=rel, line=call_line, method="applyPreset", key_expr=m.group(1), apply_prefix=None, delivery="player")
            group = _infer_group_for_literal(lit, None)
            literals.append(LiteralKeyUsage(key=lit, site=site, group=group))

        # Extract constant String[] arrays of message keys in this file so we can
        # resolve usages like getMessage(DIRECTION_KEYS[index]).
        const_string_arrays: dict[str, tuple[str, ...]] = {}
        for m_arr in const_array_re.finditer(masked):
            name = m_arr.group("name")
            body = m_arr.group("body")
            items: list[str] = []
            for m_lit in re.finditer(r'"(?:\\.|[^"\\])*"', body):
                lit = _parse_java_string_literal(m_lit.group(0))
                if lit is not None:
                    items.append(lit)
            if items:
                const_string_arrays[name] = tuple(items)

        # Also treat menu title setters as key usages (they are later used via getMessage(titleKey,...)).
        # This helps catch keys like gui.rewards.preset_title.
        for m in re.finditer(r"\.setTitleKey\s*\(\s*(\"(?:\\.|[^\"\\])*\")\s*\)", masked):
            lit = _parse_java_string_literal(m.group(1))
            if lit is None:
                continue
            line = _line_of_offset(line_starts, m.start())
            site = UsageSite(file=rel, line=line, method="setTitleKey", key_expr=m.group(1), apply_prefix=False, delivery="gui")
            literals.append(LiteralKeyUsage(key=lit, site=site, group="gui"))

        for m in re.finditer(
            r"\.setAlternateContext\s*\(\s*[^,]+,\s*(\"(?:\\.|[^\"\\])*\")\s*\)",
            masked,
        ):
            lit = _parse_java_string_literal(m.group(1))
            if lit is None:
                continue
            line = _line_of_offset(line_starts, m.start())
            site = UsageSite(file=rel, line=line, method="setAlternateContext", key_expr=m.group(1), apply_prefix=False, delivery="gui")
            literals.append(LiteralKeyUsage(key=lit, site=site, group="gui"))

        for m in call_re.finditer(masked):
            method = m.group("method")
            open_paren = m.end() - 1
            extracted = _extract_call_args(masked, open_paren)
            if extracted is None:
                continue
            raw_args, _close = extracted
            args = _split_top_level_args(raw_args)
            if not args:
                continue

            key_expr = args[0].strip()
            key_literal = _parse_java_string_literal(key_expr)

            apply_prefix: Optional[bool] = None
            if len(args) >= 2:
                second = args[1].strip()
                if second == "false":
                    apply_prefix = False
                elif second == "true":
                    apply_prefix = True

            line = _line_of_offset(line_starts, m.start())

            # Determine whether the message text is player-visible.
            stmt = _gather_statement_from(masked_lines, max(0, line - 1)) or ""
            delivery = "unknown"
            if _statement_sends_to_player(stmt):
                delivery = "player"
            else:
                assigned = _try_extract_assigned_identifier(stmt)
                if assigned and _variable_sent_to_player_soon(masked_lines, line, assigned):
                    delivery = "player"
                elif apply_prefix is False or "/menu/" in rel.replace("\\", "/"):
                    delivery = "gui"

            site = UsageSite(file=rel, line=line, method=method, key_expr=key_expr, apply_prefix=apply_prefix, delivery=delivery)

            # Expand CronEditPolicy record accessor: policy.cronTypeMessageKey().
            if cron_type_keys and re.match(r"^\s*[A-Za-z_][A-Za-z0-9_]*\s*\.\s*cronTypeMessageKey\s*\(\s*\)\s*$", key_expr):
                for lit in cron_type_keys:
                    group = _infer_group_for_literal(lit, apply_prefix)
                    literals.append(LiteralKeyUsage(key=lit, site=site, group=group))
                continue

            # Suppress helper getMessage(presetNameKey) when we've already expanded applyPreset(..., "...") sites.
            if expanded_apply_preset_keys and key_expr.strip() == "presetNameKey":
                continue

            # Suppress the internal helper lookups for buildCollectionActionItem(...)
            # when we already expanded the literal call sites.
            if expanded_collection_action_base_keys and (
                key_expr.replace(" ", "") in ("baseKey+\".name\"", "baseKey+\".lore\"", "baseKey+\".lore_disabled\"")
            ):
                continue

            if key_literal is not None:
                group = _infer_group_for_literal(key_literal, apply_prefix)
                literals.append(LiteralKeyUsage(key=key_literal, site=site, group=group))
            else:
                # Resolve constant string arrays (e.g., SOME_KEYS[i]) into concrete key usages.
                m_idx = array_index_expr_re.match(key_expr)
                if m_idx is not None:
                    arr_name = m_idx.group("name")
                    arr_items = const_string_arrays.get(arr_name)
                    if arr_items:
                        for lit in arr_items:
                            group = _infer_group_for_literal(lit, apply_prefix)
                            literals.append(LiteralKeyUsage(key=lit, site=site, group=group))
                        continue

                # Try: resolve simple ternary where both branches are literals.
                tern = _try_resolve_simple_ternary_literals(key_expr)
                if tern is not None:
                    for lit in tern:
                        group = _infer_group_for_literal(lit, apply_prefix)
                        literals.append(LiteralKeyUsage(key=lit, site=site, group=group))
                    continue

                resolved_lits, resolved_pat = _resolve_dynamic_key_expr(masked_lines, key_expr, line_1_based=line)
                if resolved_lits:
                    for lit in resolved_lits:
                        group = _infer_group_for_literal(lit, apply_prefix)
                        literals.append(LiteralKeyUsage(key=lit, site=site, group=group))
                    continue

                group_hint = _infer_group_for_dynamic(rel, apply_prefix)
                safe_pattern = resolved_pat or _derive_safe_pattern_from_concat(key_expr)
                dynamics.append(DynamicKeyUsage(site=site, group_hint=group_hint, safe_pattern=safe_pattern))

    return literals, dynamics


def _extract_cron_edit_policy_type_keys(java_root: Path) -> set[str]:
    """Extract possible cron type message keys from CronEditPolicy factories.

    This covers record accessor usage like policy.cronTypeMessageKey().
    """
    policy_path = java_root / "de/theredend2000/advancedhunt/menu/cron/CronEditPolicy.java"
    if not policy_path.exists():
        return set()
    text = _read_text(policy_path)
    # Look for: new CronEditPolicy("gui.cron.type.act", ...)
    out: set[str] = set()
    for m in re.finditer(r"new\s+CronEditPolicy\s*\(\s*(\"(?:\\.|[^\"\\])*\")", text):
        lit = _parse_java_string_literal(m.group(1))
        if lit:
            out.add(lit)
    return out


def _extract_cron_field_value_keys(java_root: Path) -> set[str]:
    """Enumerate concrete value-description keys used by CronField.getValueDescription().

    This avoids risky wildcard patterns by extracting the enum constants and their
    preset values, then applying the same sanitizeKey() logic.
    """
    field_menu_path = java_root / "de/theredend2000/advancedhunt/menu/cron/CronFieldMenu.java"
    if not field_menu_path.exists():
        return set()
    text = _read_text(field_menu_path)

    # Matches enum constant entries like:
    #   DAY("day", "*", 1, 31, List.of("1", "15", "L", "*", "?")),
    enum_const_re = re.compile(
        r"^\s*[A-Z0-9_]+\(\s*(\"(?:\\.|[^\"\\])*\")\s*,.*?List\.of\((.*?)\)\s*\)\s*(?:,|;)\s*$",
        re.MULTILINE | re.DOTALL,
    )

    str_lit_re = re.compile(r"\"(?:\\.|[^\"\\])*\"")

    def sanitize_key(value: str) -> str:
        return (
            value.replace("*", "wildcard")
            .replace("/", "_")
            .replace(",", "_")
            .replace("-", "_")
            .replace("?", "none")
            .replace("#", "nth")
            .replace("L", "last")
            .lower()
        )

    used: set[str] = set()

    for m in enum_const_re.finditer(text):
        key_lit = _parse_java_string_literal(m.group(1))
        if not key_lit:
            continue
        values_blob = m.group(2)
        for sm in str_lit_re.finditer(values_blob):
            raw_val = _parse_java_string_literal(sm.group(0))
            if raw_val is None:
                continue

            # These values use dedicated branches and should not be looked up via sanitizeKey.
            if raw_val.startswith("*/"):
                continue
            if key_lit == "year" and raw_val != "*":
                continue

            sanitized = sanitize_key(raw_val)
            used.add(f"gui.cron.builder.fields.{key_lit}.values.{sanitized}")

    return used


###############################################################################
# Reporting
###############################################################################


def _sort_unique(items: Iterable[str]) -> list[str]:
    return sorted(set(items))


def _print_section(title: str, items: list[str]) -> None:
    print(f"\n== {title} ({len(items)}) ==")
    for k in items:
        print(k)


def _print_dynamic_section(title: str, sites: list[DynamicKeyUsage], *, ignore_files: tuple[set[str], list[re.Pattern[str]]]) -> None:
    exact, patterns = ignore_files
    visible = [d for d in sites if not _matches(d.site.file, exact=exact, patterns=patterns)]

    print(f"\n== {title} ({len(visible)}) ==")
    for d in visible:
        ap = ""
        if d.site.apply_prefix is False:
            ap = " (applyPrefix=false)"
        elif d.site.apply_prefix is True:
            ap = " (applyPrefix=true)"
        extra = ""
        if d.safe_pattern:
            extra = f" -> {d.safe_pattern}"
        print(f"{d.site.file}:{d.site.line} {d.site.method}({d.site.key_expr}){ap}{extra}")


def _print_key_sites_section(title: str, key_to_sites: dict[str, list[UsageSite]], *, limit_sites: int = 4) -> None:
    keys = sorted(key_to_sites.keys())
    print(f"\n== {title} ({len(keys)}) ==")
    if not keys:
        return
    for k in keys:
        sites = key_to_sites[k]
        preview = ", ".join(f"{s.file}:{s.line}" for s in sites[:limit_sites])
        suffix = "" if len(sites) <= limit_sites else f" (+{len(sites) - limit_sites} more)"
        print(f"{k}  [{preview}]{suffix}")


def _print_type_mismatch_section(title: str, mismatches: list[dict[str, Any]]) -> None:
    print(f"\n== {title} ({len(mismatches)}) ==")
    for m in mismatches:
        key = m["key"]
        expected = m["expected"]
        actual = m["actual"]
        site = m.get("example_site") or {}
        where = ""
        if site.get("file") and site.get("line"):
            where = f"  ({site['file']}:{site['line']})"
        print(f"{key}  expected={expected} actual={actual}{where}")


def _compile_safe_pattern_regex(pattern: str) -> Optional[re.Pattern[str]]:
    """Compile a safe '*' pattern into a regex.

    The auditor guarantees these patterns:
    - do not start with '*'
    - contain at most one '*'

    Matching rule:
    - '*' matches one dot-segment (no '.') to keep risk bounded.
    """
    s = pattern.strip()
    if not s or s == "*" or s.startswith("*"):
        return None
    if s.count("*") > 1:
        return None

    regex = "^" + re.escape(s).replace("\\*", "[^.]+") + "$"
    try:
        return re.compile(regex)
    except re.error:
        return None


def _keys_matched_by_dynamic_patterns(dynamic_usages: list[DynamicKeyUsage], yaml_keys: set[str]) -> set[str]:
    matched: set[str] = set()
    for d in dynamic_usages:
        if not d.safe_pattern:
            continue
        r = _compile_safe_pattern_regex(d.safe_pattern)
        if r is None:
            continue
        for k in yaml_keys:
            if r.match(k):
                matched.add(k)
    return matched


def _write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def main(argv: list[str]) -> int:
    parser = argparse.ArgumentParser(description="Audit message keys used in code vs YAML (no wildcard output)")
    parser.add_argument("--root", default=".", help="Repo root (default: .)")
    parser.add_argument("--java", default="src/main/java", help="Java source root relative to --root")
    parser.add_argument(
        "--yaml",
        default="src/main/resources/messages/messages_en.yml",
        help="Messages YAML file relative to --root",
    )
    parser.add_argument(
        "--ignore",
        default="scripts/message_key_audit_ignores.yml",
        help="Ignore config file relative to --root (optional)",
    )
    parser.add_argument(
        "--only",
        choices=("all", "missing", "unused", "dynamic", "gui-sent", "type"),
        default="all",
        help="Only print one category",
    )
    parser.add_argument(
        "--no-fail",
        action="store_true",
        help="Always exit 0 even when missing/unused are present",
    )
    parser.add_argument(
        "--json",
        dest="json_out",
        default=None,
        help="Write JSON report to this path (relative to --root unless absolute)",
    )

    args = parser.parse_args(argv)

    root = Path(args.root).resolve()
    java_root = (root / args.java).resolve()
    yaml_path = (root / args.yaml).resolve()
    ignore_path = (root / args.ignore).resolve()

    if not java_root.exists() or not java_root.is_dir():
        print(f"Java root not found: {java_root}", file=sys.stderr)
        return 2
    if not yaml_path.exists() or not yaml_path.is_file():
        print(f"Messages YAML not found: {yaml_path}", file=sys.stderr)
        return 2

    ignore_cfg = _load_ignore_config(ignore_path)
    ignore_missing_exact, ignore_missing_patterns = _compile_matchers(ignore_cfg.ignore_missing)
    ignore_unused_exact, ignore_unused_patterns = _compile_matchers(ignore_cfg.ignore_unused)
    ignore_dynamic_exact, ignore_dynamic_patterns = _compile_matchers(ignore_cfg.ignore_dynamic_sites)

    yaml_text = _read_text(yaml_path)
    yaml_keys = extract_message_leaf_keys_from_yaml(yaml_text)
    yaml_key_types = extract_message_leaf_key_types_from_yaml(yaml_text)
    literal_usages, dynamic_usages = scan_java_for_message_usages(java_root, root=root)
    literal_keys = {u.key for u in literal_usages}

    # Extra static-derived usages to improve accuracy without risky wildcarding.
    literal_keys |= _extract_cron_edit_policy_type_keys(java_root)
    literal_keys |= _extract_cron_field_value_keys(java_root)

    dynamic_visible = [
        d
        for d in dynamic_usages
        if not _matches(d.site.file, exact=ignore_dynamic_exact, patterns=ignore_dynamic_patterns)
    ]
    dynamic_matched_yaml_keys = _keys_matched_by_dynamic_patterns(dynamic_visible, yaml_keys)

    missing = sorted(k for k in literal_keys if k not in yaml_keys)
    missing = [k for k in missing if not _matches(k, exact=ignore_missing_exact, patterns=ignore_missing_patterns)]

    unused = sorted(k for k in yaml_keys if k not in literal_keys and k not in dynamic_matched_yaml_keys)
    unused = [k for k in unused if not _matches(k, exact=ignore_unused_exact, patterns=ignore_unused_patterns)]

    # Split into GUI vs Player groups for missing/unused by key naming.
    missing_gui = [k for k in missing if k.startswith("gui.")]
    missing_player = [k for k in missing if not k.startswith("gui.")]
    unused_gui = [k for k in unused if k.startswith("gui.")]
    unused_player = [k for k in unused if not k.startswith("gui.")]

    # Split literal usages by group inference.
    used_gui = _sort_unique(u.key for u in literal_usages if u.group == "gui")
    used_player = _sort_unique(u.key for u in literal_usages if u.group == "player")

    # gui.* keys used in a player-delivery context (e.g., sendMessage).
    gui_sent_to_player: dict[str, list[UsageSite]] = {}
    for u in literal_usages:
        if not u.key.startswith("gui."):
            continue
        if u.site.delivery != "player":
            continue
        gui_sent_to_player.setdefault(u.key, []).append(u.site)

    dyn_gui = [d for d in dynamic_usages if d.group_hint == "gui"]
    dyn_player = [d for d in dynamic_usages if d.group_hint == "player"]

    # YAML type mismatches: expected list vs scalar based on usage method.
    # (Only considers literal usages so we have a method/site.)
    type_mismatches: list[dict[str, Any]] = []
    key_to_usages: dict[str, list[LiteralKeyUsage]] = {}
    for u in literal_usages:
        key_to_usages.setdefault(u.key, []).append(u)
    for key, usages in key_to_usages.items():
        actual = yaml_key_types.get(key)
        if actual is None:
            continue
        expected = "list" if any(s.site.method == "getMessageList" for s in usages) else "scalar"
        if expected != actual:
            ex = usages[0].site
            type_mismatches.append(
                {
                    "key": key,
                    "expected": expected,
                    "actual": actual,
                    "example_site": {
                        "file": ex.file,
                        "line": ex.line,
                        "method": ex.method,
                    },
                }
            )
    type_mismatches.sort(key=lambda d: d["key"])

    print(f"YAML keys: {len(yaml_keys)}")
    print(f"Literal keys used in code: {len(literal_keys)} (gui={len(used_gui)}, player={len(used_player)})")
    print(f"Dynamic usage sites: {len(dynamic_usages)} (gui_hint={len(dyn_gui)}, player_hint={len(dyn_player)})")
    if dynamic_matched_yaml_keys:
        print(f"YAML keys protected by dynamic patterns: {len(dynamic_matched_yaml_keys)}")
    if ignore_path.exists():
        print(f"Ignore config: {ignore_path.relative_to(root).as_posix()}")

    if args.only in ("all", "missing"):
        _print_section("Missing in YAML (GUI)", missing_gui)
        _print_section("Missing in YAML (Player)", missing_player)

    if args.only in ("all", "unused"):
        _print_section("Unused in code (GUI)", unused_gui)
        _print_section("Unused in code (Player)", unused_player)

    if args.only in ("all", "dynamic"):
        _print_dynamic_section(
            "Dynamic message usages (GUI hint)",
            dyn_gui,
            ignore_files=(ignore_dynamic_exact, ignore_dynamic_patterns),
        )
        _print_dynamic_section(
            "Dynamic message usages (Player hint)",
            dyn_player,
            ignore_files=(ignore_dynamic_exact, ignore_dynamic_patterns),
        )

    if args.only in ("all", "gui-sent"):
        _print_key_sites_section("GUI keys sent to player", gui_sent_to_player)

    if args.only in ("all", "type"):
        _print_type_mismatch_section("YAML type mismatches", type_mismatches)

    if args.json_out:
        out_path = Path(args.json_out)
        if not out_path.is_absolute():
            out_path = root / out_path

        payload: dict[str, Any] = {
            "yaml": {
                "file": yaml_path.relative_to(root).as_posix(),
                "count": len(yaml_keys),
            },
            "literals": {
                "count": len(literal_keys),
                "gui": used_gui,
                "player": used_player,
            },
            "guiSentToPlayer": {
                "count": len(gui_sent_to_player),
                "keys": sorted(gui_sent_to_player.keys()),
                "sites": {
                    k: [f"{s.file}:{s.line}" for s in v]
                    for k, v in sorted(gui_sent_to_player.items(), key=lambda kv: kv[0])
                },
            },
            "typeMismatches": {
                "count": len(type_mismatches),
                "items": type_mismatches,
            },
            "missing": {
                "count": len(missing),
                "gui": missing_gui,
                "player": missing_player,
            },
            "unused": {
                "count": len(unused),
                "gui": unused_gui,
                "player": unused_player,
                "note": "unused excludes keys referenced by literal usages and keys matched by safe dynamic patterns",
                "excluded_by_dynamic_patterns": len(dynamic_matched_yaml_keys),
            },
            "dynamic": {
                "count": len(dynamic_usages),
                "gui_hint": [
                    {
                        "file": d.site.file,
                        "line": d.site.line,
                        "method": d.site.method,
                        "key_expr": d.site.key_expr,
                        "apply_prefix": d.site.apply_prefix,
                        "safe_pattern": d.safe_pattern,
                    }
                    for d in dyn_gui
                    if not _matches(d.site.file, exact=ignore_dynamic_exact, patterns=ignore_dynamic_patterns)
                ],
                "player_hint": [
                    {
                        "file": d.site.file,
                        "line": d.site.line,
                        "method": d.site.method,
                        "key_expr": d.site.key_expr,
                        "apply_prefix": d.site.apply_prefix,
                        "safe_pattern": d.safe_pattern,
                    }
                    for d in dyn_player
                    if not _matches(d.site.file, exact=ignore_dynamic_exact, patterns=ignore_dynamic_patterns)
                ],
            },
        }
        _write_json(out_path, payload)
        print(f"\nWrote JSON report: {out_path.relative_to(root).as_posix()}")

    has_issues = bool(missing or unused)
    if has_issues and not args.no_fail:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))


# Everything below is leftover legacy code from the previous implementation.
# It's intentionally disabled so it can't run.
_LEGACY_DISABLED = r'''


def _compile_ignore_matchers(entries: Iterable[str]) -> tuple[set[str], list[re.Pattern[str]]]:
    exact: set[str] = set()
    patterns: list[re.Pattern[str]] = []
    for e in entries:
        s = str(e).strip()
        if not s or s.startswith("#"):
            continue
        r = _compile_key_matcher(s)
        if r is not None:
            patterns.append(r)
        else:
            exact.add(s)
    return exact, patterns


def _is_ignored_key(key: str, *, exact: set[str], patterns: list[re.Pattern[str]]) -> bool:
    if key in exact:
        return True
    return any(r.match(key) for r in patterns)


def _patterns_for_pattern_usage(
    usage: PatternKeyUsage,
    java_text: str,
    *,
    max_patterns: int = 50,
    max_inline_depth: int = 2,
) -> list[str]:
    """Return one or more patterns that represent the usage.

    Key behavior:
    - If an expr segment resolves to multiple *literal* possibilities nearby, emit one
      pattern per possibility (union) instead of collapsing to '*'.
    - Only fall back to '*' when a segment cannot be resolved.
    """

    # Convert raw parts into segments.
    segments: list[tuple[str, str]] = []
    for part in usage.raw_parts:
        lit = _parse_java_string_literal(part)
        if lit is not None:
            segments.append(("lit", lit))
        else:
            segments.append(("expr", part.strip()))

    segments = _inline_concat_symbol_segments(segments, java_text, usage.site.line, max_depth=max_inline_depth)

    # Build options per segment.
    options: list[list[str]] = []
    for kind, val in segments:
        if kind == "lit":
            options.append([val])
            continue

        sym = _extract_symbol_from_expr(val)
        if sym is None:
            options.append(["*"])
            continue

        literal_assignments = _infer_literal_assignments_near_site(java_text, usage.site.line, sym)
        if literal_assignments:
            opts = sorted(literal_assignments.keys())
            options.append(opts)
            continue

        rhs = _infer_last_assignment_expr_near_site(java_text, usage.site.line, sym)
        if rhs is None:
            options.append(["*"])
            continue

        rhs_kind, rhs_value = _expr_to_key_or_pattern(rhs)
        if rhs_kind == "certain":
            options.append([rhs_value])
            continue
        if rhs_kind == "pattern":
            options.append([rhs_value["pattern"]])
            continue

        options.append(["*"])

    # Cartesian product with caps.
    patterns: list[str] = [""]
    for seg_opts in options:
        # If a segment is already wildcard-y, avoid unnecessary explosion.
        if seg_opts == ["*"]:
            patterns = [p + "*" for p in patterns]
            continue

        next_patterns: list[str] = []
        for prefix in patterns:
            for opt in seg_opts:
                next_patterns.append(prefix + opt)
                if len(next_patterns) >= max_patterns:
                    break
            if len(next_patterns) >= max_patterns:
                break
        patterns = next_patterns
        if len(patterns) >= max_patterns:
            break

    # Normalize: collapse repeated '*' and de-dupe while preserving order.
    out: list[str] = []
    seen: set[str] = set()
    for p in patterns:
        norm = re.sub(r"\*+", "*", p)
        if norm not in seen:
            seen.add(norm)
            out.append(norm)

    return out


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

    parser.add_argument(
        "--ignore-file",
        type=Path,
        default=None,
        help=(
            "Optional ignore config file. If not provided, the script will auto-load "
            "<root>/scripts/message_key_audit_ignores.yml when it exists."
        ),
    )

    args = parser.parse_args(argv)

    root: Path = args.root.resolve()
    java_root = (args.java or root / "src" / "main" / "java").resolve()
    yaml_path = (args.yaml or root / "src" / "main" / "resources" / "messages" / "messages_en.yml").resolve()

    ignore_path = args.ignore_file.resolve() if args.ignore_file is not None else (root / "scripts" / "message_key_audit_ignores.yml")
    ignore_cfg = _load_ignore_config(ignore_path)
    ignore_expanded_exact, ignore_expanded_patterns = _compile_ignore_matchers(ignore_cfg.expanded_missing_in_yaml)

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
    expanded_keys_missing_in_yaml_all = sorted([k for k in expanded_keys if k not in yaml_keys])

    ignored_expanded_missing_in_yaml: list[str] = []
    ignored_expanded_missing_set: set[str] = set()
    if ignore_cfg.expanded_missing_in_yaml:
        for k in expanded_keys_missing_in_yaml_all:
            if _is_ignored_key(k, exact=ignore_expanded_exact, patterns=ignore_expanded_patterns):
                ignored_expanded_missing_in_yaml.append(k)
                ignored_expanded_missing_set.add(k)

    expanded_keys_missing_in_yaml = [k for k in expanded_keys_missing_in_yaml_all if k not in ignored_expanded_missing_set]

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

    for p in all_patterns:
        java_text = java_text_by_file.get(p.site.file)

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
            continue

        # Expand to multiple concrete patterns when possible.
        resolved_patterns: list[str]
        if java_text:
            resolved_patterns = _patterns_for_pattern_usage(p, java_text)
        else:
            resolved_patterns = [p.pattern]

        if len(resolved_patterns) == 1 and resolved_patterns[0] == p.pattern:
            pattern_sources.append(
                PatternMatcherSource(
                    pattern=p.pattern,
                    source_kind="concat",
                    site=p.site,
                    original_pattern=None,
                )
            )
        else:
            for rp in resolved_patterns:
                pattern_sources.append(
                    PatternMatcherSource(
                        pattern=rp,
                        source_kind="enhanced_concat",
                        site=p.site,
                        original_pattern=p.pattern,
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
    if ignore_cfg.expanded_missing_in_yaml or ignored_expanded_missing_in_yaml:
        print(f"Ignore rules (expanded):     {len(ignore_cfg.expanded_missing_in_yaml)}")
        print(f"Ignored expanded-missing:    {len(ignored_expanded_missing_in_yaml)}")
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
        print("Possibly missing in YAML (expanded from patterns)")
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

    if ignored_expanded_missing_in_yaml:
        print("Ignored expanded-missing keys")
        print("----------------------------")
        max_show = 250
        for key in ignored_expanded_missing_in_yaml[:max_show]:
            site = next((u.site for u in expanded_key_usages if u.key == key), None)
            if site is None:
                print(f"- {key}")
            else:
                print(f"- {key}  ({site.file}:{site.line})")
        if len(ignored_expanded_missing_in_yaml) > max_show:
            print(f"... and {len(ignored_expanded_missing_in_yaml) - max_show} more")
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
        # Explain WHY a YAML key landed here by grouping it by the pattern usage that inferred it.
        inferred_set = set(unused_but_inferred_by_expansion)

        pattern_usage_by_site: dict[tuple[str, int, str], PatternKeyUsage] = {}
        for p in all_patterns:
            pattern_usage_by_site.setdefault((p.site.file, p.site.line, p.site.method), p)

        groups: dict[tuple[str, str, int, str], list[ExpandedKeyUsage]] = {}
        for u in expanded_key_usages:
            if u.key not in inferred_set:
                continue
            if u.key not in yaml_keys:
                # This section is specifically for YAML keys.
                continue
            groups.setdefault((u.pattern, u.site.file, u.site.line, u.site.method), []).append(u)

        # Sort groups in a stable, readable way.
        sorted_groups = sorted(groups.items(), key=lambda kv: kv[0])

        max_pattern_groups = 40
        max_keys_per_group = 30
        total_key_cap = 500

        shown_groups = 0
        shown_keys = 0

        for (pattern, file, line, method), usages in sorted_groups:
            if shown_groups >= max_pattern_groups or shown_keys >= total_key_cap:
                break

            java_text = java_text_by_file.get(file)
            pattern_usage = pattern_usage_by_site.get((file, line, method))
            resolved_source_patterns: list[str] = []
            if java_text and pattern_usage:
                resolved_source_patterns = _patterns_for_pattern_usage(pattern_usage, java_text)

            # Summarize symbol values used to infer keys for this pattern.
            values_by_symbol: dict[str, set[str]] = {}
            for u in usages:
                for sym, vals in u.symbols.items():
                    values_by_symbol.setdefault(sym, set()).update(vals)

            symbol_summary = ""
            if values_by_symbol:
                parts: list[str] = []
                for sym in sorted(values_by_symbol.keys()):
                    vals = sorted(values_by_symbol[sym])
                    if not vals:
                        continue
                    shown = ",".join(vals[:6])
                    suffix = "" if len(vals) <= 6 else f"(+{len(vals) - 6})"
                    parts.append(f"{sym}={shown}{suffix}")
                if parts:
                    symbol_summary = "  symbols: " + "; ".join(parts)

            # Group header
            keys_in_group = sorted({u.key for u in usages})
            print(f"template: {pattern}  ({file}:{line})  ({method})  -> {len(keys_in_group)} key(s)")
            if pattern_usage is not None:
                print(f"  expr: {pattern_usage.site.arg_expr}")
            if resolved_source_patterns:
                show = 8
                shown_resolved = ", ".join(resolved_source_patterns[:show])
                suffix = "" if len(resolved_source_patterns) <= show else f" (+{len(resolved_source_patterns) - show} more)"
                print(f"  resolved_patterns: {shown_resolved}{suffix}")
            if symbol_summary:
                print(symbol_summary)

            # Show keys (with per-key symbol trace when available)
            usages_by_key: dict[str, list[ExpandedKeyUsage]] = {}
            for u in usages:
                usages_by_key.setdefault(u.key, []).append(u)

            for key in keys_in_group[:max_keys_per_group]:
                if shown_keys >= total_key_cap:
                    break

                examples = usages_by_key.get(key) or []
                # Pick one example usage for this key and render its symbols compactly.
                symbol_trace = ""
                if examples and examples[0].symbols:
                    items: list[str] = []
                    for sym in sorted(examples[0].symbols.keys()):
                        vals = examples[0].symbols.get(sym) or []
                        if not vals:
                            continue
                        if len(vals) == 1:
                            items.append(f"{sym}={vals[0]}")
                        else:
                            items.append(f"{sym}={','.join(vals[:4])}{'...' if len(vals) > 4 else ''}")
                    if items:
                        symbol_trace = "  (via " + ", ".join(items) + ")"

                print(f"- {key}{symbol_trace}")
                shown_keys += 1

            remaining = len(keys_in_group) - min(len(keys_in_group), max_keys_per_group)
            if remaining > 0:
                print(f"... and {remaining} more key(s) for this pattern")

            shown_groups += 1

        remaining_groups = len(sorted_groups) - shown_groups
        remaining_keys = len(unused_but_inferred_by_expansion) - shown_keys
        if remaining_groups > 0:
            print(f"... and {remaining_groups} more pattern group(s)")
        if remaining_keys > 0 and shown_keys >= total_key_cap:
            print(f"... and {remaining_keys} more key(s) total")
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
            java_text = java_text_by_file.get(s.file)
            if java_text:
                resolved = _patterns_for_pattern_usage(p, java_text)
                if resolved and not (len(resolved) == 1 and resolved[0] == p.pattern):
                    show = 8
                    shown = ", ".join(resolved[:show])
                    suffix = "" if len(resolved) <= show else f" (+{len(resolved) - show} more)"
                    print(f"  resolved: {shown}{suffix}")
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
            if u.key in yaml_keys:
                marker = "OK"
            elif u.key in ignored_expanded_missing_set:
                marker = "IGNORED"
            else:
                marker = "MISSING"
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
            "ignore_file": str(ignore_path),
            "ignore_rules": {
                "expanded_missing_in_yaml": list(ignore_cfg.expanded_missing_in_yaml),
            },
            "summary": {
                "java_files_scanned": len(java_files),
                "total_usages": len(all_certain) + len(all_patterns) + len(all_uncertain),
                "unique_certain_keys": len(certain_keys),
                "pattern_usages": len(all_patterns),
                "expanded_pattern_usages": len(expanded_key_usages),
                "expanded_missing_in_yaml": len(expanded_keys_missing_in_yaml),
                "ignored_expanded_missing_in_yaml": len(ignored_expanded_missing_in_yaml),
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
            "ignored_expanded_missing_in_yaml": ignored_expanded_missing_in_yaml,
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
                    "status": (
                        "ok"
                        if u.key in yaml_keys
                        else "ignored"
                        if u.key in ignored_expanded_missing_set
                        else "missing"
                    ),
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

'''
