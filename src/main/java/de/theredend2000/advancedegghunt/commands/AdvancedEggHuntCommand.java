package de.theredend2000.advancedegghunt.commands;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.*;
import de.theredend2000.advancedegghunt.util.HexColor;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.enums.Permission;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;
import java.util.stream.Collectors;

public class AdvancedEggHuntCommand implements CommandExecutor, TabCompleter {
    private MessageManager messageManager;
    private Main plugin;
    private EggManager eggManager;

    public AdvancedEggHuntCommand() {
        messageManager = Main.getInstance().getMessageManager();
        plugin = Main.getInstance();
        eggManager = Main.getInstance().getEggManager();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return handleTabComplete(sender, args);
    }

    private List<String> handleTabComplete(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        switch (args.length) {
            case 1:
                String[] tabs = {"placeEggs", "reload", "reset", "list", "help", "settings", "progress", "show", "commands", "leaderboard", "hint", "collection", "eggImport"};
                for (String permission : tabs) {
                    if (plugin.getPermissionManager().checkPermission(sender, Permission.Command.getEnum(permission)))
                        completions.add(permission);
                }
                break;
            case 2:
                if (args[0].equalsIgnoreCase("reset") && plugin.getPermissionManager().checkPermission(sender, Permission.Command.reset)) {
                    completions.add("all");
                    for (UUID uuid : Main.getInstance().getEggDataManager().savedPlayers()) {
                        String playerData = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid).getString("FoundEggs.");
                        if (playerData == null) {
                            return completions;
                        }
                        for (String eggId : Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid).getConfigurationSection("FoundEggs.").getKeys(false)) {
                            String playerName = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuid).getString("FoundEggs." + eggId + ".Name");
                            if (playerName != null && !completions.contains(playerName))
                                completions.add(playerName);
                        }
                    }
                }
                break;
            case 3:
                if (args[0].equalsIgnoreCase("reset") && !args[1].equalsIgnoreCase("all") && plugin.getPermissionManager().checkPermission(sender, Permission.Command.reset)) {
                    completions.addAll(plugin.getEggDataManager().savedEggCollections());
                }
                break;
        }
        return filterArguments(completions, args);
    }

    private List<String> filterArguments(List<String> arguments, String[] args) {
        if (arguments == null || arguments.isEmpty())
            return Collections.emptyList();

        String lastArg = args[args.length - 1].toLowerCase();
        return arguments.stream()
                .filter(arg -> arg.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }

    private void sendHelp(Player player) {
        player.sendMessage("§3-----------------------------------------");
        player.sendMessage("§5§l==========HELP==========");
        player.sendMessage("");
        player.sendMessage("§2§lInformation");
        player.sendMessage("§7Name: §6" + Main.getInstance().getDescription().getName());
        player.sendMessage("§7Plugin Version: §6" + Main.getInstance().getDescription().getVersion());
        player.sendMessage("§7Server Version: §6" + Bukkit.getBukkitVersion().split("-")[0]);
        player.sendMessage("§7Author: §6XMC-PLUGINS");
        player.sendMessage("");
        player.sendMessage("§2§lCommands");
        player.sendMessage("§6/advancedegghunt collection §7-> §bSwitch and edit collections.");
        player.sendMessage("§6/advancedegghunt help §7-> §bShows this help messages and information.");
        player.sendMessage("§6/advancedegghunt reload §7-> §bReloads the config.");
        player.sendMessage("§6/advancedegghunt list §7-> §bLists all placed eggs.");
        player.sendMessage("§6/advancedegghunt placeEggs §7-> §bEnter Place-Mode to place or break eggs.");
        player.sendMessage("§6/advancedegghunt settings §7-> §bConfigure many settings of the plugin.");
        player.sendMessage("§6/advancedegghunt hint §7-> §bOpens the hint menu to find eggs easier.");
        player.sendMessage("§6/advancedegghunt progress §7-> §bView your progress of the eggs.");
        player.sendMessage("§6/advancedegghunt show §7-> §bSpawns an glowing armorstand at every egg.");
        player.sendMessage("§6/advancedegghunt commands §7-> §bChange commands or add more.");
        player.sendMessage("§6/advancedegghunt reset [player | all] §7-> §bResets the FoundEggs.");
        player.sendMessage("§6/advancedegghunt eggImport §7-> §bImport a new place egg.");
        player.sendMessage("§5§l==========HELP==========");
        player.sendMessage("§3-----------------------------------------");
    }

    private String usage() {
        return messageManager.getMessage(MessageKey.COMMAND_NOT_FOUND);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            handleConsoleCommand(sender, args);
            return true;
        }

        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage(usage());
            return true;
        }

        String subCommand = args[0].toLowerCase();
        switch (subCommand) {
            case "placeeggs":
                handlePlaceEggs(player);
                break;
            case "list":
                handleList(player);
                break;
            case "show":
                handleShow(player);
                break;
            case "reload":
                handleReload(player);
                break;
            case "help":
                handleHelp(player);
                break;
            case "settings":
                handleSettings(player);
                break;
            case "collection":
                handleCollection(player);
                break;
            case "progress":
                handleProgress(player);
                break;
            case "commands":
                handleCommands(player);
                break;
            case "leaderboard":
                handleLeaderboard(player);
                break;
            case "hint":
                handleHint(player);
                break;
            case "eggimport":
                handleEggImport(player);
                break;
            case "reset":
                handleReset(player, args);
                break;
            default:
                player.sendMessage(usage());
        }

        return true;
    }

    private void handlePlaceEggs(Player player) {
        if (!checkPermission(player, Permission.Command.placeEggs)) return;

        if (Main.getInstance().getPlaceEggsPlayers().contains(player)) {
            eggManager.finishEggPlacing(player);
            Main.getInstance().getPlaceEggsPlayers().remove(player);
            player.sendMessage(messageManager.getMessage(MessageKey.LEAVE_PLACEMODE));
        } else {
            eggManager.startEggPlacing(player);
            Main.getInstance().getPlaceEggsPlayers().add(player);
            player.sendMessage(messageManager.getMessage(MessageKey.ENTER_PLACEMODE));
            player.getInventory().setItem(4, new ItemBuilder(XMaterial.NETHER_STAR).setDisplayName("§6§lEggs Types §7(Right-Click)").setCustomId("egghunt.eggs").build());
            player.getInventory().setItem(8, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("YTkyZTMxZmZiNTljOTBhYjA4ZmM5ZGMxZmUyNjgwMjAzNWEzYTQ3YzQyZmVlNjM0MjNiY2RiNDI2MmVjYjliNiJ9fX0=")).setDisplayName("§2§lFinish setup §7(Drop)").setLore("§7Drop to finish the setup", "§7or type §e/egghunt placeEggs §7again.").setCustomId("egghunt.finish").build());
        }
    }

    private void handleList(Player player) {
        if (!checkPermission(player, Permission.Command.list)) return;
        new EggListMenu(Main.getPlayerMenuUtility(player)).open();
    }

    private void handleShow(Player player) {
        if (!checkPermission(player, Permission.Command.show)) return;
        eggManager.showAllEggs();
        player.sendMessage(messageManager.getMessage(MessageKey.EGG_SHOW_WARNING));
        player.sendMessage(messageManager.getMessage(MessageKey.EGG_VISIBLE).replaceAll("%TIME_VISIBLE%", String.valueOf(Main.getInstance().getPluginConfig().getArmorstandGlow())));
    }

    private void handleReload(Player player) {
        if (!checkPermission(player, Permission.Command.reload)) return;
        Main.getInstance().getPluginConfig().reloadConfig();
        messageManager.reloadMessages();
        eggManager.spawnEggParticle();
        Main.getInstance().getPlayerEggDataManager().reload();
        Main.getInstance().getEggDataManager().reload();
        Main.getInstance().getGlobalPresetDataManager().reload();
        Main.getInstance().getIndividualPresetDataManager().reload();
        Main.PREFIX = HexColor.color(ChatColor.translateAlternateColorCodes('&', plugin.getPluginConfig().getPrefix()));
        player.sendMessage(messageManager.getMessage(MessageKey.RELOAD_CONFIG));
    }

    private void handleHelp(Player player) {
        if (!checkPermission(player, Permission.Command.help)) return;
        sendHelp(player);
    }

    private void handleSettings(Player player) {
        if (!checkPermission(player, Permission.Command.settings)) return;
        new SettingsMenu(Main.getPlayerMenuUtility(player)).open();
    }

    private void handleCollection(Player player) {
        if (!checkPermission(player, Permission.Command.collection)) return;
        new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
    }

    private void handleProgress(Player player) {
        if (!checkPermission(player, Permission.Command.progress)) return;
        new EggProgressMenu(Main.getPlayerMenuUtility(player)).open();
    }

    private void handleCommands(Player player) {
        if (!checkPermission(player, Permission.Command.commands)) return;
        player.sendMessage("§cThis system is outdated. You can now change commands by SHIFT + RIGHT-CLICK an egg.");
    }

    private void handleLeaderboard(Player player) {
        if (!checkPermission(player, Permission.Command.leaderboard)) return;
        new LeaderboardMenu(Main.getPlayerMenuUtility(player)).open();
    }

    private void handleHint(Player player) {
        if (!checkPermission(player, Permission.Command.hint)) return;
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
                    return;
                }
                new HintMenu(Main.getPlayerMenuUtility(player)).open();
                return;
            } else {
                if (counter == max)
                    player.sendMessage(messageManager.getMessage(MessageKey.ALL_EGGS_FOUND));
            }
        }
    }

    private void handleEggImport(Player player) {
        if (!checkPermission(player, Permission.Command.eggImport)) return;
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!(item.getItemMeta() instanceof SkullMeta)) {
            player.sendMessage(messageManager.getMessage(MessageKey.EGGIMPORT_HAND));
            return;
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
            return;
        }

        fullTexture = fullTexture.replaceFirst(".+?mUv", "");
        for (String key : Main.getInstance().getPluginConfig().getPlaceEggIds()) {
            if (Objects.equals(Main.getInstance().getPluginConfig().getPlaceEggTexture(key), fullTexture)) {
                player.sendMessage(messageManager.getMessage(MessageKey.BLOCK_LISTED));
                return;
            }
        }

        String base64Texture = fullTexture;
        Main.getInstance().getPluginConfig().setPlaceEggPlayerHead(base64Texture);
        Main.getInstance().getPluginConfig().saveData();
        player.sendMessage(messageManager.getMessage(MessageKey.EGGIMPORT_SUCCESS));
    }

    private void handleReset(Player player, String[] args) {
        if (!checkPermission(player, Permission.Command.reset)) return;
        if (args.length < 2) {
            player.sendMessage(usage());
            return;
        }

        if (args[1].equalsIgnoreCase("all")) {
            eggManager.resetStatsAll();
            player.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_RESET));
            return;
        }

        String name = args[1];
        if (args.length == 3) {
            String collection = args[2];
            if (eggManager.containsPlayer(name)) {
                if (plugin.getEggDataManager().containsCollection(collection)) {
                    eggManager.resetStatsPlayer(name, collection);
                    player.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_PLAYER_RESET_COLLECTION).replaceAll("%PLAYER%", name).replaceAll("%COLLECTION%", collection));
                }
            } else {
                player.sendMessage(messageManager.getMessage(MessageKey.PLAYER_NOT_FOUND).replaceAll("%PLAYER%", name));
            }
        } else {
            if (eggManager.containsPlayer(name)) {
                for (String collections : Main.getInstance().getEggDataManager().savedEggCollections())
                    eggManager.resetStatsPlayer(name, collections);
                player.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_PLAYER_RESET).replaceAll("%PLAYER%", name));
            } else {
                player.sendMessage(messageManager.getMessage(MessageKey.PLAYER_NOT_FOUND).replaceAll("%PLAYER%", name));
            }
        }
    }

    private void handleConsoleCommand(CommandSender sender, String[] args) {
        if (args.length == 2 && args[0].equalsIgnoreCase("reset")) {
            if (args[1].equalsIgnoreCase("all")) {
                eggManager.resetStatsAll();
                sender.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_RESET));
                return;
            }

            String name = args[1];
            if (eggManager.containsPlayer(name)) {
                for (String collections : Main.getInstance().getEggDataManager().savedEggCollections())
                    eggManager.resetStatsPlayer(name, collections);
                sender.sendMessage(messageManager.getMessage(MessageKey.FOUNDEGGS_PLAYER_RESET).replaceAll("%PLAYER%", name));
            } else {
                sender.sendMessage(messageManager.getMessage(MessageKey.PLAYER_NOT_FOUND).replaceAll("%PLAYER%", name));
            }
        } else {
            sender.sendMessage(usage());
        }
    }

    private boolean checkPermission(Player player, Permission.Command permission) {
        if (!plugin.getPermissionManager().checkPermission(player, permission)) {
            player.sendMessage(messageManager.getMessage(MessageKey.PERMISSION_ERROR).replaceAll("%PERMISSION%", permission.toString()));
            return false;
        }
        return true;
    }
}
