package de.theredend2000.advancedegghunt.util.saveinventory;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class Config {
    private Main plugin;
    private String file;
    private File folder;
    private FileConfiguration cfg;
    private File cfgFile;

    public Config(Main plugin, String file){
        this.plugin = plugin;
        this.file = file + ".yml";
        folder = new File(plugin.getDataFolder() + "//invs//");
        cfg = null;
        cfgFile = null;
        reload();
    }

    public void reload() {
        if(!folder.exists())
            folder.mkdirs();

        cfgFile = new File(folder, file);
        if(!cfgFile.exists()){
            try {
                cfgFile.createNewFile();
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        cfg = YamlConfiguration.loadConfiguration(cfgFile);
    }

    public FileConfiguration getConfig() {
        if(cfg == null)
            reload();
        return cfg;
    }

    public void saveInv(){
        if(cfg == null || cfgFile == null)
            return;

        try {
            getConfig().save(cfgFile);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
