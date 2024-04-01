package de.theredend2000.advancedhunt.util;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Updater implements Listener {

    private Main plugin;
    public static boolean isOutdated;
    private int key = 109085;

    public Updater(Main plugin){
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        isOutdated();
    }

    public boolean isOutdated(Player player) {
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL("https://api.spigotmc.org/legacy/update.php?resource=" + key).openConnection();
            String newVersion = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
            connection.disconnect();
            String oldVersion = plugin.getDescription().getVersion();
            if(VersionComparator.isLessThan(oldVersion, newVersion)) {
                isOutdated = true;
                plugin.getMessageManager().sendMessage(player, MessageKey.UPDATE_AVAILABLE, "%OLD_VERSION%", oldVersion, "%NEW_VERSION%", newVersion);
                return true;
            }
        }
        catch(Exception e) {
            plugin.getMessageManager().sendMessage(player, MessageKey.UPDATE_ERROR);
            e.printStackTrace();
        }
        return false;
    }

    public boolean isOutdated() {
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL("https://api.spigotmc.org/legacy/update.php?resource=" + key).openConnection();
            String newVersion = new BufferedReader(new InputStreamReader(connection.getInputStream())).readLine();
            connection.disconnect();
            String oldVersion = plugin.getDescription().getVersion();
            if(VersionComparator.isLessThan(oldVersion, newVersion)) {
                isOutdated = true;
                plugin.getMessageManager().sendMessage(Bukkit.getConsoleSender(), MessageKey.CONSOLE_UPDATE_AVAILABLE, "%OLD_VERSION%", oldVersion, "%NEW_VERSION%", newVersion);
                return true;
            }
        }
        catch(Exception e) {
            plugin.getMessageManager().sendMessage(Bukkit.getConsoleSender(), MessageKey.CONSOLE_UPDATE_ERROR);
            e.printStackTrace();
        }
        return false;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        boolean updates = plugin.getPluginConfig().getUpdater();
        if(updates){
            if(!player.isOp()) return;
            if(isOutdated(player));
        }
    }
}
