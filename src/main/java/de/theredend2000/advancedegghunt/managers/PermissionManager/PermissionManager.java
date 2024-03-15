package de.theredend2000.advancedegghunt.managers.PermissionManager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.enums.Permission;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class PermissionManager {
    private Main plugin;

    public PermissionManager(){
        plugin = Main.getInstance();
    }

    private boolean checkPermission(CommandSender sender, String permission){
        return sender.hasPermission(permission);
    }

    @Nullable
    public boolean checkPermission(CommandSender sender, Permission permission){
        if (permission == null) return false;
        return checkPermission(sender, permission.toString());
    }

    @Nullable
    public boolean checkPermission(CommandSender sender, Permission.Command permission){
        if (permission == null) return false;
        return checkPermission(sender, permission.toString());
    }
}
