package de.theredend2000.advancedhunt.util;

import de.tr7zw.nbtapi.*;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableItemNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import static de.theredend2000.advancedhunt.util.Constants.CustomIdKey;

public class ItemHelper {
    public static ItemStack setCustomId(ItemStack item, String id) {
        NBT.modify(item, nbt -> {
            nbt.setString(CustomIdKey, id);
        });
        return item;
    }

    public static boolean hasItemId(ItemStack item) {
        return NBT.get(item, (Function<ReadableItemNBT, Boolean>) nbt -> nbt.hasTag(CustomIdKey));
    }

    public static String getItemId(ItemStack item) {
        return NBT.get(item, (Function<ReadableItemNBT, String>) nbt -> nbt.getString(CustomIdKey));
    }

    public static String getSkullTexture(ItemStack item) {
        String base64Texture = null;
        String version = Bukkit.getBukkitVersion().split("-", 2)[0];

        if (VersionComparator.isGreaterThanOrEqual(version, "1.20.5")) {
            var components = NBT.itemStackToNBT(item).getOrCreateCompound("components");

            if (components == null) return null;
            final ReadableNBT skullOwnerCompound = components.getCompound("minecraft:profile");

            if (skullOwnerCompound == null) return null;
            ReadableNBTList<ReadWriteNBT> skullOwnerPropertiesCompound = skullOwnerCompound.getCompoundList("properties");

            for (ReadWriteNBT property : skullOwnerPropertiesCompound) {
                if (Objects.equals(property.getString("name"), "textures") && property.getString("value") != null) {
                    base64Texture = property.getString("value");
                    break;
                }
            }
        } else {
            base64Texture = NBT.get(item, nbt -> {
                final ReadableNBT skullOwnerCompound = nbt.getCompound("SkullOwner");

                if (skullOwnerCompound == null) return null;
                ReadableNBT skullOwnerPropertiesCompound = skullOwnerCompound.getCompound("Properties");

                if (skullOwnerPropertiesCompound == null) return null;
                ReadableNBTList<ReadWriteNBT> skullOwnerPropertiesTexturesCompound = skullOwnerPropertiesCompound.getCompoundList("textures");

                if (skullOwnerPropertiesTexturesCompound == null) return null;
                return skullOwnerPropertiesTexturesCompound.get(0).getString("Value");
            });
        }

        return convertSkinURLToBase64(extractSkinUrl(base64Texture));
    }

    private static String extractSkinUrl(String base64Texture) {
        if (base64Texture == null) return null;

        try {
            String decodedTexture = new String(Base64.getDecoder().decode(base64Texture), StandardCharsets.UTF_8);
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.loadFromString(decodedTexture);
            
            return yaml.getConfigurationSection("textures")
                       .getConfigurationSection("SKIN")
                       .getString("url");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String convertSkinURLToBase64(String skinURL) {
        if (skinURL == null || skinURL == "") return null;

        String modifiedURL = skinURL.replaceFirst("^http://textures\\.minecraft\\.net/texture/", "") + "\"}}}";
        return Base64.getEncoder().encodeToString(modifiedURL.getBytes());
    }

    public static String convertItemIntoCommand(ItemStack itemStack){
        String version = Bukkit.getBukkitVersion().split("-", 2)[0];

        if (VersionComparator.isGreaterThanOrEqual(version, "1.20.5")) {
            NBTCompound nbtCompound = (NBTCompound) NBT.itemStackToNBT(itemStack);

            String components = "";
            if (nbtCompound.hasTag("components")) {
                NBTCompound componentsNBT = nbtCompound.getCompound("components");
                if (componentsNBT != null && !componentsNBT.getKeys().isEmpty()) {
                    StringBuilder componentBuilder = new StringBuilder();

                    for (String key : componentsNBT.getKeys()) {
                        if (componentBuilder.length() > 0) {
                            componentBuilder.append(",");
                        }

                        componentBuilder.append(key).append("=");

                        NBTType type = componentsNBT.getType(key);

                        if (type == NBTType.NBTTagCompound) {
                            NBTCompound compound = componentsNBT.getCompound(key);
                            if (key.contains("custom_name")) {
                                String json = convertNBTToJSON(compound);
                                componentBuilder.append("'").append(json).append("'");
                            } else {
                                componentBuilder.append(compound.toString());
                            }
                        } else if (type == NBTType.NBTTagList) {
                            NBTCompoundList list = componentsNBT.getCompoundList(key);
                            if (key.contains("lore")) {
                                componentBuilder.append("[");
                                for (int i = 0; i < list.size(); i++) {
                                    if (i > 0) componentBuilder.append(",");
                                    String json = convertNBTToJSON(list.get(i));
                                    componentBuilder.append("'").append(json).append("'");
                                }
                                componentBuilder.append("]");
                            } else {
                                componentBuilder.append(list.toString());
                            }
                        } else if (type == NBTType.NBTTagString) {
                            String strValue = componentsNBT.getString(key);
                            componentBuilder.append("'").append(strValue.replace("'", "\\'")).append("'");
                        } else if (type == NBTType.NBTTagInt) {
                            componentBuilder.append(componentsNBT.getInteger(key));
                        } else {
                            Object value = componentsNBT.getString(key);
                            if (value != null && !value.toString().isEmpty()) {
                                componentBuilder.append(value);
                            }
                        }
                    }

                    components = "[" + componentBuilder.toString() + "]";
                }
            }

            return MessageFormat.format("minecraft:give %PLAYER% {0}{1} {2}",
                    itemStack.getType().name().toLowerCase(),
                    components.isEmpty() ? "" : components,
                    itemStack.getAmount());
        } else {
            String itemNBT = NBT.get(itemStack, Object::toString);
            return MessageFormat.format("minecraft:give %PLAYER% {0}{1} {2}",
                    itemStack.getType().name().toLowerCase(),
                    itemNBT,
                    itemStack.getAmount());
        }
    }

    private static String convertNBTToJSON(NBTCompound compound) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (String key : compound.getKeys()) {
            if (!first) json.append(",");
            first = false;

            json.append("\"").append(key).append("\":");

            NBTType type = compound.getType(key);
            if (type == NBTType.NBTTagString) {
                json.append("\"").append(compound.getString(key)).append("\"");
            } else if (type == NBTType.NBTTagByte) {
                byte b = compound.getByte(key);
                json.append(b == 1 ? "true" : "false");
            } else if (type == NBTType.NBTTagInt) {
                json.append(compound.getInteger(key));
            } else if (type == NBTType.NBTTagCompound) {
                json.append(convertNBTToJSON(compound.getCompound(key)));
            } else {
                json.append(compound.getString(key));
            }
        }

        json.append("}");
        return json.toString();
    }

    public static ItemStack getItemStackFromBlock(Block block) {
        Collection<ItemStack> drops = block.getDrops();

        if (!drops.isEmpty()) {
            return drops.iterator().next();
        }

        return null;
    }
}
