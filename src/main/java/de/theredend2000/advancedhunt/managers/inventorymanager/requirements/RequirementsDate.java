package de.theredend2000.advancedhunt.managers.inventorymanager.requirements;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.inventorymanager.common.PaginatedInventoryMenu;
import de.theredend2000.advancedhunt.util.DateTimeUtil;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class RequirementsDate extends PaginatedInventoryMenu {

    private MessageManager messageManager;
    protected int maxItems;
    private Main plugin;
    private int currentMode = 0;

    public RequirementsDate(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, "Requirements - Date", (short) 54, XMaterial.WHITE_STAINED_GLASS_PANE);
        this.plugin = Main.getInstance();
        messageManager = this.plugin.getMessageManager();
        this.maxItems = 7 * ((this.slots / 9) - 2);
    }

    public void open(String collection) {
        super.addMenuBorder();
        addMenuBorderButtons(collection);
        setMenuItems(collection);

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuBorderButtons(String collection) {
        inventoryContent[4]  = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setDisplayName("§6" + collection)
                .setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(Main.getInstance().getRandom().nextInt(7)))
                .build();
        inventoryContent[45] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("date.back")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.BACK_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.BACK_BUTTON))
                .setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0="))
                .build();
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("date.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
    }

    public void setMenuItems(String collection) {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("date.previous_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.PREVIOUS_PAGE_BUTTON,"%CURRENT_PAGE%", String.valueOf(page + 1),"%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.PREVIOUS_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19"))
                .build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("date.next_page")
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.NEXT_PAGE_BUTTON,"%CURRENT_PAGE%", String.valueOf(page + 1),"%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.NEXT_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ=="))
                .build());
        getInventory().setItem(51, new ItemBuilder(XMaterial.HOPPER)
                .setCustomId("date.mode_switch")
                .setDisplayName("§2Sort")
                .setLore(getLore())
                .build());

        ArrayList<String> keys = new ArrayList<>();
        if (currentMode == 0) {
            keys.addAll(DateTimeUtil.getAllDaysOfYear());
        } else {
            keys.addAll(DateTimeUtil.getDaysOfMonth(currentMode));
        }
        if(keys.isEmpty()){
            playerMenuUtility.getOwner().closeInventory();
            return;
        }

        for (int i = 0; i < getMaxItemsPerPage(); i++) {
            int slotIndex = ((9 + 1) + ((i / 7) * 9) + (i % 7));
            getInventory().setItem(slotIndex, null);
        }

        for(int i = 0; i < getMaxItemsPerPage(); i++) {
            index = getMaxItemsPerPage() * page + i;
            int slotIndex = ((9 + 1) + ((i / 7) * 9) + (i % 7));
            if(index >= keys.size()) break;
            if (keys.get(index) == null) {
                continue;
            }

            FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
            boolean enabled = placedEggs.getBoolean("Requirements.Date." + keys.get(index));
            getInventory().setItem(slotIndex, new ItemBuilder(enabled ? XMaterial.NAME_TAG : XMaterial.RED_STAINED_GLASS)
                    .setCustomId(keys.get(index))
                    .withGlow(enabled)
                    .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REQUIREMENTS_DATE,"%DATE%", keys.get(index)))
                    .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REQUIREMENTS_DATE,"%ADD_REMOVE%",(enabled ? "remove" : "add"),"%DATE%", keys.get(index),"%TO_FROM%",(enabled ? "from" : "to"),"%STATUS%",(enabled ? "§aEnabled" : "§cDisabled")))
                    .build());
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }

    public int getMaxPages(){
        int keys = 0;
        if (currentMode == 0) {
            keys = DateTimeUtil.getAllDaysOfYear().size();
        } else {
            keys = DateTimeUtil.getDaysOfMonth(currentMode).size();
        }
        if(keys == 0) return 1;
        return (int) Math.ceil((double) keys / getMaxItemsPerPage());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        String collection = ChatColor.stripColor(event.getInventory().getItem(4).getItemMeta().getDisplayName());
        Player player = (Player) event.getWhoClicked();
        ArrayList<String> dates = new ArrayList<>(DateTimeUtil.getAllDaysOfYear());
        for(String currentDate : dates){
            if (!ItemHelper.hasItemId(event.getCurrentItem()) ||
                    !ItemHelper.getItemId(event.getCurrentItem()).equals(currentDate)) {
                continue;
            }
            FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(collection);
            boolean enabled = placedEggs.getBoolean("Requirements.Date." + currentDate);
            placedEggs.set("Requirements.Date." + currentDate, !enabled);
            plugin.getEggDataManager().savePlacedEggs(collection);
            open(collection);
        }

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "date.close":
                Bukkit.getScheduler().runTaskLater(Main.getInstance(), player::closeInventory,3L);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "date.back":
                new RequirementMenu(Main.getPlayerMenuUtility(player)).open(collection);
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "date.previous_page":
                if (page == 0) {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    page = page - 1;
                    this.open(collection);
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
            case "date.next_page":
                if (!((index + 1) >= dates.size())) {
                    page = page + 1;
                    this.open(collection);
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
            case "date.mode_switch":
                page = 0;
                if (event.isLeftClick()) {
                    currentMode = (currentMode == 12) ? 0 : currentMode + 1;
                } else if (event.isRightClick()) {
                    currentMode = (currentMode == 0) ? 12 : currentMode - 1;
                }
                open(collection);
                break;
        }
    }

    public List<String> getLore() {
        ArrayList<String> lore = new ArrayList<>();
        lore.add("");
        for (int i = 0; i <= 12; i++) {
            if (i == currentMode) {
                lore.add("§6> " + getMonthName(i));
            } else {
                lore.add("§7" + getMonthName(i));
            }
        }
        lore.add("");
        lore.add("§eLEFT-CLICK to go down.");
        lore.add("§eRIGHT-CLICK to go up.");
        lore.add("§8Note: This inventory is currently NOT translated!");
        return lore;
    }

    private String getMonthName(int month) {
        switch (month) {
            case 0: return "All";
            case 1: return plugin.getRequirementsManager().getRequirementsTranslation("January");
            case 2: return plugin.getRequirementsManager().getRequirementsTranslation("February");
            case 3: return plugin.getRequirementsManager().getRequirementsTranslation("March");
            case 4: return plugin.getRequirementsManager().getRequirementsTranslation("April");
            case 5: return plugin.getRequirementsManager().getRequirementsTranslation("May");
            case 6: return plugin.getRequirementsManager().getRequirementsTranslation("June");
            case 7: return plugin.getRequirementsManager().getRequirementsTranslation("July");
            case 8: return plugin.getRequirementsManager().getRequirementsTranslation("August");
            case 9: return plugin.getRequirementsManager().getRequirementsTranslation("September");
            case 10: return plugin.getRequirementsManager().getRequirementsTranslation("October");
            case 11: return plugin.getRequirementsManager().getRequirementsTranslation("November");
            case 12: return plugin.getRequirementsManager().getRequirementsTranslation("December");
            default: return "Unknown";
        }
    }


}
