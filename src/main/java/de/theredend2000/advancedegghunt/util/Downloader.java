package de.theredend2000.advancedegghunt.util;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;

public class Downloader {

    private final Plugin plugin;
    private final String pluginName;
    private final String currentVersion;

    public Downloader(Plugin plugin) {
        this.plugin = plugin;
        this.pluginName = plugin.getName();
        this.currentVersion = plugin.getDescription().getVersion();
//        initializeDownloads();
    }

    private void initializeDownloads() {
        String downloadDir = new File(plugin.getDataFolder().getParent()).getAbsolutePath();
        renameOldPlugins(downloadDir);

        try {
            if (((Main)plugin).getPluginConfig().getAutoDownloadNBTAPI() && shouldDownload("NBTAPI", downloadDir)) {
                downloadPluginFromModrinth("eade5ea05429a49826a5c33a306a8592b47551d3", downloadDir);
            }
            if (((Main)plugin).getPluginConfig().getAutoDownloadProtocolLib() && shouldDownload("ProtocolLib", downloadDir)) {
                downloadPluginFromSpigot(1997, downloadDir);
            }
            if (((Main)plugin).getPluginConfig().getAutoDownloadPlaceholderAPI() && shouldDownload("PlaceholderAPI", downloadDir)) {
                downloadPluginFromSpigot(6245, downloadDir);
            }
            if (((Main)plugin).getPluginConfig().getAutoDownloadAdvancedEggHunt() && Updater.isOutdated && shouldDownload(pluginName, downloadDir)) {
                downloadPluginFromSpigot(109085, downloadDir);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to download plugins", e);
        }
    }

    private boolean shouldDownload(String pluginName, String downloadDir) throws IOException {
        File pluginFile = findPluginFile(pluginName, downloadDir);
        if (pluginFile == null || !pluginFile.exists()) {
            return true;
        }

        BasicFileAttributes attr = Files.readAttributes(pluginFile.toPath(), BasicFileAttributes.class);
        Instant lastModified = attr.lastModifiedTime().toInstant();
        Instant threeOldDays = Instant.now().minus(3, ChronoUnit.DAYS);

        return lastModified.isBefore(threeOldDays);
    }

    private File findPluginFile(String pluginName, String downloadDir) {
        File dir = new File(downloadDir);
        File[] files = dir.listFiles((dir1, name) -> name.startsWith(pluginName) && name.endsWith(".jar"));
        return (files != null && files.length > 0) ? files[0] : null;
    }

    private void renameOldPlugins(String directory) {
        File dir = new File(directory);
        File[] files = dir.listFiles((dir1, name) -> name.startsWith(pluginName) && name.endsWith(".jar"));
        if (files == null) return;
        for (File file : files) {
            if (file.getName().equals(pluginName + "-" + currentVersion + ".jar")) {
                continue;
            }

            File newFile = new File(file.getParent(), file.getName() + ".old");
            int counter = 1;
            while (newFile.exists()) {
                newFile = new File(file.getParent(), file.getName() + ".old" + counter);
                counter++;
            }
            if (file.renameTo(newFile)) {
                plugin.getLogger().log(Level.INFO, "Renamed old plugin version: " + file.getName() + " to " + newFile.getName());
            } else {
                plugin.getLogger().log(Level.WARNING, "Failed to rename old plugin version: " + file.getName());
            }
        }
    }

    public void downloadPluginFromSpigot(int pluginId, String saveDir) throws IOException {
        String fileURL = "https://api.spiget.org/v2/resources/" + pluginId + "/download";
        URL url = new URL(fileURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        String fileName = getFileNameFromContentDisposition(connection);
        if (fileName == null || fileName.isEmpty()) {
            plugin.getLogger().log(Level.SEVERE, "Failed to determine filename for Spigot plugin download");
            return;
        }

        String saveFilePath = getSaveFilePath(saveDir, fileName);
        downloadFile(connection, saveFilePath);
    }

    public void downloadPluginFromModrinth(String hash, String saveDir) throws IOException {
        String fileURL = "https://api.modrinth.com/v2/version_file/" + hash + "/download";
        String fileName = getFilenameFromModrinthAPI("nfGCP9fk");
        if (fileName == null || fileName.isEmpty()) {
            plugin.getLogger().log(Level.SEVERE, "Failed to determine filename for Modrinth plugin download");
            return;
        }

        String saveFilePath = getSaveFilePath(saveDir, fileName);
        File outputFile = new File(saveFilePath);

        if (outputFile.exists()) return;

        URL url = new URL(fileURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        downloadFile(connection, saveFilePath);
    }

    private String getSaveFilePath(String saveDir, String fileName) {
        String currentFile = "";

        if (isPaperOrPurpur() && isAbove1_19() || currentFile.equals(fileName)) {
            return new File(Bukkit.getUpdateFolderFile(), fileName).getAbsolutePath();
        } else {
            String filePath = saveDir + File.separator + fileName;
            ensureFirstInLoadOrder(filePath);
            return filePath;
        }
    }

    private void downloadFile(HttpURLConnection connection, String saveFilePath) throws IOException {
        try (InputStream in = connection.getInputStream();
             OutputStream out = new FileOutputStream(saveFilePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        plugin.getLogger().log(Level.INFO, "Plugin downloaded: " + new File(saveFilePath).getName());
    }

    private String getFileNameFromContentDisposition(HttpURLConnection connection) {
        String contentDisposition = connection.getHeaderField("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            return contentDisposition.substring(contentDisposition.indexOf("filename=") + 9).replace("\"", "");
        }
        return null;
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

    private void ensureFirstInLoadOrder(String filePath) {
        File pluginFile = new File(filePath);
        File pluginsDir = pluginFile.getParentFile();
        File[] jarFiles = pluginsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".jar"));

        if (jarFiles != null) {
            long currentTime = System.currentTimeMillis();
            pluginFile.setLastModified(currentTime);

            for (File file : jarFiles) {
                if (!file.equals(pluginFile)) {
                    file.setLastModified(currentTime - 1000);
                }
            }
        }
    }

    private static String getFilenameFromModrinthAPI(String versionId) {
        String apiUrl = "https://api.modrinth.com/v2/project/" + versionId + "/version";
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                return parseFilenameFromJSON(response.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String parseFilenameFromJSON(String jsonResponse) {
        int startIndex = jsonResponse.indexOf("\"filename\":") + 12;
        int endIndex = jsonResponse.indexOf("\",", startIndex);
        return jsonResponse.substring(startIndex, endIndex);
    }
}
