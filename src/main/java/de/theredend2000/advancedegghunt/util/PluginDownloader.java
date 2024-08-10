package de.theredend2000.advancedegghunt.util;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class PluginDownloader {
    private final Path pluginsDir;
    private final Path updateDir;
    private final Path oldPluginsDir;
    private final HttpClient httpClient;
    private final Gson gson;

    public PluginDownloader() {
        this.pluginsDir = Paths.get(Bukkit.getUpdateFolderFile().getPath() + File.separator + "..");
        this.updateDir = Paths.get(Bukkit.getUpdateFolderFile().getPath());
        this.oldPluginsDir = this.pluginsDir.resolve("OLD_PLUGINS");
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();

        try {
            Files.createDirectories(this.updateDir);
            Files.createDirectories(this.oldPluginsDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void downloadPlugin(String pluginId, String pluginName, String source) throws IOException, InterruptedException {
        if ("spigot".equalsIgnoreCase(source)) {
            downloadSpigotPlugin(pluginId, pluginName);
        } else if ("modrinth".equalsIgnoreCase(source)) {
            downloadModrinthPlugin(pluginId, pluginName);
        } else {
            throw new IllegalArgumentException("Invalid source. Use 'spigot' or 'modrinth'.");
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
            System.out.println("Failed to fetch plugin info for " + pluginName);
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
                System.out.println("No versions found for " + pluginName);
            }
        } else {
            System.out.println("Failed to fetch plugin info for " + pluginName);
        }
    }

    private boolean shouldUpdate(String pluginName, String latestVersion, long releaseDate) {
        Path currentPluginPath = findCurrentPlugin(pluginName);
        if (currentPluginPath == null) {
            System.out.println("Plugin " + pluginName + " not found. Will download.");
            return true;
        }

        String currentVersion = getPluginVersion(pluginName);
        if (currentVersion == null) {
            System.out.println("Unable to get current version for " + pluginName + ". Will download.");
            return true;
        }
        if (compareVersions(latestVersion, currentVersion) > 0) {
            LocalDateTime releaseDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(releaseDate), ZoneId.systemDefault());
            if (ChronoUnit.DAYS.between(releaseDateTime, LocalDateTime.now()) <= 3) {
                System.out.println("Newer version available for " + pluginName + ", but it's newer than 3 days. Skipping update.");
            } else {
                System.out.println("Newer version available for " + pluginName + ". Will update.");
                return true;
            }
        } else {
            System.out.println("Plugin " + pluginName + " is up to date.");
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

//        if (isPaperOrPurpur() && isAbove1_19()) {
//            targetDir = updateDir;
//        } else {
            targetDir = pluginsDir;
            moveOldVersion(pluginName);
//        }

        Path filePath = targetDir.resolve(filename);
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
             FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        System.out.println("Downloaded " + filename + " to " + targetDir);
    }

    private Path findCurrentPlugin(String pluginName) {
        try {
            return Files.list(pluginsDir)
                    .filter(path -> path.getFileName().toString().toLowerCase().startsWith(pluginName.toLowerCase())
                            && path.getFileName().toString().endsWith(".jar"))
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
            Path oldPluginPath = oldPluginsDir.resolve(currentPluginPath.getFileName());
            Files.move(currentPluginPath, oldPluginPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Moved old version of " + pluginName + " to " + oldPluginsDir);
        }
    }

    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) {
                return p1 - p2;
            }
        }
        return 0;
    }
}
