package de.theredend2000.advancedegghunt.listeners;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.enums.LeaderboardSortTypes;
import de.theredend2000.advancedegghunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerConnectionListener implements Listener {

    public PlayerConnectionListener(){
        for(Player player : Bukkit.getOnlinePlayers()){
            if(!Main.getInstance().getSortTypeLeaderboard().containsKey(player)){
                Main.getInstance().getSortTypeLeaderboard().put(player, LeaderboardSortTypes.ALL);
            }
        }
        Bukkit.getPluginManager().registerEvents(this, Main.getInstance());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event){
        Player player = event.getPlayer();
        if(!Main.getInstance().getSortTypeLeaderboard().containsKey(player)){
            Main.getInstance().getSortTypeLeaderboard().put(player, LeaderboardSortTypes.ALL);
        }
        Main.getInstance().getPlayerEggDataManager().createPlayerFile(player.getUniqueId());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event){
        Player player = event.getPlayer();
        Main.getInstance().getPlayerEggDataManager().unloadPlayerData(player.getUniqueId());
        if(Main.getInstance().getPlaceEggsPlayers().contains(player)){
            Main.getInstance().getEggManager().finishEggPlacing(player);
            Main.getInstance().getPlaceEggsPlayers().remove(player);
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LEAVE_PLACEMODE));
        }
    }
}
