package de.theredend2000.advancedhunt.managers.inventorymanager.requirements;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.SoundManager;
import de.theredend2000.advancedhunt.managers.inventorymanager.collection.CollectionEditor;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.PlayerMenuUtility;
import de.theredend2000.advancedhunt.util.enums.Requirements;
import de.theredend2000.advancedhunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Random;

public class RequirementMenu extends InventoryMenu {
    private MessageManager messageManager;
    protected int maxItems;
    private Main plugin;

    public RequirementMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Requirements - Selection", (short) 54, XMaterial.WHITE_STAINED_GLASS_PANE);
        this.plugin = Main.getInstance();
        messageManager = this.plugin.getMessageManager();
        this.maxItems = 7 * ((this.slots / 9) - 2);
    }

    public void open(String collection) {
        super.addMenuBorder();
        addMenuBorderButtons(collection);
        menuContent(collection);

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    private void addMenuBorderButtons(String collection) {
        inventoryContent[4] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7)))
                .setDisplayName("§6" + collection)
                .build();

        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("requirement_menu.back")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.BACK_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.BACK_BUTTON))
                .setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0="))
                .build();
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("requirement_menu.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
    }

    private void menuContent(String collection) {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);

        getInventory().setItem(10, new ItemBuilder(XMaterial.CLOCK)
                .setCustomId("requirement_menu.hour")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_SELECTION,"%SELECTION%", messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_HOUR)))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_SELECTION,"%STATUS_ACTIVE%", plugin.getRequirementsManager().getActives(Requirements.Hours, collection)))
                .build());
        getInventory().setItem(11, new ItemBuilder(XMaterial.CLOCK)
                .setCustomId("requirement_menu.date")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_SELECTION,"%SELECTION%", messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_DATE)))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_SELECTION,"%STATUS_ACTIVE%", plugin.getRequirementsManager().getActives(Requirements.Date, collection)))
                .build());
        getInventory().setItem(12, new ItemBuilder(XMaterial.CLOCK)
                .setCustomId("requirement_menu.weekday")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_SELECTION,"%SELECTION%", messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_WEEKDAY)))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_SELECTION,"%STATUS_ACTIVE%", plugin.getRequirementsManager().getActives(Requirements.Weekday, collection)))
                .build());
        getInventory().setItem(13, new ItemBuilder(XMaterial.CLOCK)
                .setCustomId("requirement_menu.month")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_SELECTION,"%SELECTION%", messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_MONTH)))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_SELECTION,"%STATUS_ACTIVE%", plugin.getRequirementsManager().getActives(Requirements.Month, collection)))
                .build());
        getInventory().setItem(14, new ItemBuilder(XMaterial.CLOCK)
                .setCustomId("requirement_menu.year")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_SELECTION,"%SELECTION%", messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_YEAR)))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_SELECTION,"%STATUS_ACTIVE%", plugin.getRequirementsManager().getActives(Requirements.Year, collection)))
                .build());
        getInventory().setItem(15, new ItemBuilder(XMaterial.CLOCK)
                .setCustomId("requirement_menu.season")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_SELECTION,"%SELECTION%", messageManager.getMessage(MessageKey.REQUIREMENTS_NAME_SEASON)))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_SELECTION,"%STATUS_ACTIVE%", plugin.getRequirementsManager().getActives(Requirements.Season, collection)))
                .build());
        getInventory().setItem(37, new ItemBuilder(XMaterial.LIME_TERRACOTTA)
                .setCustomId("requirement_menu.all_on")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_ACTIVATE))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_ACTIVATE))
                .build());
        getInventory().setItem(38, new ItemBuilder(XMaterial.RED_TERRACOTTA)
                .setCustomId("requirement_menu.all_off")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_DEACTIVATE))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_DEACTIVATE))
                .build());
        String currentOrder = placedEggs.getString("RequirementsOrder");
        getInventory().setItem(43, new ItemBuilder(XMaterial.REDSTONE_TORCH)
                .setCustomId("requirement_menu.order")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_ORDER))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_ORDER,"%CURRENT_ORDER%", currentOrder))
                .build());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player  = (Player) event.getWhoClicked();
        SoundManager soundManager = plugin.getSoundManager();

        String collection = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "requirement_menu.close":
                player.closeInventory();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "requirement_menu.back":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new CollectionEditor(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "requirement_menu.hour":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementHours(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "requirement_menu.date":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementsDate(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "requirement_menu.weekday":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementWeekday(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "requirement_menu.month":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementMonth(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "requirement_menu.year":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementYear(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "requirement_menu.season":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                new RequirementSeason(Main.getPlayerMenuUtility(player)).open(collection);
                break;
            case "requirement_menu.all_on":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                plugin.getRequirementsManager().changeActivity(collection, true);
                messageManager.sendMessage(player, MessageKey.ACTIVATE_REQUIREMENTS);
                menuContent(collection);
                break;
            case "requirement_menu.all_off":
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                plugin.getRequirementsManager().changeActivity(collection, false);
                messageManager.sendMessage(player, MessageKey.DEACTIVATE_REQUIREMENTS);
                menuContent(collection);
                break;
            case "requirement_menu.order":
                FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                String currentOrder = placedEggs.getString("RequirementsOrder");
                placedEggs.set("RequirementsOrder", currentOrder.equalsIgnoreCase("OR") ? "AND" : "OR");
                plugin.getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                break;
        }
    }
}
