package de.theredend2000.advancedegghunt.commands.Framework;

import org.bukkit.command.TabExecutor;

public interface IBaseCommand extends TabExecutor {
    String getName();
    String getDescription();
    String getUsage();
    String getPermission();
    String getPermissionMessage();
    void setUsage(String usage);
    void setPermission(String permission);
    void setPermissionMessage(String permissionMessage);
}
