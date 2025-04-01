package de.theredend2000.advancedhunt.listeners;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.util.enums.LeaderboardSortTypes;
import de.theredend2000.advancedhunt.util.messages.MessageKey;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static de.theredend2000.advancedhunt.Main.getPlayerMenuUtility;

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
        if(Main.getInstance().getPlacePlayers().contains(player)){
            Main.getInstance().getEggManager().finishEggPlacing(player);
            Main.getInstance().getPlacePlayers().remove(player);
            player.sendMessage(Main.getInstance().getMessageManager().getMessage(MessageKey.LEAVE_PLACEMODE));
        }

        Main.dropPlayerMenuUtility(player);
    }
}
