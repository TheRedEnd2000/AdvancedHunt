package de.theredend2000.advancedhunt.platform.impl;

import net.minecraft.server.v1_8_R3.ChatComponentText;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

final class LegacyActionBarSender {

    void send(Player player, String message) {
        if (player == null) return;
        try {
            PacketPlayOutChat packet = new PacketPlayOutChat(new ChatComponentText(message == null ? "" : message), (byte) 2);
            ((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
        } catch (Throwable ignored) {
        }
    }
}
