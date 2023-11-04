package de.theredend2000.advancedegghunt.versions.managers.inventorymanager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class InventoryManager_1_20_R1 implements InventoryManager {


    public void createEggsSettingsInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(player,54,"Advanced Egg Settings");
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        inventory.setItem(10, new ItemBuilder(Material.GOLD_INGOT).setDisplayname("§3One egg found reward").setLore("§7If this function is activated","§7all commands entered in the config are executed.","",Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundOneEggRewards") ? "§a§l✔ Enabled" : "§c§l❌ Disabled","§eClick to toggle.").setLocalizedName("settings.foundoneegg").withGlow(Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundOneEggRewards")).build());
        inventory.setItem(11, new ItemBuilder(Material.EMERALD).setDisplayname("§3All eggs found reward").setLore("§7If this function is activated","§7all commands entered in the config are executed.","",Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundAllEggsReward") ? "§a§l✔ Enabled" : "§c§l❌ Disabled","§eClick to toggle.").setLocalizedName("settings.foundalleggs").withGlow(Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundAllEggsReward")).build());
        inventory.setItem(12, new ItemBuilder(Material.CLOCK).setDisplayname("§3Updater").setLore("§7If this function is activated","§7all operators will get an information","§7if a new plugin version is out.","",Main.getInstance().getConfig().getBoolean("Settings.Updater") ? "§a§l✔ Enabled" : "§c§l❌ Disabled","§eClick to toggle.").setLocalizedName("settings.updater").withGlow(Main.getInstance().getConfig().getBoolean("Settings.Updater")).build());
        inventory.setItem(13, new ItemBuilder(Material.COMMAND_BLOCK).setDisplayname("§3Command feedback").setLore("§7If this function is activated","§7no more commands are sent","§7to the operators listed in the console.","",Main.getInstance().getConfig().getBoolean("Settings.DisableCommandFeedback") ? "§a§l✔ Enabled" : "§c§l❌ Disabled","§eClick to toggle.").setLocalizedName("settings.commandfeedback").withGlow(Main.getInstance().getConfig().getBoolean("Settings.DisableCommandFeedback")).build());
        inventory.setItem(14, new ItemBuilder(Material.NOTE_BLOCK).setDisplayname("§3Sound volume").setLore("§7Change the volume of all sound of the plugin","§7If volume equal 0 no sound will be played.","","§7Currently: §6"+Main.getInstance().getConfig().getInt("Settings.SoundVolume"),"§eLEFT-CLICK to add one.","§eRIGHT-CLICK to remove one.").setLocalizedName("settings.soundvolume").withGlow(true).build());
        inventory.setItem(15, new ItemBuilder(Material.COMPASS).setDisplayname("§3Show coordinates when found").setLore("§7If this function is activated","§7players can see the coordinates","§7in the progress menu.","","§2Info: §7The coordinates are only visible if","§7the player has found the egg.","",Main.getInstance().getConfig().getBoolean("Settings.ShowCoordinatesWhenEggFoundInProgressInventory") ? "§a§l✔ Enabled" : "§c§l❌ Disabled","§eClick to toggle.").setLocalizedName("settings.showcoordinates").withGlow(Main.getInstance().getConfig().getBoolean("Settings.ShowCoordinatesWhenEggFoundInProgressInventory")).build());
        inventory.setItem(16, new ItemBuilder(Material.ARMOR_STAND).setDisplayname("§3Armorstand glow").setLore("§7Set how long the armorstands are","§7visible for all players.","","§7Currently: §6"+Main.getInstance().getConfig().getInt("Settings.ArmorstandGlow"),"§eLEFT-CLICK to add one.","§eRIGHT-CLICK to remove one.").setLocalizedName("settings.armorstandglow").withGlow(true).build());
        inventory.setItem(19, new ItemBuilder(Material.OAK_SIGN).setDisplayname("§3Nearby title radius").setLore("§7Change the radius of the egg nearby message for all players","§7If radius equal 0 no title will be displayed.","","§7Currently: §6"+Main.getInstance().getConfig().getInt("Settings.ShowEggsNearbyMessageRadius"),"§eLEFT-CLICK to add one.","§eRIGHT-CLICK to remove one.").setLocalizedName("settings.eggnearbyradius").withGlow(true).build());
        inventory.setItem(20, new ItemBuilder(Material.NAME_TAG).setDisplayname("§3Show plugin prefix").setLore("§7If enabled the plugin prefix","§7will show on each message.","§cThis will effect every message in the messages.yml file.","",Main.getInstance().getConfig().getBoolean("Settings.PluginPrefixEnabled") ? "§a§l✔ Enabled" : "§c§l❌ Disabled","§eClick to toggle.").setLocalizedName("settings.pluginprefix").withGlow(Main.getInstance().getConfig().getBoolean("Settings.PluginPrefixEnabled")).build());
        inventory.setItem(21, new ItemBuilder(Material.FIREWORK_ROCKET).setDisplayname("§3Firework").setLore("§7If this function is activated","§7a firework will spawn if an egg is found.","",Main.getInstance().getConfig().getBoolean("Settings.ShowFireworkAfterEggFound") ? "§a§l✔ Enabled" : "§c§l❌ Disabled","§eClick to toggle.").setLocalizedName("settings.firework").withGlow(Main.getInstance().getConfig().getBoolean("Settings.ShowFireworkAfterEggFound")).build());
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).setDisplayname("§4Close").setLocalizedName("settings.close").build());
        player.openInventory(inventory);
    }

    public void createCommandSettingsMenu(Player player, String key) {
        Inventory inventory = Bukkit.createInventory(player,45,"Command configuration");
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,10,16,17,18,26,27,28,34,35,37,38,39,40,41,42,43,44};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        String command = Main.getInstance().getConfig().getString("Rewards."+key+".command").replaceAll("§","&");
        boolean enabled = Main.getInstance().getConfig().getBoolean("Rewards."+key+".enabled");
        int type = Main.getInstance().getConfig().getInt("Rewards."+key+".type");
        inventory.setItem(11, new ItemBuilder(Material.COMMAND_BLOCK).setDisplayname("§3Change Command").setLore("§7You can change the command to any vanilla","§7command or commands of plugins for the console types.","","§5Currently:","§6§l"+command,"","§2Available placeholders:","§b- %PLAYER% --> Name of the player","§b- & --> For color codes (&6=gold)","§b- %EGGS_FOUND% --> How many eggs the player has found","§b- %EGGS_MAX% --> How many eggs are placed","§b- %PREFIX% --> The prefix of the plugin","","§eClick to change").setLocalizedName("command.command").build());
        inventory.setItem(15, new ItemBuilder(enabled ? Material.LIME_DYE : Material.RED_DYE).setDisplayname("§3Command Enabled").setLore("§7Change if the command will be executed","§7if the player founds one or all eggs.","","§5Currently:",(enabled ? "§a§l✔ Enabled" : "§c§l❌ Disabled"),"","§eClick to toggle.").setLocalizedName("command.enabled").build());
        inventory.setItem(22, new ItemBuilder(Main.getInstance().getMaterial(Main.getInstance().getConfig().getString("Settings.RewardInventoryMaterial"))).setDisplayname("§b§lCommand §7#"+key).setLore("","§9Information:","§7Command: §6"+command,"§7Command Enabled: "+(enabled ? "§atrue" : "§cfalse"),"§7Type: §6"+type,"","§a§lNote:","§2Type 0:","§7Type 0 means that this command will be","§7be executed if the player found §7§lone §7egg.","§2Type 1:","§7Type 1 means that this command will be","§7be executed if the player had found §7§lall §7egg.").setLocalizedName(key).build());
        inventory.setItem(29, new ItemBuilder(type == 0 ? Material.WATER_BUCKET : Material.LAVA_BUCKET).setDisplayname("§3Command Type").setLore("§7Change if the command will be executed","§7if the player founds one or all eggs.","","§5Currently:","§6§l"+type+" §7§l("+(type == 0 ? "One egg found" : "All eggs found")+")","§8More information at the main information. ("+Main.getInstance().getConfig().getString("Settings.RewardInventoryMaterial").toUpperCase()+")","","§eClick to toggle.").setLocalizedName("command.type").build());
        inventory.setItem(33, new ItemBuilder(Material.RED_STAINED_GLASS).setDisplayname("§4Delete Command").setLore("","§4§l! WARNING !","§c§lYOU CAN NOT UNDO THIS ACTION","","§cAre you sure to delete command #"+key+"?","","§eClick to confirm.").setLocalizedName("command.delete").build());
        inventory.setItem(36, new ItemBuilder(Material.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").setLocalizedName("command.back").build());
        inventory.setItem(40, new ItemBuilder(Material.BARRIER).setDisplayname("§4Close").setLocalizedName("command.close").build());
        player.openInventory(inventory);
    }
}
