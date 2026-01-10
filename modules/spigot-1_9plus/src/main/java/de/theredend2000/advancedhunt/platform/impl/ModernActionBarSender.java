package de.theredend2000.advancedhunt.platform.impl;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

final class ModernActionBarSender {

    void send(Player player, String message) {
        if (player == null) return;
        BaseComponent[] components = TextComponent.fromLegacyText(message == null ? "" : message);
        try {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
        } catch (Throwable ignored) {
        }
    }
}
