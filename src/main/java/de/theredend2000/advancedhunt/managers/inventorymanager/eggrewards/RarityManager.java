package de.theredend2000.advancedhunt.managers.inventorymanager.eggrewards;

import de.theredend2000.advancedhunt.Main;

import java.util.HashMap;
import java.util.Map;

public class RarityManager {

    private Main plugin;

    public RarityManager(){
        plugin = Main.getInstance();
    }


    public String getRarity(double chance){
        Map<String, Map<String, Double>> rarityMap = getRarityMap();

        for (Map.Entry<String, Map<String, Double>> entry : rarityMap.entrySet()) {
            String rarity = entry.getKey();
            Map<String, Double> rarityData = entry.getValue();
            double minChance = rarityData.get("min");
            double maxChance = rarityData.get("max");

            if (chance >= minChance && chance <= maxChance) {
                return rarity;
            }
        }

        return "unknown";
    }

    public Map<String, Map<String, Double>> getRarityMap() {
        Map<String, Map<String, Double>> rarityMap = new HashMap<>();

        for(String r : plugin.getPluginConfig().getRarityList())
            rarityMap.put(getRarityDisplay(r), createRarityMap(r));

        return rarityMap;
    }

    private Map<String, Double> createRarityMap(String rarityName) {
        Map<String, Double> rarityData = new HashMap<>();
        rarityData.put("min", getDouble(rarityName, "min"));
        rarityData.put("max", getDouble(rarityName, "max"));
        return rarityData;
    }

    private double getDouble(String rarityName, String valueName) {
        return plugin.getConfig().getDouble("Rarity." + rarityName + "." + valueName);
    }

    public String getRarityDisplay(String rarity){
        for(String raritys : plugin.getPluginConfig().getRarityList()){
            if(raritys.equals(rarity)) return plugin.getPluginConfig().getRarityName(rarity);
        }
        return "§cNONE FOUND";
    }

}
