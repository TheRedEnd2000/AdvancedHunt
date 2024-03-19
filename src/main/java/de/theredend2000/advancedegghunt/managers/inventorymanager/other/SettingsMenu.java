package de.theredend2000.advancedegghunt.managers.inventorymanager.other;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.InventoryMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.ItemBuilder;

public class SettingsMenu extends InventoryMenu {

    public SettingsMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Settings", (short) 10);
    }

    private void addMenuBorderButtons()
    {
        inventoryContent[10] = new ItemBuilder(XMaterial.GOLD_INGOT).setDisplayname("§3One egg found reward").setLore("§7If this function is activated", "§7all commands entered in the config are executed.", "", Main.getInstance().getPluginConfig().getPlayerFoundOneEggRewards() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.foundoneegg").withGlow(Main.getInstance().getPluginConfig().getPlayerFoundOneEggRewards()).build();
        inventoryContent[11] = new ItemBuilder(XMaterial.EMERALD).setDisplayname("§3All eggs found reward").setLore("§7If this function is activated", "§7all commands entered in the config are executed.", "", Main.getInstance().getPluginConfig().getPlayerFoundAllEggsReward() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.foundalleggs").withGlow(Main.getInstance().getPluginConfig().getPlayerFoundAllEggsReward()).build();
        inventoryContent[12] = new ItemBuilder(XMaterial.CLOCK).setDisplayname("§3Updater").setLore("§7If this function is activated", "§7all operators will get an information", "§7if a new plugin version is out.", "", Main.getInstance().getPluginConfig().getUpdater() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.updater").withGlow(Main.getInstance().getPluginConfig().getUpdater()).build();
        inventoryContent[13] = new ItemBuilder(XMaterial.COMMAND_BLOCK).setDisplayname("§3Command feedback").setLore("§7If this function is activated", "§7no more commands are sent", "§7to the operators listed in the console.", "", Main.getInstance().getPluginConfig().getDisableCommandFeedback() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.commandfeedback").withGlow(Main.getInstance().getPluginConfig().getDisableCommandFeedback()).build();
        inventoryContent[14] = new ItemBuilder(XMaterial.NOTE_BLOCK).setDisplayname("§3Sound volume").setLore("§7Change the volume of all sound of the plugin", "§7If volume equal 0 no sound will be played.", "", "§7Currently: §6" + Main.getInstance().getPluginConfig().getSoundVolume(), "§eLEFT-CLICK to add one.", "§eRIGHT-CLICK to remove one.").setLocalizedName("settings.soundvolume").withGlow(true).build();
        inventoryContent[15] = new ItemBuilder(XMaterial.COMPASS).setDisplayname("§3Show coordinates when found").setLore("§7If this function is activated", "§7players can see the coordinates", "§7in the progress menu.", "", "§2Info: §7The coordinates are only visible if", "§7the player has found the egg.", "", Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.showcoordinates").withGlow(Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory()).build();
        inventoryContent[16] = new ItemBuilder(XMaterial.ARMOR_STAND).setDisplayname("§3Armorstand glow").setLore("§7Set how long the armorstands are", "§7visible for all players.", "", "§7Currently: §6" + Main.getInstance().getPluginConfig().getArmorstandGlow(), "§eLEFT-CLICK to add one.", "§eRIGHT-CLICK to remove one.").setLocalizedName("settings.armorstandglow").withGlow(true).build();
        inventoryContent[19] = new ItemBuilder(XMaterial.OAK_SIGN).setDisplayname("§3Nearby title radius").setLore("§7Change the radius of the egg nearby message for all players", "§7If radius equal 0 no title will be displayed.", "", "§7Currently: §6" + Main.getInstance().getPluginConfig().getShowEggsNearbyMessageRadius(), "§eLEFT-CLICK to add one.", "§eRIGHT-CLICK to remove one.").setLocalizedName("settings.eggnearbyradius").withGlow(true).build();
        inventoryContent[20] = new ItemBuilder(XMaterial.NAME_TAG).setDisplayname("§3Show plugin prefix").setLore("§7If enabled the plugin prefix", "§7will show on each message.", "§cThis will effect every message in the messages.yml file.", "", Main.getInstance().getPluginConfig().getPluginPrefixEnabled() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.pluginprefix").withGlow(Main.getInstance().getPluginConfig().getPluginPrefixEnabled()).build();
        inventoryContent[21] = new ItemBuilder(XMaterial.FIREWORK_ROCKET).setDisplayname("§3Firework").setLore("§7If this function is activated", "§7a firework will spawn if an egg is found.", "", Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound() ? "§a§l✔ Enabled" : "§c§l❌ Disabled", "§eClick to toggle.").setLocalizedName("settings.firework").withGlow(Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound()).build();
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").setLocalizedName("settings.close").build();
    }

    public void setMenuItems() {
        this.addMenuBorder();
        addMenuBorderButtons();
    }

    @Override
    public String getMenuName() {
        return null;
    }

    @Override
    public int getSlots() {
        return this.slots;
    }
}
