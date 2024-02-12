package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.managers.inventorymanager.eggfoundrewardmenu.EggRewardMenu;
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

    public PlayerChatEventListener(){
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
        runTimeForPlayers();
    }

    @EventHandler
    public void onPlayerChat(PlayerChatEvent event){
        Player player = event.getPlayer();
        if(Main.getInstance().getPlayerAddCommand().containsKey(player)) {
            event.setCancelled(true);
            if (event.getMessage().equalsIgnoreCase("cancel")) {
                Main.getInstance().getPlayerAddCommand().remove(player);
                player.sendMessage(Main.getInstance().getMessage("CommandCanceledMessage"));
                new EggRewardMenu(Main.getPlayerMenuUtility(player)).open();
                return;
            }
            if (!Main.getInstance().getConfig().contains("Edit." + player.getUniqueId())) {
                String id = null;
                if (Main.getInstance().getConfig().contains("Rewards.")) {
                    ConfigurationSection section = Main.getInstance().getConfig().getConfigurationSection("Rewards.");
                    int nextNumber = 0;
                    Set<String> keys = section.getKeys(false);
                    if (!keys.isEmpty()) {
                        for (int i = 0; i <= keys.size(); i++) {
                            String key = Integer.toString(i);
                            if (!keys.contains(key)) {
                                nextNumber = i;
                                break;
                            }
                        }
                    }
                    setConfiguration(String.valueOf(nextNumber), event.getMessage());
                    player.sendMessage(Main.getInstance().getMessage("CommandAddedMessage").replaceAll("%ID%", String.valueOf(nextNumber)));
                    id = String.valueOf(nextNumber);
                } else {
                    setConfiguration("0", event.getMessage());
                    player.sendMessage(Main.getInstance().getMessage("CommandAddedMessage").replaceAll("%ID%", "0"));
                    id = "0";
                }
                Main.getInstance().getPlayerAddCommand().remove(player);
                Main.getInstance().getInventoryManager().createCommandSettingsMenu(player,id);
            }else {
                String id = Main.getInstance().getConfig().getString("Edit."+player.getUniqueId()+".commandID");
                Main.getInstance().getConfig().set("Rewards."+id+".command", event.getMessage());
                Main.getInstance().saveConfig();
                player.sendMessage(Main.getInstance().getMessage("CommandChangedMessage").replaceAll("%ID%",id));
                Main.getInstance().getConfig().set("Edit."+player.getUniqueId(),null);
                Main.getInstance().saveConfig();
                Main.getInstance().getPlayerAddCommand().remove(player);
                Main.getInstance().getInventoryManager().createCommandSettingsMenu(player,id);
            }
        }
    }
    private void setConfiguration(String id, String command){
        Main.getInstance().getConfig().set("Rewards."+id+".command", command);
        Main.getInstance().getConfig().set("Rewards."+id+".enabled", true);
        Main.getInstance().getConfig().set("Rewards."+id+".type", 0);
        Main.getInstance().saveConfig();
    }

    private void runTimeForPlayers(){
        new BukkitRunnable() {
            @Override
            public void run() {
                for(Player player : Main.getInstance().getPlayerAddCommand().keySet()){
                    int currenttime = Main.getInstance().getPlayerAddCommand().get(player);
                    Main.getInstance().getPlayerAddCommand().remove(player);
                    if(currenttime == 0){
                        if(player != null){
                            player.sendMessage(Main.getInstance().getMessage("CommandTimeExpiredMessage"));
                            Main.getInstance().getConfig().set("Edit."+player.getUniqueId(),null);
                            Main.getInstance().saveConfig();
                        }
                        return;
                    }
                    Main.getInstance().getPlayerAddCommand().put(player,currenttime-1);
                }
            }
        }.runTaskTimer(Main.getInstance(),0,20);
    }

}
