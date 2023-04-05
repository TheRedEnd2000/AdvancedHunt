package de.theredend2000.advancedegghunt.versions.managers.inventorymanager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class InventoryManager_1_19_R1 implements InventoryManager {


    public void createEggsListInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(player,54,"Eggs list");
        setChests(inventory);
        player.openInventory(inventory);
    }

    public void setChests(Inventory inventory){
        if(Main.getInstance().eggs.contains("Eggs.")){
            for(String keys : Main.getInstance().eggs.getConfigurationSection("Eggs.").getKeys(false)){
                String x = Main.getInstance().eggs.getString("Eggs."+keys+".X");
                String y = Main.getInstance().eggs.getString("Eggs."+keys+".Y");
                String z = Main.getInstance().eggs.getString("Eggs."+keys+".Z");
                inventory.addItem(new ItemBuilder(Main.getInstance().getMaterial(Main.getInstance().getConfig().getString("Settings.ItemInEggList"))).setDisplayname("§2§lEgg §7(ID#"+keys+")").setLore("§9Location:","§7X: §e"+x,"§7Y: §e"+y,"§7Z: §e"+z,"","§eClick to teleport.").setLocalizedName(keys).build());
            }
        }else
            inventory.setItem(22, new ItemBuilder(Material.BARRIER).setDisplayname("§4§lNo Eggs Placed").build());
    }
}
