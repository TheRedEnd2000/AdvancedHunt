package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
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
        FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        String id = playerConfig.getString("Change.id");
        String collection = playerConfig.getString("Change.collection");
        if (event.getMessage().equalsIgnoreCase("cancel")) {
            Main.getInstance().getPlayerAddCommand().remove(player);
            player.sendMessage(messageManager.getMessage(MessageKey.COMMAND_CANCEL));
            Main.getInstance().getEggRewardsInventory().open(player,id,collection);
            return;
        }
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        if (playerConfig.contains("Change.")) {
            if (placedEggs.contains("PlacedEggs."+id+".Rewards.")) {
                ConfigurationSection rewardsSection = placedEggs.getConfigurationSection("PlacedEggs."+id+".Rewards.");
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
                setConfiguration(String.valueOf(nextNumber),id , event.getMessage(), collection);
                player.sendMessage(messageManager.getMessage(MessageKey.COMMAND_ADD).replaceAll("%ID%", String.valueOf(nextNumber)));
            } else {
                setConfiguration("0",id , event.getMessage(), collection);
                player.sendMessage(messageManager.getMessage(MessageKey.COMMAND_ADD).replaceAll("%ID%", "0"));
            }
            Main.getInstance().getPlayerAddCommand().remove(player);
            playerConfig.set("Change", null);
            Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
            Main.getInstance().getEggRewardsInventory().open(player,id,collection);
        }
    }
    private void setConfiguration(String commandID,String id, String command,String collection){
        FileConfiguration placedEggs = Main.getInstance().getEggDataManager().getPlacedEggs(collection);
        placedEggs.set("PlacedEggs."+id+".Rewards." + commandID + ".command", command);
        placedEggs.set("PlacedEggs."+id+".Rewards." + commandID + ".enabled", true);
        placedEggs.set("PlacedEggs."+id+".Rewards." + commandID + ".foundAll", false);
        Main.getInstance().getEggDataManager().savePlacedEggs(collection, placedEggs);
    }

    private void runTimeForPlayers(){
        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : Main.getInstance().getPlayerAddCommand().keySet()){
                    FileConfiguration playerConfig = Main.getInstance().getPlayerEggDataManager().getPlayerData(player.getUniqueId());
                    int currenttime = Main.getInstance().getPlayerAddCommand().get(player);
                    Main.getInstance().getPlayerAddCommand().remove(player);
                    if(currenttime == 0){
                        if(player != null){
                            player.sendMessage(messageManager.getMessage(MessageKey.COMMAND_EXPIRED));
                            playerConfig.set("Change", null);
                            Main.getInstance().getPlayerEggDataManager().savePlayerData(player.getUniqueId(), playerConfig);
                        }
                        return;
                    }
                    Main.getInstance().getPlayerAddCommand().put(player, currenttime-1);
                }
            }
        }.runTaskTimer(Main.getInstance(), 0, 20);
    }
}
