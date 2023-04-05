package de.theredend2000.advancedegghunt;

import de.theredend2000.advancedegghunt.commands.AdvancedEggHuntCommand;
import de.theredend2000.advancedegghunt.listeners.BlockBreakEventListener;
import de.theredend2000.advancedegghunt.listeners.BlockPlaceEventListener;
import de.theredend2000.advancedegghunt.listeners.InventoryClickEventListener;
import de.theredend2000.advancedegghunt.listeners.PlayerInteractEventListener;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;

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
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void initListeners(){
        new InventoryClickEventListener();
        new BlockPlaceEventListener();
        new BlockBreakEventListener();
        new PlayerInteractEventListener();
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
    public ArrayList<Player> getPlaceEggsPlayers() {
        return placeEggsPlayers;
    }
}
