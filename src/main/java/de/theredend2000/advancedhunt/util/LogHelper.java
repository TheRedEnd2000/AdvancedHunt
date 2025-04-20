package de.theredend2000.advancedhunt.util;

import de.theredend2000.advancedhunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.nio.Buffer;
import java.util.UUID;

public class LogHelper {

    private Main plugin;

    private boolean sendLog;

    public LogHelper(){
        plugin = Main.getInstance();
        sendLog = false;
    }

    public void toggleSendLog(){
        sendLog = !sendLog;
    }

    public boolean getSendLog(){
        return sendLog;
    }

    public void sendLogMessage(String message){
        if(!sendLog) return;
        for(UUID devUUID : plugin.getDevs()){
            Player dev = Bukkit.getPlayer(devUUID);
            if(dev == null) continue;
            dev.sendMessage("§c["+plugin.getDatetimeUtils().getNowTime()+"] §4§lLOG: §r"+message);
        }
    }

}
