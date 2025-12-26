#!/usr/bin/env python3
"""
Refactors a message key by renaming it in Java source files.

Usage:
    python scripts/refactor_key.py <old_key> <new_key> [--dry-run]
"""

import argparse
import re
import sys
from pathlib import Path

# Configuration
JAVA_ROOT = Path("src/main/java")
YAML_FILE = Path("src/main/resources/messages/messages_en.yml")

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

def replace_in_java(old_key: str, new_key: str, java_root: Path, dry_run: bool = False) -> int:
    """
    Replaces occurrences of old_key with new_key in Java files.
    Returns the number of files modified.
    """
    modified_count = 0
    
    # Regex to find the key as a string literal
    # We capture the quotes to ensure we are replacing the content inside them
    # Group 1: Opening quote
    # Group 2: The key
    # Group 3: Closing quote
    key_regex = re.compile(rf'(")\s*{re.escape(old_key)}\s*(")')
    
    for java_file in java_root.rglob("*.java"):
        try:
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
                
            masked_content = _mask_java_comments(content)
            
            # Check if there are any matches
            if not key_regex.search(masked_content):
                continue
                
            # We have matches. We need to reconstruct the file content.
            # Since we are changing the length of the string, we can't just map indices 1:1 easily
            # if we do multiple replacements.
            # Easiest way: iterate matches in reverse order so index shifts don't affect previous matches.
            
            matches = list(key_regex.finditer(masked_content))
            if not matches:
                continue
                
            print(f"Found {len(matches)} usage(s) in {java_file}")
            
            if dry_run:
                continue
                
            # Apply replacements in reverse
            new_content = list(content)
            for match in reversed(matches):
                start, end = match.span()
                # Verify we are replacing what we think we are (sanity check)
                # The regex matched on masked_content, which has same length as content.
                # The content inside quotes should match old_key (ignoring whitespace if regex allowed it, 
                # but our regex is strict on the key text).
                
                # Construct the replacement string: "new_key"
                replacement = f'"{new_key}"'
                
                # Replace the slice
                new_content[start:end] = list(replacement)
                
            with open(java_file, 'w', encoding='utf-8') as f:
                f.write("".join(new_content))
                
            modified_count += 1
            
        except Exception as e:
            print(f"Error processing {java_file}: {e}", file=sys.stderr)
            
    return modified_count

def main():
    parser = argparse.ArgumentParser(description="Rename a message key in Java files.")
    parser.add_argument("old_key", help="The current message key")
    parser.add_argument("new_key", help="The new message key")
    parser.add_argument("--dry-run", action="store_true", help="Show what would happen without making changes")
    args = parser.parse_args()

    print(f"Refactoring '{args.old_key}' -> '{args.new_key}'")
    if args.dry_run:
        print("(Dry run mode - no files will be changed)")

    # 1. Update Java files
    count = replace_in_java(args.old_key, args.new_key, JAVA_ROOT, args.dry_run)
    
    print(f"\nSummary: {count} Java file(s) {'would be ' if args.dry_run else ''}updated.")
    
    # 2. Instructions for YAML
    print("\n--- IMPORTANT: YAML Update Required ---")
    print(f"This script does NOT automatically update {YAML_FILE}.")
    print("You must manually update the messages file:")
    print(f"1. Open {YAML_FILE}")
    print(f"2. Find the key corresponding to '{args.old_key}'")
    print(f"3. Rename or move it to match '{args.new_key}'")

if __name__ == "__main__":
    main()
