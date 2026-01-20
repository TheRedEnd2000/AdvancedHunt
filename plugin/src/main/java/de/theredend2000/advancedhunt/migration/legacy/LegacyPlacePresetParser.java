package de.theredend2000.advancedhunt.migration.legacy;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.model.PlaceItem;
import de.theredend2000.advancedhunt.util.ItemBuilder;
import de.theredend2000.advancedhunt.util.ItemSerializer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * Parses legacy place presets from the config.yml Place: section.
 * These are the default treasure appearance items (player heads with textures).
 */
public final class LegacyPlacePresetParser {

    private static final String DEFAULT_GROUP = "Default";
    /**
     * Legacy config stores texture suffixes without this prefix to save space.
     * The prefix must be added when creating skull textures.
     */
    private static final String TEXTURE_PREFIX = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUv";

    private LegacyPlacePresetParser() {
    }

    /**
     * Parses place presets from the legacy config.yml file.
     *
     * @param legacyRootFolder The plugin data folder containing the legacy config.yml
     * @return List of parsed place presets
     */
    public static List<PlaceItem> parseAll(File legacyRootFolder) {
        File configFile = new File(legacyRootFolder, "config.yml");
        if (!configFile.exists()) {
            return Collections.emptyList();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection placeSection = config.getConfigurationSection("Place");
        if (placeSection == null) {
            return Collections.emptyList();
        }

        List<PlaceItem> presets = new ArrayList<>();
        Set<String> keys = placeSection.getKeys(false);

        for (String key : keys) {
            ConfigurationSection entry = placeSection.getConfigurationSection(key);
            if (entry == null) continue;

            PlaceItem preset = parseEntry(key, entry);
            if (preset != null) {
                presets.add(preset);
            }
        }

        return presets;
    }

    private static PlaceItem parseEntry(String key, ConfigurationSection entry) {
        String typeStr = entry.getString("type", "PLAYER_HEAD");
        String texture = entry.getString("texture");

        XMaterial material;
        try {
            material = XMaterial.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            material = XMaterial.PLAYER_HEAD;
        }

        ItemStack item;
        if (material == XMaterial.PLAYER_HEAD && texture != null && !texture.isEmpty()) {
            // Legacy config stores texture suffix only; add the base64 prefix
            String fullTexture = TEXTURE_PREFIX + texture;
            item = new ItemBuilder(XMaterial.PLAYER_HEAD)
                    .setSkullTexture(fullTexture)
                    .build();
        } else {
            item = material.parseItem();
            if (item == null) {
                item = XMaterial.PLAYER_HEAD.parseItem();
            }
        }

        String serialized = ItemSerializer.serialize(item);
        if (serialized == null) {
            return null;
        }

        // Use index as name (e.g., "Treasure 0", "Treasure 1")
        String name = "Treasure " + key;

        return new PlaceItem(UUID.randomUUID(), DEFAULT_GROUP, name, serialized);
    }
}
