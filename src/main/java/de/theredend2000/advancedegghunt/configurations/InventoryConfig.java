package de.theredend2000.advancedegghunt.configurations;

import de.theredend2000.advancedegghunt.util.saveinventory.Serialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.text.MessageFormat;
import java.util.TreeMap;
import java.util.UUID;

public class InventoryConfig extends Configuration {
    private static TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    public InventoryConfig(JavaPlugin plugin, UUID configName) {
        super(plugin, MessageFormat.format("/invs/{0}.yml", configName.toString()),  false);
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    public void saveData() {
        saveConfig();
    }

    public ItemStack[][] getInventory() {
        String[] values = new String[]{getConfig().getString("inv"), getConfig().getString("armor")};

        return Serialization.base64toInv(values);
    }

    public void setInventory(PlayerInventory inventory) {
        String[] values = Serialization.invToBase64(inventory);
        getConfig().set("inv", values[0]);
        getConfig().set("armor", values[1]);
    }

    @Override
    public void registerUpgrader() {
        getUpgrader().put(2.9, (oldConfig, NewConfig) -> {
            File file = configFile.getParentFile();
            deleteDir(file);
        });
    }
}
