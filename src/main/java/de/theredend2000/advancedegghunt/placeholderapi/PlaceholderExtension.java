package de.theredend2000.advancedegghunt.placeholderapi;

import de.theredend2000.advancedegghunt.versions.VersionManager;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderExtension extends PlaceholderExpansion {

    @Override
    public String getAuthor() {
        return "someauthor";
    }

    @Override
    public String getIdentifier() {
        return "advancedegghunt";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if(params.equalsIgnoreCase("maxeggs")){
            return String.valueOf(VersionManager.getEggManager().getMaxEggs());
        }

        if(params.equalsIgnoreCase("foundeggs")) {
            return String.valueOf(VersionManager.getEggManager().getEggsFound(player));
        }

        return null;
    }

}
