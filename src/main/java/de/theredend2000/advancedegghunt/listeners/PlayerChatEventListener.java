package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggfoundrewardmenu.EggRewardMenu;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import de.theredend2000.advancedegghunt.util.messages.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class PlayerChatEventListener implements Listener {

    private MessageManager messageManager;

    public PlayerChatEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        runTimeForPlayers();
        messageManager = Main.getInstance().getMessageManager();
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event){
        Player player = event.getPlayer();
        if (!Main.getInstance().getPlayerAddCommand().containsKey(player)) {
            return;
        }
        event.setCancelled(true);
        if (event.getMessage().equalsIgnoreCase("cancel")) {
            Main.getInstance().getPlayerAddCommand().remove(player);
            player.sendMessage(messageManager.getMessage(MessageKey.COMMAND_CANCEL));
            new EggRewardMenu(Main.getPlayerMenuUtility(player)).open();
            return;
        }
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(player.getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        if (!placedEggs.contains("Edit." + player.getUniqueId())) {
            String id = null;
            if (placedEggs.contains("Rewards.")) {
                ConfigurationSection rewardsSection = placedEggs.getConfigurationSection("Rewards.");
                int nextNumber = 0;
                Set<String> keys = rewardsSection.getKeys(false);
                if (!keys.isEmpty()) {
                    for (int i = 0; i <= keys.size(); i++) {
                        String key = Integer.toString(i);
                        if (!keys.contains(key)) {
                            nextNumber = i;
                            break;
                        }
                    }
                }
                setConfiguration(String.valueOf(nextNumber), event.getMessage(), player);
                player.sendMessage(messageManager.getMessage(MessageKey.COMMAND_ADD).replaceAll("%ID%", String.valueOf(nextNumber)));
                id = String.valueOf(nextNumber);
            } else {
                setConfiguration("0", event.getMessage(), player);
                player.sendMessage(messageManager.getMessage(MessageKey.COMMAND_ADD).replaceAll("%ID%", "0"));
                id = "0";
            }
            Main.getInstance().getPlayerAddCommand().remove(player);
            Main.getInstance().getInventoryManager().createCommandSettingsMenu(player, id);
        }else {
            String id = Main.getInstance().getPluginConfig().getEdit(player.getUniqueId());
            placedEggs.set("Rewards." + id + ".command", event.getMessage());
            Main.getInstance().getEggDataManager().savePlacedEggs(collection, placedEggs);
            player.sendMessage(messageManager.getMessage(MessageKey.COMMAND_CHANGED).replaceAll("%ID%", id));
            placedEggs.set("Edit." + player.getUniqueId(), null);
            Main.getInstance().getEggDataManager().savePlacedEggs(collection, placedEggs);
            Main.getInstance().getPlayerAddCommand().remove(player);
            Main.getInstance().getInventoryManager().createCommandSettingsMenu(player, id);
        }
    }
    private void setConfiguration(String id, String command, Player player){
        String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(player.getUniqueId());
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        placedEggs.set("Rewards." + id + ".command", command);
        placedEggs.set("Rewards." + id + ".enabled", true);
        placedEggs.set("Rewards." + id + ".type", 0);
        Main.getInstance().getEggDataManager().savePlacedEggs(collection, placedEggs);
    }

    private void runTimeForPlayers(){
        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : Main.getInstance().getPlayerAddCommand().keySet()){
                    String collection = Main.getInstance().getEggManager().getEggCollectionFromPlayerData(player.getUniqueId());
                    FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
                    int currenttime = Main.getInstance().getPlayerAddCommand().get(player);
                    Main.getInstance().getPlayerAddCommand().remove(player);
                    if(currenttime == 0){
                        if(player != null){
                            player.sendMessage(messageManager.getMessage(MessageKey.COMMAND_EXPIRED));
                            placedEggs.set("Edit." + player.getUniqueId(), null);
                            Main.getInstance().getEggDataManager().savePlacedEggs(collection, placedEggs);
                        }
                        return;
                    }
                    Main.getInstance().getPlayerAddCommand().put(player, currenttime-1);
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }
}
