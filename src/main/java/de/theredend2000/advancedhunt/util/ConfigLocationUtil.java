package de.theredend2000.advancedhunt.util;

import de.theredend2000.advancedhunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigLocationUtil {

    private static final Map<String, Location> locationCache = new ConcurrentHashMap<>();

    private final Main plugin;
    private final Location location;
    private final String root;

    public ConfigLocationUtil(Main plugin, String root) {
        this(plugin, null, root);
    }

    public ConfigLocationUtil(Main plugin, Location location, String root) {
        this.plugin = plugin;
        this.location = location;
        this.root = root;
    }

    public void saveBlockLocation(String collection) {
        if (location == null || location.getWorld() == null) return;

        FileConfiguration config = plugin.getEggDataManager().getPlacedEggs(collection);
        config.set(root + ".World", location.getWorld().getName());
        config.set(root + ".X", location.getBlockX());
        config.set(root + ".Y", location.getBlockY());
        config.set(root + ".Z", location.getBlockZ());
        config.set(root + ".Date", plugin.getDatetimeUtils().getNowDate());
        config.set(root + ".Time", plugin.getDatetimeUtils().getNowTime());
        plugin.getEggDataManager().savePlacedEggs(collection);

        // Cache aktualisieren
        locationCache.put(collection + ":" + root, location.clone());
        Main.getInstance().getLogger().info("ConfigLocationUtil: cache updated for " + collection + ":" + root);
    }

    public void saveBlockLocation(UUID uuid) {
        if (location == null || location.getWorld() == null) return;

        FileConfiguration config = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        config.set(root + ".World", location.getWorld().getName());
        config.set(root + ".X", location.getBlockX());
        config.set(root + ".Y", location.getBlockY());
        config.set(root + ".Z", location.getBlockZ());
        config.set(root + ".Date", plugin.getDatetimeUtils().getNowDate());
        config.set(root + ".Time", plugin.getDatetimeUtils().getNowTime());
        plugin.getPlayerEggDataManager().savePlayerData(uuid, config);

        // Cache aktualisieren
        locationCache.put(uuid.toString() + ":" + root, location.clone());
        Main.getInstance().getLogger().info("ConfigLocationUtil: cache updated for " + uuid + ":" + root);
    }

    public Location loadLocation(String collection) {
        String key = collection + ":" + root;
        Location cached = locationCache.get(key);
        if (cached != null) {
            Main.getInstance().getLogger().fine("ConfigLocationUtil: cache hit for " + key);
            return cached.clone();
        }

        FileConfiguration config = plugin.getEggDataManager().getPlacedEggs(collection);
        if (!config.contains(root + ".World")) return null;
        World world = Bukkit.getWorld(config.getString(root + ".World", ""));
        if (world == null) return null;

        double x = config.getDouble(root + ".X");
        double y = config.getDouble(root + ".Y");
        double z = config.getDouble(root + ".Z");

        Location loc = new Location(world, x, y, z);
        locationCache.put(key, loc.clone());
        Main.getInstance().getLogger().fine("ConfigLocationUtil: cache miss -> loaded and cached " + key);
        return loc;
    }

    public Location loadLocation(UUID uuid) {
        String key = uuid.toString() + ":" + root;
        Location cached = locationCache.get(key);
        if (cached != null) {
            Main.getInstance().getLogger().fine("ConfigLocationUtil: cache hit for " + key);
            return cached.clone();
        }

        FileConfiguration config = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        if (!config.contains(root + ".World")) return null;
        World world = Bukkit.getWorld(config.getString(root + ".World"));
        if (world == null) return null;

        double x = config.getDouble(root + ".X");
        double y = config.getDouble(root + ".Y");
        double z = config.getDouble(root + ".Z");

        Location loc = new Location(world, x, y, z);
        locationCache.put(key, loc.clone());
        Main.getInstance().getLogger().fine("ConfigLocationUtil: cache miss -> loaded and cached " + key);
        return loc;
    }
}
