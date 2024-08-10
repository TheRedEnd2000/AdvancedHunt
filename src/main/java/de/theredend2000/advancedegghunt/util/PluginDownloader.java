package de.theredend2000.advancedegghunt.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.theredend2000.advancedegghunt.configurations.PluginDataConfig;
import org.bukkit.Bukkit;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginDownloader {
    private final Path pluginsDir;
    private final Path updateDir;
    private final Path oldPluginsDir;
    private final HttpClient httpClient;
    private final Gson gson;
    private final PluginDataConfig pathConfig;
    private final Logger logger;

    public PluginDownloader(JavaPlugin plugin) {
        this.pluginsDir = Paths.get(Bukkit.getUpdateFolderFile().getParentFile().getPath());
        this.updateDir = Paths.get(Bukkit.getUpdateFolderFile().getPath());
        this.oldPluginsDir = this.pluginsDir.resolve("OLD_PLUGINS");
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.pathConfig = new PluginDataConfig(plugin);
        logger = plugin.getLogger();
        
        try {
            Files.createDirectories(this.updateDir);
            Files.createDirectories(this.oldPluginsDir);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Unexpected error occurred.", e);
        }
    }

    public void downloadPlugin(String pluginId, String pluginName, String source) {
        try {
            if ("spigot".equalsIgnoreCase(source)) {
                downloadSpigotPlugin(pluginId, pluginName);
            } else if ("modrinth".equalsIgnoreCase(source)) {
                downloadModrinthPlugin(pluginId, pluginName);
            } else {
                logger.log(Level.WARNING, "Invalid source. Use 'spigot' or 'modrinth'.");
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Unexpected error occurred.",e);
        }
    }

    private void downloadSpigotPlugin(String pluginId, String pluginName) throws IOException, InterruptedException {
        String apiUrl = "https://api.spiget.org/v2/resources/" + pluginId + "/versions/latest";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String jsonString = response.body();
            JsonObject pluginInfo = gson.fromJson(jsonString, JsonObject.class);

            String latestVersion = pluginInfo.get("name").getAsString();
            long releaseDate = pluginInfo.get("releaseDate").getAsLong();

            if (shouldUpdate(pluginName, latestVersion, releaseDate)) {
                String downloadUrl = "https://api.spiget.org/v2/resources/" + pluginId + "/download";
                downloadAndPlacePlugin(downloadUrl, pluginName, latestVersion);
            }
        } else {
            logger.warning("Failed to fetch plugin info for " + pluginName);
        }
    }

    private void downloadModrinthPlugin(String pluginId, String pluginName) throws IOException, InterruptedException {
        String apiUrl = "https://api.modrinth.com/v2/project/" + pluginId + "/version";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String jsonString = response.body();
            JsonArray versions = gson.fromJson(jsonString, JsonArray.class);

            if (versions.size() > 0) {
                JsonObject latestVersion = versions.get(0).getAsJsonObject();
                String versionNumber = latestVersion.get("version_number").getAsString();
                String releaseDate = latestVersion.get("date_published").getAsString();

                if (shouldUpdate(pluginName, versionNumber, Instant.parse(releaseDate).toEpochMilli())) {
                    String downloadUrl = latestVersion.getAsJsonArray("files").get(0).getAsJsonObject().get("url").getAsString();
                    downloadAndPlacePlugin(downloadUrl, pluginName, versionNumber);
                }
            } else {
                logger.warning("No versions found for " + pluginName);
            }
        } else {
            logger.warning("Failed to fetch plugin info for " + pluginName);
        }
    }

    private boolean shouldUpdate(String pluginName, String latestVersion, long releaseDate) {
        Path currentPluginPath = findCurrentPlugin(pluginName);
        if (currentPluginPath == null) {
            logger.info("Plugin " + pluginName + " not found. Will download.");
            return true;
        }

        String currentVersion = getPluginVersion(pluginName);
        if (currentVersion == null) {
            logger.info("Unable to get current version for " + pluginName + ". Will download.");
            return true;
        }
        if (VersionComparator.isGreaterThan(latestVersion, currentVersion)) {
            LocalDateTime releaseDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(releaseDate), ZoneId.systemDefault());
            if (ChronoUnit.DAYS.between(releaseDateTime, LocalDateTime.now()) <= 3) {
                logger.info("Newer version available for " + pluginName + ", but it's newer than 3 days. Skipping update.");
            } else {
                logger.info("Newer version available for " + pluginName + ". Will update.");
                return true;
            }
        } else {
            logger.info("Plugin " + pluginName + " is up to date.");
        }
        return false;
    }

    private boolean isPaperOrPurpur() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isAbove1_19() {
        String version = Bukkit.getBukkitVersion();
        return VersionComparator.compare(version, "1.19") >= 0;
    }

    private void downloadAndPlacePlugin(String downloadUrl, String pluginName, String version) throws IOException {
        URL url = new URL(downloadUrl);
        String filename = pluginName + "-" + version + ".jar";
        Path targetDir;
        Path currentFile = findCurrentPlugin(pluginName);

        if (isPaperOrPurpur() && isAbove1_19() && currentFile != null) {
            targetDir = updateDir;
        } else {
            if (currentFile != null && !FileOrderChecker.isFileNameFirst(filename, currentFile.getFileName().toString())) {
                logger.warning("Can not continue with automatic download of " + pluginName);
                return;
            }

            targetDir = pluginsDir;
            moveOldVersion(pluginName);
        }

        Path filePath = targetDir.resolve(filename);
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
             FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        logger.info("Downloaded " + filename + " to " + targetDir);

        if (!FileOrderChecker.isNewFileFirst(pluginsDir.toFile(), filename)) {
            logger.warning("Downloaded plugin " + pluginName + " will not get loaded first. Scheduling removal.");

            pathConfig.savePluginPath(filename, filePath.toString());
        }
        else {
            pathConfig.savePluginPath(pluginName, filePath.toString());
        }

        if (currentFile == null) {
            loadPlugin(pluginName);
        }
    }

    /**
     * Loads or reloads a plugin.
     *
     * @param pluginName The name of the plugin to load
     * @return true if the plugin was successfully loaded, false otherwise
     */
    public boolean loadPlugin(String pluginName) {
        PluginManager pluginManager = Bukkit.getPluginManager();

        // Check if the plugin is already loaded
        Plugin targetPlugin = pluginManager.getPlugin(pluginName);
        if (targetPlugin != null) {
            pluginManager.disablePlugin(targetPlugin);
            logger.info("Disabling plugin: " + pluginName);
        }

        // Get the plugins folder
        File pluginsDir = new File("plugins");

        // Find the plugin file
        File pluginFile = new File(pluginsDir, pluginName + ".jar");
        if (!pluginFile.exists()) {
            logger.severe("Cannot find plugin file: " + pluginFile.getAbsolutePath());
            return false;
        }

        try {
            // Load and enable the plugin
            Plugin plugin = pluginManager.loadPlugin(pluginFile);
            if (plugin == null) {
                logger.severe("Failed to load plugin: " + pluginName);
                return false;
            }
            pluginManager.enablePlugin(plugin);
            logger.info("Successfully loaded and enabled plugin: " + pluginName);
            return true;
        } catch (InvalidPluginException e) {
            logger.log(Level.SEVERE, "Invalid plugin: " + pluginName, e);
        } catch (InvalidDescriptionException e) {
            logger.log(Level.SEVERE, "Invalid plugin description for: " + pluginName, e);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error while loading plugin: " + pluginName, e);
        }

        return false;
    }

    private Path findCurrentPlugin(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin != null) {
            try {
                Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
                getFileMethod.setAccessible(true);
                return ((File) getFileMethod.invoke(plugin)).toPath();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Unexpected error occurred.", e);
            }
        }
        return null;
    }

    private String getPluginVersion(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin != null) {
            return plugin.getDescription().getVersion();
        }
        return null;
    }

    private void moveOldVersion(String pluginName) throws IOException {
        Path currentPluginPath = findCurrentPlugin(pluginName);
        if (currentPluginPath != null) {
            String storedPath = pathConfig.getStoredPluginPath(pluginName);
            if (storedPath != null && !storedPath.equals(currentPluginPath.toString())) {
                Path oldPluginPath = oldPluginsDir.resolve(currentPluginPath.getFileName());
                Files.move(currentPluginPath, oldPluginPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Moved old version of " + pluginName + " to " + oldPluginsDir);
            } else {
                logger.warning("Current version of " + pluginName + " is the same as the stored version. Skipping move.");
            }
        }
    }
}
