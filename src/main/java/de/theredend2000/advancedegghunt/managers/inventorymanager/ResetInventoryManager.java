package de.theredend2000.advancedegghunt.managers.inventorymanager;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.enums.Requirements;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.Random;

public class ResetInventoryManager {

    private Main plugin;

    public ResetInventoryManager(){
        this.plugin = Main.getInstance();
    }

    public void createSelectInventory(Player player, String section) {
        Inventory inventory = Bukkit.createInventory(player,54,"Reset - Selection");
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(XMaterial.GRAY_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs(section);
        inventory.setItem(4,new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(plugin.getEggManager().getRandomEggTexture(new Random().nextInt(7))).setDisplayname("§6"+section).build());
        String overall = plugin.getRequirementsManager().getConvertedTime(section);
        inventory.setItem(10, new ItemBuilder(XMaterial.REDSTONE).setDisplayname("§6Reset - Year").setLore("§7Current: §b"+placedEggs.getInt("Reset.Year")+"Y","§7Overall: §6"+overall,"","§eLEFT-CLICK add one.","§eMIDDLE-CLICK reset it.","§eRIGHT-CLICK remove one.").build());
        inventory.setItem(11, new ItemBuilder(XMaterial.REDSTONE).setDisplayname("§6Reset - Month").setLore("§7Current: §b"+placedEggs.getInt("Reset.Month")+"M","§7Overall: §6"+overall,"","§eLEFT-CLICK add one.","§eMIDDLE-CLICK reset it.","§eRIGHT-CLICK remove one.").build());
        inventory.setItem(12, new ItemBuilder(XMaterial.REDSTONE).setDisplayname("§6Reset - Day").setLore("§7Current: §b"+placedEggs.getInt("Reset.Day")+"d","§7Overall: §6"+overall,"","§eLEFT-CLICK add one.","§eMIDDLE-CLICK reset it.","§eRIGHT-CLICK remove one.").build());
        inventory.setItem(13, new ItemBuilder(XMaterial.REDSTONE).setDisplayname("§6Reset - Hour").setLore("§7Current: §b"+placedEggs.getInt("Reset.Hour")+"h","§7Overall: §6"+overall,"","§eLEFT-CLICK add one.","§eMIDDLE-CLICK reset it.","§eRIGHT-CLICK remove one.").build());
        inventory.setItem(14, new ItemBuilder(XMaterial.REDSTONE).setDisplayname("§6Reset - Minute").setLore("§7Current: §b"+placedEggs.getInt("Reset.Minute")+"m","§7Overall: §6"+overall,"","§eLEFT-CLICK add one.","§eMIDDLE-CLICK reset it.","§eRIGHT-CLICK remove one.").build());
        inventory.setItem(15, new ItemBuilder(XMaterial.REDSTONE).setDisplayname("§6Reset - Second").setLore("§7Current: §b"+placedEggs.getInt("Reset.Second")+"s","§7Overall: §6"+overall,"","§eLEFT-CLICK add one.","§eMIDDLE-CLICK reset it.","§eRIGHT-CLICK remove one.").build());
        inventory.setItem(37, new ItemBuilder(XMaterial.RED_TERRACOTTA).setDisplayname("§cReset all").setLore("§eClick to reset all.").build());
        inventory.setItem(45, new ItemBuilder(XMaterial.PLAYER_HEAD).setSkullOwner(Main.getTexture("ODFjOTZhNWMzZDEzYzMxOTkxODNlMWJjN2YwODZmNTRjYTJhNjUyNzEyNjMwM2FjOGUyNWQ2M2UxNmI2NGNjZiJ9fX0=")).setDisplayname("§eBack").build());
        inventory.setItem(49, new ItemBuilder(XMaterial.BARRIER).setDisplayname("§4Close").build());
        player.openInventory(inventory);
    }
}
