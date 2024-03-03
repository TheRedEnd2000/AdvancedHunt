package de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egginformation.EggInformationMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.sectionselection.SelectionSelectListMenu;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class EggListMenu extends ListPaginatedMenu {

    public EggListMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    @Override
    public String getMenuName() {
        return "Eggs list";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        String section = Main.getInstance().getEggManager().getEggSectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        SoundManager soundManager = Main.getInstance().getSoundManager();
        Player p = (Player) e.getWhoClicked();

        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs.")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
            for(String id : placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false)){
                if (!Objects.requireNonNull(e.getCurrentItem().getItemMeta()).getLocalizedName().equals(id)) {
                    continue;
                }
                if(e.getAction() == InventoryAction.PICKUP_ALL){
                    ConfigLocationUtil location = new ConfigLocationUtil(Main.getInstance(), "PlacedEggs." + id);
                    if (location.loadLocation(section) != null)
                        p.teleport(location.loadLocation(section).add(0.5, 0, 0.5));
                    p.closeInventory();
                    p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.TELEPORT_TO_EGG).replaceAll("%ID%", id));
                    p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                }else if(e.getAction() == InventoryAction.PICKUP_HALF){
                    new EggInformationMenu(Main.getPlayerMenuUtility(p)).open(id);
                    p.playSound(p.getLocation(), soundManager.playInventorySuccessSound(), soundManager.getSoundVolume(), 1);
                }
            }
        }

        if(e.getCurrentItem().getType().equals(Material.PAPER) &&
                ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Selected Collection")){
            new SelectionSelectListMenu(Main.getPlayerMenuUtility(p)).open();
            return;
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
                new EggListMenu(Main.getPlayerMenuUtility(p)).open();
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
                }
                break;
        }
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        String section = Main.getInstance().getEggManager().getEggSectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs.")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
        }else
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Eggs Placed").setLore("§7You can add eggs by using", "§e/egghunt placeEggs§7.").build());

        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    String x = placedEggs.getString("PlacedEggs." + keys.get(index) + ".X");
                    String y = placedEggs.getString("PlacedEggs." + keys.get(index) + ".Y");
                    String z = placedEggs.getString("PlacedEggs." + keys.get(index) + ".Z");
                    int random = new Random().nextInt(7);
                    String date = Main.getInstance().getEggManager().getEggDatePlaced(keys.get(index), section);
                    String time = Main.getInstance().getEggManager().getEggTimePlaced(keys.get(index), section);
                    String timesFound = String.valueOf(Main.getInstance().getEggManager().getTimesFound(keys.get(index), section));
                    inventory.addItem(new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(random)).setDisplayname("§2§lEgg §7(ID#" + keys.get(index) + ")").setLore("§9Location:", "§7X: §e" + x, "§7Y: §e" + y, "§7Z: §e" + z, "", "§9Information:", "§7Times found: §6" + timesFound, "", "§9Placed:", "§7Date: §6" + date, "§7Time: §6" + time, "", "§eLEFT-CLICK to teleport.", "§eRIGHT-CLICK for information.").setLocalizedName(keys.get(index)).build());
                }
            }
        }
    }
}

