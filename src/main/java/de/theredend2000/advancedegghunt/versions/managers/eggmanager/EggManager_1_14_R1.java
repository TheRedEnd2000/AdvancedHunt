package de.theredend2000.advancedegghunt.versions.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.saveinventory.Config;
import de.theredend2000.advancedegghunt.util.saveinventory.Serialization;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class EggManager_1_14_R1 implements EggManager {

    public String getRandomEggTexture(int id){
        String texture = "";
        switch (id){
            case 0:
                texture = Main.getTexture("ZWNlZGRjMjNmOWQ5NmJhYWEwZDJkN2I5ZWMxODBjZDdiZWE1NDQ3ZDM5YzQyNWNhOWU0NGQ4ODA4ZWExMWVhMCJ9fX0=");
                break;
            case 1:
                texture = Main.getTexture("ODYyMWE1MjY5ODY5ODQ3NTMxMDE1NjYzMDBhMzU2YjVmMzBkNjk3NWExZWZlNjI5YWJmMjY5NDc2NWQ5NmNjIn19fQ==");
                break;
            case 2:
                texture = Main.getTexture("NmUzMmE3ZGU3YTY3MmNjNjhmYTdhMjcyYmFhNmE4OWViZDQ0MGMzMmRjZjQ0ZTc3MDU3MDY4OTg5MDQyZjdjNiJ9fX0=");
                break;
            case 3:
                texture = Main.getTexture("NmI3NDQ2NTUwZjBmOTU3NmI3MzE3MjhiNWNiZWIyYmNlYTI1ZmQxYTU1NjBhMTdiMjM1N2U2MTZmYmM2NTYyMSJ9fX0=");
                break;
            case 4:
                texture = Main.getTexture("ZmU2ZmFiMDkxZTQ5NmMwOTY5MTA0ODBkYTBkODVlZTkxOWJjNDlhYTMxNzc1Y2FkYmJmNTA1ZWY0MTFiNWY5NCJ9fX0=");
                break;
            case 5:
                texture = Main.getTexture("ODUzMWNjMjY5YzhlNDcwNmU4OTJmOGEwZmIzNTFiMTA5MDE1NmIzZjYyNjFkODE2MzVkMDdhY2FkYmU2Y2UwZSJ9fX0=");
                break;
            case 6:
                texture = Main.getTexture("YTZhNjA1MWY3ZjZmNDM5ZDhmMjE0YzIzNGU4ZTJjNDc3NjMwMDUyNDMyZTQyNjA3ZjA0MDRiODQwYjUzY2VhYiJ9fX0=");
                break;
            case 7:
                texture = Main.getTexture("MjEzYjJlMjhlMDM3MTAwNzM5MTMwYjJlYjkwZWY0OTFjNmMzZGZmNWRlNTYxNWQyZjZkZmQxZTQ2YzljMmY3YyJ9fX0=");
                break;
        }
        return texture;
    }

    public ItemStack giveFinishedEggToPlayer(int id){
        return new ItemBuilder(Material.PLAYER_HEAD).setDisplayname("§6Easter Egg").setLore("§7Place this egg around the map","§7that everyone can search and find it.").setSkullOwner(getRandomEggTexture(id)).build();
    }

    public void finishEggPlacing(Player player){
        Config cfg = new Config(Main.getInstance(),player.getName());
        String[] values = new String[]{cfg.getConfig().getString("inv"), cfg.getConfig().getString("armor")};
        ItemStack[][] items = Serialization.base64toInv(values);
        player.getInventory().clear();
        player.getInventory().setContents(items[0]);
        player.getInventory().setArmorContents(items[1]);
    }

    public void startEggPlacing(Player player){
        Config cfg = new Config(Main.getInstance(),player.getName());
        String[] values = Serialization.invToBase64(player.getInventory());
        cfg.getConfig().set("inv", values[0]);
        cfg.getConfig().set("armor", values[1]);
        cfg.saveInv();
        player.getInventory().clear();
    }

    public void saveEgg(Player player, Location location){
        if(Main.getInstance().eggs.contains("Eggs.")){
            ConfigurationSection chestsSection = Main.getInstance().eggs.getConfigurationSection("Eggs.");
            int nextNumber = 0;
            Set<String> keys = chestsSection.getKeys(false);
            if (!keys.isEmpty()) {
                for (int i = 0; i <= keys.size(); i++) {
                    String key = Integer.toString(i);
                    if (!keys.contains(key)) {
                        nextNumber = i;
                        break;
                    }
                }
            }
            new ConfigLocationUtil(Main.getInstance(), location, "Eggs."+ nextNumber).saveBlockLocation();
            player.sendMessage(Main.getInstance().getMessage("EggPlacedMessage").replaceAll("%ID%", String.valueOf(nextNumber)));
        }else {
            new ConfigLocationUtil(Main.getInstance(), location, "Eggs.0").saveBlockLocation();
            player.sendMessage(Main.getInstance().getMessage("EggPlacedMessage").replaceAll("%ID%","0"));
        }
        updateMaxEggs();
    }

    public void removeEgg(Player player, Block block){
        Main plugin = Main.getInstance();
        if(plugin.eggs.contains("Eggs.") && plugin.eggs.contains("FoundEggs.")){
            Set<String> keys = new HashSet<>();
            keys.clear();
            for (String key : plugin.eggs.getConfigurationSection("Eggs.").getKeys(false)) {
                keys.add(key);
                ConfigLocationUtil location = new ConfigLocationUtil(plugin, "Eggs."+key+".");
                if (location.loadBlockLocation() != null) {
                    if (block.getX() == location.loadLocation().getBlockX() && block.getY() == location.loadLocation().getBlockY() && block.getZ() == location.loadLocation().getBlockZ()) {
                        plugin.eggs.set("Eggs."+key,null);
                        plugin.saveEggs();
                        player.sendMessage(plugin.getMessage("EggBreakMessage").replaceAll("%ID%",key));
                        keys.remove(key);
                    }
                }
            }
            for(String uuids : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)){
                for(String keys2 : Main.getInstance().eggs.getConfigurationSection("FoundEggs."+uuids).getKeys(false)){
                    ConfigLocationUtil location = new ConfigLocationUtil(plugin, "FoundEggs."+uuids+"."+keys2+".");
                    if (location.loadBlockLocation() != null) {
                        if (block.getX() == location.loadLocation().getBlockX() && block.getY() == location.loadLocation().getBlockY() && block.getZ() == location.loadLocation().getBlockZ()) {
                            plugin.eggs.set("FoundEggs."+uuids+"."+keys2,null);
                            plugin.eggs.set("FoundEggs."+uuids+".Count", plugin.eggs.getInt("FoundEggs."+uuids+".Count")-1);
                            plugin.saveEggs();
                        }
                    }
                }
            }
            if(keys.isEmpty()){
                plugin.eggs.set("Eggs",null);
                plugin.saveEggs();
            }
            updateMaxEggs();
        }
    }

    public boolean containsEgg(Block block){
        Main plugin = Main.getInstance();
        if(!Main.getInstance().eggs.contains("Eggs.")){
            return false;
        }
        for (String key : plugin.eggs.getConfigurationSection("Eggs.").getKeys(false)) {
            ConfigLocationUtil location = new ConfigLocationUtil(plugin, "Eggs."+key+".");
            if (location.loadBlockLocation() != null) {
                if (block.getX() == location.loadLocation().getBlockX() && block.getY() == location.loadLocation().getBlockY() && block.getZ() == location.loadLocation().getBlockZ()) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getEggID(Block block){
        Main plugin = Main.getInstance();
        if(!Main.getInstance().eggs.contains("Eggs.")){
            return null;
        }
        for (String key : plugin.eggs.getConfigurationSection("Eggs.").getKeys(false)) {
            ConfigLocationUtil location = new ConfigLocationUtil(plugin, "Eggs."+key+".");
            if (location.loadBlockLocation() != null) {
                if (block.getX() == location.loadLocation().getBlockX() && block.getY() == location.loadLocation().getBlockY() && block.getZ() == location.loadLocation().getBlockZ()) {
                    return key;
                }
            }
        }
        return null;
    }

    public void saveFoundEggs(Player player, Block block, String id){
        new ConfigLocationUtil(Main.getInstance(),block.getLocation(),"FoundEggs."+player.getUniqueId()+"."+id).saveBlockLocation();
        Main.getInstance().eggs.set("FoundEggs."+player.getUniqueId()+".Count", Main.getInstance().eggs.contains("FoundEggs."+player.getUniqueId()+".Count") ? Main.getInstance().eggs.getInt("FoundEggs." + player.getUniqueId() + ".Count")+1 : 1);
        Main.getInstance().saveEggs();
        if(!Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundOneEggRewards") || !Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundAllEggsReward"))
            player.sendMessage(Main.getInstance().getMessage("EggFoundMessage").replaceAll("%EGGS_FOUND%", String.valueOf(getEggsFound(player))).replaceAll("%EGGS_MAX%", String.valueOf(getMaxEggs())));
    }
    public boolean hasFound(Player player, String id){
        return Main.getInstance().eggs.contains("FoundEggs."+player.getUniqueId()+"."+id);
    }

    public int getMaxEggs(){
        return Main.getInstance().eggs.getInt("MaxEggs");
    }

    public int getEggsFound(Player player){
        return Main.getInstance().eggs.getInt("FoundEggs."+player.getUniqueId()+".Count");
    }

    public void updateMaxEggs(){
        ArrayList<String> maxEggs = new ArrayList<>();
        maxEggs.clear();
        if(!Main.getInstance().eggs.contains("Eggs.")){
            return;
        }
        for(String id : Main.getInstance().eggs.getConfigurationSection("Eggs.").getKeys(false)){
            maxEggs.add(id);
        }
        Main.getInstance().eggs.set("MaxEggs",maxEggs.size());
        Main.getInstance().saveEggs();
    }

    public boolean checkFoundAll(Player player){
        return getEggsFound(player) == getMaxEggs();
    }
    public void spawnEggParticle(){
        new BukkitRunnable() {
            @Override
            public void run() {
                if(!Main.getInstance().eggs.contains("Eggs.")){
                    return;
                }
                for (String key: Main.getInstance().eggs.getConfigurationSection("Eggs.").getKeys(false)){
                    ConfigLocationUtil locationUtil = new ConfigLocationUtil(Main.getInstance(),"Eggs."+key);
                    if (locationUtil.loadBlockLocation() != null){
                        String world = Main.getInstance().eggs.getString("Eggs."+key+".World");
                        int x = Main.getInstance().eggs.getInt("Eggs."+key+".X");
                        int y = Main.getInstance().eggs.getInt("Eggs."+key+".Y");
                        int z = Main.getInstance().eggs.getInt("Eggs."+key+".Z");
                        Location loc = new Location(Bukkit.getWorld(world),x,y,z).add(0.5,0.5,0.5);
                        for (Entity e: loc.getWorld().getNearbyEntities(loc, 10,10,10)){
                            if (e instanceof Player){
                                Player p = (Player) e;
                                if(VersionManager.getEggManager().hasFound(p,key)){
                                    p.spawnParticle(Particle.VILLAGER_HAPPY,loc,1,0.2,0.1,0.2,0);
                                }else
                                    p.spawnParticle(Particle.CRIT,loc,1,0.2,0.1,0.2,0);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(),0,5);
    }
}
