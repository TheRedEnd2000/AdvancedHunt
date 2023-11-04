package de.theredend2000.advancedegghunt.util;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.UUID;

public class ItemBuilder {
    private ItemMeta itemMeta;
    private ItemStack itemStack;
    public ItemBuilder(Material mat){
        itemStack = new ItemStack(mat);
        itemMeta = itemStack.getItemMeta();
    }
    public ItemBuilder setDisplayname(String s){
        itemMeta.setDisplayName(s);
        return this;
    }
    public ItemBuilder setLocalizedName(String s){
        itemMeta.setLocalizedName(s);
        return this;
    }
    public ItemBuilder setOwner(String name){
        SkullMeta skullMeta = (SkullMeta) this.itemMeta;
        skullMeta.setOwner(name);
        return this;
    }
    public ItemBuilder withGlow(boolean s){
        if(s) {
            itemMeta.addEnchant(Enchantment.LURE, 1, true);
            itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }
    public ItemBuilder setSoulbound(boolean soulbound){
        if(soulbound){
            itemMeta.getPersistentDataContainer().set(
                    NamespacedKey.minecraft("soulbound"), PersistentDataType.BYTE, (byte) 1);
        }
        return this;
    }
    public ItemBuilder setLore(String... s){
        itemMeta.setLore(Arrays.asList(s));
        return this;
    }
    public ItemBuilder setUnbreakable(boolean s){
        itemMeta.setUnbreakable(s);
        return this;
    }
    public ItemBuilder addItemFlags(ItemFlag... s){
        itemMeta.addItemFlags(s);
        return this;
    }

    @Override
    public String toString() {
        return "ItemBuilder{" +
                "itemMeta=" + itemMeta +
                ", itemStack=" + itemStack +
                '}';
    }
    public ItemStack build(){
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
    public ItemBuilder setSkullOwner(String texture) {
        try {
            SkullMeta skullMeta = (SkullMeta) this.itemMeta;
            GameProfile profile = new GameProfile(UUID.randomUUID(), texture);
            profile.getProperties().put("textures", new Property("textures", texture));
            Field field = skullMeta.getClass().getDeclaredField("profile");
            field.setAccessible(true);
            field.set(skullMeta, profile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return this;
    }
    public ItemBuilder setColor(Color color) {
        try {
            LeatherArmorMeta armorMeta = (LeatherArmorMeta) this.itemMeta;
            armorMeta.setColor(color);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return this;
    }

    public ItemBuilder addEnchant(Enchantment ench, int level) {
        itemMeta.addEnchant(ench, level, true);
        return this;
    }

}
