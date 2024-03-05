package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.enums.DeletionTypes;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Random;

public class InventoryManager {

    public void createEggsSettingsInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(player, 54, "Advanced Egg Settings");
        int[] glass = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 46, 47, 48, 50, 51, 52, 53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        inventory.setItem(10, new ItemBuilder(XMaterial.GOLD_INGOT).setDisplayname("§3One egg found reward").setLore("§7If this function is activated", "§7all commands entered in the config are executed.", "", Main.getInstance().getPluginConfig().getPlayerFoundOneEggRewards() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.foundoneegg").withGlow(Main.getInstance().getPluginConfig().getPlayerFoundOneEggRewards()).build());
        inventory.setItem(11, new ItemBuilder(XMaterial.EMERALD).setDisplayname("§3All eggs found reward").setLore("§7If this function is activated", "§7all commands entered in the config are executed.", "", Main.getInstance().getPluginConfig().getPlayerFoundAllEggsReward() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.foundalleggs").withGlow(Main.getInstance().getPluginConfig().getPlayerFoundAllEggsReward()).build());
        inventory.setItem(12, new ItemBuilder(XMaterial.CLOCK).setDisplayname("§3Updater").setLore("§7If this function is activated", "§7all operators will get an information", "§7if a new plugin version is out.", "", Main.getInstance().getPluginConfig().getUpdater() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.updater").withGlow(Main.getInstance().getPluginConfig().getUpdater()).build());
        inventory.setItem(13, new ItemBuilder(XMaterial.COMMAND_BLOCK).setDisplayname("§3Command feedback").setLore("§7If this function is activated", "§7no more commands are sent", "§7to the operators listed in the console.", "", Main.getInstance().getPluginConfig().getDisableCommandFeedback() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.commandfeedback").withGlow(Main.getInstance().getPluginConfig().getDisableCommandFeedback()).build());
        inventory.setItem(14, new ItemBuilder(XMaterial.NOTE_BLOCK).setDisplayname("§3Sound volume").setLore("§7Change the volume of all sound of the plugin", "§7If volume equal 0 no sound will be played.", "", "§7Currently: §6" + Main.getInstance().getPluginConfig().getSoundVolume(), "§eLEFT-CLICK to add one.", "§eRIGHT-CLICK to remove one.").setLocalizedName("settings.soundvolume").withGlow(true).build());
        inventory.setItem(15, new ItemBuilder(XMaterial.COMPASS).setDisplayname("§3Show coordinates when found").setLore("§7If this function is activated", "§7players can see the coordinates", "§7in the progress menu.", "", "§2Info: §7The coordinates are only visible if", "§7the player has found the egg.", "", Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.showcoordinates").withGlow(Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory()).build());
        inventory.setItem(16, new ItemBuilder(XMaterial.ARMOR_STAND).setDisplayname("§3Armorstand glow").setLore("§7Set how long the armorstands are", "§7visible for all players.", "", "§7Currently: §6" + Main.getInstance().getPluginConfig().getArmorstandGlow(), "§eLEFT-CLICK to add one.", "§eRIGHT-CLICK to remove one.").setLocalizedName("settings.armorstandglow").withGlow(true).build());
        inventory.setItem(19, new ItemBuilder(XMaterial.OAK_SIGN).setDisplayname("§3Nearby title radius").setLore("§7Change the radius of the egg nearby message for all players", "§7If radius equal 0 no title will be displayed.", "", "§7Currently: §6" + Main.getInstance().getPluginConfig().getShowEggsNearbyMessageRadius(), "§eLEFT-CLICK to add one.", "§eRIGHT-CLICK to remove one.").setLocalizedName("settings.eggnearbyradius").withGlow(true).build());
        inventory.setItem(20, new ItemBuilder(XMaterial.NAME_TAG).setDisplayname("§3Show plugin prefix").setLore("§7If enabled the plugin prefix", "§7will show on each message.", "§cThis will effect every message in the messages.yml file.", "", Main.getInstance().getPluginConfig().getPluginPrefixEnabled() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.pluginprefix").withGlow(Main.getInstance().getPluginConfig().getPluginPrefixEnabled()).build());
        inventory.setItem(21, new ItemBuilder(XMaterial.FIREWORK_ROCKET).setDisplayname("§3Firework").setLore("§7If this function is activated", "§7a firework will spawn if an egg is found.", "", Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.firework").withGlow(Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound()).build());
        inventory.setItem(49, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").setLocalizedName("settings.close").build());
        player.openInventory(inventory);
    }

    public void createCommandSettingsMenu(Player player, String key) {
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(player.getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        Inventory inventory = Bukkit.createInventory(player, 45, "Command configuration");
        int[] glass = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 16, 17, 18, 26, 27, 28, 34, 35, 37, 38, 39, 40, 41, 42, 43, 44};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        String command = placedEggs.getString("Rewards." + key + ".command").replaceAll("§", "&");
        boolean enabled = placedEggs.getBoolean("Rewards." + key + ".enabled");
        int type = placedEggs.getInt("Rewards." + key + ".type");
        inventory.setItem(11, new ItemBuilder(XMaterial.COMMAND_BLOCK).setDisplayname("§3Change Command").setLore("§7You can change the command to any vanilla", "§7command or commands of plugins for the console types.", "", "§5Currently:", "§6§l" + command, "", "§2Available placeholders:", "§b- %PLAYER% --> Name of the player", "§b- & --> For color codes (&6=gold)", "§b- %EGGS_FOUND% --> How many eggs the player has found", "§b- %EGGS_MAX% --> How many eggs are placed", "§b- %PREFIX% --> The prefix of the plugin", "", "§eClick to change").setLocalizedName("command.command").build());
        inventory.setItem(15, new ItemBuilder(enabled ? XMaterial.LIME_DYE : XMaterial.RED_DYE).setDisplayname("§3Command Enabled").setLore("§7Change if the command will be executed", "§7if the player founds one or all eggs.", "", "§5Currently:", (enabled ? "§a§l✔ Enabled" : "§c§l❌ Disabled"), "", "§eClick to toggle.").setLocalizedName("command.enabled").build());
        inventory.setItem(22, new ItemBuilder(Main.getInstance().getPluginConfig().getRewardInventoryMaterial()).setDisplayname("§b§lCommand §7#" + key).setLore("", "§9Information:", "§7Command: §6" + command, "§7Command Enabled: " + (enabled ? "§atrue" : "§cfalse"), "§7Type: §6" + type, "", "§a§lNote:", "§2Type 0:", "§7Type 0 means that this command will be", "§7be executed if the player found §7§lone §7egg.", "§2Type 1:", "§7Type 1 means that this command will be", "§7be executed if the player had found §7§lall §7egg.").setLocalizedName(key).build());
        inventory.setItem(29, new ItemBuilder(type == 0 ? XMaterial.WATER_BUCKET : XMaterial.LAVA_BUCKET).setDisplayname("§3Command Type").setLore("§7Change if the command will be executed", "§7if the player founds one or all eggs.", "", "§5Currently:", "§6§l" + type + " §7§l(" + (type == 0 ? "One egg found" : "All eggs found") + ")", "§8More information at the main information. (" + Main.getInstance().getPluginConfig().getRewardInventoryMaterial().toString().toUpperCase() + ")", "", "§eClick to toggle.").setLocalizedName("command.type").build());
        inventory.setItem(33, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4Delete Command").setLore("", "§4§l! WARNING !", "§c§lYOU CAN NOT UNDO THIS ACTION", "", "§cAre you sure to delete command #" + key + "?", "", "§eClick to confirm.").setLocalizedName("command.delete").build());
        inventory.setItem(36, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").setLocalizedName("command.back").build());
        inventory.setItem(40, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").setLocalizedName("command.close").build());
        player.openInventory(inventory);
    }

    public void createAddCollectionMenu(Player player){
        Inventory inventory = Bukkit.createInventory(player, 45, "Collection creator");
        int[] glass = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        String name = playerConfig.getString("CollectionEdit.Name");
        boolean enabled = playerConfig.getBoolean("CollectionEdit.enabled");
        inventory.setItem(20, new ItemBuilder(XMaterial.PAPER).setDisplayname("§3Name").setLore("§7Currently: " + (name != null ? name : "§cnone"), "", "§eClick to change.").build());
        inventory.setItem(22, new ItemBuilder(enabled ? XMaterial.LIME_DYE : XMaterial.RED_DYE).setDisplayname("§3Status").setLore("§7Currently: " + (enabled ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle.").build());
        inventory.setItem(24, new ItemBuilder(XMaterial.COMPARATOR).setDisplayname("§3Requirements").setLore("§cYou can change the requirements", "§cafter creating the new collection.", "", "§7All Requirements will be active", "§7on creating a new collection.").build());
        inventory.setItem(40, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build());
        inventory.setItem(44, new ItemBuilder(XMaterial.EMERALD_BLOCK).setDisplayname("§2Create").setLore("", "§eClick to create.").build());
        inventory.setItem(36, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build());
        player.openInventory(inventory);
    }

    public void createEditCollectionMenu(Player player, String collection){
        Inventory inventory = Bukkit.createInventory(player, 45, "Collection editor");
        int[] glass = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        boolean enabled = placedEggs.getBoolean("Enabled");
        inventory.setItem(4, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7))).setDisplayname("§6" + collection).build());
        //inventory.setItem(20, new ItemBuilder(XMaterial.PAPER).setDisplayname("§3Rename").setLore("§7Currently: " + name, "", "§eClick to change.").build());
        inventory.setItem(20, new ItemBuilder(enabled ? XMaterial.LIME_DYE : XMaterial.RED_DYE).setDisplayname("§3Status").setLore("§7Currently: " + (enabled ? "§aEnabled" : "§cDisabled"), "", "§eClick to toggle.").build());
        inventory.setItem(24, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4Delete").setLore("§8Check if your deletion type is correct. (WOODEN_AXE)", "", "§4§lYOU CAN NOT UNDO THIS", "", "§eClick to delete.").build());
        inventory.setItem(13, new ItemBuilder(XMaterial.COMPARATOR).setDisplayname("§3Requirements").setDefaultLore(Main.getInstance().getRequirementsManager().getListRequirementsLore(collection)).build());
        inventory.setItem(31, new ItemBuilder(XMaterial.REPEATER).setDisplayname("§3Reset §e§l(BETA)").setLore("", "§cResets after:", "§6  " + Main.getInstance().getRequirementsManager().getConvertedTime(collection), "", "§4If the time get changed, the value", "§4of the current cooldown of the", "§4player will not change!", "", "§eClick to change.").build());
        inventory.setItem(40, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build());
        DeletionTypes deletionTypes = Main.getInstance().getPlayerEggDataManager().getDeletionType(player.getUniqueId());
        inventory.setItem(44, new ItemBuilder(XMaterial.WOODEN_AXE).setDisplayname("§3Deletion Types").setLore("§8Every player can configure that himself.", "§7Change what happens after the deletion", "§7of an collection.", "", (deletionTypes == DeletionTypes.Noting ? "§b➤ " : "§7") + "Nothing", "§8All blocks that were eggs will stay. (includes player heads)", (deletionTypes == DeletionTypes.Player_Heads ? "§b➤ " : "§7") + "Player Heads", "§8All blocks that are player heads will be removed.", (deletionTypes == DeletionTypes.Everything ? "§b➤ " : "§7") + "Everything", "§8All blocks and will be set to air. (includes player heads)", "", "§eClick to change.").build());
        inventory.setItem(36, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build());
        player.openInventory(inventory);
    }
}
