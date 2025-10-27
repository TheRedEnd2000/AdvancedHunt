package de.theredend2000.advancedhunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.IInventoryMenuOpen;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.PlayerMenuUtility;
import de.theredend2000.advancedhunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;

public class SettingsMenu extends PaginatedInventoryMenu implements IInventoryMenuOpen {

    private MessageManager messageManager;
    private ArrayList<ItemStack> keys = new ArrayList<>();

    public SettingsMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Advanced " + StringUtils.capitalize(Main.getInstance().getPluginConfig().getPluginNameSingular()) + " Settings", (short) 54, XMaterial.RED_STAINED_GLASS_PANE);
        messageManager = Main.getInstance().getMessageManager();
    }

    public void open() {
        fillMenuItems();
        super.addMenuBorder();
        addMenuBorderButtons();
        setMenuItems();

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    private void fillMenuItems(){
        keys.clear();
        keys.add(new ItemBuilder(XMaterial.CLOCK)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_UPDATER))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_UPDATER,
                        "%STATUS%", Main.getInstance().getPluginConfig().getUpdater() ? "§a§l✔ Enabled" : "§c§l❌ Disabled"))
                .setCustomId("settings.updater")
                .withGlow(Main.getInstance().getPluginConfig().getUpdater())
                .build());

        keys.add(new ItemBuilder(XMaterial.COMMAND_BLOCK)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_COMMAND_FEEDBACK))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_COMMAND_FEEDBACK,
                        "%STATUS%", "§c§l❌ Discontinued"))
                .setCustomId("settings.commandfeedback")
                .build());

        keys.add(new ItemBuilder(XMaterial.NOTE_BLOCK)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_SOUND_VOLUME))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_SOUND_VOLUME,
                        "%VOLUME%", "§6" + Main.getInstance().getPluginConfig().getSoundVolume()))
                .setCustomId("settings.soundvolume")
                .withGlow(true)
                .build());

        keys.add(new ItemBuilder(XMaterial.COMPASS)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_SHOW_COORDINATES))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_SHOW_COORDINATES,
                        "%STATUS%", Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory() ? "§a§l✔ Enabled" : "§c§l❌ Disabled"))
                .setCustomId("settings.showcoordinates")
                .withGlow(Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory())
                .build());

        keys.add(new ItemBuilder(XMaterial.ARMOR_STAND)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_ARMORSTAND_GLOW))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_ARMORSTAND_GLOW,
                        "%GLOW_DURATION%", "§6" + Main.getInstance().getPluginConfig().getArmorstandGlow()))
                .setCustomId("settings.armorstandglow")
                .withGlow(true)
                .build());

        keys.add(new ItemBuilder(XMaterial.OAK_SIGN)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_EGG_NEARBY_RADIUS))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_EGG_NEARBY_RADIUS,
                        "%RADIUS%", "§6" + Main.getInstance().getPluginConfig().getShowEggsNearbyMessageRadius()))
                .setCustomId("settings.eggnearbyradius")
                .withGlow(true)
                .build());

        keys.add(new ItemBuilder(XMaterial.NAME_TAG)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_PLUGIN_PREFIX))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_PLUGIN_PREFIX,
                        "%STATUS%", Main.getInstance().getPluginConfig().getPluginPrefixEnabled() ? "§a§l✔ Enabled" : "§c§l❌ Disabled"))
                .setCustomId("settings.pluginprefix")
                .withGlow(Main.getInstance().getPluginConfig().getPluginPrefixEnabled())
                .build());

        keys.add(new ItemBuilder(XMaterial.FIREWORK_ROCKET)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_FIREWORK))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_FIREWORK,
                        "%STATUS%", Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound() ? "§a§l✔ Enabled" : "§c§l❌ Disabled"))
                .setCustomId("settings.firework")
                .withGlow(Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound())
                .build());

        keys.add(new ItemBuilder(XMaterial.CLOCK)
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SETTINGS_HINT_COOLDOWN))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SETTINGS_HINT_COOLDOWN,
                        "%STATUS%", Main.getInstance().getPluginConfig().getHintApplyCooldownOnFail() ? "§a§l✔ Enabled" : "§c§l❌ Disabled"))
                .setCustomId("settings.hintcooldown")
                .withGlow(Main.getInstance().getPluginConfig().getHintApplyCooldownOnFail())
                .build());
    }

    public void addMenuBorderButtons() {
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("settings.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
    }

    public void setMenuItems() {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.PREVIOUS_PAGE_BUTTON, "%CURRENT_PAGE%", String.valueOf(page + 1), "%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.PREVIOUS_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19"))
                .setCustomId("settings.previous_page")
                .build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.NEXT_PAGE_BUTTON, "%CURRENT_PAGE%", String.valueOf(page + 1), "%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.NEXT_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ=="))
                .setCustomId("settings.next_page")
                .build());

        if (keys.isEmpty()) {
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS)
                    .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.LIST_ERROR))
                    .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LIST_ERROR))
                    .build());
            return;
        }
        for(int i = 0; i < getMaxItemsPerPage(); i++) {
            index = getMaxItemsPerPage() * page + i;
            if (index >= keys.size()) break;
            if (keys.get(index) == null) {
                continue;
            }
            int slotIndex = ((9 + 1) + ((i / 7) * 9) + (i % 7));
            getInventory().setItem(slotIndex,keys.get(index));
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }
    public int getMaxPages(){
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / getMaxItemsPerPage());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player player = (Player) event.getWhoClicked();

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "settings.close":
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::closeInventory,3L);
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.updater":
                Main.getInstance().getPluginConfig().setUpdater(!Main.getInstance().getPluginConfig().getUpdater());
                Main.getInstance().getPluginConfig().saveData();
                this.open();
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
                this.open();
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
                this.open();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.showcoordinates":
                Main.getInstance().getPluginConfig().setShowCoordinatesWhenEggFoundInProgressInventory(!Main.getInstance().getPluginConfig().getShowCoordinatesWhenEggFoundInProgressInventory());
                Main.getInstance().getPluginConfig().saveData();
                this.open();
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
                this.open();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.pluginprefix":
                Main.getInstance().getPluginConfig().setPluginPrefixEnabled(!Main.getInstance().getPluginConfig().getPluginPrefixEnabled());
                Main.getInstance().getPluginConfig().saveData();
                this.open();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.firework":
                Main.getInstance().getPluginConfig().setShowFireworkAfterEggFound(!Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound());
                Main.getInstance().getPluginConfig().saveData();
                this.open();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.hintcooldown":
                Main.getInstance().getPluginConfig().setHintApplyCooldownOnFails(!Main.getInstance().getPluginConfig().getHintApplyCooldownOnFail());
                Main.getInstance().getPluginConfig().saveData();
                this.open();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "settings.previous_page":
                if (page == 0) {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    page = page - 1;
                    this.open();
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
            case "settings.next_page":
                if (!((index + 1) >= keys.size())) {
                    page = page + 1;
                    this.open();
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
        }
    }

    @Override
    public void onOpen(InventoryOpenEvent event) {
        Main.getInstance().setLastOpenedInventory(getInventory(), playerMenuUtility.getOwner());
        getInventory().setContents(inventoryContent);
        setMenuItems();
    }
}

