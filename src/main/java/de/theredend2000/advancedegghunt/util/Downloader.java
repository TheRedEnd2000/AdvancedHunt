package de.theredend2000.advancedegghunt.util;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;

public class Downloader {

    private final Main plugin;

    public Downloader() {
        this.plugin = Main.getInstance();
        initializeDownloads();
    }

    private void initializeDownloads() {
        String downloadDir = new File(plugin.getDataFolder().getParent()).getAbsolutePath();
        renameOldPlugins(downloadDir);

        try {
            if (plugin.getPluginConfig().getAutoDownloadNBTAPI()) {
                downloadPluginFromModrinth("eade5ea05429a49826a5c33a306a8592b47551d3", downloadDir);
            }
            if (plugin.getPluginConfig().getAutoDownloadAdvancedEggHunt() && Updater.isOutdated) {
                downloadPluginFromSpigot(109085, downloadDir);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to download plugins", e);
        }
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

    private void renameOldPlugins(String directory) {
        File dir = new File(directory);
        File[] files = dir.listFiles((dir1, name) -> (name.startsWith("AdvancedEggHunt") || name.startsWith("NBTAPI")) && name.endsWith(".jar"));
        if (files == null) return;
        for (File file : files) {
            if (file.getName().equals("AdvancedEggHunt2.jar") ||
                    file.getName().equals(getFilenameFromModrinthAPI("nfGCP9fk"))) {
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
        String fileName = "AdvancedEggHunt2.jar";
        String saveFilePath;

        if (isPaperOrPurpur() && isAbove1_19()) {
            saveFilePath = new File(Bukkit.getUpdateFolderFile(), fileName).getAbsolutePath();
        } else {
            saveFilePath = saveDir + File.separator + fileName;
            ensureFirstInLoadOrder(saveFilePath);
        }

        downloadFile(fileURL, saveFilePath);
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

    public void downloadPluginFromModrinth(String hash, String saveDir) throws IOException {
        String fileURL = "https://api.modrinth.com/v2/version_file/" + hash + "/download";
        String fileName = getFilenameFromModrinthAPI("nfGCP9fk");
        if (fileName == null) {
            fileName = "plugin.jar";
        }

        String saveFilePath = saveDir + File.separator + fileName;
        File outputFile = new File(saveFilePath);

        if (outputFile.exists()) return;

        downloadFile(fileURL, saveFilePath);
        loadPlugin(saveFilePath);
    }

    private void downloadFile(String fileURL, String saveFilePath) throws IOException {
        URL url = new URL(fileURL);
        try (InputStream in = url.openStream();
             OutputStream out = new FileOutputStream(saveFilePath)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        plugin.getLogger().log(Level.INFO, "Plugin downloaded: " + new File(saveFilePath).getName());
    }

    public static String getFilenameFromModrinthAPI(String versionId) {
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

    public static String parseFilenameFromJSON(String jsonResponse) {
        int startIndex = jsonResponse.indexOf("\"filename\":") + 12;
        int endIndex = jsonResponse.indexOf("\",", startIndex);
        return jsonResponse.substring(startIndex, endIndex);
    }

    private void loadPlugin(String filePath) {
        try {
            Plugin newPlugin = Bukkit.getPluginManager().loadPlugin(new File(filePath));
            if (newPlugin != null) {
                Bukkit.getPluginManager().enablePlugin(newPlugin);
                plugin.getLogger().log(Level.INFO, "Plugin loaded and enabled successfully: " + newPlugin.getName());
            } else {
                plugin.getLogger().log(Level.WARNING, "Failed to load the plugin.");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading plugin", e);
        }
    }
}
