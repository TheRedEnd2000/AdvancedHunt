#!/usr/bin/env python3
"""
Applies refactoring operations to both Java source code and YAML messages file.
Handles renaming keys and moving YAML blocks while preserving comments.
"""

import re
import sys
from pathlib import Path
from typing import List, Tuple, Optional

# Configuration
JAVA_ROOT = Path("src/main/java")
YAML_FILE = Path("src/main/resources/messages/messages_en.yml")

class JavaRefactorer:
    def __init__(self, root: Path):
        self.root = root

    def replace_key(self, old_key: str, new_key: str) -> int:
        """Replaces occurrences of old_key with new_key in Java files."""
        count = 0
        # Regex matches "old_key..." literals
        # We capture the suffix (if any) in group 1
        regex = re.compile(rf'"{re.escape(old_key)}(.*?)"')
        
        for java_file in self.root.rglob("*.java"):
            try:
                with open(java_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                if regex.search(content):
                    # Replace with "new_key" + suffix
                    new_content = regex.sub(f'"{new_key}\\1"', content)
                    with open(java_file, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    count += 1
            except Exception as e:
                print(f"Error processing {java_file}: {e}")
        return count

class YamlRefactorer:
    def __init__(self, file_path: Path):
        self.path = file_path
        with open(file_path, 'r', encoding='utf-8') as f:
            self.lines = f.readlines()

    def save(self):
        with open(self.path, 'w', encoding='utf-8') as f:
            f.writelines(self.lines)

    def _get_indent(self, line: str) -> int:
        return len(line) - len(line.lstrip())

    def _find_key_line(self, key: str, start_line: int, indent: int) -> int:
        """Finds the line number of a key at a specific indentation."""
        key_part = f"{key}:"
        for i in range(start_line, len(self.lines)):
            line = self.lines[i]
            stripped = line.strip()
            if not stripped or stripped.startswith('#'):
                continue
            
            curr_indent = self._get_indent(line)
            if curr_indent < indent:
                return -1 # Left the scope
            
            if curr_indent == indent and stripped.startswith(key_part):
                return i
        return -1

    def _find_block_range(self, start_line: int) -> Tuple[int, int]:
        """Returns (start, end) inclusive of the block starting at start_line."""
        indent = self._get_indent(self.lines[start_line])
        end_line = start_line
        
        for i in range(start_line + 1, len(self.lines)):
            line = self.lines[i]
            stripped = line.strip()
            
            # Empty lines and comments belong to the previous block usually, 
            # but for simplicity we include them if they are indented or blank
            if not stripped:
                end_line = i
                continue
                
            curr_indent = self._get_indent(line)
            if curr_indent <= indent and not stripped.startswith('-'):
                # Found a sibling or parent, stop
                # But wait, if it's a comment at the same level?
                if stripped.startswith('#'):
                     end_line = i
                     continue
                return start_line, end_line
            
            end_line = i
            
        return start_line, end_line

    def rename_key(self, parent_path: List[str], old_name: str, new_name: str) -> bool:
        """Renames a key in place."""
        # 1. Navigate to parent
        curr_line = 0
        curr_indent = 0
        
        for p in parent_path:
            curr_line = self._find_key_line(p, curr_line, curr_indent)
            if curr_line == -1:
                print(f"Could not find parent path: {parent_path}")
                return False
            curr_indent += 2 # Assumption: 2 space indent increase
            curr_line += 1

        # 2. Find the key
        key_line = self._find_key_line(old_name, curr_line, curr_indent)
        if key_line == -1:
            print(f"Could not find key '{old_name}' under {parent_path}")
            return False

        # 3. Rename
        line = self.lines[key_line]
        self.lines[key_line] = line.replace(f"{old_name}:", f"{new_name}:", 1)
        return True

    def move_block(self, src_path: List[str], dest_path: List[str], new_name: str) -> bool:
        """Moves a block from src_path to dest_path, optionally renaming it."""
        # 1. Find Source
        src_parent = src_path[:-1]
        src_key = src_path[-1]
        
        # Navigate to source parent
        curr_line = 0
        curr_indent = 0
        for p in src_parent:
            curr_line = self._find_key_line(p, curr_line, curr_indent)
            if curr_line == -1: return False
            curr_indent += 2
            curr_line += 1
            
        src_line_idx = self._find_key_line(src_key, curr_line, curr_indent)
        if src_line_idx == -1:
            print(f"Source not found: {src_path}")
            return False
            
        src_start, src_end = self._find_block_range(src_line_idx)
        
        # 2. Extract lines
        block_lines = self.lines[src_start : src_end + 1]
        
        # 3. Find Destination
        # Navigate to dest parent
        curr_line = 0
        curr_indent = 0
        dest_line_idx = -1
        
        # We need to handle creating missing intermediate keys in dest_path
        # For simplicity, let's assume we only create ONE level if missing, or fail
        
        # Logic: Iterate dest_path. If found, enter. If not found, check if it's the last one.
        
        parent_indent = 0
        search_start = 0
        
        for i, p in enumerate(dest_path):
            found_line = self._find_key_line(p, search_start, parent_indent)
            
            if found_line != -1:
                # Found, go deeper
                search_start = found_line + 1
                parent_indent += 2
                dest_line_idx = found_line
            else:
                # Not found. Is it the last one?
                # If we are at "gui.rewards" and looking for "add", and "add" is missing,
                # we are effectively inserting "add" as a new block.
                # But wait, move_block moves INTO dest_path.
                # So dest_path MUST exist.
                print(f"Destination path not found: {dest_path} (missing '{p}')")
                return False

        # 4. Prepare Block
        # Calculate indent shift
        old_indent = self._get_indent(block_lines[0])
        new_indent = parent_indent
        indent_diff = new_indent - old_indent
        
        new_block_lines = []
        # Rename the first line key
        first_line = block_lines[0]
        # Replace key: "  old: ..." -> "  new: ..."
        # We need to be careful to only replace the key part
        key_match = re.match(r"^(\s*)([\w\-]+):", first_line)
        if key_match:
            prefix = key_match.group(1)
            rest = first_line[len(key_match.group(0)):]
            # Apply new indent to prefix? No, prefix is old indent.
            # We construct new line
            new_prefix = " " * new_indent
            new_block_lines.append(f"{new_prefix}{new_name}:{rest}")
        else:
            new_block_lines.append(first_line) # Should not happen

        # Process rest of lines
        for line in block_lines[1:]:
            if not line.strip():
                new_block_lines.append(line)
                continue
            # Adjust indent
            curr_line_indent = self._get_indent(line)
            # Preserve relative indentation
            rel_indent = curr_line_indent - old_indent
            final_indent = new_indent + rel_indent
            new_block_lines.append(" " * final_indent + line.lstrip())

        # 5. Remove old block (careful with indices, do this last or use offset)
        # We will remove first, then insert.
        # Remove from self.lines
        del self.lines[src_start : src_end + 1]
        
        # 6. Insert new block
        # We need to find the insertion point again because indices shifted
        # Re-find destination
        curr_line = 0
        curr_indent = 0
        dest_line_idx = -1
        for p in dest_path:
            curr_line = self._find_key_line(p, curr_line, curr_indent)
            curr_indent += 2
            dest_line_idx = curr_line
            curr_line += 1
            
        # Insert at the end of the destination block
        dest_start, dest_end = self._find_block_range(dest_line_idx)
        
        # We insert after dest_end
        insert_pos = dest_end + 1
        
        # If dest_end is the last line of file, append
        if insert_pos > len(self.lines):
            insert_pos = len(self.lines)
            
        self.lines[insert_pos:insert_pos] = new_block_lines
        return True

    def ensure_key(self, parent_path: List[str], key: str) -> bool:
        """Ensures a key exists under parent_path. If not, creates it."""
        # Navigate to parent
        curr_line = 0
        curr_indent = 0
        parent_line_idx = -1
        
        for p in parent_path:
            curr_line = self._find_key_line(p, curr_line, curr_indent)
            if curr_line == -1: return False
            curr_indent += 2
            parent_line_idx = curr_line
            curr_line += 1
            
        # Check if key exists
        if self._find_key_line(key, curr_line, curr_indent) != -1:
            return True
            
        # Create it
        # Insert at end of parent block
        parent_start, parent_end = self._find_block_range(parent_line_idx)
        insert_pos = parent_end + 1
        
        new_line = " " * curr_indent + f"{key}:\n"
        self.lines.insert(insert_pos, new_line)
        return True

def main():
    print("Starting refactoring...")
    java_refactorer = JavaRefactorer(JAVA_ROOT)
    yaml_refactorer = YamlRefactorer(YAML_FILE)
    
    # Define Operations
    # Format: (Old Java Key, New Java Key, Operation Type, Yaml Args)
    
    operations = [
        # 1. gui.who_found -> gui.finders
        ("gui.who_found.title", "gui.finders.title", "rename_key", (["gui"], "who_found", "finders")),
        ("gui.who_found.loading", "gui.finders.loading", "java_only", None),
        ("gui.who_found.no_one", "gui.finders.empty", "rename_key", (["gui", "finders"], "no_one", "empty")),
        ("gui.who_found.player_entry", "gui.finders.entry", "rename_key", (["gui", "finders"], "player_entry", "entry")),
        
        # 2. gui.reward_action -> gui.rewards.editor
        ("gui.reward_action", "gui.rewards.editor", "move_block", (["gui", "reward_action"], ["gui", "rewards"], "editor")),
        
        # 3. gui.add_item_reward -> gui.rewards.add.item
        # Need to ensure 'add' exists under 'gui.rewards'
        ("gui.add_item_reward", "gui.rewards.add.item", "ensure_key", (["gui", "rewards"], "add")),
        ("gui.add_item_reward", "gui.rewards.add.item", "move_block", (["gui", "add_item_reward"], ["gui", "rewards", "add"], "item")),
        
        # 4. gui.act_rules -> gui.act.rules.list
        # Ensure 'rules' exists under 'gui.act'
        ("gui.act_rules", "gui.act.rules.list", "ensure_key", (["gui", "act"], "rules")),
        ("gui.act_rules", "gui.act.rules.list", "move_block", (["gui", "act_rules"], ["gui", "act", "rules"], "list")),
        
        # 5. gui.act_rule_editor -> gui.act.rules.editor
        ("gui.act_rule_editor", "gui.act.rules.editor", "move_block", (["gui", "act_rule_editor"], ["gui", "act", "rules"], "editor")),
        
        # 6. gui.act_format -> gui.act.error.format
        # Ensure 'error' exists under 'gui.act'
        ("gui.act_format", "gui.act.error.format", "ensure_key", (["gui", "act"], "error")),
        ("gui.act_format", "gui.act.error.format", "move_block", (["gui", "act_format"], ["gui", "act", "error"], "format")),
        
        # 7. gui.act.duration -> gui.act.components.duration
        # Ensure 'components' exists under 'gui.act'
        ("gui.act.duration", "gui.act.components.duration", "ensure_key", (["gui", "act"], "components")),
        ("gui.act.duration", "gui.act.components.duration", "move_block", (["gui", "act", "duration"], ["gui", "act", "components"], "duration")),
        
        # 8. gui.act.date_range -> gui.act.components.date_range
        ("gui.act.date_range", "gui.act.components.date_range", "move_block", (["gui", "act", "date_range"], ["gui", "act", "components"], "date_range")),
        
        # 9. gui.progress_reset_cron -> gui.settings.reset_schedule
        ("gui.progress_reset_cron", "gui.settings.reset_schedule", "move_block", (["gui", "progress_reset_cron"], ["gui", "settings"], "reset_schedule")),

        # --- New Operations ---

        # 10. Separate Chat Messages (Feedback)
        # Ensure 'feedback' root key exists (we need to add it manually or ensure it)
        # Since ensure_key works on parent path, we need empty parent for root.
        # But our ensure_key implementation assumes parent_path has at least one element?
        # Let's check ensure_key implementation. It iterates parent_path. If empty, it does nothing loop-wise.
        # But then it checks key in root.
        # Wait, _find_key_line with empty parent path?
        # The implementation of ensure_key:
        # for p in parent_path: ...
        # If parent_path is [], it skips loop. curr_indent is 0. parent_line_idx is -1.
        # Then checks key at indent 0.
        # If not found, inserts at parent_end + 1.
        # _find_block_range(-1) -> starts at 0? No.
        # We need to fix ensure_key or just assume 'feedback' doesn't exist and we can append it?
        # Or we can just use "ensure_key" with empty list.
        
        # Let's try to ensure 'feedback' exists at root.
        ("feedback", "feedback", "ensure_key", ([], "feedback")),
        ("feedback.gui", "feedback.gui", "ensure_key", (["feedback"], "gui")),
        ("feedback.rewards", "feedback.rewards", "ensure_key", (["feedback"], "rewards")),
        ("feedback.rewards.prompt", "feedback.rewards.prompt", "ensure_key", (["feedback", "rewards"], "prompt")),
        ("feedback.teleport", "feedback.teleport", "ensure_key", (["feedback"], "teleport")),
        ("feedback.act", "feedback.act", "ensure_key", (["feedback"], "act")),
        ("feedback.cron", "feedback.cron", "ensure_key", (["feedback"], "cron")),

        # Move gui.common.first_page -> feedback.gui.first_page
        ("gui.common.first_page", "feedback.gui.first_page", "move_block", (["gui", "common", "first_page"], ["feedback", "gui"], "first_page")),
        ("gui.common.last_page", "feedback.gui.last_page", "move_block", (["gui", "common", "last_page"], ["feedback", "gui"], "last_page")),

        # Move gui.rewards prompts
        ("gui.rewards.enter_command", "feedback.rewards.prompt.command", "move_block", (["gui", "rewards", "enter_command"], ["feedback", "rewards", "prompt"], "command")),
        ("gui.rewards.enter_chat_message", "feedback.rewards.prompt.chat_message", "move_block", (["gui", "rewards", "enter_chat_message"], ["feedback", "rewards", "prompt"], "chat_message")),
        ("gui.rewards.enter_broadcast_message", "feedback.rewards.prompt.broadcast_message", "move_block", (["gui", "rewards", "enter_broadcast_message"], ["feedback", "rewards", "prompt"], "broadcast_message")),
        ("gui.rewards.enter_chance", "feedback.rewards.prompt.chance", "move_block", (["gui", "rewards", "enter_chance"], ["feedback", "rewards", "prompt"], "chance")),
        ("gui.rewards.enter_new_chance", "feedback.rewards.prompt.new_chance", "move_block", (["gui", "rewards", "enter_new_chance"], ["feedback", "rewards", "prompt"], "new_chance")),
        
        # Move gui.rewards feedback
        ("gui.rewards.reward_added", "feedback.rewards.added", "move_block", (["gui", "rewards", "reward_added"], ["feedback", "rewards"], "added")),
        ("gui.rewards.reward_deleted", "feedback.rewards.deleted", "move_block", (["gui", "rewards", "reward_deleted"], ["feedback", "rewards"], "deleted")),
        ("gui.rewards.instance_given", "feedback.rewards.instance_given", "move_block", (["gui", "rewards", "instance_given"], ["feedback", "rewards"], "instance_given")),
        ("gui.rewards.instance_dropped", "feedback.rewards.instance_dropped", "move_block", (["gui", "rewards", "instance_dropped"], ["feedback", "rewards"], "instance_dropped")),
        ("gui.rewards.instance_invalid", "feedback.rewards.instance_invalid", "move_block", (["gui", "rewards", "instance_invalid"], ["feedback", "rewards"], "instance_invalid")),

        # Move gui.treasure_action.teleport feedback
        ("gui.treasure_action.teleport.success", "feedback.teleport.success", "move_block", (["gui", "treasure_action", "teleport", "success"], ["feedback", "teleport"], "success")),
        ("gui.treasure_action.teleport.unsafe", "feedback.teleport.unsafe", "move_block", (["gui", "treasure_action", "teleport", "unsafe"], ["feedback", "teleport"], "unsafe")),
        ("gui.treasure_action.teleport.invalid_location", "feedback.teleport.invalid_location", "move_block", (["gui", "treasure_action", "teleport", "invalid_location"], ["feedback", "teleport"], "invalid_location")),
        ("gui.treasure_action.teleport.adjusted", "feedback.teleport.adjusted", "move_block", (["gui", "treasure_action", "teleport", "adjusted"], ["feedback", "teleport"], "adjusted")),

        # Move ACT/Cron feedback
        ("gui.act.success.applied", "feedback.act.applied", "move_block", (["gui", "act", "success", "applied"], ["feedback", "act"], "applied")),
        ("gui.cron.success.applied", "feedback.cron.applied", "move_block", (["gui", "cron", "success", "applied"], ["feedback", "cron"], "applied")),
        ("gui.cron.success.cleared", "feedback.cron.cleared", "move_block", (["gui", "cron", "success", "cleared"], ["feedback", "cron"], "cleared")),
        ("gui.cron.success.preset", "feedback.cron.preset_applied", "move_block", (["gui", "cron", "success", "preset"], ["feedback", "cron"], "preset_applied")),
        
        # Note: gui.act.date_range.success and gui.act.duration.success might be tricky if they are inside components now?
        # They were at gui.act.date_range.success. We moved gui.act.date_range to gui.act.components.date_range.
        # So now they are at gui.act.components.date_range.success?
        # Wait, the previous move_block moved the whole block.
        # So yes, they are now at gui.act.components.date_range.success.
        ("gui.act.date_range.success", "feedback.act.date_range.success", "ensure_key", (["feedback", "act"], "date_range")),
        ("gui.act.date_range.success", "feedback.act.date_range.success", "move_block", (["gui", "act", "components", "date_range", "success"], ["feedback", "act", "date_range"], "success")),
        
        ("gui.act.duration.success", "feedback.act.duration.success", "ensure_key", (["feedback", "act"], "duration")),
        ("gui.act.duration.success", "feedback.act.duration.success", "move_block", (["gui", "act", "components", "duration", "success"], ["feedback", "act", "duration"], "success")),

        # 11. Standardize Errors
        # Ensure 'error.validation' exists
        ("error.validation", "error.validation", "ensure_key", (["error"], "validation")),
        ("gui.common.error.validation.date_range", "error.validation.date_range", "move_block", (["gui", "common", "error", "validation", "date_range"], ["error", "validation"], "date_range")),
        ("gui.common.error.validation.duration", "error.validation.duration", "move_block", (["gui", "common", "error", "validation", "duration"], ["error", "validation"], "duration")),
        
        # Move other errors
        ("error.rewards", "error.rewards", "ensure_key", (["error"], "rewards")),
        ("gui.rewards.invalid_chance", "error.rewards.invalid_chance", "move_block", (["gui", "rewards", "invalid_chance"], ["error", "rewards"], "invalid_chance")),
        ("gui.rewards.command_blacklisted", "error.rewards.command_blacklisted", "move_block", (["gui", "rewards", "command_blacklisted"], ["error", "rewards"], "command_blacklisted")),
        
        ("error.cron", "error.cron", "ensure_key", (["error"], "cron")),
        ("gui.cron.error.invalid", "error.cron.invalid", "move_block", (["gui", "cron", "error", "invalid"], ["error", "cron"], "invalid")),

        # 12. Consolidate Feature Messages
        # Minigame error
        ("minigame.error", "minigame.error", "ensure_key", (["minigame"], "error")),
        ("command.minigame.invalid_type", "minigame.error.invalid_type", "move_block", (["command", "minigame", "invalid_type"], ["minigame", "error"], "invalid_type")),
    ]
    
    # Execute
    for old_key, new_key, op_type, args in operations:
        print(f"Processing: {old_key} -> {new_key} ({op_type})")
        
        # Java Update (Always replace the full key string)
        # Note: For "move_block" operations where we move a parent, we need to be careful.
        # If we move "gui.reward_action" to "gui.rewards.editor", we want to replace
        # "gui.reward_action.title" with "gui.rewards.editor.title".
        # The simple string replacement "gui.reward_action" -> "gui.rewards.editor" handles this!
        
        if op_type != "ensure_key" and op_type != "skip":
            count = java_refactorer.replace_key(old_key, new_key)
            print(f"  Java: Updated {count} files")
        
        # YAML Update
        if op_type == "rename_key":
            parent, old, new = args
            success = yaml_refactorer.rename_key(parent, old, new)
            print(f"  YAML Rename: {'Success' if success else 'Failed'}")
            
        elif op_type == "move_block":
            src, dest, new_name = args
            success = yaml_refactorer.move_block(src, dest, new_name)
            print(f"  YAML Move: {'Success' if success else 'Failed'}")
            
        elif op_type == "ensure_key":
            parent, key = args
            success = yaml_refactorer.ensure_key(parent, key)
            print(f"  YAML Ensure Key: {'Success' if success else 'Failed'}")
            
    yaml_refactorer.save()
    print("Done! Please verify messages_en.yml formatting.")

if __name__ == "__main__":
    main()
