package de.theredend2000.advancedhunt.configurations;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigMigration {
    private final boolean hasTemplate;
    private final List<ReplacementEntry> keyReplacements;
    private final List<ReplacementEntry> valueReplacements;

    public ConfigMigration(boolean hasTemplate, List<ReplacementEntry> keyReplacements, List<ReplacementEntry> valueReplacements) {
        this.hasTemplate = hasTemplate;
        this.keyReplacements = keyReplacements;
        this.valueReplacements = valueReplacements;
    }

    public void standardUpgrade(YamlConfiguration oldConfig, YamlConfiguration newConfig) {
        List<String> keyList = new ArrayList<>(oldConfig.getKeys(true));
        keyList.sort((key1, key2) -> Integer.compare(key2.split("\\.").length, key1.split("\\.").length));

        for (String oldKey : keyList) {
            if (oldConfig.isSet(oldKey) && !oldConfig.isConfigurationSection(oldKey)) {
                String newKey = applyReplacements(oldKey, keyReplacements);
                Object value = oldConfig.get(oldKey);

                if (value instanceof String) {
                    value = applyReplacements((String) value, valueReplacements);
                } else if (value instanceof List) {
                    value = applyReplacementsToList((List<?>) value, valueReplacements);
                }

                if (!hasTemplate || newConfig.contains(newKey)) {
                    newConfig.set(newKey, value);
                }
            }
        }
    }

    private String applyReplacements(String input, List<ReplacementEntry> replacements) {
        if (replacements == null || replacements.isEmpty()) {
            return input;
        }
        for (ReplacementEntry replacement : replacements) {
            if (replacement.isRegex()) {
                input = Pattern.compile(replacement.getSearch()).matcher(input).replaceAll(replacement.getReplace());
            } else if (replacement.isIgnoreCase()) {
                input = input.replaceAll("(?i)" + Pattern.quote(replacement.getSearch()), Matcher.quoteReplacement(replacement.getReplace()));
            } else {
                input = input.replace(replacement.getSearch(), replacement.getReplace());
            }
        }
        return input;
    }

    private List<?> applyReplacementsToList(List<?> list, List<ReplacementEntry> replacements) {
        if (replacements == null || replacements.isEmpty()) {
            return list;
        }
        List<Object> newList = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String) {
                newList.add(applyReplacements((String) item, replacements));
            } else if (item instanceof List) {
                newList.add(applyReplacementsToList((List<?>) item, replacements));
            } else {
                newList.add(item);
            }
        }
        return newList;
    }

    public static class ReplacementEntry {
        private final String search;
        private final String replace;
        private final boolean regex;
        private final boolean ignoreCase;

        public ReplacementEntry(String search, String replace, boolean regex, boolean ignoreCase) {
            this.search = search;
            this.replace = replace;
            this.regex = regex;
            this.ignoreCase = ignoreCase;
        }

        public String getSearch() {
            return search;
        }

        public String getReplace() {
            return replace;
        }

        public boolean isRegex() {
            return regex;
        }

        public boolean isIgnoreCase() {
            return ignoreCase;
        }
    }
}
