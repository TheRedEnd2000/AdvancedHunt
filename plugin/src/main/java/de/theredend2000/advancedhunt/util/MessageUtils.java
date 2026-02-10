package de.theredend2000.advancedhunt.util;

import de.theredend2000.advancedhunt.Main;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class MessageUtils {

    public static void sendActionBar(Player player, String message) {
        if (player == null) return;
        try {
            Main plugin = JavaPlugin.getPlugin(Main.class);
            BukkitAudiences audiences = plugin.getAdventure();
            if (audiences == null) return;

            String safe = message == null ? "" : message;
            safe = ChatColor.translateAlternateColorCodes('&',safe);
            Component component = LegacyComponentSerializer.legacySection().deserialize(safe);
            audiences.player(player).sendActionBar(component);
        } catch (Throwable ignored) {
        }
    }
}
