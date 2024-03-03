package de.theredend2000.advancedegghunt.util;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public class ConfigLocationUtil {
    private Main plugin;
    private Location location;
    private String root;
    private String section;
    public ConfigLocationUtil(Main plugin,Location location, String root, String section){
        this.plugin = plugin;
        this.location = location;
        this.root = root;
        this.section = section;
    }
    public ConfigLocationUtil(Main plugin, String root, String section){
        this.plugin = plugin;
        this.root = root;
        this.section = section;
    }

    public void saveBlockLocation() {
        FileConfiguration config = plugin.getEggDataManager().getPlacedEggs(section);
        config.set(root + ".World", location.getWorld().getName());
        config.set(root + ".X", location.getBlockX());
        config.set(root + ".Y", location.getBlockY());
        config.set(root + ".Z", location.getBlockZ());
        config.set(root + ".Date", plugin.getDatetimeUtils().getNowDate());
        config.set(root + ".Time", plugin.getDatetimeUtils().getNowTime());
        Main.getInstance().getEggDataManager().savePlacedEggs(section,config);
    }

    public Block loadBlockLocation() {
        FileConfiguration config = plugin.getEggDataManager().getPlacedEggs(section);
        if(config.contains(root)) {
            World world = Bukkit.getWorld(config.getString(root+".World"));
            int x = config.getInt(root +".X"),
                    y = config.getInt(root +".Y"),
                    z = config.getInt(root +".Z");
            return new Location(world, x,y,z).getBlock();
        }
        return null;
    }

    public void saveBlockLocation(UUID uuid,FileConfiguration config) {
        config.set(root + ".World", location.getWorld().getName());
        config.set(root + ".X", location.getBlockX());
        config.set(root + ".Y", location.getBlockY());
        config.set(root + ".Z", location.getBlockZ());
        config.set(root + ".Date", plugin.getDatetimeUtils().getNowDate());
        config.set(root + ".Time", plugin.getDatetimeUtils().getNowTime());
        plugin.getPlayerEggDataManager().savePlayerData(uuid,config);
    }

    public Block loadBlockLocation(UUID uuid) {
        FileConfiguration config = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        if(config.contains(root)) {
            World world = Bukkit.getWorld(config.getString(root+".World"));
            int x = config.getInt(root +".X"),
                    y = config.getInt(root +".Y"),
                    z = config.getInt(root +".Z");
            return new Location(world, x,y,z).getBlock();
        }
        return null;
    }

    public ConfigLocationUtil(Main plugin, String root) {
        this(plugin, null, root,null);
    }

    public void saveLocation() {
        FileConfiguration config = plugin.getEggDataManager().getPlacedEggs(section);
        config.set(root+".World", location.getWorld().getName());
        config.set(root+".X", location.getX());
        config.set(root+".Y", location.getY());
        config.set(root+".Z", location.getZ());
        config.set(root+".Yaw", location.getYaw());
        config.set(root+".Pitch", location.getPitch());
        Main.getInstance().getEggDataManager().savePlacedEggs(section,config);
    }
    public Location loadLocation() {
        FileConfiguration config = plugin.getEggDataManager().getPlacedEggs(section);
        if(config.contains(root)) {
            World world = Bukkit.getWorld(config.getString(root + ".World"));
            double x = config.getInt(root+".X"),
                    y = config.getInt(root+".Y"),
                    z = config.getInt(root+".Z");
            return new Location(world,x,y,z);
        }else
            return  null;

    }

    public Location loadLocation(UUID uuid) {
        FileConfiguration config = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        if(config.contains(root)) {
            World world = Bukkit.getWorld(config.getString(root + ".World"));
            double x = config.getInt(root+".X"),
                    y = config.getInt(root+".Y"),
                    z = config.getInt(root+".Z");
            return new Location(world,x,y,z);
        }else
            return  null;

    }
}
