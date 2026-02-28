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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class ModrinthSource implements UpdateSource {

    private static final String API_URL = "https://api.modrinth.com/v2/project/%s/version";
    private static final String LOADER = detectLoader();

    /**
     * Detects the running server platform and returns the single Modrinth loader name
     * that best describes it.  The hierarchy is:
     *   Folia → Purpur → Paper → Spigot → Bukkit
     */
    private static String detectLoader() {
        // Folia (fork of Paper with region threading)
        if (isClassPresent("io.papermc.paper.threadedregions.RegionizedServer")) return "folia";
        // Purpur
        if (isClassPresent("org.purpurmc.purpur.PurpurConfig")) return "purpur";
        // Paper (new config location, 1.19+)
        if (isClassPresent("io.papermc.paper.configuration.Configuration")) return "paper";
        // Paper (legacy config location, pre-1.19)
        if (isClassPresent("com.destroystokyo.paper.PaperConfig")) return "paper";
        // Spigot
        if (isClassPresent("org.spigotmc.SpigotConfig")) return "spigot";
        // Plain CraftBukkit / Bukkit
        return "bukkit";
    }

    private static boolean isClassPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public CompletableFuture<UpdateInfo> getLatestUpdate(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String loadersParam = URLEncoder.encode("[\"" + LOADER + "\"]", StandardCharsets.UTF_8.name());
                String mcVersion = getServerMinecraftVersion();

                // Prefer an exact MC-version match; fall back to loader-only if none found
                UpdateInfo result = fetchLatestRelease(id, loadersParam,
                        URLEncoder.encode("[\"" + mcVersion + "\"]", StandardCharsets.UTF_8.name()));
                if (result == null) {
                    result = fetchLatestRelease(id, loadersParam, null);
                }
                return result;
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to check Modrinth updates", e);
            }
            return null;
        });
    }

    /**
     * Fetches the first stable release from the Modrinth API that is compatible
     * with a Bukkit-based server platform.
     *
     * @param id               Modrinth project ID / slug
     * @param loadersParam     URL-encoded JSON array of loaders (query param value)
     * @param gameVersionsParam URL-encoded JSON array of MC versions, or {@code null} to skip
     */
    private UpdateInfo fetchLatestRelease(String id, String loadersParam, String gameVersionsParam) throws Exception {
        StringBuilder urlBuilder = new StringBuilder(String.format(API_URL, id))
                .append("?loaders=").append(loadersParam);
        if (gameVersionsParam != null) {
            urlBuilder.append("&game_versions=").append(gameVersionsParam);
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(urlBuilder.toString()).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "AdvancedHunt/Updater");

        if (connection.getResponseCode() != 200) return null;

        JsonArray versions = JsonParser.parseReader(new InputStreamReader(connection.getInputStream()))
                .getAsJsonArray();

        for (JsonElement element : versions) {
            JsonObject versionObj = element.getAsJsonObject();
            String versionType = versionObj.get("version_type").getAsString();

            // Only stable releases, and guard against a mis-tagged non-Bukkit release
            if (!versionType.equalsIgnoreCase("release")) continue;
            if (!isCompatibleLoader(versionObj)) continue;

            String sourceVersion = versionObj.get("version_number").getAsString();
            String version = cleanVersion(sourceVersion);
            Instant publishedAt = null;
            if (versionObj.has("date_published") && !versionObj.get("date_published").isJsonNull()) {
                publishedAt = Instant.parse(versionObj.get("date_published").getAsString());
            }
            return new UpdateInfo(version, sourceVersion, publishedAt);
        }
        return null;
    }

    /**
     * Returns {@code true} if the release lists at least one loader that is compatible
     * with the currently running server, or if the {@code loaders} field is absent.
     * <p>
     * Compatibility is hierarchical: a Purpur server can run Paper/Spigot/Bukkit jars,
     * a Paper server can run Spigot/Bukkit jars, etc.
     */
    private boolean isCompatibleLoader(JsonObject versionObj) {
        if (!versionObj.has("loaders")) return true;
        String detected = LOADER;
        JsonArray loaders = versionObj.getAsJsonArray("loaders");
        for (JsonElement loader : loaders) {
            if (isLoaderCompatibleWith(loader.getAsString().toLowerCase(), detected)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if {@code candidate} (a loader in the release metadata) is
     * usable on a server running {@code detected}.
     * <p>
     * Hierarchy (most specific first): folia > purpur > paper > spigot > bukkit
     */
    private static boolean isLoaderCompatibleWith(String candidate, String detected) {
        // A release targeting a more-specific fork is not safe on a less-specific server
        // (e.g. a Purpur-only jar should not load on plain Paper).
        // But a jar for a less-specific platform is always safe on a more-specific one.
        switch (detected) {
            case "folia":   return candidate.equals("folia")   || candidate.equals("purpur")
                                || candidate.equals("paper")   || candidate.equals("spigot") || candidate.equals("bukkit");
            case "purpur":  return candidate.equals("purpur")  || candidate.equals("paper")
                                || candidate.equals("spigot")  || candidate.equals("bukkit");
            case "paper":   return candidate.equals("paper")   || candidate.equals("spigot") || candidate.equals("bukkit");
            case "spigot":  return candidate.equals("spigot")  || candidate.equals("bukkit");
            default:        return candidate.equals("bukkit");
        }
    }

    /** Extracts the plain {@code X.Y.Z} Minecraft version from Bukkit's version string. */
    private String getServerMinecraftVersion() {
        String bukkit = Bukkit.getBukkitVersion(); // e.g. "1.20.4-R0.1-SNAPSHOT"
        int dash = bukkit.indexOf('-');
        return dash > 0 ? bukkit.substring(0, dash) : bukkit;
    }

    private String cleanVersion(String version) {
        if (version.toLowerCase().startsWith("v") && version.length() > 1 && Character.isDigit(version.charAt(1))) {
            return version.substring(1);
        }
        return version;
    }

    @Override
    public CompletableFuture<Boolean> downloadPlugin(String id, UpdateInfo update, File destination) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Re-fetch versions with loader filter to locate the matching release
                String detectedLoader = LOADER;
                String loadersParam = URLEncoder.encode("[\"" + detectedLoader + "\"]", StandardCharsets.UTF_8.name());
                URL url = new URL(String.format(API_URL, id) + "?loaders=" + loadersParam);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "AdvancedHunt/Updater");

                if (connection.getResponseCode() == 200) {
                    JsonArray versions = JsonParser.parseReader(new InputStreamReader(connection.getInputStream()))
                            .getAsJsonArray();

                    for (JsonElement element : versions) {
                        JsonObject versionObj = element.getAsJsonObject();
                        String sourceVersion = versionObj.get("version_number").getAsString();
                        if (!sourceVersion.equals(update.sourceVersion()) && !cleanVersion(sourceVersion).equals(update.version())) {
                            continue;
                        }
                        if (!isCompatibleLoader(versionObj)) {
                            Bukkit.getLogger().warning("[Modrinth] Version " + sourceVersion
                                    + " does not target a Bukkit-compatible platform. Aborting download.");
                            return false;
                        }
                        JsonArray files = versionObj.getAsJsonArray("files");
                        String downloadUrl = selectPrimaryFile(files);
                        if (downloadUrl != null) {
                            return downloadFile(downloadUrl, destination);
                        }
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to download from Modrinth", e);
            }
            return false;
        });
    }

    /**
     * Returns the URL of the file marked {@code primary}, falling back to the first file.
     */
    private String selectPrimaryFile(JsonArray files) {
        if (files == null || files.size() == 0) return null;
        for (JsonElement file : files) {
            JsonObject fileObj = file.getAsJsonObject();
            if (fileObj.has("primary") && fileObj.get("primary").getAsBoolean()) {
                return fileObj.get("url").getAsString();
            }
        }
        return files.get(0).getAsJsonObject().get("url").getAsString();
    }

    private boolean downloadFile(String urlString, File destination) {
        try {
            URL url = new URL(urlString);
            try (InputStream in = url.openStream()) {
                Files.copy(in, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to download file from Modrinth: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getName() {
        return "Modrinth";
    }
}
