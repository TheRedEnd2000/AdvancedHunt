package de.theredend2000.advancedhunt.util;

import de.theredend2000.advancedhunt.platform.PlatformAccess;
import org.bukkit.entity.Player;

public class MessageUtils {

    public static void sendActionBar(Player player, String message) {
        PlatformAccess.get().sendActionBar(player, message);
    }
}
