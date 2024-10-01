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
    private static final String SPIGOT_API_URL = "https://api.spiget.org/v2/resources/";
    private static final String MODRINTH_API_URL = "https://api.modrinth.com/v2/project/";

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
        this.logger = plugin.getLogger();
        
        initializeDirectories();
    }

    private void initializeDirectories() {
        try {
            Files.createDirectories(this.updateDir);
            Files.createDirectories(this.oldPluginsDir);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to create directories", e);
        }
    }

    /**
     * Downloads a plugin from the specified source.
     *
     * @param pluginId The ID of the plugin to download
     * @param pluginName The name of the plugin
     * @param source The source to download from ('spigot' or 'modrinth')
     */
    public void downloadPlugin(String pluginId, String pluginName, String source) {
        try {
            // Move old version before downloading
            moveOldVersion(pluginName);

            switch (source.toLowerCase()) {
                case "spigot":
                    downloadSpigotPlugin(pluginId, pluginName);
                    break;
                case "modrinth":
                    downloadModrinthPlugin(pluginId, pluginName);
                    break;
                default:
                    logger.log(Level.WARNING, "Invalid source. Use 'spigot' or 'modrinth'.");
            }
        } catch (IOException | InterruptedException e) {
            logger.log(Level.SEVERE, "Failed to download plugin", e);
        }
    }

    /**
     * Downloads a plugin from Spigot.
     *
     * @param pluginId The ID of the plugin on Spigot
     * @param pluginName The name of the plugin
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    private void downloadSpigotPlugin(String pluginId, String pluginName) throws IOException, InterruptedException {
        String apiUrl = SPIGOT_API_URL + pluginId + "/versions/latest";
        JsonObject pluginInfo = fetchJsonFromUrl(apiUrl);

        if (pluginInfo != null) {
            String latestVersion = pluginInfo.get("name").getAsString();
            long releaseDate = pluginInfo.get("releaseDate").getAsLong();

            if (shouldUpdate(pluginName, latestVersion, Instant.ofEpochSecond(releaseDate).toEpochMilli())) {
                String downloadUrl = SPIGOT_API_URL + pluginId + "/download";
                downloadAndPlacePlugin(downloadUrl, pluginName, latestVersion);
            }
        } else {
            logger.warning("Failed to fetch plugin info for " + pluginName);
        }
    }

    /**
     * Downloads a plugin from Modrinth.
     *
     * @param pluginId The ID of the plugin on Modrinth
     * @param pluginName The name of the plugin
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    private void downloadModrinthPlugin(String pluginId, String pluginName) throws IOException, InterruptedException {
        String apiUrl = MODRINTH_API_URL + pluginId + "/version";
        JsonArray versions = fetchJsonArrayFromUrl(apiUrl);

        if (versions != null && versions.size() > 0) {
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
    }

    /**
     * Fetches JSON data from a given URL and returns it as a JsonObject.
     *
     * @param url The URL to fetch JSON data from
     * @return A JsonObject containing the fetched data, or null if the request failed
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    private JsonObject fetchJsonFromUrl(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), JsonObject.class);
        }
        return null;
    }

    /**
     * Fetches JSON data from a given URL and returns it as a JsonArray.
     *
     * @param url The URL to fetch JSON data from
     * @return A JsonArray containing the fetched data, or null if the request failed
     * @throws IOException If an I/O error occurs
     * @throws InterruptedException If the operation is interrupted
     */
    private JsonArray fetchJsonArrayFromUrl(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return gson.fromJson(response.body(), JsonArray.class);
        }
        return null;
    }

    /**
     * Determines if a plugin should be updated.
     *
     * @param pluginName The name of the plugin
     * @param latestVersion The latest version available
     * @param releaseDate The release date of the latest version
     * @return true if the plugin should be updated, false otherwise
     */
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
            long daysSinceRelease = ChronoUnit.DAYS.between(releaseDateTime, LocalDateTime.now());

            if (Instant.ofEpochMilli(releaseDate).isAfter(Instant.now()) || Instant.ofEpochMilli(releaseDate).isBefore(Instant.ofEpochSecond(1409616000))) {
                logger.warning("Unexpected date for " + pluginName + ", Allowing download but please contact developer.");
                return true;
            }
            if (daysSinceRelease <= 3) {
                logger.info("Newer version available for " + pluginName + ", but it's less than 3 days old. Skipping update.");
                return false;
            } else {
                logger.info("Newer version available for " + pluginName + ". Will update.");
                return true;
            }
        } else {
            logger.info("Plugin " + pluginName + " is up to date.");
            return false;
        }
    }

    /**
     * Checks if the server is running Paper or Purpur.
     *
     * @return true if the server is running Paper or Purpur, false otherwise
     */
    /**
     * Checks if the server is running Paper or Purpur.
     *
     * @return true if the server is running Paper or Purpur, false otherwise
     */
    private boolean isPaperOrPurpur() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Checks if the server version is above 1.19.
     *
     * @return true if the server version is above 1.19, false otherwise
     */
    /**
     * Checks if the server version is above 1.19.
     *
     * @return true if the server version is above 1.19, false otherwise
     */
    private boolean isAbove1_19() {
        String version = Bukkit.getBukkitVersion();
        return VersionComparator.compare(version, "1.19") >= 0;
    }

    /**
     * Downloads and places a plugin in the appropriate directory.
     *
     * @param downloadUrl The URL to download the plugin from
     * @param pluginName The name of the plugin
     * @param version The version of the plugin
     * @throws IOException If an I/O error occurs
     */
    private void downloadAndPlacePlugin(String downloadUrl, String pluginName, String version) throws IOException {
        String filename = pluginName + "-" + version + ".jar";
        Path currentFile = findCurrentPlugin(pluginName);
        Path targetDir = determineTargetDirectory(pluginName, filename, currentFile);

        if (targetDir == null) {
            return;
        }

        Path filePath = targetDir.resolve(filename);
        downloadFile(downloadUrl, filePath);
        logger.info("Downloaded " + filename + " to " + targetDir);

        if (currentFile == null) {
            loadPlugin(pluginName, filename);
            return;
        }
        handleFileOrder(pluginName, filename, filePath, currentFile);
    }

    /**
     * Determines the target directory for downloading a plugin.
     *
     * @param pluginName The name of the plugin
     * @param filename The filename of the new plugin version
     * @param currentFile The path to the current plugin file, if it exists
     * @return The path to the target directory, or null if download should not proceed
     * @throws IOException If an I/O error occurs
     */
    private Path determineTargetDirectory(String pluginName, String filename, Path currentFile) throws IOException {
        if (isPaperOrPurpur() && isAbove1_19() && currentFile != null) {
            return updateDir;
        }

        if (currentFile != null && !FileOrderChecker.isFileNameFirst(filename, currentFile.getFileName().toString())) {
            logger.warning("Cannot continue with automatic download of " + pluginName);
            return null;
        }

        return pluginsDir;
    }

    /**
     * Downloads a file from a given URL and saves it to the specified path.
     *
     * @param downloadUrl The URL to download the file from
     * @param filePath The path where the downloaded file should be saved
     * @throws IOException If an I/O error occurs during the download or file writing
     */
    private void downloadFile(String downloadUrl, Path filePath) throws IOException {
        URL url = new URL(downloadUrl);
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
             FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }

    /**
     * Handles the file order of the downloaded plugin and updates the plugin path configuration.
     *
     * @param pluginName The name of the plugin
     * @param filename The filename of the downloaded plugin
     * @param filePath The path where the plugin was downloaded
     */
    private void handleFileOrder(String pluginName, String filename, Path filePath, Path currentFilePath) {
        if (!FileOrderChecker.isNewFileFirst(pluginsDir.toFile(), filename)) {
            logger.warning("Downloaded plugin " + pluginName + " will not get loaded first. Scheduling removal.");
            pathConfig.savePluginPath(pluginName, currentFilePath.toString());
        } else {
            pathConfig.savePluginPath(pluginName, filePath.toString());
        }
    }

    /**
     * Loads or reloads a plugin.
     *
     * @param pluginName The name of the plugin to load
     * @return true if the plugin was successfully loaded, false otherwise
     */
    public boolean loadPlugin(String pluginName, String filename) {
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
        File pluginFile = new File(pluginsDir, filename);
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

    /**
     * Finds the current plugin file.
     *
     * @param pluginName The name of the plugin
     * @return The path to the current plugin file, or null if not found
     */
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

    /**
     * Gets the version of a plugin.
     *
     * @param pluginName The name of the plugin
     * @return The version of the plugin, or null if the plugin is not found
     */
    private String getPluginVersion(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        if (plugin != null) {
            return plugin.getDescription().getVersion();
        }
        return null;
    }

    /**
     * Moves the old version of a plugin to the OLD_PLUGINS directory.
     *
     * @param pluginName The name of the plugin
     * @throws IOException If an I/O error occurs
     */
    private void moveOldVersion(String pluginName) throws IOException {
        Path currentPluginPath = findCurrentPlugin(pluginName);
        if (currentPluginPath != null) {
            String storedPathString = pathConfig.getStoredPluginPath(pluginName);
            if (storedPathString != null && !storedPathString.equals(currentPluginPath.toString())) {
                Path storedPath = Path.of(storedPathString);
                Path oldPluginPath = oldPluginsDir.resolve(storedPath.getFileName());
                Files.move(storedPath, oldPluginPath, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Moved old version of " + pluginName + " to " + oldPluginsDir);
                pathConfig.savePluginPath(pluginName, null);
            } else {
                logger.warning("Current version of " + pluginName + " is the same as the stored version. Skipping move.");
            }
        }
    }
}
