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