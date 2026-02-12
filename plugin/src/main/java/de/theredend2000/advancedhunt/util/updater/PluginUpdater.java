package de.theredend2000.advancedhunt.util.updater;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.VersionComparator;
import de.theredend2000.advancedhunt.util.updater.source.BukkitSource;
import de.theredend2000.advancedhunt.util.updater.source.ModrinthSource;
import de.theredend2000.advancedhunt.util.updater.source.SpigotSource;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class PluginUpdater {

    private static final Duration MINIMUM_RELEASE_AGE = Duration.ofDays(3);

    private final Main plugin;
    private final Map<String, UpdateSource> sources;
    private final Map<String, TrackedPlugin> trackedPlugins;
    private final VersionComparator versionComparator;
    private final boolean paperOrPurpur;
    private final boolean above1_19;

    public PluginUpdater(Main plugin) {
        this.plugin = plugin;
        this.sources = new HashMap<>();
        this.trackedPlugins = new HashMap<>();
        this.versionComparator = new VersionComparator();
        this.paperOrPurpur = detectPaperOrPurpur();
        this.above1_19 = detectAbove1_19();

        registerSource(new ModrinthSource());
        registerSource(new SpigotSource());
        registerSource(new BukkitSource());
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

                    if (!isOldEnoughToAutoDownload(update)) {
                        plugin.getLogger().info(
                                "[" + tracked.name + "] Found a new version on " + sourceName + ": " + update.version()
                                        + " (not auto-downloading; release is newer than " + MINIMUM_RELEASE_AGE.toDays() + " days or has no publish date)"
                        );
                        return;
                    }

                    plugin.getLogger().info("[" + tracked.name + "] Found a new version on " + sourceName + ": " + update.version());
                    downloadUpdate(tracked, sourceName, update);
                });
            }
        }
    }

    private boolean isOldEnoughToAutoDownload(UpdateInfo update) {
        Instant publishedAt = update.publishedAt();
        if (publishedAt == null) {
            return false;
        }
        Instant threshold = Instant.now().minus(MINIMUM_RELEASE_AGE);
        return !publishedAt.isAfter(threshold);
    }

    public void downloadUpdate(TrackedPlugin tracked, String sourceName, UpdateInfo update) {
        UpdateSource source = sources.get(sourceName);
        String id = tracked.ids.get(sourceName);

        if (source != null && id != null) {
            File updateFolder = Bukkit.getUpdateFolderFile();
            if (!updateFolder.exists()) {
                updateFolder.mkdirs();
            }

            String destinationName;
            if (paperOrPurpur && above1_19) {
                destinationName = tracked.name + "-" + update.version() + ".jar";
            } else {
                File currentFile = resolvePluginFile(tracked.name);
                destinationName = (currentFile != null) ? currentFile.getName() : tracked.name + ".jar";
            }
            File destination = new File(updateFolder, destinationName);
            
            if (destination.exists()) {
                 plugin.getLogger().info("[" + tracked.name + "] Update " + update.version() + " is already downloaded. It will be applied on next restart.");
                 return;
            }

            source.downloadPlugin(id, update, destination).thenAccept(success -> {
                if (success) {
                    plugin.getLogger().info("[" + tracked.name + "] Successfully downloaded update: " + update.version() + ". It will be applied on next restart.");
                } else {
                    plugin.getLogger().warning("[" + tracked.name + "] Failed to download update from " + sourceName);
                }
            });
        }
    }

    /**
     * Checks if the server is running Paper or Purpur.
     */
    private static boolean detectPaperOrPurpur() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
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
