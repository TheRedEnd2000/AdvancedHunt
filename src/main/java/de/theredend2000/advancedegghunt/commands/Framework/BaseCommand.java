package de.theredend2000.advancedegghunt.commands.Framework;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public abstract class BaseCommand implements IBaseCommand {
    protected String name;
    protected String description;
    protected String usageMessage;
    protected String permission;
    protected String permissionMessage;

    public BaseCommand(String name, String description, String syntax, String permission, String permissionMessage) {
        this.name = name;
        this.description = description;
        this.usageMessage = syntax;
        this.permission = permission;
        this.permissionMessage = permissionMessage;
    }

    public BaseCommand() {}

    @Override
    public void setUsage(@NotNull String usage) {
        this.usageMessage = usage.isEmpty() ? "" : usage;
    }

    @Override
    public void setPermission(@Nullable String permission) {
        this.permission = permission;
    }

    @Override
    public void setPermissionMessage(@Nullable String permissionMessage) {
        this.permissionMessage = permissionMessage;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getUsage() {
        return usageMessage;
    }

    @Override
    public String getPermission() {
        return permission;
    }

    @Override
    public String getPermissionMessage() {
        return permissionMessage;
    }

    @Override
    public abstract boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args);

    @Nullable
    @Override
    public abstract List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args);

    public boolean testPermission(@NotNull CommandSender target) {
        if (testPermissionSilent(target)) {
            return true;
        }

        if (permissionMessage == null) {
            target.sendMessage(ChatColor.RED + "I'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is a mistake.");
        } else if (!permissionMessage.isEmpty()) {
            for (String line : permissionMessage.replace("<permission>", permission).split("\n")) {
                target.sendMessage(line);
            }
        }

        return false;
    }

    public boolean testPermissionSilent(@NotNull CommandSender target) {
        if (permission == null || permission.isEmpty()) {
            return true;
        }

        return permission.contains(";") 
            ? Arrays.stream(permission.split(";")).anyMatch(target::hasPermission)
            : target.hasPermission(permission);
    }
}
