package de.theredend2000.advancedegghunt.commands;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.*;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.enums.Permission;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBT;
import de.tr7zw.changeme.nbtapi.iface.ReadableNBTList;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
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
        if(sender instanceof Player){
            Player player = (Player) sender;
                if(args.length == 1){
                    if(args[0].equalsIgnoreCase("placeEggs")) {
                        if(plugin.getPermissionManager().checkPermission(player, Permission.Command.placeEggs)) {
                            if (Main.getInstance().getPlaceEggsPlayers().contains(player)) {
                                eggManager.finishEggPlacing(player);
                                Main.getInstance().getPlaceEggsPlayers().remove(player);
                                player.sendMessage(messageManager.getMessage(MessageKey.LEAVE_PLACEMODE));
                            } else {
                                eggManager.startEggPlacing(player);
                                Main.getInstance().getPlaceEggsPlayers().add(player);
                                player.sendMessage(messageManager.getMessage(MessageKey.ENTER_PLACEMODE));
                                player.getInventory().setItem(4, new ItemBuilder(XMaterial.NETHER_STAR).setDisplayname("§6§lEggs Types §7(Right-Click)").setLocalizedName("egghunt.eggs").build());
                                player.getInventory().setItem(8, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("YTkyZTMxZmZiNTljOTBhYjA4ZmM5ZGMxZmUyNjgwMjAzNWEzYTQ3YzQyZmVlNjM0MjNiY2RiNDI2MmVjYjliNiJ9fX0=")).setDisplayname("§2§lFinish setup §7(Drop)").setLore("§7Drop to finish the setup", "§7or type §e/egghunt placeEggs §7again.").setLocalizedName("egghunt.finish").build());
                            }
                        }else
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.placeEggs.toString()));
                    } else if(args[0].equalsIgnoreCase("list")) {
                        if (!plugin.getPermissionManager().checkPermission(player, Permission.Command.list)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.list.toString()));
                            return true;
                        }
                        new EggListMenu(Main.getPlayerMenuUtility(player)).open();
                    } else if(args[0].equalsIgnoreCase("show")) {
                        if (!plugin.getPermissionManager().checkPermission(player, Permission.Command.show)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.show.toString()));
                            return true;
                        }
                        eggManager.showAllEggs();
                        player.sendMessage(messageManager.getMessage(MessageKey.EGG_VISIBLE).replaceAll("%TIME_VISIBLE%", String.valueOf(Main.getInstance().getPluginConfig().getArmorstandGlow())));
                    } else if(args[0].equalsIgnoreCase("reload")) {
                        if (!plugin.getPermissionManager().checkPermission(player, Permission.Command.reload)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.reload.toString()));
                            return true;
                        }
                        Main.getInstance().reloadConfig();
                        Main.getInstance().checkCommandFeedback();
                        messageManager.reloadMessages();
                        eggManager.spawnEggParticle();
                        Main.getInstance().getPlayerEggDataManager().reload();
                        Main.getInstance().getEggDataManager().reload();
                        player.sendMessage(messageManager.getMessage(MessageKey.RELOAD_CONFIG));
                    } else if(args[0].equalsIgnoreCase("help")) {
                        if (!plugin.getPermissionManager().checkPermission(player, Permission.Command.help)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.help.toString()));
                            return true;
                        }
                        sendHelp(player);
                    } else if(args[0].equalsIgnoreCase("settings")) {
                        if (!plugin.getPermissionManager().checkPermission(player, Permission.Command.settings)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.settings.toString()));
                            return true;
                        }
                        new SettingsMenu(Main.getPlayerMenuUtility(player)).open();
                    } else if(args[0].equalsIgnoreCase("collection")) {
                        if (!plugin.getPermissionManager().checkPermission(player, Permission.Command.collection)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.collection.toString()));
                            return true;
                        }
                        new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                    } else if(args[0].equalsIgnoreCase("progress")) {
                        if (!plugin.getPermissionManager().checkPermission(player, Permission.Command.progress)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.progress.toString()));
                            return true;
                        }
                        new EggProgressMenu(Main.getPlayerMenuUtility(player)).open();
                    } else if(args[0].equalsIgnoreCase("commands")) {
                        if (!plugin.getPermissionManager().checkPermission(player, Permission.Command.commands)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.commands.toString()));
                            return true;
                        }
                        player.sendMessage("§cThis system is outdated. You can now change commands by SHIFT + RIGHT-CLICK an egg.");
                    } else if(args[0].equalsIgnoreCase("leaderboard")) {
                        if (!plugin.getPermissionManager().checkPermission(player, Permission.Command.leaderboard)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.leaderboard.toString()));
                            return true;
                        }
                        new LeaderboardMenu(Main.getPlayerMenuUtility(player)).open();
                    } else if(args[0].equalsIgnoreCase("hint")) {
                        if (!plugin.getPermissionManager().checkPermission(player, Permission.Command.hint)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.hint.toString()));
                            return true;
                        }
                        int counter = 0;
                        int max = Main.getInstance().getEggDataManager().savedEggCollections().size();
                        for (String collections : Main.getInstance().getEggDataManager().savedEggCollections()) {
                            counter++;
                            if (!eggManager.checkFoundAll(player, collections) && eggManager.getMaxEggs(collections) >= 1) {
                                if (!Main.getInstance().getCooldownManager().isAllowReward(player) && !plugin.getPermissionManager().checkPermission(player, Permission.IgnoreCooldown)) {
                                    long current = System.currentTimeMillis();
                                    long release = Main.getInstance().getCooldownManager().getCooldown(player);
                                    long millis = release - current;
                                    player.sendMessage(Main.getInstance().getCooldownManager().getRemainingTime(millis));
                                    return true;
                                }
                                new HintMenu(Main.getPlayerMenuUtility(player)).open(true);
                                return true;
                            } else {
                                if (counter == max)
                                    player.sendMessage(messageManager.getMessage(MessageKey.ALL_EGGS_FOUND));
                            }
                        }
                    } else if (args[0].equalsIgnoreCase("eggImport")) {
                        if (!plugin.getPermissionManager().checkPermission(player, Permission.Command.eggImport)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", Permission.Command.eggImport.toString()));
                            return true;
                        }
                        ItemStack item = player.getInventory().getItemInMainHand();
                        if (!(item.getItemMeta() instanceof  SkullMeta)) {
                            player.sendMessage(messageManager.getMessage(MessageKey.EGGIMPORT_HAND));
                            return true;
                        }

                        String fullTexture = NBT.get(item, nbt -> {
                            final ReadableNBT skullOwnerCompound = nbt.getCompound("SkullOwner");
                            if (skullOwnerCompound == null) return null;
                            ReadableNBT skullOwnerPropertiesCompound = skullOwnerCompound.getCompound("Properties");
                            if (skullOwnerPropertiesCompound == null) return null;
                            ReadableNBTList<ReadWriteNBT> skullOwnerPropertiesTexturesCompound = skullOwnerPropertiesCompound.getCompoundList("textures");
                            if (skullOwnerPropertiesTexturesCompound == null) return null;

                            return skullOwnerPropertiesTexturesCompound.get(0).getString("Value");
                        });
                        if (fullTexture == null) {
                            player.sendMessage(messageManager.getMessage(MessageKey.EGGIMPORT_FAILED_PROFILE));
                            return true;
                        }

                        String base64Texture = fullTexture.replaceFirst(".+?mUv", "");
                        Main.getInstance().getPluginConfig().setPlaceEggPlayerHead(base64Texture);
                        Main.getInstance().getPluginConfig().saveData();
                        player.sendMessage(messageManager.getMessage(MessageKey.EGGIMPORT_SUCCESS));
                    }
                    else
                        player.sendMessage(usage());
                } else if(args.length == 2){
                    if(args[0].equalsIgnoreCase("reset")) {
                        if(args[1].equalsIgnoreCase("all")) {
                            eggManager.resetStatsAll();
                            player.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_RESET));
                            return true;
                        }
                        String name = args[1];
                        if(eggManager.containsPlayer(name)) {
                            for(String collections : Main.getInstance().getEggDataManager().savedEggCollections())
                                eggManager.resetStatsPlayer(name, collections);
                            player.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_PLAYER_RESET).replaceAll("%PLAYER%", name));
                        }else
                            player.sendMessage(messageManager.getMessage(MessageKey.PLAYER_NOT_FOUND).replaceAll("%PLAYER%", name));
                    }else
                        player.sendMessage(usage());
                }else
                    player.sendMessage(usage());
        } else if(sender instanceof ConsoleCommandSender){
            if(args.length == 2){
                if(args[0].equalsIgnoreCase("reset")) {
                    if(args[1].equalsIgnoreCase("all")) {
                        eggManager.resetStatsAll();
                        sender.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_RESET));
                        return true;
                    }
                    String name = args[1];
                    if(eggManager.containsPlayer(name)) {
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
        ArrayList<String> complete = null;
        if(args.length == 1){
            complete = new ArrayList<>();
            String[] tabs = {"placeEggs", "reload", "reset", "list", "help", "settings", "progress", "show", "commands", "leaderboard", "hint", "collection", "eggImport"};
            for(String permissions : tabs){
                if(plugin.getPermissionManager().checkPermission(sender, Permission.Command.getEnum(permissions)))
                    complete.add(permissions);
            }
            return FilterArguments(complete, args);
        }
        if(args.length == 2){
            if(plugin.getPermissionManager().checkPermission(sender, Permission.Command.reset)) {
                if(args[0].equalsIgnoreCase("reset")) {
                    complete = new ArrayList<>();
                    for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()) {
                        if(Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getString("FoundEggs.") == null) continue;
                        for(String eggId : Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getConfigurationSection("FoundEggs.").getKeys(false)) {
                            if(!complete.contains(Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getString("FoundEggs." + eggId + ".Name")))
                                complete.add(Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getString("FoundEggs." + eggId + ".Name"));
                        }
                    }
                    complete.add("all");
                    return FilterArguments(complete, args);
                }
            }
        }
        if(args.length >= 3){
            complete = new ArrayList<>();
            return complete;
        }
        return null;
    }

    private List<String> FilterArguments(List<String> arguments, String[] args) {
        if (arguments == null)
            return Collections.emptyList();

        if (!arguments.isEmpty()) {
            int lastArgIndex = args.length-1;
            List<String> result = new ArrayList<>();
            for (String arg : arguments) {
                if (arg.toLowerCase().startsWith(args[lastArgIndex].toLowerCase()))
                    if (arg.toLowerCase().startsWith(args[lastArgIndex].toLowerCase()))
                        result.add(arg);
            }

            return result;
        }

        return arguments;
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
