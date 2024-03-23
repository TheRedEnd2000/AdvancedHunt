package de.theredend2000.advancedegghunt.util;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class Downloader {

    private Main plugin;

    public Downloader(){
        this.plugin = Main.getInstance();

        try {
            String downloadFile = new File(plugin.getDataFolder().getParent()).getAbsolutePath();
            if(plugin.getPluginConfig().getAutoDownloadNBTAPI())
                downloadPluginFromModrinth("eade5ea05429a49826a5c33a306a8592b47551d3", downloadFile);
            /*if(plugin.getPluginConfig().getAutoDownloadAdvancedEggHunt() && check is outdated)
                downloadPluginFromSpigot(109085, downloadFile); //returns 403*/
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadPluginFromSpigot(int pluginId, String saveDir) throws IOException {
        String fileURL = "https://api.spiget.org/v2/resources/" + pluginId + "/versions/latest/download";
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = "AdvancedEggHunt";
            /*if (fileName == null) {
                fileName = "plugin.jar";
            } else {
                int index = fileName.indexOf("filename=");
                if (index > 0) {
                    fileName = fileName.substring(index + 9, fileName.length());
                }
            }*/

            String saveFilePath = saveDir + File.separator + fileName;

            InputStream inputStream = httpConn.getInputStream();
            OutputStream outputStream = new FileOutputStream(saveFilePath);

            int bytesRead;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            System.out.println("Plugin downloaded: " + fileName);
            loadPlugin(saveFilePath);
        } else {
            System.out.println("No plugin to download from SpigotMC. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }

    public void downloadPluginFromModrinth(String hash, String saveDir) throws IOException {
        String fileURL = "https://api.modrinth.com/v2/version_file/"+hash+"/download";
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        int responseCode = httpConn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            String fileName = getFilenameFromModrinthAPI("nfGCP9fk");
            if (fileName == null) {
                fileName = "plugin.jar";
            }

            String saveFilePath = saveDir + File.separator + fileName;
            File outputFile = new File(saveFilePath);
            if(outputFile.exists()) return;

            InputStream inputStream = httpConn.getInputStream();
            OutputStream outputStream = new FileOutputStream(saveFilePath);

            int bytesRead;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            System.out.println("Plugin downloaded: " + fileName);
            loadPlugin(saveFilePath);
        } else {
            System.out.println("No plugin to download from Modrinth. Server replied HTTP code: " + responseCode);
        }
        httpConn.disconnect();
    }

    public static String getFilenameFromModrinthAPI(String versionId) {
        String apiUrl = "https://api.modrinth.com/v2/project/" + versionId+"/version";

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            String jsonResponse = response.toString();
            return parseFilenameFromJSON(jsonResponse);

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
                System.out.println("Plugin loaded and enabled successfully: " + newPlugin.getName());
            } else {
                System.out.println("Failed to load the plugin.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
