package de.theredend2000.advancedegghunt.versions.managers.inventorymanager.egginformation;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.egglistmenu.EggListMenu;
import de.theredend2000.advancedegghunt.versions.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Collections;

public class EggInformationMenu extends InformationPaginatedMenu {

    public EggInformationMenu(PlayerMenuUtility playerMenuUtility) {
        super(playerMenuUtility);
    }

    @Override
    public String getMenuName() {
        return "Egg information";
    }

    @Override
    public int getSlots() {
        return 54;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        String id = inventory.getItem(0).getItemMeta().getLocalizedName();
        ArrayList<String> keys = new ArrayList<>();
        for(String uuids : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)){
            if(Main.getInstance().eggs.contains("FoundEggs."+uuids+"."+id)){
                Collections.addAll(keys, Main.getInstance().eggs.getString("FoundEggs."+uuids+".Name"));
            }
        }

        if (e.getCurrentItem().getType().equals(Material.BARRIER)) {
            p.closeInventory();
            p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
        }else if (e.getCurrentItem().getType().equals(Material.EMERALD_BLOCK)) {
            if(Main.getInstance().getRefreshCooldown().containsKey(p.getName())){
                if(Main.getInstance().getRefreshCooldown().get(p.getName()) > System.currentTimeMillis()){
                    p.sendMessage(Main.getInstance().getMessage("RefreshWaitMessage"));
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventoryFailedSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                    return;
                }
            }
            Main.getInstance().getRefreshCooldown().put(p.getName(), System.currentTimeMillis()+ (3*1000));
            new EggInformationMenu(Main.getPlayerMenuUtility(p)).open(inventory.getItem(0).getItemMeta().getLocalizedName());
            p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
        }else if(e.getCurrentItem().getType().equals(Material.PLAYER_HEAD)){
            if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")){
                if (page == 0){
                    p.sendMessage(Main.getInstance().getMessage("AlreadyOnFirstPageMessage"));
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventoryFailedSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }else{
                    page = page - 1;
                    super.open(id);
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }
            }else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")){
                if (!((index + 1) >= keys.size())){
                    page = page + 1;
                    super.open(id);
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }else{
                    p.sendMessage(Main.getInstance().getMessage("AlreadyOnLastPageMessage"));
                    p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventoryFailedSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
                }
            }else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Back")){
                new EggListMenu(Main.getPlayerMenuUtility(p)).open();
                p.playSound(p.getLocation(),VersionManager.getSoundManager().playInventorySuccessSound(),VersionManager.getSoundManager().getSoundVolume(), 1);
            }
        }
    }

    @Override
    public void setMenuItems(String eggId) {
        inventory.setItem(0,new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setDisplayname("§c").setLocalizedName(eggId).build());
        addMenuBorder();
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> uuid = new ArrayList<>();
        for(String uuids : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)){
            if(Main.getInstance().eggs.contains("FoundEggs."+uuids+"."+eggId)){
                Collections.addAll(keys, Main.getInstance().eggs.getString("FoundEggs."+uuids+".Name"));
                Collections.addAll(uuid, uuids);
            }
        }

        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    String maxEggs = String.valueOf(VersionManager.getEggManager().getMaxEggs());
                    String date = VersionManager.getEggManager().getEggDateCollected(uuid.get(index), eggId);
                    String time = VersionManager.getEggManager().getEggTimeCollected(uuid.get(index),eggId);
                    String eggsFound = Main.getInstance().eggs.getString("FoundEggs."+uuid.get(index)+".Count");
                    inventory.addItem(new ItemBuilder(Material.PLAYER_HEAD).setOwner(keys.get(index)).setDisplayname("§6§l"+keys.get(index)+" §7("+uuid.get(index)+")").setLore("§7"+keys.get(index)+" have found the §2egg #"+eggId+"§7.","","§9Information of "+keys.get(index)+":","§7Eggs found: §6"+eggsFound+"/"+maxEggs,"","§9Collected:","§7Date: §6"+date,"§7Time: §6"+time).setLocalizedName(keys.get(index)).build());
                }
            }
        }else
            inventory.setItem(22, new ItemBuilder(Material.RED_STAINED_GLASS).setDisplayname("§4§lNo Founds").setLore("§7No player has found this egg yet.").build());
    }
}

