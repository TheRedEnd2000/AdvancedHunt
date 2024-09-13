package de.theredend2000.advancedegghunt.managers.inventorymanager.requirements;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedegghunt.util.DateTimeUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.ItemHelper;
import de.theredend2000.advancedegghunt.util.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.util.enums.Seasons;
import de.theredend2000.advancedegghunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RequirementSeason extends InventoryMenu {
    private MessageManager messageManager;
    protected int maxItems;
    private Main plugin;

    public RequirementSeason(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Requirements - Season", (short) 54, XMaterial.WHITE_STAINED_GLASS_PANE);
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
                .setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(new Random().nextInt(7)))
                .build();
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("requirement_season.back")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("requirement_season.back")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.BACK_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.BACK_BUTTON))
                .setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0="))
                .build();
    }

    private void menuContent(String collection) {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        getInventory().setContents(inventoryContent);
        for(String season : new ArrayList<>(DateTimeUtil.getSeasonList())){
            boolean enabled = placedEggs.getBoolean("Requirements.Season." + season);
            getInventory().addItem(new ItemBuilder(enabled ? XMaterial.OAK_LEAVES : XMaterial.RED_STAINED_GLASS)
                    .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_SEASON,"%SEASON%",plugin.getRequirementsManager().getRequirementsTranslation(season)))
                    .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_SEASON,"%ADD_REMOVE%",(enabled ? "remove" : "add"),"%SEASON_INFORMATION%", getSeasonInformation(Seasons.valueOf(season)),"%SEASON%", plugin.getRequirementsManager().getRequirementsTranslation(season),"%TO_FROM%",(enabled ? "from" : "to"),"%STATUS%",(enabled ? "§aEnabled" : "§cDisabled")))
                    .withGlow(enabled)
                    .build());
        }
    }

    private String getSeasonInformation(Seasons seasons){
        switch (seasons){
            case Winter:
                return messageManager.getMessage(MessageKey.MONTH_DECEMBER)+" | "+messageManager.getMessage(MessageKey.MONTH_JANUARY)+" | "+messageManager.getMessage(MessageKey.MONTH_FEBRUARY);
            case Summer:
                return messageManager.getMessage(MessageKey.MONTH_JUNE)+" | "+messageManager.getMessage(MessageKey.MONTH_JULI)+" | "+messageManager.getMessage(MessageKey.MONTH_AUGUST);
            case Spring:
                return messageManager.getMessage(MessageKey.MONTH_MARCH)+" | "+messageManager.getMessage(MessageKey.MONTH_APRIL)+" | "+messageManager.getMessage(MessageKey.MONTH_MAY);
            case Fall:
                return messageManager.getMessage(MessageKey.MONTH_SEPTEMBER)+" | "+messageManager.getMessage(MessageKey.MONTH_OCTOBER)+" | "+messageManager.getMessage(MessageKey.MONTH_NOVEMBER);
            case Unknown:
                return "§4UNKNOWN";
        }
        return null;
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player  = (Player) event.getWhoClicked();

        String collection = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        for (String season : new ArrayList<>(DateTimeUtil.getSeasonList())) {
            if (ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName()).equals(season)) {
                boolean enabled = placedEggs.getBoolean("Requirements.Season." + season);
                placedEggs.set("Requirements.Season." + season, !enabled);
                plugin.getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                return;
            }
        }

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "requirement_season.close":
                player.closeInventory();
                break;
            case "requirement_season.back":
                new RequirementMenu(Main.getPlayerMenuUtility(player)).open(collection);
                break;
        }
    }
}
