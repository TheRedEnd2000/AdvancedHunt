package de.theredend2000.advancedegghunt;

import de.theredend2000.advancedegghunt.commands.AdvancedEggHuntCommand;
import de.theredend2000.advancedegghunt.listeners.*;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.Updater;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.paginatedMenu.PlayerMenuUtility;
import org.bukkit.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;

public final class Main extends JavaPlugin {

    private static Main plugin;
    private ArrayList<Player> placeEggsPlayers;
    public YamlConfiguration messages;
    public File messagesData;
    public YamlConfiguration eggs;
    private File data = new File(getDataFolder(), "eggs.yml");

    @Override
    public void onEnable() {
        plugin = this;
        setupConfigs();
        VersionManager.registerAllManagers();
        placeEggsPlayers = new ArrayList<>();
        getCommand("advancedegghunt").setExecutor(new AdvancedEggHuntCommand());
        initListeners();
        VersionManager.getEggManager().spawnEggParticle();
        checkCommandFeedback();
    }

    @Override
    public void onDisable() {
        giveAllItemsBack();
    }

    public void checkCommandFeedback(){
        if(getConfig().getBoolean("Settings.DisableCommandFeedback")){
            for(World worlds : Bukkit.getServer().getWorlds())
                worlds.setGameRule(GameRule.SEND_COMMAND_FEEDBACK,false);
        }else{
            for(World worlds : Bukkit.getServer().getWorlds())
                worlds.setGameRule(GameRule.SEND_COMMAND_FEEDBACK,true);
        }
    }

    private void initListeners(){
        new InventoryClickEventListener();
        new BlockPlaceEventListener();
        new BlockBreakEventListener();
        new PlayerInteractEventListener();
        new PlayerInteractItemEvent();
        new Updater(this);
    }

    private void giveAllItemsBack(){
        for(Player player : Bukkit.getServer().getOnlinePlayers()){
            if(placeEggsPlayers.contains(player)){
                VersionManager.getEggManager().finishEggPlacing(player);
            }
        }
    }

    private void setupConfigs(){
        saveDefaultConfig();
        messagesData = new File(getDataFolder(),"messages.yml");
        try {
            if(!messagesData.exists()){
                InputStream in = getResource("messages.yml");
                assert in != null;
                Files.copy(in,messagesData.toPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.messages = YamlConfiguration.loadConfiguration(this.messagesData);
        saveMessages();
        this.eggs = YamlConfiguration.loadConfiguration(this.data);
        this.saveEggs();
    }

    public void saveMessages() {
        try {
            this.messages.save(this.messagesData);
        } catch (IOException var2) {
            var2.printStackTrace();
        }
    }

    public void saveEggs() {
        try {
            this.eggs.save(this.data);
        } catch (IOException var2) {
            var2.printStackTrace();
        }
    }

    public Material getMaterial(String materialString) {
        try {
            Material material = Material.getMaterial(materialString);
            if (material == null) {
                return Material.BARRIER;
            }
            return material;
        } catch (Exception ex) {
            Bukkit.getConsoleSender().sendMessage("§4Material Error: "+ex);
            return Material.STONE;
        }
    }


    public static Main getInstance() {
        return plugin;
    }

    public static String getTexture(String texture){
        String prefix = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv";
        texture = prefix+texture;
        return texture;
    }
    public String getMessage(String message){
        return messages.getString("Prefix").replace("&","§")+messages.getString(message).replace("&","§");
    }
    private static final HashMap<Player, PlayerMenuUtility> playerMenuUtilityMap = new HashMap<>();
    public static PlayerMenuUtility getPlayerMenuUtility(Player p) {
        PlayerMenuUtility playerMenuUtility;
        if (!(playerMenuUtilityMap.containsKey(p))) { //See if the player has a playermenuutility "saved" for them

            //This player doesn't. Make one for them add add it to the hashmap
            playerMenuUtility = new PlayerMenuUtility(p);
            playerMenuUtilityMap.put(p, playerMenuUtility);

            return playerMenuUtility;
        } else {
            return playerMenuUtilityMap.get(p); //Return the object by using the provided player
        }
    }

    public ArrayList<Player> getPlaceEggsPlayers() {
        return placeEggsPlayers;
    }
}
