package de.theredend2000.advancedegghunt.placeholderapi;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

public class PlaceholderExtension extends PlaceholderExpansion {
    private EggManager eggManager = Main.getInstance().getEggManager();

    @Override
    public String getAuthor() {
        return "theredend2000";
    }

    @Override
    public String getIdentifier() {
        return "advancedegghunt";
    }

    @Override
    public String getVersion() {
        return Main.getInstance().getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (params.equalsIgnoreCase("selected_collection")) {
            String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(player.getUniqueId());
            if(collection == null) return String.valueOf(Main.getInstance().getPluginConfig().getPlaceholderAPICollection());
            return String.valueOf(collection);
        }
        if (params.equalsIgnoreCase("collection_size")) {
            int collection = Main.getInstance().getEggDataManager().savedEggCollections().size();
            if(collection == 0) return String.valueOf(Main.getInstance().getPluginConfig().getPlaceholderAPICollection());
            return String.valueOf(collection);
        }
        if (params.equalsIgnoreCase("max_eggs")) {
            String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(player.getUniqueId());
            if(collection == null) return String.valueOf(Main.getInstance().getPluginConfig().getPlaceholderAPICollection());
            return String.valueOf(eggManager.getMaxEggs(collection));
        }

        if (params.equalsIgnoreCase("found_eggs")) {
            String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(player.getUniqueId());
            if(collection == null) return String.valueOf(Main.getInstance().getPluginConfig().getPlaceholderAPICollection());
            return String.valueOf(eggManager.getEggsFound(player, collection));
        }

        if (params.equalsIgnoreCase("remaining_eggs")) {
            String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(player.getUniqueId());
            if(collection == null) return String.valueOf(Main.getInstance().getPluginConfig().getPlaceholderAPICollection());
            return String.valueOf(eggManager.getMaxEggs(collection) - eggManager.getEggsFound(player, collection));
        }

        if (params.matches("player_name_\\d+")) {
            int number = extractNumberFromPlaceholder(params);
            return String.valueOf(eggManager.getLeaderboardPositionName(number-1, player.getUniqueId()));
        }

        if (params.matches("player_count_\\d+")) {
            int number = extractNumberFromPlaceholder(params);
            return String.valueOf(eggManager.getLeaderboardPositionCount(number-1, player.getUniqueId()));
        }

        return null;
    }

    public static int extractNumberFromPlaceholder(String placeholder) {
        String[] parts = placeholder.split("_");
        if (parts.length >= 3) {
            String numberString = parts[2];

            try {
                return Integer.parseInt(numberString);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return -1;
    }
}
