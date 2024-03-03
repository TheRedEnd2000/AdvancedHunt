package de.theredend2000.advancedegghunt.managers.inventorymanager.eggprogress;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.managers.inventorymanager.sectionselection.SelectionSelectListMenu;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Random;

public class EggProgressMenu extends ProgressPaginatedMenu {

    public EggProgressMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    @Override
    public String getMenuName() {
        return "Egg progress";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        String section = Main.getInstance().getEggManager().getEggSectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        SoundManager soundManager = Main.getInstance().getSoundManager();
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        Player p = (Player) e.getWhoClicked();

        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs.")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
        }

        if(e.getCurrentItem().getType().equals(Material.PAPER) && ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Selected Collection")){
            new SelectionSelectListMenu(Main.getPlayerMenuUtility(p)).open();
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
                new EggProgressMenu(Main.getPlayerMenuUtility(p)).open();
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
    public void setMenuItems(String playerUUID) {
        String section = Main.getInstance().getEggManager().getEggSectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(section);
        addMenuBorder();
        ArrayList<String> keys = new ArrayList<>();
        if(placedEggs.contains("PlacedEggs.")){
            keys.addAll(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
        }else
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Eggs Available").setLore("§7There are no eggs no find", "§7please contact an admin.").build());

        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    boolean showcoordinates = Main.getInstance().getConfig().getBoolean("Settings.ShowCoordinatesWhenEggFoundInProgressInventory");
                    String x = placedEggs.getString("PlacedEggs." + keys.get(index) + ".X");
                    String y = placedEggs.getString("PlacedEggs." + keys.get(index) + ".Y");
                    String z = placedEggs.getString("PlacedEggs." + keys.get(index) + ".Z");
                    boolean hasFound = Main.getInstance().getEggManager().hasFound(playerMenuUtility.getOwner(), keys.get(index), section);
                    int random = new Random().nextInt(7);
                    String date = Main.getInstance().getEggManager().getEggDateCollected(playerUUID, keys.get(index), section);
                    String time = Main.getInstance().getEggManager().getEggTimeCollected(playerUUID, keys.get(index), section);
                    if(showcoordinates && hasFound){
                        inventory.addItem(new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(random)).setDisplayname("§2§lEgg §7(ID#" + keys.get(index) + ")").setLore("", "§9Location:", "§7X: §e" + x, "§7Y: §e" + y, "§7Z: §e" + z, "", (hasFound ? "§2§lYou have found this egg." : "§4§lYou haven't found this egg yet."), "", "§9Collected:", "§7Date: §6" + date, "§7Time: §6" + time, "").setLocalizedName(keys.get(index)).build());
                    }else if(hasFound && !showcoordinates) {
                        inventory.addItem(new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(random)).setDisplayname("§2§lEgg §7(ID#" + keys.get(index) + ")").setLore("", (hasFound ? "§2§lYou have found this egg." : "§4§lYou haven't found this egg yet."), "", "§9Collected:", "§7Date: §6" + date, "§7Time: §6" + time, "").setLocalizedName(keys.get(index)).build());
                    }else
                        inventory.addItem(new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getInstance().getEggManager().getRandomEggTexture(random)).setDisplayname("§2§lEgg §7(ID#" + keys.get(index) + ")").setLore("", (hasFound ? "§2§lYou have found this egg." : "§4§lYou haven't found this egg yet.")).setLocalizedName(keys.get(index)).build());
                }
            }
        }
    }
}

