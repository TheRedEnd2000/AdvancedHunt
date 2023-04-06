package de.theredend2000.advancedegghunt.commands;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.paginatedMenu.EggListMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdvancedEggHuntCommand implements CommandExecutor, TabCompleter {

    private final String permission = Main.getInstance().getConfig().getString("Permissions.AdvancedEggHuntCommandPermission");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender instanceof Player){
            Player player = (Player) sender;
            assert permission != null;
            if(player.hasPermission(permission)){
                if(args.length == 1){
                    if(args[0].equalsIgnoreCase("placeEggs")){
                        if(Main.getInstance().getPlaceEggsPlayers().contains(player)){
                            VersionManager.getEggManager().finishEggPlacing(player);
                            Main.getInstance().getPlaceEggsPlayers().remove(player);
                            player.sendMessage(Main.getInstance().getMessage("LeftPlaceMode"));
                        }else{
                            VersionManager.getEggManager().startEggPlacing(player);
                            Main.getInstance().getPlaceEggsPlayers().add(player);
                            player.sendMessage(Main.getInstance().getMessage("EnterPlaceMode"));
                            player.getInventory().setItem(0,VersionManager.getEggManager().giveFinishedEggToPlayer(0));
                            player.getInventory().setItem(1,VersionManager.getEggManager().giveFinishedEggToPlayer(1));
                            player.getInventory().setItem(2,VersionManager.getEggManager().giveFinishedEggToPlayer(2));
                            player.getInventory().setItem(3,VersionManager.getEggManager().giveFinishedEggToPlayer(3));
                            player.getInventory().setItem(4,VersionManager.getEggManager().giveFinishedEggToPlayer(4));
                            player.getInventory().setItem(5,VersionManager.getEggManager().giveFinishedEggToPlayer(5));
                            player.getInventory().setItem(6,VersionManager.getEggManager().giveFinishedEggToPlayer(6));
                            player.getInventory().setItem(8, new ItemBuilder(Material.PLAYER_HEAD).setSkullOwner(Main.getTexture("YTkyZTMxZmZiNTljOTBhYjA4ZmM5ZGMxZmUyNjgwMjAzNWEzYTQ3YzQyZmVlNjM0MjNiY2RiNDI2MmVjYjliNiJ9fX0=")).setDisplayname("§2§lFinish setup §7(Drop)").setLore("§7Drop to finish the setup","§7or type §e/egghunt placeEggs §7again.").setLocalizedName("egghunt.finish").build());
                        }
                    }else if(args[0].equalsIgnoreCase("list")){
                        new EggListMenu(Main.getPlayerMenuUtility(player)).open();
                    }else if(args[0].equalsIgnoreCase("reload")){
                        Main.getInstance().reloadConfig();
                        Main.getInstance().checkCommandFeedback();
                        player.sendMessage(Main.getInstance().getMessage("ReloadedConfig"));
                    }else if(args[0].equalsIgnoreCase("help")){
                        sendHelp(player);
                    }else if(args[0].equalsIgnoreCase("settings")){
                        VersionManager.getInventoryManager().createEggsSettingsInventory(player);
                    }else
                        player.sendMessage(usage());
                }else
                    player.sendMessage(usage());
            }else
                player.sendMessage(Main.getInstance().getMessage("NoPermissionMessage").replaceAll("%PERMISSION%",permission));
        }else
            sender.sendMessage(Main.getInstance().getMessage("OnlyPlayerCanUseThisCommandMessage"));
        return false;
    }

    private String usage(){
        return Main.getInstance().getMessage("AdvancedEggHuntCommandUsageMessage");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1 && sender.hasPermission(permission)){
            String[] tabs = {"placeEggs","reload","list","help","settings"};
            ArrayList<String> complete = new ArrayList<>();
            Collections.addAll(complete,tabs);
            return complete;
        }
        return null;
    }

    private void sendHelp(Player player){
        player.sendMessage("§3-----------------------------------------");
        player.sendMessage("§5§l==========HELP==========");
        player.sendMessage("");
        player.sendMessage("§2§lInformation");
        player.sendMessage("§7Name: §6"+Main.getInstance().getDescription().getName());
        player.sendMessage("§7Plugin Version: §6"+Main.getInstance().getDescription().getVersion());
        player.sendMessage("§7Api Version: §6"+Main.getInstance().getDescription().getAPIVersion());
        player.sendMessage("§7Server Version: §6"+ Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3]);
        player.sendMessage("§7Author: §6XMC-PLUGINS");
        player.sendMessage("");
        player.sendMessage("§2§lCommands");
        player.sendMessage("§6/advancedegghunt help §7-> §bShows this help messages and information.");
        player.sendMessage("§6/advancedegghunt reload §7-> §bReloads the config.");
        player.sendMessage("§6/advancedegghunt list §7-> §bLists all placed eggs.");
        player.sendMessage("§6/advancedegghunt placeEggs §7-> §bEnter Place-Mode the place or break eggs.");
        player.sendMessage("§6/advancedegghunt settings §7-> §bConfigure many settings of the plugin.");
        player.sendMessage("§5§l==========HELP==========");
        player.sendMessage("§3-----------------------------------------");
    }
}
