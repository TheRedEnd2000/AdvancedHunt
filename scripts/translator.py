import yaml
import re
from googletrans import Translator
import time
import os

# =========================
# User input
# =========================
source_file = input("Enter the source file (e.g., messages_en.yml) [default: messages_en.yml]: ").strip() or "messages_en.yml"
target_file = input("Enter the target file (e.g., messages_de.yml) [default: messages_de.yml]: ").strip() or "messages_de.yml"
dest_lang = input("Enter the target language (e.g., de, fr, es) [default: de]: ").strip() or "de"

if not os.path.exists(source_file):
    print(f"Error: File '{source_file}' does not exist!")
    exit()

# =========================
# Load YAML
# =========================
with open(source_file, "r", encoding="utf-8") as f:
    data = yaml.safe_load(f)

translator = Translator()
placeholder_pattern = re.compile(r"%\w+%|&\w")
failed_translations = {}

# =========================
# Translate a single string with retry
# =========================
def translate_string(text, path, retries=3, delay=0.5):
    # Skip None or empty/quote-only strings
    if text is None or str(text).strip() in ("", "''", '""'):
        return str(text)

    placeholders = {}
    temp_text = str(text)

    # Protect placeholders
    for i, match in enumerate(re.findall(placeholder_pattern, temp_text)):
        token = f"__PLACEHOLDER_{i}__"
        placeholders[token] = match
        temp_text = temp_text.replace(match, token)

    if not temp_text.strip():
        return temp_text

    for attempt in range(1, retries + 1):
        try:
            translated = translator.translate(temp_text, src="en", dest=dest_lang).text
            if translated is None or translated.strip() == "":
                raise ValueError("Empty translation received")
            break
        except Exception as e:
            print(f"⚠ Attempt {attempt}/{retries} failed for {path}: {e}")
            time.sleep(delay)
    else:
        print(f"✖ Failed to translate {path} after {retries} attempts")
        failed_translations[path] = text
        translated = temp_text

    for token, original in placeholders.items():
        translated = translated.replace(token, original)

    return translated

# =========================
# Count all strings
# =========================
def count_strings(d):
    if isinstance(d, dict):
        return sum(count_strings(v) for v in d.values())
    if isinstance(d, list):
        return sum(count_strings(v) for v in d)
    if isinstance(d, str):
        return 1
    return 0

total_strings = count_strings(data)
current_count = 0
start_time = time.time()

# =========================
# Recursively translate YAML with ETA
# =========================
def translate_yaml(d, path=""):
    global current_count

    if isinstance(d, dict):
        return {k: translate_yaml(v, f"{path}.{k}" if path else k) for k, v in d.items()}

    if isinstance(d, list):
        return [translate_yaml(item, f"{path}[{i}]") for i, item in enumerate(d)]

    if isinstance(d, str):
        # Skip empty/quote-only strings for speed
        if d.strip() in ("", "''", '""'):
            translated = d
        else:
            translated = translate_string(d, path)

        current_count += 1
        percent = (current_count / total_strings) * 100

        elapsed = time.time() - start_time
        avg_per_item = elapsed / current_count
        remaining = avg_per_item * (total_strings - current_count)
        eta_min, eta_sec = divmod(int(remaining), 60)

        print(f"[{percent:6.2f}%] {current_count}/{total_strings}  {path}  ETA: {eta_min}m{eta_sec}s")
        print(f"  EN: {d[:60]} ...")
        print(f"  {dest_lang.upper()}: {translated[:60]} ...\n")

        time.sleep(0.2)
        return translated

    return d

# =========================
# First translation pass
# =========================
translated_data = translate_yaml(data)

# =========================
# Retry failed translations
# =========================
if failed_translations:
    print("\n🔁 Retrying failed translations...\n")
    for path, original_text in failed_translations.copy().items():
        try:
            cursor = translated_data
            parts = re.findall(r"[^\.\[\]]+|\[\d+\]", path)
            for part in parts[:-1]:
                cursor = cursor[int(part[1:-1])] if part.startswith("[") else cursor[part]
            last = parts[-1]
            translated = translate_string(original_text, path)
            if last.startswith("["):
                cursor[int(last[1:-1])] = translated
            else:
                cursor[last] = translated
            failed_translations.pop(path)
            print(f"✔ Retried {path}")
            time.sleep(0.2)
        except Exception as e:
            print(f"✖ Failed again for {path}: {e}")

# =========================
# YAML Dumper (keys normal, values immer doppelt-quoted)
# =========================
class ValueQuotedDumper(yaml.SafeDumper):
    def represent_mapping(self, tag, mapping, flow_style=None):
        value = []
        for k, v in mapping.items():
            key_node = yaml.SafeDumper.represent_data(self, k)
            value_node = self.represent_value(v)
            value.append((key_node, value_node))
        return yaml.nodes.MappingNode(tag, value, flow_style=flow_style)

    def represent_sequence(self, tag, sequence, flow_style=None):
        nodes = [self.represent_value(i) for i in sequence]
        return yaml.nodes.SequenceNode(tag, nodes, flow_style=flow_style)

    def represent_value(self, data):
        if isinstance(data, str):
            return self.represent_scalar("tag:yaml.org,2002:str", data, style='"')
        elif isinstance(data, dict):
            return self.represent_mapping("tag:yaml.org,2002:map", data)
        elif isinstance(data, list):
            return self.represent_sequence("tag:yaml.org,2002:seq", data)
        else:
            return self.represent_data(data)

# =========================
# Save translated YAML
# =========================
with open(target_file, "w", encoding="utf-8") as f:
    yaml.dump(translated_data, f,
              Dumper=ValueQuotedDumper,
              allow_unicode=True,
              sort_keys=False,
              width=10**9,
              default_flow_style=False)

# =========================
# Summary
# =========================
if failed_translations:
    print(f"\n⚠ Some translations failed ({len(failed_translations)} / {total_strings}):")
    for k in failed_translations:
        print(f" - {k}")
else:
    print("\n✅ All strings translated successfully!")

print(f"\n✅ Translation complete! File saved as {target_file}")
