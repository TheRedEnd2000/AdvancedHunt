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
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Random;

public class RequirementMonth extends InventoryMenu {
    private MessageManager messageManager;
    protected int maxItems;
    private Main plugin;

    public RequirementMonth(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Requirements - Month", (short) 54, XMaterial.WHITE_STAINED_GLASS_PANE);
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
                .setCustomId("requirement_month.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("requirement_month.back")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.BACK_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.BACK_BUTTON))
                .setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0="))
                .build();
    }

    private void menuContent(String collection) {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        getInventory().setContents(inventoryContent);
        for(String month : new ArrayList<>(DateTimeUtil.getMonthList())){
            boolean enabled = placedEggs.getBoolean("Requirements.Month." + month);
            getInventory().addItem(new ItemBuilder(enabled ? XMaterial.GRASS_BLOCK : XMaterial.RED_STAINED_GLASS)
                    .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_MONTH,"%MONTH%", plugin.getRequirementsManager().getRequirementsTranslation(month)))
                    .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_MONTH,"%ADD_REMOVE%",(enabled ? "remove" : "add"),"%MONTH%", plugin.getRequirementsManager().getRequirementsTranslation(month),"%TO_FROM%",(enabled ? "from" : "to"),"%STATUS%",(enabled ? "§aEnabled" : "§cDisabled")))
                    .withGlow(enabled)
                    .setCustomId(month)
                    .build());
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player  = (Player) event.getWhoClicked();

        String collection = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        for (String month : new ArrayList<>(DateTimeUtil.getMonthList())) {
            if (month.equals(ItemHelper.getItemId(event.getCurrentItem()))) {
                boolean enabled = placedEggs.getBoolean("Requirements.Month." + month);
                placedEggs.set("Requirements.Month." + month, !enabled);
                plugin.getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                return;
            }
        }

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "requirement_month.close":
                player.closeInventory();
                break;
            case "requirement_month.back":
                new RequirementMenu(Main.getPlayerMenuUtility(player)).open(collection);
                break;
        }
    }
}
