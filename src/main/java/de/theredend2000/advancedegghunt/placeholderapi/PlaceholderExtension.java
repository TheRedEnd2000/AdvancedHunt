package de.theredend2000.advancedegghunt.placeholderapi;


import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import org.bukkit.entity.Player;

public class PlaceholderExtension extends PlaceholderExpansion {
     private EggManager eggManager = Main.getInstance().getEggManager();

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
            return String.valueOf(eggManager.getMaxEggs());
        }

        if(params.equalsIgnoreCase("found_eggs")) {
            return String.valueOf(eggManager.getEggsFound(player));
        }

        if(params.equalsIgnoreCase("remaining_eggs")) {
            return String.valueOf(eggManager.getMaxEggs() - eggManager.getEggsFound(player));
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
