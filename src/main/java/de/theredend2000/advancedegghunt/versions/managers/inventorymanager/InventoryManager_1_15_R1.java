package de.theredend2000.advancedegghunt.versions.managers.inventorymanager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class InventoryManager_1_15_R1 implements InventoryManager {


    public void createEggsSettingsInventory(Player player) {
        Inventory inventory = Bukkit.createInventory(player,54,"Advanced Egg Settings");
        int[] glass = new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,27,35,36,44,45,46,47,48,50,51,52,53};
        for (int i = 0; i<glass.length;i++){inventory.setItem(glass[i], new ItemBuilder(Material.RED_STAINED_GLASS_PANE).setDisplayname("§c").build());}
        inventory.setItem(10, new ItemBuilder(Material.GOLD_INGOT).setDisplayname("§3One egg found reward").setLore("§7If this function is activated","§7all commands entered in the config are executed.","",Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundOneEggRewards") ? "§a§l✔ Enabled" : "§c§l❌ Disabled","§eClick to toggle.").setLocalizedName("settings.foundoneegg").withGlow(Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundOneEggRewards")).build());
        inventory.setItem(11, new ItemBuilder(Material.EMERALD).setDisplayname("§3All eggs found reward").setLore("§7If this function is activated","§7all commands entered in the config are executed.","",Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundAllEggsReward") ? "§a§l✔ Enabled" : "§c§l❌ Disabled","§eClick to toggle.").setLocalizedName("settings.foundalleggs").withGlow(Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundAllEggsReward")).build());
        inventory.setItem(12, new ItemBuilder(Material.FIREWORK_ROCKET).setDisplayname("§3Updater").setLore("§7If this function is activated","§7all operators will get an information","§7if a new plugin version is out.","",Main.getInstance().getConfig().getBoolean("Settings.Updater") ? "§a§l✔ Enabled" : "§c§l❌ Disabled","§eClick to toggle.").setLocalizedName("settings.updater").withGlow(Main.getInstance().getConfig().getBoolean("Settings.Updater")).build());
        inventory.setItem(13, new ItemBuilder(Material.COMMAND_BLOCK).setDisplayname("§3Command feedback").setLore("§7If this function is activated","§7no more commands are sent","§7to the operators listed in the console.","",Main.getInstance().getConfig().getBoolean("Settings.DisableCommandFeedback") ? "§a§l✔ Enabled" : "§c§l❌ Disabled","§eClick to toggle.").setLocalizedName("settings.commandfeedback").withGlow(Main.getInstance().getConfig().getBoolean("Settings.DisableCommandFeedback")).build());
        inventory.setItem(49, new ItemBuilder(Material.BARRIER).setDisplayname("§4Close").setLocalizedName("settings.close").build());
        player.openInventory(inventory);
    }
}
