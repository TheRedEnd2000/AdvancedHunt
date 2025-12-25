package de.theredend2000.advancedHunt.util.updater;

import de.theredend2000.advancedHunt.Main;
import de.theredend2000.advancedHunt.util.VersionComparator;
import de.theredend2000.advancedHunt.util.updater.source.BukkitSource;
import de.theredend2000.advancedHunt.util.updater.source.ModrinthSource;
import de.theredend2000.advancedHunt.util.updater.source.SpigotSource;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PluginUpdater {

    private final Main plugin;
    private final Map<String, UpdateSource> sources;
    private final Map<String, TrackedPlugin> trackedPlugins;
    private final VersionComparator versionComparator;

    public PluginUpdater(Main plugin) {
        this.plugin = plugin;
        this.sources = new HashMap<>();
        this.trackedPlugins = new HashMap<>();
        this.versionComparator = new VersionComparator();

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
                source.getLatestVersion(id).thenAccept(latestVersion -> {
                    if (latestVersion != null) {
                        if (versionComparator.isGreaterThan(latestVersion, tracked.currentVersion)) {
                            plugin.getLogger().info("[" + tracked.name + "] Found a new version on " + sourceName + ": " + latestVersion);
                            downloadUpdate(tracked, sourceName, latestVersion);
                        }
                    }
                });
            }
        }
    }

    public void downloadUpdate(TrackedPlugin tracked, String sourceName, String version) {
        UpdateSource source = sources.get(sourceName);
        String id = tracked.ids.get(sourceName);

        if (source != null && id != null) {
            File updateFolder = Bukkit.getUpdateFolderFile();
            if (!updateFolder.exists()) {
                updateFolder.mkdirs();
            }
            
            File destination = new File(updateFolder, tracked.name + "-" + version + ".jar");
            
            if (destination.exists()) {
                 plugin.getLogger().info("[" + tracked.name + "] Update " + version + " is already downloaded. It will be applied on next restart.");
                 return;
            }

            source.downloadPlugin(id, version, destination).thenAccept(success -> {
                if (success) {
                    plugin.getLogger().info("[" + tracked.name + "] Successfully downloaded update: " + version + ". It will be applied on next restart.");
                } else {
                    plugin.getLogger().warning("[" + tracked.name + "] Failed to download update from " + sourceName);
                }
            });
        }
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
