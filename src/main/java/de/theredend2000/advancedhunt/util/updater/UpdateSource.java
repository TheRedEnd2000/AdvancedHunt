package de.theredend2000.advancedhunt.util.updater;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public interface UpdateSource {
    /**
     * Checks for the latest stable update of the plugin.
     * @param id The identifier of the plugin on this platform (slug, id, etc.)
     * @return A future completing with update info, or null if not found/error.
     */
    CompletableFuture<UpdateInfo> getLatestUpdate(String id);

    /**
    * Downloads the specified update of the plugin.
     * @param id The identifier of the plugin on this platform.
    * @param update The update to download.
     * @param destination The file to save the plugin to.
     * @return A future completing with true if successful, false otherwise.
     */
    CompletableFuture<Boolean> downloadPlugin(String id, UpdateInfo update, File destination);
    
    /**
     * @return The name of the source (e.g. "Modrinth", "SpigotMC").
     */
    String getName();
}
