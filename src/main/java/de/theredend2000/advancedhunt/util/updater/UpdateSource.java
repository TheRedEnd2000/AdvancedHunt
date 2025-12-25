package de.theredend2000.advancedHunt.util.updater;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public interface UpdateSource {
    /**
     * Checks for the latest version of the plugin.
     * @param id The identifier of the plugin on this platform (slug, id, etc.)
     * @return A future completing with the latest version string, or null if not found/error.
     */
    CompletableFuture<String> getLatestVersion(String id);

    /**
     * Downloads the specified version of the plugin.
     * @param id The identifier of the plugin on this platform.
     * @param version The version to download.
     * @param destination The file to save the plugin to.
     * @return A future completing with true if successful, false otherwise.
     */
    CompletableFuture<Boolean> downloadPlugin(String id, String version, File destination);
    
    /**
     * @return The name of the source (e.g. "Modrinth", "SpigotMC").
     */
    String getName();
}
