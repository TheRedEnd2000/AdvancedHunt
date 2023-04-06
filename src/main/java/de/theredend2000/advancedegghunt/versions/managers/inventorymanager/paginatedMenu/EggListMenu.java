package de.theredend2000.advancedegghunt.versions.managers.inventorymanager.paginatedMenu;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;

public class EggListMenu extends PaginatedMenu {

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
        Player p = (Player) e.getWhoClicked();

        ArrayList<String> keys = new ArrayList<>();
        if(Main.getInstance().eggs.contains("Eggs.")){
            keys.addAll(Main.getInstance().eggs.getConfigurationSection("Eggs.").getKeys(false));
            for(String id : Main.getInstance().eggs.getConfigurationSection("Eggs.").getKeys(false)){
                if(Objects.requireNonNull(e.getCurrentItem().getItemMeta()).getLocalizedName().equals(id)){
                    ConfigLocationUtil location = new ConfigLocationUtil(Main.getInstance(), "Eggs." + id);
                    if (location.loadBlockLocation() != null)
                        p.teleport(location.loadLocation().add(0.5,0,0.5));
                    p.closeInventory();
                    p.sendMessage(Main.getInstance().getMessage("TeleportedToEggMessage").replaceAll("%ID%", id));
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }
            }
        }

        if (e.getCurrentItem().getType().equals(Material.BARRIER)) {
            p.closeInventory();
            p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
        }else if(e.getCurrentItem().getType().equals(Material.PLAYER_HEAD)){
            if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")){
                if (page == 0){
                    p.sendMessage(Main.getInstance().getMessage("AlreadyOnFirstPageMessage"));
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventoryFailedSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }else{
                    page = page - 1;
                    super.open();
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }
            }else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")){
                if (!((index + 1) >= keys.size())){
                    page = page + 1;
                    super.open();
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }else{
                    p.sendMessage(Main.getInstance().getMessage("AlreadyOnLastPageMessage"));
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventoryFailedSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }
            }
        }
    }

    @Override
    public void setMenuItems() {
        addMenuBorder();
        ArrayList<String> keys = new ArrayList<>();
        if(Main.getInstance().eggs.contains("Eggs.")){
            keys.addAll(Main.getInstance().eggs.getConfigurationSection("Eggs.").getKeys(false));
        }else
            inventory.setItem(22, new ItemBuilder(Material.RED_STAINED_GLASS).setDisplayname("§4§lNo Eggs Placed").setLore("§7You can add eggs by using","§e/egghunt placeEggs§7.").build());

        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    String x = Main.getInstance().eggs.getString("Eggs."+keys.get(index)+".X");
                    String y = Main.getInstance().eggs.getString("Eggs."+keys.get(index)+".Y");
                    String z = Main.getInstance().eggs.getString("Eggs."+keys.get(index)+".Z");
                    int random = new Random().nextInt(7);
                    inventory.addItem(new ItemBuilder(Material.PLAYER_HEAD).setSkullOwner(VersionManager.getEggManager().getRandomEggTexture(random)).setDisplayname("§2§lEgg §7(ID#"+keys.get(index)+")").setLore("§9Location:","§7X: §e"+x,"§7Y: §e"+y,"§7Z: §e"+z,"","§eClick to teleport.").setLocalizedName(keys.get(index)).build());
                }
            }
        }
    }
}

