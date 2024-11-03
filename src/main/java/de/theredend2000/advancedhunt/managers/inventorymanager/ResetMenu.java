package de.theredend2000.advancedhunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.inventorymanager.collection.CollectionEditor;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.PlayerMenuUtility;
import de.theredend2000.advancedhunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Random;

public class ResetMenu extends InventoryMenu {
    private MessageManager messageManager;
    private Main plugin;

    public ResetMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Reset - Selection", (short) 54);
        messageManager = Main.getInstance().getMessageManager();
        this.plugin = Main.getInstance();
    }

    public void open(String collection) {
        super.addMenuBorder();
        addMenuBorderButtons(collection);
        getInventory().setContents(inventoryContent);
        menuContent(collection);

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    private void addMenuBorderButtons(String collection) {
        inventoryContent[4] =  new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("reset.collection")
                .setDisplayName("§6" + collection)
                .setSkullOwner(plugin.getEggManager().getRandomEggTexture(Main.getInstance().getRandom().nextInt(7)))
                .build();

        inventoryContent[37] = new ItemBuilder(XMaterial.RED_TERRACOTTA)
                .setCustomId("reset.reset_all")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.RESET_RESET_ALL))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.RESET_RESET_ALL))
                .build();

        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("reset.back")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.BACK_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.BACK_BUTTON))
                .setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0="))
                .build();
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("reset.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
    }

    private void menuContent(String collection) {
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        String overall = plugin.getRequirementsManager().getConvertedTime(collection);

        getInventory().setItem(10, new ItemBuilder(XMaterial.REDSTONE)
                .setCustomId("reset.reset_year")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.RESET_YEAR))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.RESET_YEAR,"%YEAR%", String.valueOf(placedEggs.getInt("Reset.Year")),"%OVERALL%", overall))
                .build());
        getInventory().setItem(11, new ItemBuilder(XMaterial.REDSTONE)
                .setCustomId("reset.reset_month")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.RESET_MONTH))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.RESET_MONTH,"%MONTH%", String.valueOf(placedEggs.getInt("Reset.Month")),"%OVERALL%", overall))
                .build());
        getInventory().setItem(12, new ItemBuilder(XMaterial.REDSTONE)
                .setCustomId("reset.reset_day")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.RESET_DAY))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.RESET_DAY,"%DAY%", String.valueOf(placedEggs.getInt("Reset.Day")),"%OVERALL%", overall))
                .build());
        getInventory().setItem(13, new ItemBuilder(XMaterial.REDSTONE)
                .setCustomId("reset.reset_hour")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.RESET_HOUR))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.RESET_HOUR,"%HOUR%", String.valueOf(placedEggs.getInt("Reset.Hour")),"%OVERALL%", overall))
                .build());
        getInventory().setItem(14, new ItemBuilder(XMaterial.REDSTONE)
                .setCustomId("reset.reset_minute")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.RESET_MINUTE))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.RESET_MINUTE,"%MINUTE%", String.valueOf(placedEggs.getInt("Reset.Minute")),"%OVERALL%", overall))
                .build());
        getInventory().setItem(15, new ItemBuilder(XMaterial.REDSTONE)
                .setCustomId("reset.reset_second")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.RESET_SECOND))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.RESET_SECOND,"%SECOND%", String.valueOf(placedEggs.getInt("Reset.Second")),"%OVERALL%", overall))
                .build());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        if (!event.getCurrentItem().getItemMeta().hasDisplayName()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        SoundManager soundManager = Main.getInstance().getSoundManager();

        String collection = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "reset.close":
                player.closeInventory();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "reset.back":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new CollectionEditor(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "reset.reset_year":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                int currentYear = placedEggs.getInt("Reset.Year");

                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                    placedEggs.set("Reset.Year", currentYear + 1);
                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    if (currentYear - 1 >= 0) {
                        placedEggs.set("Reset.Year", currentYear - 1);
                    }
                } else if (event.getClick() == ClickType.MIDDLE) {
                    placedEggs.set("Reset.Year", 0);
                }
                plugin.getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                break;
            case "reset.reset_month":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                int currentMonth = placedEggs.getInt("Reset.Month");

                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                    placedEggs.set("Reset.Month", currentMonth + 1);
                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    if (currentMonth - 1 >= 0) {
                        placedEggs.set("Reset.Month", currentMonth - 1);
                    }
                } else if (event.getClick() == ClickType.MIDDLE) {
                    placedEggs.set("Reset.Month", 0);
                }
                plugin.getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                break;
            case "reset.reset_day":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                int currentDay = placedEggs.getInt("Reset.Day");

                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                    placedEggs.set("Reset.Day", currentDay + 1);
                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    if (currentDay - 1 >= 0) {
                        placedEggs.set("Reset.Day", currentDay - 1);
                    }
                } else if (event.getClick() == ClickType.MIDDLE) {
                    placedEggs.set("Reset.Day", 0);
                }
                plugin.getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                break;
            case "reset.reset_hour":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                int currentHour = placedEggs.getInt("Reset.Hour");

                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                    placedEggs.set("Reset.Hour", currentHour + 1);
                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    if (currentHour - 1 >= 0) {
                        placedEggs.set("Reset.Hour", currentHour - 1);
                    }
                } else if (event.getClick() == ClickType.MIDDLE) {
                    placedEggs.set("Reset.Hour", 0);
                }
                plugin.getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                break;
            case "reset.reset_minute":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                int currentMin = placedEggs.getInt("Reset.Minute");

                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                    placedEggs.set("Reset.Minute", currentMin + 1);
                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    if (currentMin - 1 >= 0) {
                        placedEggs.set("Reset.Minute", currentMin - 1);
                    }
                } else if (event.getClick() == ClickType.MIDDLE) {
                    placedEggs.set("Reset.Minute", 0);
                }
                plugin.getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                break;
            case "reset.reset_second":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                int currentSec = placedEggs.getInt("Reset.Second");

                if (event.getAction() == InventoryAction.PICKUP_ALL) {
                    placedEggs.set("Reset.Second", currentSec + 1);
                } else if (event.getAction() == InventoryAction.PICKUP_HALF) {
                    if (currentSec - 1 >= 0) {
                        placedEggs.set("Reset.Second", currentSec - 1);
                    }
                } else if (event.getClick() == ClickType.MIDDLE) {
                    placedEggs.set("Reset.Second", 0);
                }
                plugin.getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                break;
            case "reset.reset_all":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                plugin.getRequirementsManager().resetReset(collection);
                menuContent(collection);
                break;
        }
    }
}
