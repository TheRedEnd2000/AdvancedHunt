package de.theredend2000.advancedhunt.util;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedhunt.platform.PlatformAccess;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;
    private String customId;
    private String skullTexture;
    private String skullOwnerName;
    private UUID skullOwnerUuid;

    private ItemMeta ensureMeta() {
        if (meta != null) {
            return meta;
        }
        meta = item.getItemMeta();
        if (meta != null) {
            return meta;
        }
        try {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
        } catch (Exception ignored) {
        }
        return meta;
    }

    public ItemBuilder(Material material) {
        if (material == null) {
            material = Material.STONE;
        }
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
        ensureMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
        ensureMeta();
        try {
            NBT.get(this.item, nbt -> {
                if (nbt.hasTag("custom_id")) {
                    this.customId = nbt.getString("custom_id");
                }
            });
        } catch (Exception ignored) {
        }
    }

    public ItemBuilder(XMaterial material) {
        this.item = material.parseItem();
        if (this.item == null) {
            this.item = new ItemStack(XMaterial.STONE.get()); // Fallback
        }
        this.meta = this.item.getItemMeta();
        ensureMeta();
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder setDisplayName(String name) {
        ItemMeta meta = ensureMeta();
        if (meta != null) {
            meta.setDisplayName(name);
        }
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        ItemMeta meta = ensureMeta();
        if (meta != null) {
            meta.setLore(Arrays.asList(lore));
        }
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        ItemMeta meta = ensureMeta();
        if (meta != null) {
            meta.setLore(lore);
        }
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        ItemMeta meta = ensureMeta();
        if (meta == null) {
            return this;
        }
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.add(line);
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level) {
        ItemMeta meta = ensureMeta();
        if (meta != null) {
            meta.addEnchant(enchantment, level, true);
        }
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        ItemMeta meta = ensureMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder hideTooltip(boolean tooltip) {
        ItemMeta meta = ensureMeta();
        meta.setDisplayName(" ");
        PlatformAccess.get().applyHideTooltip(meta, tooltip);
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        ItemMeta meta = ensureMeta();
        PlatformAccess.get().applyUnbreakable(meta, unbreakable);
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        ItemMeta meta = ensureMeta();
        PlatformAccess.get().applyCustomModelData(meta, data);
        return this;
    }

    public ItemBuilder setSkullOwner(UUID uuid) {
        this.skullOwnerUuid = uuid;
        return this;
    }

    public ItemBuilder setSkullOwner(String playerName) {
        if (playerName != null && !playerName.isEmpty()) {
            this.skullOwnerName = playerName;
        }
        return this;
    }
    
    public ItemBuilder setLeatherColor(Color color) {
        ItemMeta meta = ensureMeta();
        if (meta instanceof LeatherArmorMeta) {
            ((LeatherArmorMeta) meta).setColor(color);
        }
        return this;
    }

    public ItemBuilder setCustomId(String id) {
        this.customId = id;
        return this;
    }

    public boolean hasCustomId() {
        return customId != null;
    }

    public String getCustomId() {
        return customId;
    }

    public ItemBuilder setSkullTexture(String texture) {
        this.skullTexture = texture;
        return this;
    }

    public ItemStack build() {
        if (skullTexture != null || skullOwnerName != null || skullOwnerUuid != null) {
            item = PlatformAccess.get().ensurePlayerHeadItem(item);
        }

        ItemMeta meta = ensureMeta();
        if (meta != null) {
            if (skullOwnerUuid != null || skullOwnerName != null) {
                if (skullOwnerUuid != null) {
                    PlatformAccess.get().applySkullOwner(meta, skullOwnerUuid);
                }
                if (skullOwnerName != null) {
                    PlatformAccess.get().applySkullOwner(meta, skullOwnerName);
                }
            }
            item.setItemMeta(meta);
        }
        if (customId != null || skullTexture != null) {
            try {
                applyNbtData();
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to apply NBT data to item: " + e.getMessage());
            }
        }
        return item;
    }

    private void applyNbtData() {
        String version = Bukkit.getBukkitVersion().split("-")[0];
        VersionComparator comparator = new VersionComparator();

        if (comparator.isGreaterThanOrEqual(version, "1.20.5")) {
            apply1_20_5PlusNbt();
        } else {
            applyLegacyNbt();
        }
    }

    private void apply1_20_5PlusNbt() {
        if (skullTexture != null) {
            NBT.modifyComponents(item, nbt -> {
                ReadWriteNBT profile = nbt.getOrCreateCompound("minecraft:profile");
                profile.setUUID("id", UUID.randomUUID());
                ReadWriteNBT properties = profile.getCompoundList("properties").addCompound();
                properties.setString("name", "textures");
                properties.setString("value", skullTexture);
            });
        }
        if (customId != null) {
            NBT.modify(item, nbt -> {
                nbt.setString("custom_id", customId);
            });
        }
    }

    private void applyLegacyNbt() {
        NBT.modify(item, nbt -> {
            if (customId != null) {
                nbt.setString("custom_id", customId);
            }
            if (skullTexture != null) {
                ReadWriteNBT skullOwner = nbt.getOrCreateCompound("SkullOwner");
                skullOwner.setUUID("Id", UUID.randomUUID());
                ReadWriteNBT properties = skullOwner.getOrCreateCompound("Properties");
                ReadWriteNBT textures = properties.getCompoundList("textures").addCompound();
                textures.setString("Value", skullTexture);
            }
        });
    }
}
