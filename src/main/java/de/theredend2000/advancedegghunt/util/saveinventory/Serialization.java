package de.theredend2000.advancedegghunt.util.saveinventory;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class Serialization {
    public static String[] invToBase64(PlayerInventory inv){
        String content = toBase64(inv.getContents());
        String armor = toBase64(inv.getArmorContents());

        return new String[] {content, armor};
    }

    public static ItemStack[][] base64toInv(String[] values){
        ItemStack[] content = fromBase64(values[0]);
        ItemStack[] armor = fromBase64(values[1]);
        return new ItemStack[][] {content, armor};
    }

    public static String toBase64(ItemStack[] items){
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            BukkitObjectOutputStream dadaOut = new BukkitObjectOutputStream(out);

            dadaOut.writeInt(items.length);

            for(ItemStack item : items){
                dadaOut.writeObject(item);
            }
            dadaOut.close();
            return Base64Coder.encodeLines(out.toByteArray());
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public static ItemStack[] fromBase64(String data){
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(Base64Coder.decodeLines(data));
            BukkitObjectInputStream dataIn = new BukkitObjectInputStream(in);

            ItemStack[] items = new ItemStack[dataIn.readInt()];

            for(int i = 0; i < items.length; i++){
                items[i] = (ItemStack) dataIn.readObject();
            }
            dataIn.close();
            return items;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
