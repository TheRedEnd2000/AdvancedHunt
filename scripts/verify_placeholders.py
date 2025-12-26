#!/usr/bin/env python3
"""
Verifies that placeholders used in Java code match those in the YAML message file.

Usage:
    python scripts/verify_placeholders.py
"""

import re
import sys
from pathlib import Path
from typing import Dict, List, Set

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

def load_yaml_messages(yaml_path: Path) -> Dict[str, str]:
    """
    Loads messages from YAML into a flat dict {key: message_string}.
    This is a simplified parser that assumes standard formatting.
    """
    messages = {}
    if not yaml_path.exists():
        return messages

    # We'll use a stack to track current key hierarchy
    key_stack = []
    
    # Regex for "key:" or "key: value"
    # Group 1: indent
    # Group 2: key
    # Group 3: value (optional)
    line_re = re.compile(r'^(\s*)([\w\-\.]+):\s*(.*)$')
    
    with open(yaml_path, 'r', encoding='utf-8') as f:
        for line in f:
            line_stripped = line.strip()
            if not line_stripped or line_stripped.startswith('#'):
                continue
                
            match = line_re.match(line)
            if match:
                indent = len(match.group(1))
                key_part = match.group(2)
                value = match.group(3).strip()
                
                # Determine depth based on indent (assuming 2 spaces per level)
                depth = indent // 2
                
                # Adjust stack
                key_stack = key_stack[:depth]
                key_stack.append(key_part)
                
                full_key = ".".join(key_stack)
                
                # If value is present and looks like a string
                if value:
                    # Remove quotes if present
                    if (value.startswith('"') and value.endswith('"')) or \
                       (value.startswith("'") and value.endswith("'")):
                        value = value[1:-1]
                    messages[full_key] = value
                    
    return messages

def extract_placeholders_from_java(java_root: Path) -> Dict[str, Set[str]]:
    """
    Scans Java files for getMessage calls and extracts placeholders.
    Returns {key: {placeholder1, placeholder2, ...}}
    """
    key_placeholders = {}
    
    # Regex to find getMessage calls
    # We look for: getMessage("key", ... args ...)
    # This is tricky with regex. We'll find the key, then scan ahead for string literals starting with %
    
    key_regex = re.compile(r'getMessage\s*\(\s*"([^"]+)"')
    placeholder_regex = re.compile(r'"(%[^%]+%)"')
    
    for java_file in java_root.rglob("*.java"):
        try:
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
                
            masked_content = _mask_java_comments(content)
            
            for match in key_regex.finditer(masked_content):
                key = match.group(1)
                end_pos = match.end()
                
                # Scan ahead for arguments until the closing parenthesis of the method call
                # This is a heuristic: we scan until we see a semicolon or a closing paren that seems to end the statement
                # A better way is to just scan the next N characters or lines
                
                # Let's look at the next 500 chars
                snippet = masked_content[end_pos:end_pos+500]
                
                # Find all "%...%" literals in this snippet
                # We stop if we hit a semicolon (end of statement) to avoid bleeding into next lines too much
                statement_end = snippet.find(';')
                if statement_end != -1:
                    snippet = snippet[:statement_end]
                    
                found_placeholders = set(placeholder_regex.findall(snippet))
                
                if key not in key_placeholders:
                    key_placeholders[key] = set()
                key_placeholders[key].update(found_placeholders)
                
        except Exception as e:
            print(f"Error reading {java_file}: {e}", file=sys.stderr)
            
    return key_placeholders

def main():
    print("Loading YAML messages...")
    yaml_messages = load_yaml_messages(YAML_FILE)
    
    print("Scanning Java files for placeholders...")
    java_placeholders = extract_placeholders_from_java(JAVA_ROOT)
    
    print("\n--- Placeholder Verification Report ---")
    
    issues_found = False
    
    for key, used_placeholders in java_placeholders.items():
        if not used_placeholders:
            continue
            
        if key not in yaml_messages:
            # Missing keys are handled by the audit script, but we can mention it
            # print(f"Key '{key}' not found in YAML.")
            continue
            
        message = yaml_messages[key]
        
        # Find placeholders in the YAML message
        # Regex: %word%
        yaml_placeholders = set(re.findall(r'%[\w\-]+%', message))
        
        # Check for mismatches
        # 1. Placeholders provided in Java but NOT in YAML (Superfluous)
        unused = used_placeholders - yaml_placeholders
        # 2. Placeholders in YAML but NOT provided in Java (Missing)
        # This is harder because Java might provide them in a way we didn't detect (variables)
        # So we focus on "Java provides literal %foo% but YAML doesn't have it"
        
        if unused:
            print(f"Key: {key}")
            print(f"  Message: \"{message}\"")
            print(f"  Java provides: {', '.join(used_placeholders)}")
            print(f"  YAML expects:  {', '.join(yaml_placeholders)}")
            print(f"  WARNING: Java provides unused placeholders: {', '.join(unused)}")
            print("")
            issues_found = True

    if not issues_found:
        print("No obvious placeholder mismatches found.")

if __name__ == "__main__":
    main()
