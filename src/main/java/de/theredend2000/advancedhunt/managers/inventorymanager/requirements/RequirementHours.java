package de.theredend2000.advancedhunt.managers.inventorymanager.requirements;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.InventoryMenu;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemHelper;
import de.theredend2000.advancedhunt.util.PlayerMenuUtility;
import de.theredend2000.advancedhunt.util.messages.MenuMessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import de.theredend2000.advancedhunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Random;

public class RequirementHours extends InventoryMenu {
    private MessageManager messageManager;
    protected int maxItems;
    private Main plugin;

    public RequirementHours(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Requirements - Hours", (short) 54, XMaterial.WHITE_STAINED_GLASS_PANE);
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
                .setCustomId("requirement_hour.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("requirement_hour.back")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.BACK_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.BACK_BUTTON))
                .setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0="))
                .build();
    }

    private void menuContent(String collection) {
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        for(int i = 0; i < 24; i++){
            int index = ((9 + 1) + ((i / 7) * 9) + (i % 7));
            boolean enabled = placedEggs.getBoolean("Requirements.Hours." + i);
            String hour = String.valueOf(i);
            getInventory().setItem(index, new ItemBuilder(enabled ? XMaterial.CLOCK : XMaterial.RED_STAINED_GLASS)
                    .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_HOUR,"%HOUR%", hour))
                    .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_HOUR,"%ADD_REMOVE%",(enabled ? "remove" : "add"),"%HOUR_FORMAT%", getHourFormat(messageManager.getMessage(MessageKey.HOUR_FORMAT), i),"%HOUR%", hour,"%TO_FROM%",(enabled ? "from" : "to"),"%STATUS%",(enabled ? "§aEnabled" : "§cDisabled")))
                    .withGlow(enabled)
                    .setCustomId(String.valueOf(i))
                    .build());
        }
    }

    public String getHourFormat(String hourFormat, int hour) {
        switch (hourFormat) {
            case "12": {
                String period = (hour <= 12) ? "AM" : "PM";
                int adjustedHour = (hour % 12 == 0) ? 12 : hour % 12;
                int nextHour = (hour + 1) % 24;
                String nextPeriod = (nextHour <= 12) ? "AM" : "PM";
                int adjustedNextHour = (nextHour % 12 == 0) ? 12 : nextHour % 12;
                return adjustedHour + period + " - " + adjustedNextHour + nextPeriod;
            }
            case "24": {
                int nextHour = (hour + 1) % 24;
                return hour + " - " + nextHour;
            }
            default:
                return "§4UNKNOWN";
        }
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        Player player  = (Player) event.getWhoClicked();

        String collection = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
        for (int i = 0; i < 24; i++) {
            if (String.valueOf(i).equals(ItemHelper.getItemId(event.getCurrentItem()))) {
                boolean enabled = placedEggs.getBoolean("Requirements.Hours." + i);
                placedEggs.set("Requirements.Hours." + i, !enabled);
                plugin.getEggDataManager().savePlacedEggs(collection);
                menuContent(collection);
                return;
            }
        }

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "requirement_hour.close":
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::closeInventory,3L);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "requirement_hour.back":
                new RequirementMenu(Main.getPlayerMenuUtility(player)).open(collection);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
        }
    }
}
