package de.theredend2000.advancedegghunt.placeholderapi;

import de.theredend2000.advancedegghunt.versions.VersionManager;
import de.theredend2000.advancedegghunt.versions.managers.eggmanager.EggManager;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.PlaceholderHook;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
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
        if(params.equalsIgnoreCase("max_eggs")){
            return String.valueOf(VersionManager.getEggManager().getMaxEggs());
        }

        if(params.equalsIgnoreCase("found_eggs")) {
            return String.valueOf(VersionManager.getEggManager().getEggsFound(player));
        }

        if(params.equalsIgnoreCase("remaining_eggs")) {
            return String.valueOf(VersionManager.getEggManager().getMaxEggs() - VersionManager.getEggManager().getEggsFound(player));
        }
        if(params.equalsIgnoreCase("top_player_name")) {
            return VersionManager.getEggManager().getTopPlayerName();
        }
        if(params.equalsIgnoreCase("top_player_count")) {
            return String.valueOf(VersionManager.getEggManager().getTopPlayerEggsFound());
        }
        if(params.equalsIgnoreCase("second_player_name")) {
            return VersionManager.getEggManager().getSecondPlayerName();
        }
        if(params.equalsIgnoreCase("second_player_count")) {
            return String.valueOf(VersionManager.getEggManager().getSecondPlayerEggsFound());
        }
        if(params.equalsIgnoreCase("third_player_name")) {
            return VersionManager.getEggManager().getThirdPlayerName();
        }
        if(params.equalsIgnoreCase("third_player_count")) {
            return String.valueOf(VersionManager.getEggManager().getThirdPlayerEggsFound());
        }

        return null;
    }

}
