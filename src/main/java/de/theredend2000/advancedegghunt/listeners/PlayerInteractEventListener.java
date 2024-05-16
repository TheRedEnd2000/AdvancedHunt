package de.theredend2000.advancedegghunt.listeners;

import com.cryptomorin.xseries.XMaterial;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.ExtraManager;
import de.theredend2000.advancedegghunt.managers.SoundManager;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggrewards.individual.IndividualEggRewardsMenu;
import de.theredend2000.advancedegghunt.util.enums.Permission;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import de.tr7zw.changeme.nbtapi.NBT;
import de.tr7zw.changeme.nbtapi.iface.ReadWriteItemNBT;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;

public class PlayerInteractEventListener implements Listener {

    private MessageManager messageManager;

    public PlayerInteractEventListener() {
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        messageManager = Main.getInstance().getMessageManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteractEvent(PlayerInteractEvent event) {
        EggManager eggManager = Main.getInstance().getEggManager();
        SoundManager soundManager = Main.getInstance().getSoundManager();
        ExtraManager extraManager = Main.getInstance().getExtraManager();
        Player player = event.getPlayer();
        Action action = event.getAction();

        if ((!action.equals(Action.RIGHT_CLICK_BLOCK) ||
                    !Main.getInstance().getPluginConfig().getRightClickEgg() ||
                    event.getHand() != EquipmentSlot.HAND) &&
                (!action.equals(Action.LEFT_CLICK_BLOCK) ||
                    !Main.getInstance().getPluginConfig().getLeftClickEgg()) ||
                event.getClickedBlock() == null ||
                !eggManager.containsEgg(event.getClickedBlock()) ||
                Main.getInstance().getPlaceEggsPlayers().contains(player)) {
            return;
        }
        for (String collection : Main.getInstance().getEggDataManager().savedEggCollections()) {
            if (!eggManager.getEggCollection(event.getClickedBlock()).equals(collection)) {
                continue;
            }
            String id = eggManager.getEggID(event.getClickedBlock(), collection);
            if(Main.getInstance().getPermissionManager().checkPermission(player, Permission.OpenRewards) && player.isSneaking()){
                new IndividualEggRewardsMenu(Main.getPlayerMenuUtility(player)).open(id, collection);
                return;
            }

            FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
            if(!placedEggs.getBoolean("Enabled")){
                player.sendMessage(messageManager.getMessage(MessageKey.COLLECTION_DISABLED));
                return;
            }
            if (eggManager.hasFound(player, id, collection)) {
                player.sendMessage(messageManager.getMessage(MessageKey.EGG_ALREADY_FOUND));
                player.playSound(player.getLocation(), soundManager.playEggAlreadyFoundSound(), soundManager.getSoundVolume(), 1);
                return;
            }

            if(!Main.getInstance().getRequirementsManager().canBeAccessed(collection)){
                player.sendMessage(messageManager.getMessage(MessageKey.EGG_NOT_ACCESSED));
                return;
            }
            if(eggManager.isMarkedAsFound(collection,id)){
                player.sendMessage(messageManager.getMessage(MessageKey.EGG_ALREADY_FOUND_BY_PLAYER));
                return;
            }

            if(Main.getInstance().getRequirementsManager().getOverallTime(collection) > 0)
                Main.getInstance().getPlayerEggDataManager().setResetTimer(player.getUniqueId(), collection, id);

            eggManager.saveFoundEggs(player, event.getClickedBlock(), id, collection);
            Location loc = new Location(event.getClickedBlock().getWorld(), event.getClickedBlock().getLocation().getX(), event.getClickedBlock().getLocation().getY(), event.getClickedBlock().getLocation().getZ());

            if (Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound())
                extraManager.spawnFireworkRocket(loc.add(0.5, 1.5, 0.5));
            if(placedEggs.getBoolean("OnePlayer") && !eggManager.isMarkedAsFound(collection,id))
                eggManager.markEggAsFound(collection,id,true);
            if (Main.getInstance().getPluginConfig().getPlayerFoundOneEggRewards()) {
                player.playSound(player.getLocation(), soundManager.playEggFoundSound(), soundManager.getSoundVolume(), 1);
                if(!placedEggs.contains("PlacedEggs." + id + ".Rewards.")) continue;
                for (String commandID : placedEggs.getConfigurationSection("PlacedEggs." + id + ".Rewards.").getKeys(false)) {
                    boolean enabled = placedEggs.getBoolean("PlacedEggs." + id + ".Rewards." + commandID + ".enabled");
                    if (enabled) {
                        String cmd = placedEggs.getString("PlacedEggs." + id + ".Rewards." + commandID + ".command");
                        double chance = placedEggs.getDouble("PlacedEggs." + id + ".Rewards." + commandID + ".chance") / 100;
                        double random = new Random().nextDouble();
                        boolean startsWithGive = cmd.toLowerCase().startsWith("give") || cmd.toLowerCase().startsWith("minecraft:give");
                        if(random < chance) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceAll("%PLAYER%", player.getName()).replaceAll("&", "§").replaceAll("%EGGS_FOUND%", String.valueOf(eggManager.getEggsFound(player, collection))).replaceAll("%EGGS_MAX%", String.valueOf(eggManager.getMaxEggs(collection))).replaceAll("%PREFIX%", Main.PREFIX));
                            if(startsWithGive)
                                player.sendMessage(messageManager.getMessage(MessageKey.RARITY_MESSAGE).replaceAll("%RARITY%",Main.getInstance().getRarityManager().getRarity(chance*100)).replaceAll("%ITEM%",getItemName(cmd).getType().name()).replaceAll("%COUNT%",String.valueOf(getItemCount(cmd))));
                        }
                    }
                }
            }
            if (eggManager.checkFoundAll(player, collection)) {
                player.playSound(player.getLocation(), soundManager.playAllEggsFound(), 1, 1);
                if(!placedEggs.contains("GlobalRewards.")) continue;
                for (String commandID : placedEggs.getConfigurationSection("GlobalRewards.").getKeys(false)) {
                    boolean enabled = placedEggs.getBoolean("GlobalRewards." + commandID + ".enabled");
                    if (enabled) {
                        String cmd = placedEggs.getString("GlobalRewards." + commandID + ".command");
                        double chance = placedEggs.getDouble("GlobalRewards." + commandID + ".chance") / 100;
                        double random = new Random().nextDouble();
                        boolean startsWithGive = cmd.toLowerCase().startsWith("give") || cmd.toLowerCase().startsWith("minecraft:give");
                        if(random < chance) {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replaceAll("%PLAYER%", player.getName()).replaceAll("&", "§").replaceAll("%EGGS_FOUND%", String.valueOf(eggManager.getEggsFound(player, collection))).replaceAll("%EGGS_MAX%", String.valueOf(eggManager.getMaxEggs(collection))).replaceAll("%PREFIX%", Main.PREFIX));
                            if(startsWithGive)
                                player.sendMessage(messageManager.getMessage(MessageKey.RARITY_MESSAGE).replaceAll("%RARITY%",Main.getInstance().getRarityManager().getRarity(chance*100)).replaceAll("%ITEM%",getItemName(cmd).getType().name()).replaceAll("%COUNT%",String.valueOf(getItemCount(cmd))));
                        }
                    }
                }
            }
        }
    }

    private ItemStack getItemName(String command){
        boolean startsWithGive = command.toLowerCase().startsWith("give") || command.toLowerCase().startsWith("minecraft:give");
        ItemStack itemStack = XMaterial.PAPER.parseItem();
        if (startsWithGive) {
            String[] parts = command.split(" ");

            if (parts.length >= 2 && (parts[0].equalsIgnoreCase("minecraft:give") || parts[0].equalsIgnoreCase("give"))) {
                String materialName = parts[2];

                itemStack = getItem(materialName);
            }
        }
        return itemStack;
    }

    private int getItemCount(String command){
        boolean startsWithGive = command.toLowerCase().startsWith("give") || command.toLowerCase().startsWith("minecraft:give");
        int endcount = 1;
        if (startsWithGive) {
            String[] parts = command.split(" ");

            if (parts.length >= 4 && (parts[0].equalsIgnoreCase("minecraft:give") || parts[0].equalsIgnoreCase("give"))) {
                int count = Integer.parseInt(parts[3]);

                endcount = count;
            }
        }
        return endcount;
    }

    public ItemStack getItem(String itemString) {
        int metaDataStartIndex = itemString.indexOf('{');
        int metaDataEndIndex = itemString.lastIndexOf('}'); //TODO gets the first } and not the last idk why
        if (metaDataEndIndex == -1) metaDataEndIndex = itemString.length() - 1;
        else metaDataEndIndex += 1;

        Optional<XMaterial> material;
        if (metaDataStartIndex == -1){
            material = XMaterial.matchXMaterial(itemString);
            if (material.isEmpty()) return XMaterial.PAPER.parseItem();
            return material.get().parseItem();
        }

        material = XMaterial.matchXMaterial(itemString.substring(0, metaDataStartIndex));

        if (material.isEmpty()) return XMaterial.PAPER.parseItem();

        var json = itemString.substring(metaDataStartIndex, metaDataEndIndex);
        var item = material.get().parseItem();
        NBT.modify(item, (Consumer<ReadWriteItemNBT>) nbt -> nbt.mergeCompound(NBT.parseNBT(json)));

        return item;
    }
}
