package de.theredend2000.advancedhunt.util;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ItemSerializer {

    private static final int MAX_SERIALIZED_SIZE = 10 * 1024 * 1024; // 10MB limit

    public static String serialize(ItemStack item) {
        if (item == null) {
            throw new IllegalArgumentException("Item cannot be null");
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeObject(item);
            dataOutput.flush();
            return Base64Coder.encodeLines(outputStream.toByteArray());
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to serialize item: " + e.getMessage());
            return null;
        }
    }

    public static ItemStack deserialize(String data) {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }

        byte[] decodedData = Base64Coder.decodeLines(data);
        if (decodedData.length > MAX_SERIALIZED_SIZE) {
            throw new IllegalArgumentException("Serialized data exceeds maximum allowed size of " + MAX_SERIALIZED_SIZE + " bytes");
        }

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(decodedData);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            Object obj = dataInput.readObject();
            if (!(obj instanceof ItemStack)) {
                throw new IllegalArgumentException("Deserialized object is not an ItemStack");
            }
            return (ItemStack) obj;
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to deserialize item (IO error): " + e.getMessage());
            return null;
        } catch (ClassNotFoundException e) {
            Bukkit.getLogger().severe("Failed to deserialize item (class not found): " + e.getMessage());
            return null;
        }
    }
}
