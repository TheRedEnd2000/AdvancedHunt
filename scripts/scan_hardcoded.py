#!/usr/bin/env python3
"""
Scans Java files for hardcoded strings that should likely be localized.

Usage:
    python scripts/scan_hardcoded.py
"""

import re
import sys
from pathlib import Path

# Configuration
JAVA_ROOT = Path("src/main/java")

# Methods that typically take user-facing strings
SUSPICIOUS_METHODS = {
    "sendMessage", "sendRichMessage", "sendActionBar", "sendTitle",
    "text", "content", # Adventure/Paper components
    "setDisplayName", "setLore", "setName", # ItemMeta
    "title", "header", "footer", # Inventory/Tablist
    "broadcast", "broadcastMessage"
}

# Strings to ignore (internal keys, config paths, etc.)
IGNORE_PATTERNS = [
    r'^[\w\.-]+$', # Likely a config key or permission node (no spaces)
    r'^SELECT .*', # SQL
    r'^INSERT .*',
    r'^UPDATE .*',
    r'^DELETE .*',
    r'^CREATE .*',
    r'^%', # Likely a standalone placeholder
    r'^&[0-9a-fk-or]$', # Color code only
    r'^$', # Empty string
]

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

        # Start of comments?
        if ch == "/" and i + 1 < len(out):
            next_ch = out[i + 1]
            if next_ch == "/":
                in_line_comment = True
                out[i] = " "
                out[i + 1] = " "
                i += 2
                continue
            elif next_ch == "*":
                in_block_comment = True
                out[i] = " "
                out[i + 1] = " "
                i += 2
                continue

        # Start of literals?
        if ch == '"':
            in_string = True
        elif ch == "'":
            in_char = True

        i += 1

    return "".join(out)

def scan_file(java_file: Path):
    try:
        with open(java_file, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception as e:
        print(f"Error reading {java_file}: {e}", file=sys.stderr)
        return

    masked_content = _mask_java_comments(content)
    
    # Regex to find string literals
    string_literal_re = re.compile(r'"((?:\\.|[^"\\])*)"')
    
    for match in string_literal_re.finditer(masked_content):
        literal = match.group(1)
        start_pos = match.start()
        
        # Filter out obvious non-messages
        should_ignore = False
        for pattern in IGNORE_PATTERNS:
            if re.match(pattern, literal):
                should_ignore = True
                break
        if should_ignore:
            continue
            
        # Check context (preceding text)
        # Look backwards from start_pos for the method name
        # We scan back say 50 chars
        context_start = max(0, start_pos - 50)
        preceding_text = masked_content[context_start:start_pos]
        
        # Check if any suspicious method is in the preceding text
        # We look for "methodName(\s*" or "methodName\s*\("
        is_suspicious = False
        found_method = ""
        
        for method in SUSPICIOUS_METHODS:
            # Simple check: is the method name present followed by an open paren?
            # This is loose but effective enough
            if re.search(rf'{method}\s*\(.*$', preceding_text, re.DOTALL):
                is_suspicious = True
                found_method = method
                break
                
        # Also check for ItemBuilder pattern: .name("...") or .lore("...")
        if not is_suspicious:
            if re.search(r'\.(name|lore|addLoreLine)\s*\(\s*$', preceding_text):
                is_suspicious = True
                found_method = "ItemBuilder"

        if is_suspicious:
            line_num = content.count('\n', 0, start_pos) + 1
            print(f"{java_file}:{line_num} [{found_method}]")
            print(f"  \"{literal}\"")

def main():
    print("Scanning for hardcoded strings in suspicious contexts...")
    print(f"Looking for methods: {', '.join(SUSPICIOUS_METHODS)}\n")
    
    for java_file in JAVA_ROOT.rglob("*.java"):
        scan_file(java_file)

if __name__ == "__main__":
    main()
