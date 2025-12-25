package de.theredend2000.advancedhunt.util;

import com.cryptomorin.xseries.XMaterial;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;
    private String customId;
    private String skullTexture;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(ItemStack item) {
        this.item = item.clone();
        this.meta = this.item.getItemMeta();
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
            this.item = new ItemStack(Material.STONE); // Fallback
        }
        this.meta = this.item.getItemMeta();
    }

    public ItemBuilder setAmount(int amount) {
        item.setAmount(amount);
        return this;
    }

    public ItemBuilder setDisplayName(String name) {
        meta.setDisplayName(name);
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        meta.setLore(Arrays.asList(lore));
        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder addLoreLine(String line) {
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        }
        lore.add(line);
        meta.setLore(lore);
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level) {
        meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder addItemFlags(ItemFlag... flags) {
        meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        meta.setUnbreakable(unbreakable);
        return this;
    }

    public ItemBuilder setCustomModelData(int data) {
        meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder setSkullOwner(String owner) {
        if (meta instanceof SkullMeta) {
            ((SkullMeta) meta).setOwner(owner);
        }
        return this;
    }
    
    public ItemBuilder setLeatherColor(Color color) {
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
        item.setItemMeta(meta);
        if (customId != null || skullTexture != null) {
            try {
                String version = Bukkit.getBukkitVersion().split("-")[0];
                VersionComparator comparator = new VersionComparator();
                if (comparator.isGreaterThanOrEqual(version, "1.20.5")) {
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
                } else {
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
            } catch (Exception ignored) {
            }
        }
        return item;
    }
}
