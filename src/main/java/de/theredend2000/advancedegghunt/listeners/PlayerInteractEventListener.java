package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.eggmanager.EggManager;
import de.theredend2000.advancedegghunt.managers.extramanager.ExtraManager;
import de.theredend2000.advancedegghunt.managers.soundmanager.SoundManager;
import de.theredend2000.advancedegghunt.util.enums.Permission;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

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
        for (String collections : Main.getInstance().getEggDataManager().savedEggCollections()) {
            if (!eggManager.getEggCollection(event.getClickedBlock()).equals(collections)) {
                continue;
            }
            String id = eggManager.getEggID(event.getClickedBlock(), collections);
            if(Main.getInstance().getPermissionManager().checkPermission(player, Permission.OpenRewards) && player.isSneaking()){
                Main.getInstance().getEggRewardsInventory().open(player,id,collections);
                return;
            }
            FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collections);
            if(!placedEggs.getBoolean("Enabled")){
                player.sendMessage(messageManager.getMessage(MessageKey.COLLECTION_DISABLED));
                return;
            }
            if (!eggManager.hasFound(player, id, collections)) {
                if(!Main.getInstance().getRequirementsManager().canBeAccessed(collections)){
                    player.sendMessage(messageManager.getMessage(MessageKey.EGG_NOT_ACCESSED));
                    return;
                }
                if(Main.getInstance().getRequirementsManager().getOverallTime(collections) <= 0)
                    Main.getInstance().getPlayerEggDataManager().setResetTimer(player.getUniqueId(), collections, id);
                eggManager.saveFoundEggs(player, event.getClickedBlock(), id, collections);
                Location loc = new Location(event.getClickedBlock().getWorld(), event.getClickedBlock().getLocation().getX(), event.getClickedBlock().getLocation().getY(), event.getClickedBlock().getLocation().getZ());
                if (Main.getInstance().getPluginConfig().getShowFireworkAfterEggFound())
                    extraManager.spawnFireworkRocket(loc.add(0.5, 1.5, 0.5));
                if (eggManager.checkFoundAll(player, collections)) {
                    player.playSound(player.getLocation(), soundManager.playAllEggsFound(), 1, 1);
                    if(!placedEggs.contains("PlacedEggs."+id+".Rewards")) continue;
                    for (String commandID : placedEggs.getConfigurationSection("PlacedEggs."+id+".Rewards.").getKeys(false)) {
                        boolean enabled = placedEggs.getBoolean("PlacedEggs."+id+".Rewards." + commandID + ".enabled");
                        if (placedEggs.getBoolean("PlacedEggs."+id+".Rewards." + commandID + ".foundAll") && enabled) {
                            String cmd = placedEggs.getString("PlacedEggs."+id+".Rewards." + commandID + ".command");
                            Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(), cmd.replaceAll("%PLAYER%", player.getName()).replaceAll("&", "§").replaceAll("%EGGS_FOUND%", String.valueOf(eggManager.getEggsFound(player, collections))).replaceAll("%EGGS_MAX%", String.valueOf(eggManager.getMaxEggs(collections))).replaceAll("%PREFIX%", Main.PREFIX));
                        }
                    }
                } else {
                    player.playSound(player.getLocation(), soundManager.playEggFoundSound(), soundManager.getSoundVolume(), 1);
                    if (Main.getInstance().getPluginConfig().getPlayerFoundOneEggRewards()) {
                        if(!placedEggs.contains("PlacedEggs."+id+".Rewards")) continue;
                        for (String commandID : placedEggs.getConfigurationSection("PlacedEggs."+id+".Rewards.").getKeys(false)) {
                            boolean enabled = placedEggs.getBoolean("PlacedEggs."+id+".Rewards." + commandID + ".enabled");
                            if (!placedEggs.getBoolean("PlacedEggs."+id+".Rewards." + commandID + ".foundAll") && enabled) {
                                String cmd = placedEggs.getString("PlacedEggs."+id+".Rewards." + commandID + ".command");
                                Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(), cmd.replaceAll("%PLAYER%", player.getName()).replaceAll("&", "§").replaceAll("%EGGS_FOUND%", String.valueOf(eggManager.getEggsFound(player, collections))).replaceAll("%EGGS_MAX%", String.valueOf(eggManager.getMaxEggs(collections))).replaceAll("%PREFIX%", Main.PREFIX));
                            }
                        }
                    }
                }
            } else {
                player.sendMessage(messageManager.getMessage(MessageKey.EGG_ALREADY_FOUND));
                player.playSound(player.getLocation(), soundManager.playEggAlreadyFoundSound(), soundManager.getSoundVolume(), 1);
            }
        }
    }
}
