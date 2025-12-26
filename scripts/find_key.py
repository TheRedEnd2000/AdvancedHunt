#!/usr/bin/env python3
"""
Finds definition and usages of a specific message key.

Usage:
    python scripts/find_key.py <key_name>
"""

import argparse
import re
import sys
from pathlib import Path
from typing import List, Tuple

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

def find_in_yaml(key: str, yaml_path: Path) -> List[Tuple[int, str]]:
    """
    Finds the line number where the key is defined in the YAML file.
    This is a simple text-based search and might be fragile with complex YAML features,
    but works for standard message files.
    """
    if not yaml_path.exists():
        return []

    parts = key.split('.')
    current_indent = -1
    part_index = 0
    
    # This is a simplified search. For exact YAML parsing with line numbers, 
    # we would need a library like ruamel.yaml. 
    # Here we try to match the indentation hierarchy.
    
    found_lines = []
    
    # Heuristic: Search for the leaf key "key:"
    leaf_key = parts[-1]
    regex = re.compile(rf'^\s*{re.escape(leaf_key)}:')
    
    with open(yaml_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
        
    for i, line in enumerate(lines):
        if regex.search(line):
            # Verify context if possible (hard without full parser)
            # For now, just return all matches of the leaf key
            found_lines.append((i + 1, line.strip()))
            
    return found_lines

def find_in_java(key: str, java_root: Path) -> List[Tuple[Path, int, str]]:
    """Finds usages of the key in Java files."""
    usages = []
    
    # Regex to find the key as a string literal
    # Matches "key"
    key_regex = re.compile(rf'"\s*{re.escape(key)}\s*"')
    
    for java_file in java_root.rglob("*.java"):
        try:
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
                
            masked_content = _mask_java_comments(content)
            
            # Find all occurrences
            for match in key_regex.finditer(masked_content):
                # Calculate line number
                start_pos = match.start()
                line_num = content.count('\n', 0, start_pos) + 1
                
                # Get the line content for display
                line_start = content.rfind('\n', 0, start_pos) + 1
                line_end = content.find('\n', start_pos)
                if line_end == -1: line_end = len(content)
                line_content = content[line_start:line_end].strip()
                
                usages.append((java_file, line_num, line_content))
                
        except Exception as e:
            print(f"Error reading {java_file}: {e}", file=sys.stderr)
            
    return usages

def main():
    parser = argparse.ArgumentParser(description="Find definition and usages of a message key.")
    parser.add_argument("key", help="The message key to search for (e.g., 'command.list.header')")
    args = parser.parse_args()

    print(f"Searching for key: '{args.key}'\n")

    # 1. Find in YAML
    print(f"--- Definition in {YAML_FILE} ---")
    yaml_matches = find_in_yaml(args.key, YAML_FILE)
    if yaml_matches:
        for line_num, content in yaml_matches:
            print(f"Line {line_num}: {content}")
    else:
        print("Not found in YAML file (or could not be parsed with simple heuristic).")

    print("\n--- Usages in Java Code ---")
    java_usages = find_in_java(args.key, JAVA_ROOT)
    if java_usages:
        for path, line, content in java_usages:
            print(f"{path}:{line}")
            print(f"  {content}")
    else:
        print("No usages found in Java files.")

if __name__ == "__main__":
    main()
