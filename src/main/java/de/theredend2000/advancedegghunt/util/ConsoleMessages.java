package de.theredend2000.advancedegghunt.util;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

public class ConsoleMessages {

    public static void sendNotCompatibleVersion(){
        ConsoleCommandSender s = Bukkit.getConsoleSender();
        s.sendMessage("§4============================================================");
        s.sendMessage("§4THIS VERSION IS NOT COMPATIBLE!");
        s.sendMessage("§4PLEASE USE VERSIONS BETWEEN 1.13.x - 1.20.x");
        s.sendMessage("§4============================================================");
    }
}
