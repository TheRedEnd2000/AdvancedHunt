package de.theredend2000.advancedegghunt.commands;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.InventoryManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggfoundrewardmenu.EggRewardMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.EggListMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggprogress.EggProgressMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.hintInventory.HintInventoryCreator;
import de.theredend2000.advancedegghunt.managers.inventorymanager.leaderboardmenu.EggLeaderboardMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.sectionselection.CollectionSelectListMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;

import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class AdvancedEggHuntCommand implements CommandExecutor, TabCompleter {
    private MessageManager messageManager;
    private Main plugin;

    public AdvancedEggHuntCommand(){
        messageManager = Main.getInstance().getMessageManager();
        plugin = Main.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        EggManager eggManager = Main.getInstance().getEggManager();
        InventoryManager inventoryManager = Main.getInstance().getInventoryManager();
        if(sender instanceof Player){
            Player player = (Player) sender;
                if(args.length == 1){
                    if(args[0].equalsIgnoreCase("placeEggs")){
                        if(plugin.getPermissionManager().checkCommandPermission(player,args[0])) {
                            if (Main.getInstance().getPlaceEggsPlayers().contains(player)) {
                                eggManager.finishEggPlacing(player);
                                Main.getInstance().getPlaceEggsPlayers().remove(player);
                                player.sendMessage(messageManager.getMessage(MessageKey.LEAVE_PLACEMODE));
                            } else {
                                eggManager.startEggPlacing(player);
                                Main.getInstance().getPlaceEggsPlayers().add(player);
                                player.sendMessage(messageManager.getMessage(MessageKey.ENTER_PLACEMODE));
                                player.getInventory().setItem(4, new ItemBuilder(XMaterial.NETHER_STAR).setDisplayname("§6§lEggs Types §7(Right-Click)").setLocalizedName("egghunt.eggs").build());
                                player.getInventory().setItem(8, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("YTkyZTMxZmZiNTljOTBhYjA4ZmM5ZGMxZmUyNjgwMjAzNWEzYTQ3YzQyZmVlNjM0MjNiY2RiNDI2MmVjYjliNiJ9fX0=")).setDisplayname("§2§lFinish setup §7(Drop)").setLore("§7Drop to finish the setup", "§7or type §e/egghunt placeEggs §7again.").setLocalizedName("egghunt.finish").setSoulbound(true).build());
                            }
                        }else
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%",plugin.getPermissionManager().getPermission(args[0])));
                    }else if(args[0].equalsIgnoreCase("list")){
                        if(plugin.getPermissionManager().checkCommandPermission(player,args[0]))
                            new EggListMenu(Main.getPlayerMenuUtility(player)).open();
                        else
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%",plugin.getPermissionManager().getPermission(args[0])));
                    }else if(args[0].equalsIgnoreCase("show")){
                        if(plugin.getPermissionManager().checkCommandPermission(player,args[0])) {
                            eggManager.showAllEggs();
                            player.sendMessage(messageManager.getMessage(MessageKey.EGG_VISIBLE).replaceAll("%TIME_VISIBLE%", String.valueOf(Main.getInstance().getPluginConfig().getArmorstandGlow())));
                        }else
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%",plugin.getPermissionManager().getPermission(args[0])));
                    }else if(args[0].equalsIgnoreCase("reload")){
                        if(plugin.getPermissionManager().checkCommandPermission(player,args[0])) {
                            Main.getInstance().reloadConfig();
                            Main.getInstance().checkCommandFeedback();
                            messageManager.reloadMessages();
                            eggManager.spawnEggParticle();
                            Main.getInstance().getPlayerEggDataManager().reload();
                            Main.getInstance().getEggDataManager().reload();
                            player.sendMessage(messageManager.getMessage(MessageKey.RELOAD_CONFIG));
                        }else
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%",plugin.getPermissionManager().getPermission(args[0])));
                    }else if(args[0].equalsIgnoreCase("help")){
                        if(plugin.getPermissionManager().checkCommandPermission(player,args[0]))
                            sendHelp(player);
                        else
                           player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%",plugin.getPermissionManager().getPermission(args[0])));
                    }else if(args[0].equalsIgnoreCase("settings")){
                        if(plugin.getPermissionManager().checkCommandPermission(player,args[0]))
                            inventoryManager.createEggsSettingsInventory(player);
                        else
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%",plugin.getPermissionManager().getPermission(args[0])));
                    }else if(args[0].equalsIgnoreCase("collection")){
                        if(plugin.getPermissionManager().checkCommandPermission(player,args[0]))
                            new CollectionSelectListMenu(Main.getPlayerMenuUtility(player)).open();
                        else
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%",plugin.getPermissionManager().getPermission(args[0])));
                    }else if(args[0].equalsIgnoreCase("progress")) {
                        if (plugin.getPermissionManager().checkCommandPermission(player, args[0]))
                            new EggProgressMenu(Main.getPlayerMenuUtility(player)).open();
                        else
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%",plugin.getPermissionManager().getPermission(args[0])));
                    }else if(args[0].equalsIgnoreCase("commands")){
                        if(plugin.getPermissionManager().checkCommandPermission(player,args[0]))
                            new EggRewardMenu(Main.getPlayerMenuUtility(player)).open();
                        else
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%",plugin.getPermissionManager().getPermission(args[0])));
                    }else if(args[0].equalsIgnoreCase("leaderboard")){
                        if(plugin.getPermissionManager().checkCommandPermission(player,args[0]))
                            new EggLeaderboardMenu(Main.getPlayerMenuUtility(player)).open();
                        else
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%",plugin.getPermissionManager().getPermission(args[0])));
                    }else if(args[0].equalsIgnoreCase("hint")) {
                        if (plugin.getPermissionManager().checkCommandPermission(player, args[0])) {
                            int counter = 0;
                            int max = Main.getInstance().getEggDataManager().savedEggCollections().size();
                            for (String collections : Main.getInstance().getEggDataManager().savedEggCollections()) {
                                counter++;
                                if (!eggManager.checkFoundAll(player, collections) && eggManager.getMaxEggs(collections) >= 1) {
                                    if (!Main.getInstance().getCooldownManager().isAllowReward(player) && !plugin.getPermissionManager().checkOtherPermission(player,"IgnoreCooldownPermission")) {
                                        long current = System.currentTimeMillis();
                                        long release = Main.getInstance().getCooldownManager().getCooldown(player);
                                        long millis = release - current;
                                        player.sendMessage(Main.getInstance().getCooldownManager().getRemainingTime(millis));
                                        return true;
                                    }
                                    new HintInventoryCreator(player, Bukkit.createInventory(player, 54, "Eggs Hint"), true);
                                } else {
                                    if (counter == max)
                                        player.sendMessage(messageManager.getMessage(MessageKey.ALL_EGGS_FOUND));
                                }
                            }
                        }else
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%",plugin.getPermissionManager().getPermission(args[0])));
                    } else if (args[0].equalsIgnoreCase("import")) {
                        ItemStack item = player.getInventory().getItemInMainHand();
                        PlayerProfile playerProfile = ((SkullMeta)item.getItemMeta()).getOwnerProfile();
                        if (playerProfile == null) {
                            player.sendMessage("Failed");
                            return true;
                        }
                        URL textureURL = playerProfile.getTextures().getSkin();
                        String toEncode = "{\"textures\":{\"SKIN\":{\"url\":\"" + textureURL.toString() + "\"}}}";
                        String base64Texture = Base64.getEncoder().encodeToString(toEncode.getBytes()).replaceFirst(".+?mUv", "");
                        Main.getInstance().getPluginConfig().setPlaceEggPlayerHead(base64Texture);
                        Main.getInstance().getPluginConfig().saveData();
                    }
                    else
                        player.sendMessage(usage());
                }else if(args.length == 2){
                    if(args[0].equalsIgnoreCase("reset")){
                        if(args[1].equalsIgnoreCase("all")){
                            eggManager.resetStatsAll();
                            player.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_RESET));
                            return true;
                        }
                        String name = args[1];
                        if(eggManager.containsPlayer(name)){
                            for(String collections : Main.getInstance().getEggDataManager().savedEggCollections())
                                eggManager.resetStatsPlayer(name, collections);
                            player.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_PLAYER_RESET).replaceAll("%PLAYER%", name));
                        }else
                            player.sendMessage(messageManager.getMessage(MessageKey.PLAYER_NOT_FOUND).replaceAll("%PLAYER%", name));
                    }else
                        player.sendMessage(usage());
                }else
                    player.sendMessage(usage());
        }else if(sender instanceof ConsoleCommandSender){
            if(args.length == 2){
                if(args[0].equalsIgnoreCase("reset")){
                    if(args[1].equalsIgnoreCase("all")){
                        eggManager.resetStatsAll();
                        sender.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_RESET));
                        return true;
                    }
                    String name = args[1];
                    if(eggManager.containsPlayer(name)){
                        for(String collections : Main.getInstance().getEggDataManager().savedEggCollections())
                            eggManager.resetStatsPlayer(name, collections);
                        sender.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_PLAYER_RESET).replaceAll("%PLAYER%", name));
                    }else
                        sender.sendMessage(messageManager.getMessage(MessageKey.PLAYER_NOT_FOUND).replaceAll("%PLAYER%", name));
                }else
                    sender.sendMessage(usage());
            }else
                sender.sendMessage(usage());
        }else
            sender.sendMessage(messageManager.getMessage(MessageKey.ONLY_PLAYER));
        return false;
    }

    private String usage(){
        return messageManager.getMessage(MessageKey.COMMAND_NOT_FOUND);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1){
            ArrayList<String> complete = new ArrayList<>();
            String[] tabs = {"placeEggs", "reload", "reset", "list", "help", "settings", "progress", "show", "commands", "leaderboard", "hint", "collection", "import"};
            for(String permissions : tabs){
                if(plugin.getPermissionManager().checkCommandPermission((Player) sender,permissions))
                    complete.add(permissions);
            }
            return complete;
        }
        if(args.length == 2){
            if(plugin.getPermissionManager().checkCommandPermission((Player) sender,"reset")) {
                if(args[0].equalsIgnoreCase("reset")){
                    ArrayList<String> complete = new ArrayList<>();
                    for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()){
                        if(Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getString("FoundEggs.") == null) continue;
                        for(String eggId : Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getConfigurationSection("FoundEggs.").getKeys(false)) {
                            if(!complete.contains(Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getString("FoundEggs." + eggId + ".Name")))
                                complete.add(Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getString("FoundEggs." + eggId + ".Name"));
                        }
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
        player.sendMessage("§7Name: §6" + Main.getInstance().getDescription().getName());
        player.sendMessage("§7Plugin Version: §6" + Main.getInstance().getDescription().getVersion());
        player.sendMessage("§7Api Version: §6" + Main.getInstance().getDescription().getAPIVersion());
        player.sendMessage("§7Server Version: §6" + getServer().getClass().getPackage().getName().split("\\.")[3]);
        player.sendMessage("§7Author: §6XMC-PLUGINS");
        player.sendMessage("");
        player.sendMessage("§2§lCommands");
        player.sendMessage("§6/advancedegghunt collection §7-> §bSwitch and edit collections.");
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
