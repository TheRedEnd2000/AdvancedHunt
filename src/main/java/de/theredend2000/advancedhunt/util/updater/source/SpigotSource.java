package de.theredend2000.advancedhunt.util.updater.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.theredend2000.advancedhunt.util.updater.UpdateInfo;
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
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SpigotSource implements UpdateSource {

    private static final String API_URL = "https://api.spiget.org/v2/resources/%s/versions?size=10&sort=-releaseDate";
    private static final String DOWNLOAD_URL = "https://api.spiget.org/v2/resources/%s/versions/%s/download";

    @Override
    public CompletableFuture<UpdateInfo> getLatestUpdate(String id) {
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
                        String versionName = versionObj.get("name").getAsString();
                        
                        // Simple check for alpha/beta in name
                        if (!isUnstable(versionName)) {
                            String cleaned = cleanVersion(versionName);
                            Instant publishedAt = null;
                            if (versionObj.has("releaseDate") && !versionObj.get("releaseDate").isJsonNull()) {
                                long raw = versionObj.get("releaseDate").getAsLong();
                                publishedAt = Instant.ofEpochMilli(normalizeEpochMillis(raw));
                            }
                            return new UpdateInfo(cleaned, versionName, publishedAt);
                        }
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to check Spigot updates", e);
            }
            return null;
        });
    }

    private long normalizeEpochMillis(long value) {
        // Spiget typically uses millis, but guard against seconds.
        if (value > 0 && value < 1_000_000_000_000L) {
            return value * 1000L;
        }
        return value;
    }

    private String cleanVersion(String version) {
        // Remove "v" prefix if present
        if (version.toLowerCase().startsWith("v") && version.length() > 1 && Character.isDigit(version.charAt(1))) {
            version = version.substring(1);
        }
        
        // If it contains spaces, take the last part if it looks like a version
        if (version.contains(" ")) {
            String[] parts = version.split(" ");
            String last = parts[parts.length - 1];
            if (Character.isDigit(last.charAt(0)) || (last.toLowerCase().startsWith("v") && last.length() > 1 && Character.isDigit(last.charAt(1)))) {
                return cleanVersion(last);
            }
        }
        return version;
    }

    private boolean isUnstable(String version) {
        String v = version.toLowerCase();
        return v.contains("alpha") || v.contains("beta") || v.contains("snapshot") || v.contains("rc");
    }

    @Override
    public CompletableFuture<Boolean> downloadPlugin(String id, UpdateInfo update, File destination) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Need to find version ID first
                String versionId = getVersionId(id, update.sourceVersion());
                if (versionId == null) return false;

                String downloadUrl = String.format(DOWNLOAD_URL, id, versionId);
                return downloadFile(downloadUrl, destination);
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to download from Spigot", e);
            }
            return false;
        });
    }

    private String getVersionId(String resourceId, String versionName) {
        try {
            URL url = new URL(String.format(API_URL, resourceId));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "AdvancedHunt/Updater");

            if (connection.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();
                
                for (JsonElement element : versions) {
                    JsonObject versionObj = element.getAsJsonObject();
                    if (versionObj.get("name").getAsString().equals(versionName)) {
                        return versionObj.get("id").getAsString();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean downloadFile(String urlString, File destination) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "AdvancedHunt/Updater");
            
            try (InputStream in = connection.getInputStream()) {
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
        return "SpigotMC";
    }
}
