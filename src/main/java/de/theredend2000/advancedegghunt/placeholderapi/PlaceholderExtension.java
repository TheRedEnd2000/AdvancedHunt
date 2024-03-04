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
        if(params.equalsIgnoreCase("max_eggs")){
            String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(player.getUniqueId());
            return String.valueOf(eggManager.getMaxEggs(collection));
        }

        if(params.equalsIgnoreCase("found_eggs")) {
            String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(player.getUniqueId());
            return String.valueOf(eggManager.getEggsFound(player, collection));
        }

        if(params.equalsIgnoreCase("remaining_eggs")) {
            String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(player.getUniqueId());
            return String.valueOf(eggManager.getMaxEggs(collection) - eggManager.getEggsFound(player, collection));
        }
        if(params.equalsIgnoreCase("top_player_name")) {
            return eggManager.getTopPlayerName();
        }
        if(params.equalsIgnoreCase("top_player_count")) {
            return String.valueOf(eggManager.getTopPlayerEggsFound());
        }
        if(params.equalsIgnoreCase("second_player_name")) {
            return eggManager.getSecondPlayerName();
        }
        if(params.equalsIgnoreCase("second_player_count")) {
            return String.valueOf(eggManager.getSecondPlayerEggsFound());
        }
        if(params.equalsIgnoreCase("third_player_name")) {
            return eggManager.getThirdPlayerName();
        }
        if(params.equalsIgnoreCase("third_player_count")) {
            return String.valueOf(eggManager.getThirdPlayerEggsFound());
        }

        return null;
    }
}
