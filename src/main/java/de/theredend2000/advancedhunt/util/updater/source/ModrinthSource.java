package de.theredend2000.advancedhunt.util.updater.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.theredend2000.advancedhunt.util.updater.UpdateSource;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ModrinthSource implements UpdateSource {

    private static final String API_URL = "https://api.modrinth.com/v2/project/%s/version";

    @Override
    public CompletableFuture<String> getLatestVersion(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = new URL(String.format(API_URL, id));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "AdvancedHunt/Updater");

                if (connection.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();
                    
                    for (JsonElement element : versions) {
                        JsonObject versionObj = element.getAsJsonObject();
                        String versionType = versionObj.get("version_type").getAsString();
                        
                        // Ignore alpha/beta if requested (user said ignore beta/alpha/snapshot)
                        if (versionType.equalsIgnoreCase("release")) {
                            return cleanVersion(versionObj.get("version_number").getAsString());
                        }
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to check Modrinth updates", e);
            }
            return null;
        });
    }

    private String cleanVersion(String version) {
        if (version.toLowerCase().startsWith("v") && version.length() > 1 && Character.isDigit(version.charAt(1))) {
            return version.substring(1);
        }
        return version;
    }

    @Override
    public CompletableFuture<Boolean> downloadPlugin(String id, String version, File destination) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // First find the version again to get the download URL
                URL url = new URL(String.format(API_URL, id));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "AdvancedHunt/Updater");

                if (connection.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();
                    
                    for (JsonElement element : versions) {
                        JsonObject versionObj = element.getAsJsonObject();
                        if (versionObj.get("version_number").getAsString().equals(version)) {
                            JsonArray files = versionObj.getAsJsonArray("files");
                            if (files.size() > 0) {
                                String downloadUrl = files.get(0).getAsJsonObject().get("url").getAsString();
                                return downloadFile(downloadUrl, destination);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to download from Modrinth", e);
            }
            return false;
        });
    }

    private boolean downloadFile(String urlString, File destination) {
        try {
            URL url = new URL(urlString);
            try (InputStream in = url.openStream()) {
                Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public String getName() {
        return "Modrinth";
    }
}
