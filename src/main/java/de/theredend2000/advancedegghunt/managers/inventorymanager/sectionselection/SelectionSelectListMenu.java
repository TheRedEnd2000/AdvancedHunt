package de.theredend2000.advancedegghunt.managers.inventorymanager.sectionselection;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Objects;

public class SelectionSelectListMenu extends SelectionSelectPaginatedMenu {

    public SelectionSelectListMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    @Override
    public String getMenuName() {
        return "Select collection";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        String section = Objects.requireNonNull(e.getCurrentItem().getItemMeta()).getLocalizedName();
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player p = (Player) e.getWhoClicked();

        ArrayList<String> keys = new ArrayList<>(Main.getInstance().getEggDataManager().savedEggCollections());
        for(String selection : keys){
            if (!Objects.requireNonNull(e.getCurrentItem().getItemMeta()).getLocalizedName().equals(selection)) {
                continue;
            }
            if(e.getAction() == InventoryAction.PICKUP_ALL){
                Main.getInstance().getPlayerEggDataManager().savePlayerSection(p.getUniqueId(), selection);
                p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                super.open();
                p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.COLLECTION_SELECTION).replaceAll("%SELECTION%", selection));
            }else if(e.getAction() == InventoryAction.PICKUP_HALF){
                if(p.hasPermission(Objects.requireNonNull(Main.getInstance().getConfig().getString("Permissions.ChangeCollectionsPermission")))) {
                    Main.getInstance().getInventoryManager().createEditCollectionMenu(p, section);
                    p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                }
            }
        }

        switch (e.getCurrentItem().getType()) {
            case BARRIER:
                p.closeInventory();
                p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case EMERALD_BLOCK:
                if (Main.getInstance().getRefreshCooldown().containsKey(p.getName())) {
                    if (Main.getInstance().getRefreshCooldown().get(p.getName()) > System.currentTimeMillis()) {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.WAIT_REFRESH));
                        p.playSound(p.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                        return;
                    }
                }
                Main.getInstance().getRefreshCooldown().put(p.getName(), System.currentTimeMillis() + (3 * 1000));
                new SelectionSelectListMenu(Main.getPlayerMenuUtility(p)).open();
                p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                break;
            case PLAYER_HEAD:
                if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")) {
                    if (page == 0) {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                        p.playSound(p.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        page = page - 1;
                        super.open();
                        p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")) {
                    if (!((index + 1) >= keys.size())) {
                        page = page + 1;
                        super.open();
                        p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                    } else {
                        p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                        p.playSound(p.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                    }
                } else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Add collection")) {
                    Main.getInstance().getInventoryManager().createAddCollectionMenu(p);
                    p.playSound(p.getLocation(), soundManager.playInventoryFailedSound(), soundManager.getSoundVolume(), 1);
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        ArrayList<String> keys = new ArrayList<>(Main.getInstance().getEggDataManager().savedEggCollections());
        if(keys.isEmpty()){
            playerMenuUtility.getOwner().closeInventory();
            playerMenuUtility.getOwner().sendMessage("§cThere was an error please restart your server.");
            return;
        }

        if (keys == null || keys.isEmpty()) {
            return;
        }
        for(int i = 0; i < getMaxItemsPerPage(); i++) {
            index = getMaxItemsPerPage() * page + i;
            if(index >= keys.size()) break;
            if (keys.get(index) == null) {
                continue;
            }
            String selectedSection = Main.getInstance().getEggManager().getEggSectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
            int maxEggs = Main.getInstance().getEggManager().getMaxEggs(keys.get(index));
            boolean applied = selectedSection.equals(keys.get(index));
            boolean permission = playerMenuUtility.getOwner().hasPermission(Objects.requireNonNull(Main.getInstance().getConfig().getString("Permissions.ChangeCollectionsPermission")));
            inventory.addItem(new ItemBuilder(XMaterial.PAPER).withGlow(applied).setDisplayname("§6Collection: §6§l" + keys.get(index) + (applied ? " §a(selected)" : "")).setLore("", "§9Collection Information:", "§7   - Placed eggs: §6" + maxEggs, "", "§aNote:", "§7This collection applies to all actions that are carried out.", "§7It can be changed at any time in this menu.", "", "§eLEFT-CLICK to select.", permission ? "§eRIGHT-CLICK to edit." : "§7§mRIGHT-CLICK to edit.").setLocalizedName(keys.get(index)).build());
        }
    }
}

