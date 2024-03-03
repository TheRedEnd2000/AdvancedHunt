package de.theredend2000.advancedegghunt.util;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.UUID;

public class ConfigLocationUtil {
    private Main plugin;
    private Location location;
    private String root;
    public ConfigLocationUtil(Main plugin, Location location, String root){
        this.plugin = plugin;
        this.location = location;
        this.root = root;
    }

    public void saveBlockLocation(String section) {
        FileConfiguration config = plugin.getEggDataManager().getPlacedEggs(section);
        config.set(root + ".World", location.getWorld().getName());
        config.set(root + ".X", location.getBlockX());
        config.set(root + ".Y", location.getBlockY());
        config.set(root + ".Z", location.getBlockZ());
        config.set(root + ".Date", plugin.getDatetimeUtils().getNowDate());
        config.set(root + ".Time", plugin.getDatetimeUtils().getNowTime());
        Main.getInstance().getEggDataManager().savePlacedEggs(section, config);
    }

    public void saveBlockLocation(UUID uuid) {
        FileConfiguration config = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        config.set(root + ".World", location.getWorld().getName());
        config.set(root + ".X", location.getBlockX());
        config.set(root + ".Y", location.getBlockY());
        config.set(root + ".Z", location.getBlockZ());
        config.set(root + ".Date", plugin.getDatetimeUtils().getNowDate());
        config.set(root + ".Time", plugin.getDatetimeUtils().getNowTime());
        plugin.getPlayerEggDataManager().savePlayerData(uuid, config);
    }

    public ConfigLocationUtil(Main plugin, String root) {
        this(plugin, null, root);
    }
    public Location loadLocation(String section) {
        FileConfiguration config = plugin.getEggDataManager().getPlacedEggs(section);
        if (config.contains(root)) {
            World world = Bukkit.getWorld(config.getString(root + ".World"));
            if (world != null) {
                double x = config.getInt(root + ".X"),
                        y = config.getInt(root + ".Y"),
                        z = config.getInt(root + ".Z");
                return new Location(world, x, y, z);
            }
        }
        return null;
    }

    public Location loadLocation(UUID uuid) {
        FileConfiguration config = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        if (!config.contains(root)) {
            return null;
        }
        World world = Bukkit.getWorld(config.getString(root + ".World"));
        if (world == null) {
            return null;
        }
        int x = config.getInt(root + ".X"),
                y = config.getInt(root + ".Y"),
                z = config.getInt(root + ".Z");
        return new Location(world, x, y, z);
    }
}
