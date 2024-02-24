package de.theredend2000.advancedegghunt.managers.inventorymanager.egginformation;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.EggListMenu;
import de.theredend2000.advancedegghunt.managers.inventorymanager.egglistmenu.PlayerMenuUtility;
import de.theredend2000.advancedegghunt.managers.inventorymanager.sectionselection.SelectionSelectListMenu;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

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
        String section = Main.getInstance().getEggManager().getEggSectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()){
            if(Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).contains("FoundEggs."+id)){
                Collections.addAll(keys, Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids).getString("FoundEggs."+section+".Name"));
            }
        }

        if(e.getCurrentItem().getType().equals(Material.PAPER) && ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Selected Collection")){
            new SelectionSelectListMenu(Main.getPlayerMenuUtility(p)).open();
        }

        if (e.getCurrentItem().getType().equals(Material.BARRIER)) {
            p.closeInventory();
            p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
        }else if (e.getCurrentItem().getType().equals(Material.EMERALD_BLOCK)) {
            if(Main.getInstance().getRefreshCooldown().containsKey(p.getName())){
                if(Main.getInstance().getRefreshCooldown().get(p.getName()) > System.currentTimeMillis()){
                    p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.WAIT_REFRESH));
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventoryFailedSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                    return;
                }
            }
            Main.getInstance().getRefreshCooldown().put(p.getName(), System.currentTimeMillis()+ (3*1000));
            new EggInformationMenu(Main.getPlayerMenuUtility(p)).open(inventory.getItem(0).getItemMeta().getLocalizedName());
            p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
        }else if(e.getCurrentItem().getType().equals(Material.PLAYER_HEAD)){
            if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Left")){
                if (page == 0){
                    p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.FIRST_PAGE));
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventoryFailedSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }else{
                    page = page - 1;
                    super.open(id);
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
            }else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Right")){
                if (!((index + 1) >= keys.size())){
                    page = page + 1;
                    super.open(id);
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }else{
                    p.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LAST_PAGE));
                    p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventoryFailedSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
                }
            }else if (ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName()).equalsIgnoreCase("Back")){
                new EggListMenu(Main.getPlayerMenuUtility(p)).open();
                p.playSound(p.getLocation(),Main.getInstance().getSoundManager().playInventorySuccessSound(),Main.getInstance().getSoundManager().getSoundVolume(), 1);
            }
        }
    }

    @Override
    public void setMenuItems(String eggId) {
        inventory.setItem(0,new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).setDisplayname("§c").setLocalizedName(eggId).build());
        addMenuBorder(eggId);
        ArrayList<String> keys = new ArrayList<>();
        ArrayList<String> uuid = new ArrayList<>();
        String section = Main.getInstance().getEggManager().getEggSectionFromPlayerData(playerMenuUtility.getOwner().getUniqueId());
        for(UUID uuids : Main.getInstance().getEggDataManager().savedPlayers()){
            FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(uuids);
            if(playerConfig.contains("FoundEggs."+section+"."+eggId)){
                Collections.addAll(keys, playerConfig.getString("FoundEggs."+section+".Name"));
                Collections.addAll(uuid, String.valueOf(uuids));
            }
        }

        if(keys != null && !keys.isEmpty()) {
            for(int i = 0; i < getMaxItemsPerPage(); i++) {
                index = getMaxItemsPerPage() * page + i;
                if(index >= keys.size()) break;
                if (keys.get(index) != null){
                    String maxEggs = String.valueOf(Main.getInstance().getEggManager().getMaxEggs(section));
                    String date = Main.getInstance().getEggManager().getEggDateCollected(uuid.get(index), eggId,section);
                    String time = Main.getInstance().getEggManager().getEggTimeCollected(uuid.get(index),eggId,section);
                    String eggsFound = Main.getInstance().getPlayerEggDataManager().getPlayerData(UUID.fromString(uuid.get(index))).getString("FoundEggs."+section+".Count");
                    inventory.addItem(new ItemBuilder(XMaterial.PLAYER_HEAD).setOwner(keys.get(index)).setDisplayname("§6§l"+keys.get(index)+" §7("+uuid.get(index)+")").setLore("§7"+keys.get(index)+" has found the §2egg #"+eggId+"§7.","","§9Information of "+keys.get(index)+":","§7Eggs found: §6"+eggsFound+"/"+maxEggs,"","§9Collected:","§7Date: §6"+date,"§7Time: §6"+time).setLocalizedName(keys.get(index)).build());
                }
            }
        }else
            inventory.setItem(22, new ItemBuilder(XMaterial.RED_STAINED_GLASS).setDisplayname("§4§lNo Founds").setLore("§7No player has found this egg yet.").build());
    }
}

