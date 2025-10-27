package de.theredend2000.advancedhunt.managers.inventorymanager.requirements;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedhunt.util.DateTimeUtil;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.PlayerMenuUtility;
import de.theredend2000.advancedhunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Random;

public class RequirementWeekday extends InventoryMenu {
    private MessageManager messageManager;
    protected int maxItems;
    private Main plugin;

    public RequirementWeekday(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Requirements - Weekday", (short) 54, XMaterial.WHITE_STAINED_GLASS_PANE);
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
        inventoryContent[4]  = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setDisplayName("§6" + collection)
                .setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(Main.getInstance().getRandom().nextInt(7)))
                .build();
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("requirement_weekday.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("requirement_weekday.back")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.BACK_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.BACK_BUTTON))
                .setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0="))
                .build();
    }

    private void menuContent(String collection) {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        getInventory().setContents(inventoryContent);
        for(String weekdays : new ArrayList<>(DateTimeUtil.getWeekList())){
            boolean enabled = placedEggs.getBoolean("Requirements.Weekday." + weekdays);
            getInventory().addItem(new ItemBuilder(enabled ? XMaterial.LIME_BED : XMaterial.RED_STAINED_GLASS)
                    .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_WEEKDAY,"%WEEKDAY%", plugin.getRequirementsManager().getRequirementsTranslation(weekdays)))
                    .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_WEEKDAY,"%ADD_REMOVE%",(enabled ? "remove" : "add"),"%WEEKDAY%", plugin.getRequirementsManager().getRequirementsTranslation(weekdays),"%TO_FROM%",(enabled ? "from" : "to"),"%STATUS%",(enabled ? "§aEnabled" : "§cDisabled")))
                    .withGlow(enabled)
                    .setCustomId(weekdays)
                    .build());
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player  = (Player) event.getWhoClicked();

        String collection = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        for (String weekdays : DateTimeUtil.getWeekList()) {
            if (weekdays.equals(ItemHelper.getItemId(event.getCurrentItem()))) {
                boolean enabled = placedEggs.getBoolean("Requirements.Weekday." + weekdays);
                placedEggs.set("Requirements.Weekday." + weekdays, !enabled);
                plugin.getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                return;
            }
        }

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "requirement_weekday.close":
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::closeInventory,3L);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "requirement_weekday.back":
                new RequirementMenu(Main.getPlayerMenuUtility(player)).open(collection);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
        }
    }
}
