package de.theredend2000.advancedhunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.PlayerMenuUtility;
import de.theredend2000.advancedhunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

public class SettingsMenu extends InventoryMenu {
    private MessageManager messageManager;

    public SettingsMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Advanced " + StringUtils.capitalize(Main.getInstance().getPluginConfig().getPluginNameSingular()) + " Settings", (short) 54, XMaterial.RED_STAINED_GLASS_PANE);
        messageManager = Main.getInstance().getMessageManager();
    }

    public void open() {
        super.addMenuBorder();
        addMenuBorderButtons();
        menuContent();

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    private void addMenuBorderButtons() {
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .setCustomId("settings.close")
                .build();
    }

    private void menuContent() {
        getInventory().setItem(12, new ItemBuilder(XMaterial.CLOCK)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_UPDATER))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_UPDATER,
                        "%STATUS%", Main.getInstance().getPluginConfig().getUpdater() ? "§a§l✔ Enabled" : "§c§l❌ Disabled"))
                .setCustomId("settings.updater")
                .withGlow(Main.getInstance().getPluginConfig().getUpdater())
                .build());

        getInventory().setItem(13, new ItemBuilder(XMaterial.COMMAND_BLOCK)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_COMMAND_FEEDBACK))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_COMMAND_FEEDBACK,
                        "%STATUS%", "§c§l❌ Discontinued"))
                .setCustomId("settings.commandfeedback")
                .build());

        getInventory().setItem(14, new ItemBuilder(XMaterial.NOTE_BLOCK)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_SOUND_VOLUME))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_SOUND_VOLUME,
                        "%VOLUME%", "§6" + Main.getInstance().getPluginConfig().getSoundVolume()))
                .setCustomId("settings.soundvolume")
                .withGlow(true)
                .build());

        getInventory().setItem(15, new ItemBuilder(XMaterial.COMPASS)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_SHOW_COORDINATES))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_SHOW_COORDINATES,
                        "%STATUS%", Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory() ? "§a§l✔ Enabled" : "§c§l❌ Disabled"))
                .setCustomId("settings.showcoordinates")
                .withGlow(Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory())
                .build());

        getInventory().setItem(16, new ItemBuilder(XMaterial.ARMOR_STAND)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_ARMORSTAND_GLOW))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_ARMORSTAND_GLOW,
                        "%GLOW_DURATION%", "§6" + Main.getInstance().getPluginConfig().getArmorstandGlow()))
                .setCustomId("settings.armorstandglow")
                .withGlow(true)
                .build());

        getInventory().setItem(19, new ItemBuilder(XMaterial.OAK_SIGN)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_EGG_NEARBY_RADIUS))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_EGG_NEARBY_RADIUS,
                        "%RADIUS%", "§6" + Main.getInstance().getPluginConfig().getShowEggsNearbyMessageRadius()))
                .setCustomId("settings.eggnearbyradius")
                .withGlow(true)
                .build());

        getInventory().setItem(20, new ItemBuilder(XMaterial.NAME_TAG)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_PLUGIN_PREFIX))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_PLUGIN_PREFIX,
                        "%STATUS%", Main.getInstance().getPluginConfig().getPluginPrefixEnabled() ? "§a§l✔ Enabled" : "§c§l❌ Disabled"))
                .setCustomId("settings.pluginprefix")
                .withGlow(Main.getInstance().getPluginConfig().getPluginPrefixEnabled())
                .build());

        getInventory().setItem(21, new ItemBuilder(XMaterial.FIREWORK_ROCKET)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_FIREWORK))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_FIREWORK,
                        "%STATUS%", Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound() ? "§a§l✔ Enabled" : "§c§l❌ Disabled"))
                .setCustomId("settings.firework")
                .withGlow(Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound())
                .build());

        getInventory().setItem(22, new ItemBuilder(XMaterial.CLOCK)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_HINT_COOLDOWN))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_HINT_COOLDOWN,
                        "%STATUS%", Main.getInstance().getPluginConfig().getHintApplyCooldownOnFail() ? "§a§l✔ Enabled" : "§c§l❌ Disabled"))
                .setCustomId("settings.hintcooldown")
                .withGlow(Main.getInstance().getPluginConfig().getHintApplyCooldownOnFail())
                .build());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (!ItemHelper.hasItemId(event.getCurrentItem())) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        SoundManager soundManager = Main.getInstance().getSoundManager();

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "settings.close":
                player.closeInventory();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.updater":
                Main.getInstance().getPluginConfig().setUpdater(!Main.getInstance().getPluginConfig().getUpdater());
                Main.getInstance().getPluginConfig().saveData();
                menuContent();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.commandfeedback":
                player.sendMessage(messageManager.getMessage(MessageKey.SETTING_COMMANDFEEDBACK));
                break;
            case "settings.soundvolume":
                int currentVolume = Main.getInstance().getPluginConfig().getSoundVolume();
                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                    if (currentVolume == 15) {
                        player.sendMessage(messageManager.getMessage(MessageKey.SOUND_VOLUME));
                        return;
                    }
                    Main.getInstance().getPluginConfig().setSoundVolume(currentVolume + 1);

                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    if (currentVolume == 0) {
                        player.sendMessage(messageManager.getMessage(MessageKey.SOUND_VOLUME));
                        return;
                    }
                    Main.getInstance().getPluginConfig().setSoundVolume(currentVolume - 1);
                }
                Main.getInstance().getPluginConfig().saveData();
                menuContent();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.armorstandglow":
                int currentTime = Main.getInstance().getPluginConfig().getArmorstandGlow();
                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                    if (currentTime == 120) {
                        player.sendMessage(messageManager.getMessage(MessageKey.ARMORSTAND_GLOW));
                        return;
                    }
                    Main.getInstance().getPluginConfig().setArmorstandGlow(currentTime + 1);

                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    if (currentTime == 0) {
                        player.sendMessage(messageManager.getMessage(MessageKey.ARMORSTAND_GLOW));
                        return;
                    }
                    Main.getInstance().getPluginConfig().setArmorstandGlow(currentTime - 1);
                }
                Main.getInstance().getPluginConfig().saveData();
                menuContent();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.showcoordinates":
                Main.getInstance().getPluginConfig().setShowCoordinatesWhenEggFoundInProgressInventory(!Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory());
                Main.getInstance().getPluginConfig().saveData();
                menuContent();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.eggnearbyradius":
                int currentRadius = Main.getInstance().getPluginConfig().getShowEggsNearbyMessageRadius();
                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                    if (currentRadius == 50) {
                        player.sendMessage(messageManager.getMessage(MessageKey.EGG_RADIUS));
                        return;
                    }
                    Main.getInstance().getPluginConfig().setShowEggsNearbyMessageRadius(currentRadius + 1);

                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    if (currentRadius == 0) {
                        player.sendMessage(messageManager.getMessage(MessageKey.EGG_RADIUS));
                        return;
                    }
                    Main.getInstance().getPluginConfig().setShowEggsNearbyMessageRadius(currentRadius - 1);
                }
                Main.getInstance().getPluginConfig().saveData();
                menuContent();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.pluginprefix":
                Main.getInstance().getPluginConfig().setPluginPrefixEnabled(!Main.getInstance().getPluginConfig().getPluginPrefixEnabled());
                Main.getInstance().getPluginConfig().saveData();
                menuContent();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.firework":
                Main.getInstance().getPluginConfig().setShowFireworkAfterEggFound(!Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound());
                Main.getInstance().getPluginConfig().saveData();
                menuContent();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.hintcooldown":
                Main.getInstance().getPluginConfig().setHintApplyCooldownOnFails(!Main.getInstance().getPluginConfig().getHintApplyCooldownOnFail());
                Main.getInstance().getPluginConfig().saveData();
                menuContent();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
        }
    }
}
