package de.theredend2000.advancedhunt.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for updating configuration files while preserving comments and structure.
 */
public class ConfigUpdater {

    private static final int INDENTATION_SIZE = 2;

    @FunctionalInterface
    public interface MigrationCallback {
        void migrate(FileConfiguration config, int currentVersion, int newVersion);
    }

    /**
     * Updates the given configuration file to match the structure of the internal resource.
     * Preserves user values where keys match.
     *
     * @param plugin       The plugin instance.
     * @param resourceName The name of the resource in the JAR (e.g., "config.yml").
     * @param file         The file on disk to update.
     */
    public static void update(Plugin plugin, String resourceName, File file) {
        update(plugin, resourceName, file, null);
    }

    /**
     * Updates the given configuration file with custom migration logic.
     *
     * @param plugin       The plugin instance.
     * @param resourceName The name of the resource in the JAR.
     * @param file         The file on disk to update.
     * @param migrator     A callback that accepts the current user config, current version, and new version.
     *                     Use this to modify the config in-memory before the update is applied.
     */
    public static void update(Plugin plugin, String resourceName, File file, MigrationCallback migrator) {
        FileConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
        
        // If the file doesn't exist, just save the default
        if (!file.exists()) {
            plugin.saveResource(resourceName, false);
            return;
        }

        int currentVersion = userConfig.getInt("config-version", 0);
        
        // Load the default config from JAR to check its version
        InputStream resource = plugin.getResource(resourceName);
        if (resource == null) return;
        
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
        int newVersion = defaultConfig.getInt("config-version", 1);

        if (currentVersion >= newVersion) {
            return;
        }

        // Run migration logic if provided
        if (migrator != null) {
            migrator.migrate(userConfig, currentVersion, newVersion);
        }

        plugin.getLogger().info("Updating " + resourceName + " from version " + currentVersion + " to " + newVersion + "...");

        createBackup(plugin, file);

        // Perform update
        try {
            // Re-open resource for reading lines
            resource = plugin.getResource(resourceName);
            List<String> newLines = updateLines(resource, userConfig);
            
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
                for (String line : newLines) {
                    writer.write(line);
                    writer.newLine();
                }
            }
            
            plugin.getLogger().info("Successfully updated " + resourceName);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update " + resourceName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void createBackup(Plugin plugin, File file) {
        try {
            File backup = new File(file.getParentFile(), file.getName() + ".bak");
            if (backup.exists()) backup.delete();
            Files.copy(file.toPath(), backup.toPath());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create backup for " + file.getName());
        }
    }

    private static List<String> updateLines(InputStream resource, FileConfiguration userConfig) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8));
        List<String> lines = new ArrayList<>();
        String line;
        
        // Stack to keep track of current section path
        List<String> pathStack = new ArrayList<>();
        
        while ((line = reader.readLine()) != null) {
            if (shouldSkipLine(line)) {
                lines.add(line);
                continue;
            }

            int indentation = getIndentation(line);
            int depth = indentation / INDENTATION_SIZE;

            // Adjust stack based on depth
            while (pathStack.size() > depth) {
                pathStack.remove(pathStack.size() - 1);
            }

            String key = getKeyFromLine(line);
            if (key == null) {
                lines.add(line);
                continue;
            }

            pathStack.add(key);
            String fullPath = String.join(".", pathStack);

            if (userConfig.contains(fullPath)) {
                Object userValue = userConfig.get(fullPath);
                
                if (userValue instanceof ConfigurationSection) {
                    lines.add(line);
                } else if (userValue instanceof List) {
                    writeList(lines, indentation, key, (List<?>) userValue);
                    skipListInResource(reader, indentation);
                } else {
                    lines.add(getIndentationString(indentation) + key + ": " + formatValue(userValue));
                }
            } else {
                // User doesn't have this key, keep JAR line (new default)
                lines.add(line);
            }
        }
        
        return lines;
    }

    private static boolean shouldSkipLine(String line) {
        String trimmed = line.trim();
        return trimmed.isEmpty() || trimmed.startsWith("#");
    }

    private static void writeList(List<String> lines, int indentation, String key, List<?> list) {
        lines.add(getIndentationString(indentation) + key + ":");
        for (Object item : list) {
            if (item instanceof String) {
                lines.add(getIndentationString(indentation) + "  - \"" + escapeString((String) item) + "\"");
            } else {
                lines.add(getIndentationString(indentation) + "  - " + item);
            }
        }
    }

    private static void skipListInResource(BufferedReader reader, int indentation) throws IOException {
        reader.mark(1000);
        String nextLine;
        while ((nextLine = reader.readLine()) != null) {
            int nextIndent = getIndentation(nextLine);
            // If line is not empty, not a list item, and has same or lower indentation -> End of list
            if (!nextLine.trim().isEmpty() && !nextLine.trim().startsWith("-") && nextIndent <= indentation) {
                reader.reset();
                break;
            }
            reader.mark(1000);
        }
    }

    private static String formatValue(Object value) {
        if (value instanceof String) {
            return "\"" + escapeString((String) value) + "\"";
        }
        return String.valueOf(value);
    }

    private static int getIndentation(String line) {
        int count = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') count++;
            else break;
        }
        return count;
    }

    private static String getIndentationString(int n) {
        return new String(new char[n]).replace("\0", " ");
    }

    private static String getKeyFromLine(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("-") || trimmed.startsWith("#")) return null;
        int colonIndex = trimmed.indexOf(':');
        if (colonIndex == -1) return null;
        return trimmed.substring(0, colonIndex);
    }
    
    private static String escapeString(String str) {
        return str.replace("\"", "\\\"");
    }
}
