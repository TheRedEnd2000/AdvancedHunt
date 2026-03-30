package de.theredend2000.advancedhunt.util.updater;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.VersionComparator;
import de.theredend2000.advancedhunt.util.updater.source.BukkitSource;
import de.theredend2000.advancedhunt.util.updater.source.ModrinthSource;
import de.theredend2000.advancedhunt.util.updater.source.SpigotSource;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PluginUpdater {

    private static final Duration MINIMUM_RELEASE_AGE = Duration.ofDays(3);

    private final Main plugin;
    private final Map<String, UpdateSource> sources;
    private final Map<String, TrackedPlugin> trackedPlugins;
    private final VersionComparator versionComparator;
    private final boolean paperOrPurpur;
    private final boolean above1_19;
    private final File pluginsDir;
    private final File updateDir;
    private final File oldPluginsDir;
    private final Set<String> activeDownloads = ConcurrentHashMap.newKeySet();

    public PluginUpdater(Main plugin) {
        this.plugin = plugin;
        this.sources = new HashMap<>();
        this.trackedPlugins = new HashMap<>();
        this.versionComparator = new VersionComparator();
        this.paperOrPurpur = IS_PAPER_OR_PURPUR;
        this.above1_19 = detectAbove1_19();
        this.pluginsDir = Bukkit.getUpdateFolderFile().getParentFile();
        this.updateDir = Bukkit.getUpdateFolderFile();
        this.oldPluginsDir = new File(pluginsDir, "OLD_PLUGINS");

        initializeDirectories();

        registerSource(new ModrinthSource());
        registerSource(new SpigotSource());
        registerSource(new BukkitSource());
    }

    private void initializeDirectories() {
        if (!updateDir.exists()) updateDir.mkdirs();
        if (!oldPluginsDir.exists()) oldPluginsDir.mkdirs();
    }

    public void registerSource(UpdateSource source) {
        sources.put(source.getName(), source);
    }

    public void trackPlugin(String name, String currentVersion, Map<String, String> ids) {
        trackedPlugins.put(name, new TrackedPlugin(name, currentVersion, ids));
    }

    public void checkForUpdates() {
        for (TrackedPlugin tracked : trackedPlugins.values()) {
            checkUpdatesFor(tracked);
        }
    }

    private void checkUpdatesFor(TrackedPlugin tracked) {
        for (Map.Entry<String, String> entry : tracked.ids.entrySet()) {
            String sourceName = entry.getKey();
            String id = entry.getValue();
            UpdateSource source = sources.get(sourceName);

            if (source != null && id != null && !id.isEmpty()) {
                source.getLatestUpdate(id).thenAccept(update -> {
                    if (update == null) {
                        return;
                    }

                    if (!versionComparator.isGreaterThan(update.version(), tracked.currentVersion)) {
                        return;
                    }

                    Instant publishedAt = update.publishedAt();
                    if (publishedAt == null) {
                        plugin.getLogger().info("[" + tracked.name + "] Skipping automatic download from " + sourceName
                                + " for version " + update.version() + " because the release date is unavailable.");
                        return;
                    }

                    if (!isOldEnoughToAutoDownload(publishedAt)) {
                        plugin.getLogger().info("[" + tracked.name + "] Skipping automatic download from " + sourceName
                                + " for version " + update.version() + " because it is newer than "
                                + MINIMUM_RELEASE_AGE.toDays() + " days (published at " + publishedAt + ").");
                        return;
                    }

                    // Prevent concurrent downloads of the same plugin from multiple sources
                    if (!activeDownloads.add(tracked.name)) {
                        plugin.getLogger().info("[" + tracked.name + "] Download already in progress from another source. Skipping " + sourceName + ".");
                        return;
                    }

                    plugin.getLogger().info("[" + tracked.name + "] Found a new version on " + sourceName + ": " + update.version());
                    downloadUpdate(tracked, sourceName, update);
                });
            }
        }
    }

    private boolean isOldEnoughToAutoDownload(Instant publishedAt) {
        Instant threshold = Instant.now().minus(MINIMUM_RELEASE_AGE);
        return !publishedAt.isAfter(threshold);
    }

    public void downloadUpdate(TrackedPlugin tracked, String sourceName, UpdateInfo update) {
        UpdateSource source = sources.get(sourceName);
        String id = tracked.ids.get(sourceName);

        if (source == null || id == null) return;

        // Move old version to OLD_PLUGINS before downloading
        moveOldVersion(tracked.name);

        // Always use versioned filename (like legacy)
        String filename = tracked.name + "-" + update.version() + ".jar";
        File currentFile = resolvePluginFile(tracked.name);

        // Determine target directory based on server type and scenario
        File targetDir = determineTargetDirectory(tracked.name, filename, currentFile);
        if (targetDir == null) {
            return; // Cannot proceed (file order issue on non-Paper)
        }
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }

        File destination = new File(targetDir, filename);

        if (destination.exists()) {
            plugin.getLogger().info("[" + tracked.name + "] Update " + update.version() + " is already downloaded.");
            return;
        }

        source.downloadPlugin(id, update, destination).thenAccept(success -> {
            try {
                if (success) {
                    plugin.getLogger().info("[" + tracked.name + "] Downloaded " + filename + " to " + targetDir.getName());

                    if (currentFile == null) {
                        loadPlugin(tracked.name, destination);
                        return;
                    }

                    if (paperOrPurpur && above1_19) {
                        plugin.getLogger().info("[" + tracked.name + "] Update will be applied on next restart.");
                    } else {
                        handleFileOrder(tracked.name, filename, destination, currentFile);
                    }
                } else {
                    plugin.getLogger().warning("[" + tracked.name + "] Failed to download update from " + sourceName);
                    if (destination.exists()) {
                        destination.delete();
                    }
                }
            } finally {
                activeDownloads.remove(tracked.name);
            }
        });
    }

    /**
     * Determines the target directory for downloading a plugin (legacy logic).
     * <ul>
     *     <li>Paper/Purpur ≥1.19 + existing plugin → updateDir</li>
     *     <li>Non-Paper + existing plugin + new versioned name sorts before old → pluginsDir</li>
     *     <li>Non-Paper + existing plugin + new name doesn't sort first → abort (null)</li>
     *     <li>No existing plugin → pluginsDir</li>
     * </ul>
     */
    private File determineTargetDirectory(String pluginName, String filename, File currentFile) {
        if (paperOrPurpur && above1_19 && currentFile != null) {
            return updateDir;
        }

        if (currentFile != null && !isVersionedFileNameFirst(filename, currentFile.getName())) {
            plugin.getLogger().warning("Cannot continue with automatic download of " + pluginName
                    + ": new filename would not load before the current one.");
            return null;
        }

        return pluginsDir;
    }

    /**
     * Checks if filename1's version is greater than filename2's version,
     * meaning the new versioned file would correctly replace the old one
     * when Bukkit loads plugins alphabetically.
     */
    private boolean isVersionedFileNameFirst(String filename1, String filename2) {
        String v1 = extractVersionFromFilename(filename1);
        String v2 = extractVersionFromFilename(filename2);
        return compareVersionSegments(v1, v2) > 0;
    }

    private String extractVersionFromFilename(String filename) {
        int dashIndex = filename.lastIndexOf('-');
        int dotJarIndex = filename.lastIndexOf(".jar");
        if (dashIndex != -1 && dotJarIndex != -1 && dashIndex < dotJarIndex) {
            return filename.substring(dashIndex + 1, dotJarIndex);
        }
        return "";
    }

    private int compareVersionSegments(String version1, String version2) {
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? parseIntSafe(parts1[i]) : 0;
            int v2 = i < parts2.length ? parseIntSafe(parts2[i]) : 0;
            if (v1 != v2) return Integer.compare(v1, v2);
        }
        return 0;
    }

    private int parseIntSafe(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    /**
     * Handles file ordering on non-Paper servers where both old and new jars coexist.
     * Logs a warning if the new file would not load first among the plugins directory.
     */
    private void handleFileOrder(String pluginName, String filename, File newFile, File currentFile) {
        File[] files = pluginsDir.listFiles();
        if (files != null) {
            boolean isFirst = true;
            for (File f : files) {
                if (f.getName().endsWith(".jar") && f.getName().compareToIgnoreCase(filename) < 0
                        && !f.getName().equalsIgnoreCase(currentFile.getName())) {
                    isFirst = false;
                    break;
                }
            }
            if (!isFirst) {
                plugin.getLogger().warning("[" + pluginName + "] Downloaded plugin may not load first due to file ordering.");
            }
        }
    }

    /**
     * Moves the old version of a plugin (if tracked) to the OLD_PLUGINS directory.
     */
    private void moveOldVersion(String pluginName) {
        File currentFile = resolvePluginFile(pluginName);
        if (currentFile == null || !currentFile.exists()) return;

        // Only move if there's a different file in the plugins dir with the same plugin name prefix
        File[] files = pluginsDir.listFiles();
        if (files == null) return;
        String prefix = pluginName + "-";
        for (File f : files) {
            if (f.getName().startsWith(prefix) && f.getName().endsWith(".jar")
                    && !f.getName().equals(currentFile.getName())) {
                File dest = new File(oldPluginsDir, f.getName());
                if (f.renameTo(dest)) {
                    plugin.getLogger().info("Moved old version " + f.getName() + " to OLD_PLUGINS");
                } else {
                    plugin.getLogger().severe("Failed to move old version " + f.getName() + " to OLD_PLUGINS");
                }
            }
        }
    }

    /**
     * Loads a newly downloaded plugin, disabling any existing instance first.
     */
    private void loadPlugin(String pluginName, File pluginFile) {
        Runnable loadTask = () -> {
            if (!pluginFile.exists()) {
                plugin.getLogger().severe("[" + pluginName + "] Cannot find plugin file: " + pluginFile.getAbsolutePath());
                return;
            }

            PluginManager pluginManager = Bukkit.getPluginManager();

            Plugin existing = pluginManager.getPlugin(pluginName);
            if (existing != null) {
                pluginManager.disablePlugin(existing);
                plugin.getLogger().info("[" + pluginName + "] Disabled existing instance before loading new version.");
            }

            try {
                Plugin loadedPlugin = pluginManager.loadPlugin(pluginFile);
                if (loadedPlugin != null) {
                    pluginManager.enablePlugin(loadedPlugin);
                    plugin.getLogger().info("[" + pluginName + "] Successfully loaded and enabled plugin.");
                } else {
                    plugin.getLogger().severe("[" + pluginName + "] Failed to load plugin from " + pluginFile.getName());
                }
            } catch (InvalidPluginException | InvalidDescriptionException e) {
                plugin.getLogger().log(Level.SEVERE, "[" + pluginName + "] Failed to load plugin", e);
            }
        };

        if (Bukkit.isPrimaryThread()) {
            loadTask.run();
        } else if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(plugin, loadTask);
        } else {
            plugin.getLogger().info("[" + pluginName + "] Download complete. Will be loaded on next restart.");
        }
    }

    private static final boolean IS_PAPER_OR_PURPUR = detectPaperOrPurpur();

    /**
     * Checks if the server is running Paper or Purpur.
     */
    private static boolean detectPaperOrPurpur() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Checks if the server version is 1.19 or above.
     */
    private boolean detectAbove1_19() {
        return versionComparator.isGreaterThanOrEqual(Bukkit.getBukkitVersion(), "1.19");
    }

    /**
     * Resolves the jar file of a currently loaded plugin by name.
     * On non-Paper or pre-1.19 servers the update folder requires an exact filename match.
     */
    private File resolvePluginFile(String pluginName) {
        Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);
        if (target instanceof JavaPlugin) {
            try {
                Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
                getFileMethod.setAccessible(true);
                return (File) getFileMethod.invoke(target);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not resolve jar file for " + pluginName + ": " + e.getMessage());
            }
        }
        return null;
    }

    private static class TrackedPlugin {
        String name;
        String currentVersion;
        Map<String, String> ids;

        public TrackedPlugin(String name, String currentVersion, Map<String, String> ids) {
            this.name = name;
            this.currentVersion = currentVersion;
            this.ids = ids;
        }
    }
}
