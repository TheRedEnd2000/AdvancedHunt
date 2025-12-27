package de.theredend2000.advancedhunt.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ConfigUpdater {

    public static void update(Plugin plugin, String resourceName, File file) {
        update(plugin, resourceName, file, null);
    }

    public static void update(Plugin plugin, String resourceName, File file, BiConsumer<FileConfiguration, Integer> migrator) {
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
            migrator.accept(userConfig, currentVersion);
        }

        plugin.getLogger().info("Updating " + resourceName + " from version " + currentVersion + " to " + newVersion + "...");

        // Create backup
        try {
            File backup = new File(file.getParentFile(), file.getName() + ".bak");
            if (backup.exists()) backup.delete();
            java.nio.file.Files.copy(file.toPath(), backup.toPath());
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create backup for " + file.getName());
        }

        // Perform update
        try {
            // Re-open resource for reading lines
            resource = plugin.getResource(resourceName);
            List<String> newLines = updateLines(resource, userConfig);
            
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
            for (String line : newLines) {
                writer.write(line);
                writer.newLine();
            }
            writer.close();
            
            plugin.getLogger().info("Successfully updated " + resourceName);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update " + resourceName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<String> updateLines(InputStream resource, FileConfiguration userConfig) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.UTF_8));
        List<String> lines = new ArrayList<>();
        String line;
        
        // Stack to keep track of current section path
        // Each element is a key name.
        // Indentation level determines depth.
        // We assume standard 2-space indentation.
        List<String> pathStack = new ArrayList<>();
        
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                lines.add(line);
                continue;
            }

            int indentation = getIndentation(line);
            int depth = indentation / 2;

            // Adjust stack based on depth
            while (pathStack.size() > depth) {
                pathStack.remove(pathStack.size() - 1);
            }

            String key = getKeyFromLine(line);
            if (key == null) {
                // Could be a list item or weird formatting, just keep it if we are not skipping
                lines.add(line);
                continue;
            }

            pathStack.add(key);
            String fullPath = String.join(".", pathStack);

            if (userConfig.contains(fullPath)) {
                Object userValue = userConfig.get(fullPath);
                
                if (userValue instanceof ConfigurationSection) {
                    // It's a section, just write the key line and continue to process children
                    lines.add(line);
                } else if (userValue instanceof List) {
                    // It's a list. Write the key, then write the user's list, then skip JAR's list items
                    lines.add(getIndentationString(indentation) + key + ":");
                    List<?> list = (List<?>) userValue;
                    for (Object item : list) {
                        // Simple serialization for list items
                        if (item instanceof String) {
                            lines.add(getIndentationString(indentation) + "  - \"" + escapeString((String) item) + "\"");
                        } else {
                            lines.add(getIndentationString(indentation) + "  - " + item);
                        }
                    }
                    
                    // Skip lines in JAR that are part of this list
                    reader.mark(1000);
                    String nextLine;
                    while ((nextLine = reader.readLine()) != null) {
                        if (nextLine.trim().isEmpty() || nextLine.trim().startsWith("#")) {
                            // Keep comments/empty lines inside lists? Maybe not if we replaced the list.
                            // But usually comments inside lists are rare or specific to items.
                            // Let's assume we skip everything until next key or lower indentation
                            // Actually, safer to stop at next key or lower indentation
                        }
                        
                        int nextIndent = getIndentation(nextLine);
                        if (!nextLine.trim().isEmpty() && !nextLine.trim().startsWith("-") && nextIndent <= indentation) {
                            // Found a line that is not a list item and has same or lower indentation -> End of list
                            reader.reset();
                            break;
                        }
                        reader.mark(1000);
                    }
                } else {
                    // Simple value
                    String valueString;
                    if (userValue instanceof String) {
                        valueString = "\"" + escapeString((String) userValue) + "\"";
                    } else {
                        valueString = String.valueOf(userValue);
                    }
                    lines.add(getIndentationString(indentation) + key + ": " + valueString);
                }
            } else {
                // User doesn't have this key, keep JAR line (new default)
                lines.add(line);
            }
        }
        
        return lines;
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
