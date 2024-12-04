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
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;

public class EggPlaceMenu extends PaginatedInventoryMenu implements IInventoryMenuOpen {
    private MessageManager messageManager;

    public EggPlaceMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility, StringUtils.capitalize(Main.getInstance().getPluginConfig().getPluginNamePlural()) + " place list", (short) 54);
        messageManager = Main.getInstance().getMessageManager();

        super.addMenuBorder();
        addMenuBorderButtons();
    }

    public void open() {
        Main.getInstance().setLastOpenedInventory(getInventory(), playerMenuUtility.getOwner());
        getInventory().setContents(inventoryContent);
        setMenuItems();

        playerMenuUtility.getOwner().openInventory(getInventory());
    }

    public void addMenuBorderButtons() {
        inventoryContent[49] = new ItemBuilder(XMaterial.BARRIER)
                .setCustomId("egg_place.close")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.CLOSE_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.CLOSE_BUTTON))
                .build();
        inventoryContent[53] = new ItemBuilder(XMaterial.EMERALD_BLOCK)
                .setCustomId("egg_place.refresh")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.REFRESH_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.REFRESH_BUTTON))
                .build();
        inventoryContent[8] = new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setCustomId("egg_place.information")
                .setSkullOwner(Main.getTexture("MTY0MzlkMmUzMDZiMjI1NTE2YWE5YTZkMDA3YTdlNzVlZGQyZDUwMTVkMTEzYjQyZjQ0YmU2MmE1MTdlNTc0ZiJ9fX0="))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.EGGPLACE_INFORMATION))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.EGGPLACE_INFORMATION))
                .build();
        String selectedSection = Main.getInstance().getPlayerEggDataManager().getPlayerData(playerMenuUtility.getOwner().getUniqueId()).getString("SelectedSection");
        inventoryContent[45] = new ItemBuilder(XMaterial.PAPER)
                .setCustomId("egg_place.collection_selected")
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.SELECTED_COLLECTION_BUTTON))
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.SELECTED_COLLECTION_BUTTON, "%COLLECTION%", selectedSection))
                .build();
    }

    public void setMenuItems() {
        getInventory().setItem(48, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.PREVIOUS_PAGE_BUTTON,"%CURRENT_PAGE%", String.valueOf(page + 1),"%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.PREVIOUS_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("ZDU5YmUxNTU3MjAxYzdmZjFhMGIzNjk2ZDE5ZWFiNDEwNDg4MGQ2YTljZGI0ZDVmYTIxYjZkYWE5ZGIyZDEifX19"))
                .build());
        getInventory().setItem(50, new ItemBuilder(XMaterial.PLAYER_HEAD)
                .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.NEXT_PAGE_BUTTON,"%CURRENT_PAGE%", String.valueOf(page + 1),"%MAX_PAGES%", String.valueOf(getMaxPages())))
                .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.NEXT_PAGE_BUTTON))
                .setSkullOwner(Main.getTexture("NDJiMGMwN2ZhMGU4OTIzN2Q2NzllMTMxMTZiNWFhNzVhZWJiMzRlOWM5NjhjNmJhZGIyNTFlMTI3YmRkNWIxIn19fQ=="))
                .build());

        ArrayList<String> keys = new ArrayList<>();
        if(Main.getInstance().getPluginConfig().hasPlaceEggs()){
            keys.addAll(Main.getInstance().getPluginConfig().getPlaceEggIds());
        }else
            getInventory().setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS)
                    .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.LIST_ERROR))
                    .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.LIST_ERROR))
                    .build());
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for(int i = 0; i < getMaxItemsPerPage(); i++) {
            index = getMaxItemsPerPage() * page + i;
            if(index >= keys.size()) break;
            if (keys.get(index) == null) {
                continue;
            }
            int slotIndex = ((9 + 1) + ((i / 7) * 9) + (i % 7));

            XMaterial mat = Main.getInstance().getMaterial(Objects.requireNonNull(Main.getInstance().getPluginConfig().getPlaceEggType(keys.get(index))).toUpperCase());
            if(mat.equals(XMaterial.PLAYER_HEAD))
                getInventory().setItem(slotIndex, new ItemBuilder(mat)
                        .setSkullOwner(Main.getTexture(Main.getInstance().getPluginConfig().getPlaceEggTexture(keys.get(index))))
                        .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.EGGPLACE_EGG,"%TREASURE_ID%", keys.get(index)))
                        .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.EGGPLACE_EGG))
                        .setCustomId(keys.get(index))
                .build());
            else
                getInventory().setItem(slotIndex, new ItemBuilder(mat)
                        .setDisplayName(menuMessageManager.getMenuItemName(MenuMessageKey.EGGPLACE_EGG,"%TREASURE_ID%", keys.get(index)))
                        .setLore(menuMessageManager.getMenuItemLore(MenuMessageKey.EGGPLACE_EGG))
                        .setCustomId(keys.get(index))
                        .build());
        }
    }

    public int getMaxItemsPerPage() {
        return maxItemsPerPage;
    }
    public int getMaxPages(){
        ArrayList<String> keys = new ArrayList<>();
        if(Main.getInstance().getPluginConfig().hasPlaceEggs()){
            keys.addAll(Main.getInstance().getPluginConfig().getPlaceEggIds());
        }
        if(keys.isEmpty()) return 1;
        return (int) Math.ceil((double) keys.size() / getMaxItemsPerPage());
    }

    @Override
    public void handleMenu(InventoryClickEvent event) {
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player player = (Player) event.getWhoClicked();

        if(super.playerMenuUtility.getOwner().getInventory().equals(event.getClickedInventory())) {
            Set<String> keys = Main.getInstance().getPluginConfig().getPlaceEggIds();
            String fullTexture = ItemHelper.getSkullTexture(event.getCurrentItem());

            for(String key : keys){
                if(event.getCurrentItem().getType().name().equalsIgnoreCase(Main.getInstance().getPluginConfig().getPlaceEggType(key)) &&
                        !(event.getCurrentItem().getType().name().equalsIgnoreCase(XMaterial.PLAYER_HEAD.name()) &&
                                fullTexture != null &&
                                !Objects.equals(Main.getInstance().getPluginConfig().getPlaceEggTexture(key), fullTexture))) {
                    super.playerMenuUtility.getOwner().sendMessage(messageManager.getMessage(MessageKey.BLOCK_LISTED));
                    return;
                }
            }

            int nextNumber = 0;
            if (!keys.isEmpty()) {
                for (int i = 0; i <= keys.size(); i++) {
                    String key = Integer.toString(i);
                    if (!keys.contains(key)) {
                        nextNumber = i;
                        break;
                    }
                }
            }
            Main.getInstance().getPluginConfig().setPlaceEggType(nextNumber, event.getCurrentItem().getType().name().toUpperCase());
            if (fullTexture != null) {
                Main.getInstance().getPluginConfig().setPlaceEggTexture(nextNumber, fullTexture);
            }
            Main.getInstance().getPluginConfig().saveData();
            this.open();
            return;
        }

        ArrayList<String> keys = new ArrayList<>();
        if(Main.getInstance().getPluginConfig().hasPlaceEggs()){
            keys.addAll(Main.getInstance().getPluginConfig().getPlaceEggIds());
            for(String id : keys){
                if (!ItemHelper.hasItemId(event.getCurrentItem()) ||
                        !ItemHelper.getItemId(event.getCurrentItem()).equals(id)) {
                    continue;
                }
                if(event.getAction() == InventoryAction.PICKUP_ALL){
                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);

                    ItemBuilder itemBuilder = new ItemBuilder(XMaterial.PLAYER_HEAD)
                            .setSkullOwner(Main.getTexture(Main.getInstance().getPluginConfig().getPlaceEggTexture(id)))
                            .setDisplayName("§6Easter Egg")
                            .setLore("§7Place this egg around the map", "§7that everyone can search and find it.");

                    if (event.getCurrentItem().getType().equals(XMaterial.PLAYER_HEAD.parseMaterial()))
                        itemBuilder.setItemType(XMaterial.PLAYER_HEAD)
                                .setSkullOwner(Main.getTexture(Main.getInstance().getPluginConfig().getPlaceEggTexture(id)));
                    else
                        itemBuilder.setItemType(XMaterial.matchXMaterial(event.getCurrentItem().getType()));

                    player.getInventory().addItem(itemBuilder.build());
                }else if(event.getAction() == InventoryAction.PICKUP_HALF){
                    player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    Main.getInstance().getPluginConfig().removePlaceEggType(Integer.parseInt(id));
                    Main.getInstance().getPluginConfig().saveData();
                    this.open();
                }
                return;
            }
        }

        switch (ItemHelper.getItemId(event.getCurrentItem())) {
            case "egg_place.close":
                player.closeInventory();
                player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                break;
            case "egg_place.refresh":
                if (Main.getInstance().getRefreshCooldown().containsKey(player.getName())) {
                    if (Main.getInstance().getRefreshCooldown().get(player.getName()) > System.currentTimeMillis()) {
                        player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.WAIT_REFRESH));
                        player.playSound(player.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                        return;
                    }
                }
                Main.getInstance().getRefreshCooldown().put(player.getName(), System.currentTimeMillis() + (3 * 1000));
                open();
                player.playSound(player.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case "egg_place.collection_selected":
                new CollectionSelectMenu(Main.getPlayerMenuUtility(player)).open();
                return;
            case "egg_place.previous_page":
                if (page == 0) {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    page = page - 1;
                    this.open();
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
            case "egg_place.next_page":
                if (!((index + 1) >= keys.size())) {
                    page = page + 1;
                    this.open();
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventorySuccessSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                } else {
                    player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                    player.playSound(player.getLocation(), Main.getInstance().getSoundManager().playInventoryFailedSound(), Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
                break;
            case "egg_place.information":
                TextComponent textComponent = new TextComponent("§7Discord link: ");
                TextComponent clickMe = new TextComponent("§6§l[CLICK HERE]");
                clickMe.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText("§7Click to join.")));
                clickMe.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://discord.gg/hNj9erE5EA"));
                textComponent.addExtra(clickMe);
                player.spigot().sendMessage(textComponent);
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

