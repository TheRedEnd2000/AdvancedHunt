package de.theredend2000.advancedegghunt.managers.PermissionManager;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.entity.Player;

public class PermissionManager {

    private Main plugin;

    public PermissionManager(){
        plugin = Main.getInstance();
    }

    public boolean checkCommandPermission(Player player, String command){
        boolean use = plugin.getConfig().getBoolean("Permissions.AdvancedEggHuntCommandPermission.commands."+command+".use");
        if(!use) return true;
        String permission = plugin.getConfig().getString("Permissions.AdvancedEggHuntCommandPermission.commands."+command+".permission");
        assert permission != null;
        return player.hasPermission(permission);
    }

    public String getPermission(String command){
        return plugin.getConfig().getString("Permissions.AdvancedEggHuntCommandPermission.commands."+command+".permission");
    }

    public boolean checkOtherPermission(Player player, String per){
        boolean use = plugin.getConfig().getBoolean("Permissions."+per+".use");
        if(!use) return true;
        String permission = plugin.getConfig().getString("Permissions."+per+".permission");
        assert permission != null;
        return player.hasPermission(permission);
    }

    public String getOtherPermission(Player player, String per){
        return plugin.getConfig().getString("Permissions."+per+".permission");
    }

}
