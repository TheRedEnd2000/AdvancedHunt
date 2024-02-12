package de.theredend2000.advancedegghunt.commands;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.sectionselection.SelectionSelectListMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.sectionselection.SelectionSelectMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.InventoryManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggfoundrewardmenu.EggRewardMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggprogress.EggProgressMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.hintInventory.HintInventoryCreator;
import de.theredend2000.advancedegghunt.managers.inventorymanager.leaderboardmenu.EggLeaderboardMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.EggListMenu;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class AdvancedEggHuntCommand implements CommandExecutor, TabCompleter {

    private final String permission = Main.getInstance().getConfig().getString("Permissions.AdvancedEggHuntCommandPermission");

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EggManager eggManager = Main.getInstance().getEggManager();
        InventoryManager inventoryManager = Main.getInstance().getInventoryManager();
        if(sender instanceof Player){
            Player player = (Player) sender;
            assert permission != null;
            if(player.hasPermission(permission)){
                if(args.length == 1){
                    if(args[0].equalsIgnoreCase("placeEggs")){
                        if(Main.getInstance().getPlaceEggsPlayers().contains(player)){
                            eggManager.finishEggPlacing(player);
                            Main.getInstance().getPlaceEggsPlayers().remove(player);
                            player.sendMessage(Main.getInstance().getMessage("LeftPlaceMode"));
                        }else{
                            eggManager.startEggPlacing(player);
                            Main.getInstance().getPlaceEggsPlayers().add(player);
                            player.sendMessage(Main.getInstance().getMessage("EnterPlaceMode"));
                            player.getInventory().setItem(4,new ItemBuilder(XMaterial.NETHER_STAR).setDisplayname("§6§lEggs Types §7(Right-Click)").setLocalizedName("egghunt.eggs").build());
                            player.getInventory().setItem(8, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("YTkyZTMxZmZiNTljOTBhYjA4ZmM5ZGMxZmUyNjgwMjAzNWEzYTQ3YzQyZmVlNjM0MjNiY2RiNDI2MmVjYjliNiJ9fX0=")).setDisplayname("§2§lFinish setup §7(Drop)").setLore("§7Drop to finish the setup","§7or type §e/egghunt placeEggs §7again.").setLocalizedName("egghunt.finish").setSoulbound(true).build());
                        }
                    }else if(args[0].equalsIgnoreCase("list")){
                        new EggListMenu(Main.getPlayerMenuUtility(player)).open();
                    }else if(args[0].equalsIgnoreCase("show")){
                        eggManager.showAllEggs();
                        player.sendMessage(Main.getInstance().getMessage("EggsVisible").replaceAll("%TIME_VISIBLE%", String.valueOf(Main.getInstance().getConfig().getInt("Settings.ArmorstandGlow"))));
                    }else if(args[0].equalsIgnoreCase("reload")){
                        Main.getInstance().reloadConfig();
                        Main.getInstance().checkCommandFeedback();
                        player.sendMessage(Main.getInstance().getMessage("ReloadedConfig"));
                    }else if(args[0].equalsIgnoreCase("help")){
                        sendHelp(player);
                    }else if(args[0].equalsIgnoreCase("settings")){
                        inventoryManager.createEggsSettingsInventory(player);
                    }else if(args[0].equalsIgnoreCase("collection")){
                        new SelectionSelectListMenu(Main.getPlayerMenuUtility(player)).open();
                    }else if(args[0].equalsIgnoreCase("progress")){
                        new EggProgressMenu(Main.getPlayerMenuUtility(player)).open();
                    }else if(args[0].equalsIgnoreCase("commands")){
                        new EggRewardMenu(Main.getPlayerMenuUtility(player)).open();
                    }else if(args[0].equalsIgnoreCase("leaderboard")){
                        new EggLeaderboardMenu(Main.getPlayerMenuUtility(player)).open();
                    }else if(args[0].equalsIgnoreCase("hint")){
                        for(String sections : Main.getInstance().getEggDataManager().savedEggSections()) {
                            if (!eggManager.checkFoundAll(player,sections) && eggManager.getMaxEggs(sections) >= 1) {
                                if (!Main.getInstance().getCooldownManager().isAllowReward(player) && !player.hasPermission(Objects.requireNonNull(Main.getInstance().getConfig().getString("Permissions.IgnoreCooldownPermission")))) {
                                    long current = System.currentTimeMillis();
                                    long release = Main.getInstance().getCooldownManager().getCooldown(player);
                                    long millis = release - current;
                                    player.sendMessage(Main.getInstance().getCooldownManager().getRemainingTime(millis));
                                    return true;
                                }
                                new HintInventoryCreator(player, Bukkit.createInventory(player, 54, "Eggs Hint"), true);
                            } else
                                player.sendMessage(Main.getInstance().getMessage("PlayerFoundAllEggs"));
                        }
                    }else
                        player.sendMessage(usage());
                }else if(args.length == 2){
                    if(args[0].equalsIgnoreCase("reset")){
                        if(args[1].equalsIgnoreCase("all")){
                            eggManager.resetStatsAll();
                            player.sendMessage(Main.getInstance().getMessage("ResetedAllFoundEggs"));
                            return true;
                        }
                        String name = args[1];
                        if(eggManager.containsPlayer(name)){
                            for(String sections : Main.getInstance().getEggDataManager().savedEggSections())
                                eggManager.resetStatsPlayer(name,sections);
                            player.sendMessage(Main.getInstance().getMessage("ResetedPlayerFoundEggs").replaceAll("%PLAYER%", name));
                        }else
                            player.sendMessage(Main.getInstance().getMessage("PlayerNotFound").replaceAll("%PLAYER%", name));
                    }else
                        player.sendMessage(usage());
                }else
                    player.sendMessage(usage());
            }else if(args.length == 1){
                if(args[0].equalsIgnoreCase("progress")){
                    new EggProgressMenu(Main.getPlayerMenuUtility(player)).open();
                }else if(args[0].equalsIgnoreCase("collection")){
                    new SelectionSelectListMenu(Main.getPlayerMenuUtility(player)).open();
                }else if(args[0].equalsIgnoreCase("leaderboard")){
                    new EggLeaderboardMenu(Main.getPlayerMenuUtility(player)).open();
                }else if(args[0].equalsIgnoreCase("hint")){
                    for(String sections : Main.getInstance().getEggDataManager().savedEggSections()) {
                        if (!eggManager.checkFoundAll(player,sections) && eggManager.getMaxEggs(sections) >= 1) {
                            if (!Main.getInstance().getCooldownManager().isAllowReward(player) && !player.hasPermission(Objects.requireNonNull(Main.getInstance().getConfig().getString("Permissions.IgnoreCooldownPermission")))) {
                                long current = System.currentTimeMillis();
                                long release = Main.getInstance().getCooldownManager().getCooldown(player);
                                long millis = release - current;
                                player.sendMessage(Main.getInstance().getCooldownManager().getRemainingTime(millis));
                                return true;
                            }
                            new HintInventoryCreator(player, Bukkit.createInventory(player, 54, "Eggs Hint"), true);
                        } else
                            player.sendMessage(Main.getInstance().getMessage("PlayerFoundAllEggs"));
                    }
                }else
                    player.sendMessage(usage());
            }else
                player.sendMessage(usage());
        }else if(sender instanceof ConsoleCommandSender){
            if(args.length == 2){
                if(args[0].equalsIgnoreCase("reset")){
                    if(args[1].equalsIgnoreCase("all")){
                        eggManager.resetStatsAll();
                        sender.sendMessage(Main.getInstance().getMessage("ResetedAllFoundEggs"));
                        return true;
                    }
                    String name = args[1];
                    if(eggManager.containsPlayer(name)){
                        for(String sections : Main.getInstance().getEggDataManager().savedEggSections())
                            eggManager.resetStatsPlayer(name,sections);
                        sender.sendMessage(Main.getInstance().getMessage("ResetedPlayerFoundEggs").replaceAll("%PLAYER%", name));
                    }else
                        sender.sendMessage(Main.getInstance().getMessage("PlayerNotFound").replaceAll("%PLAYER%", name));
                }else
                    sender.sendMessage(usage());
            }else
                sender.sendMessage(usage());
        }else
            sender.sendMessage(Main.getInstance().getMessage("OnlyPlayerCanUseThisCommandMessage"));
        return false;
    }

    private String usage(){
        return Main.getInstance().getMessage("AdvancedEggHuntCommandUsageMessage");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1){
            if(sender.hasPermission(permission)) {
                String[] tabs = {"placeEggs", "reload","reset", "list", "help", "settings","progress","show","commands","leaderboard","hint","collection"};
                ArrayList<String> complete = new ArrayList<>();
                Collections.addAll(complete, tabs);
                return complete;
            }else{
                String[] tabs = {"progress","leaderboard","hint","collection"};
                ArrayList<String> complete = new ArrayList<>();
                Collections.addAll(complete, tabs);
                return complete;
            }
        }
        if(args.length == 2){
            if(sender.hasPermission(permission)) {
                if(args[0].equalsIgnoreCase("reset")){
                    ArrayList<String> complete = new ArrayList<>();
                    for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()){
                        complete.add(Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getString("FoundEggs.Name"));
                    }
                    complete.add("all");
                    return complete;
                }
            }
        }
        if(args.length >= 3){
            ArrayList<String> complete = new ArrayList<>();
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
        player.sendMessage("§7Server Version: §6"+ getServer().getClass().getPackage().getName().split("\\.")[3]);
        player.sendMessage("§7Author: §6XMC-PLUGINS");
        player.sendMessage("");
        player.sendMessage("§2§lCommands");
        player.sendMessage("§6/advancedegghunt help §7-> §bShows this help messages and information.");
        player.sendMessage("§6/advancedegghunt reload §7-> §bReloads the config.");
        player.sendMessage("§6/advancedegghunt list §7-> §bLists all placed eggs.");
        player.sendMessage("§6/advancedegghunt placeEggs §7-> §bEnter Place-Mode the place or break eggs.");
        player.sendMessage("§6/advancedegghunt settings §7-> §bConfigure many settings of the plugin.");
        player.sendMessage("§6/advancedegghunt hint §7-> §bOpens the hint menu to find eggs easier.");
        player.sendMessage("§6/advancedegghunt progress §7-> §bView your progress of the eggs.");
        player.sendMessage("§6/advancedegghunt show §7-> §bSpawns an glowing armorstand at every egg.");
        player.sendMessage("§6/advancedegghunt commands §7-> §bChange commands or add more.");
        player.sendMessage("§6/advancedegghunt reset [player | all] §7-> §bResets the FoundEggs.");
        player.sendMessage("§5§l==========HELP==========");
        player.sendMessage("§3-----------------------------------------");
    }
}
