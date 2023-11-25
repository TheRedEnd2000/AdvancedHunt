package de.theredend2000.advancedegghunt.versions.managers.eggmanager;

import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.saveinventory.Config;
import de.theredend2000.advancedegghunt.util.saveinventory.Serialization;
import de.theredend2000.advancedegghunt.versions.VersionManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EggManager_1_16_R3 implements EggManager {

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
        if(plugin.eggs.contains("Eggs.")){
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
            if(plugin.eggs.contains("FoundEggs.")) {
                for (String uuids : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)) {
                    for (String keys2 : Main.getInstance().eggs.getConfigurationSection("FoundEggs." + uuids).getKeys(false)) {
                        ConfigLocationUtil location = new ConfigLocationUtil(plugin, "FoundEggs." + uuids + "." + keys2 + ".");
                        if (location.loadBlockLocation() != null) {
                            if (block.getX() == location.loadLocation().getBlockX() && block.getY() == location.loadLocation().getBlockY() && block.getZ() == location.loadLocation().getBlockZ()) {
                                plugin.eggs.set("FoundEggs." + uuids + "." + keys2, null);
                                plugin.eggs.set("FoundEggs." + uuids + ".Count", plugin.eggs.getInt("FoundEggs." + uuids + ".Count") - 1);
                                plugin.saveEggs();
                            }
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

    public int getRandomNotFoundEgg(Player player){
        Main plugin = Main.getInstance();
        if(plugin.eggs.contains("Eggs.") && plugin.eggs.contains("FoundEggs."+player.getUniqueId())) {
            for (int i = 0; i < VersionManager.getEggManager().getMaxEggs(); i++) {
                if (!VersionManager.getEggManager().hasFound(player, String.valueOf(i))) {
                    return i;
                }
            }
        }
        return -1;
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
        Main.getInstance().eggs.set("Eggs."+id+".TimesFound", Main.getInstance().eggs.contains("Eggs."+id+".TimesFound") ? Main.getInstance().eggs.getInt("Eggs."+id+".TimesFound")+1 : 1);
        Main.getInstance().saveConfig();
        new ConfigLocationUtil(Main.getInstance(),block.getLocation(),"FoundEggs."+player.getUniqueId()+"."+id).saveBlockLocation();
        Main.getInstance().eggs.set("FoundEggs."+player.getUniqueId()+".Count", Main.getInstance().eggs.contains("FoundEggs."+player.getUniqueId()+".Count") ? Main.getInstance().eggs.getInt("FoundEggs." + player.getUniqueId() + ".Count")+1 : 1);
        Main.getInstance().saveEggs();
        Main.getInstance().eggs.set("FoundEggs."+player.getUniqueId()+".Name", player.getName());
        Main.getInstance().saveEggs();
        if(!Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundOneEggRewards") || !Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundAllEggsReward"))
            player.sendMessage(Main.getInstance().getMessage("EggFoundMessage").replaceAll("%EGGS_FOUND%", String.valueOf(getEggsFound(player))).replaceAll("%EGGS_MAX%", String.valueOf(getMaxEggs())));
    }

    @Override
    public int getTimesFound(String id) {
        return Main.getInstance().eggs.contains("Eggs."+id+".TimesFound") ? Main.getInstance().eggs.getInt("Eggs."+id+".TimesFound") : 0;
    }

    @Override
    public String getEggDatePlaced(String id) {
        return Main.getInstance().eggs.getString("Eggs."+id+".Date");
    }

    @Override
    public String getEggTimePlaced(String id) {
        return Main.getInstance().eggs.getString("Eggs."+id+".Time");
    }

    @Override
    public String getEggDateCollected(String uuid,String id) {
        return Main.getInstance().eggs.getString("FoundEggs."+uuid+"."+id+".Date");
    }

    @Override
    public String getEggTimeCollected(String uuid, String id) {
        return Main.getInstance().eggs.getString("FoundEggs."+uuid+"."+id+".Time");
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
        if(!Main.getInstance().eggs.contains("Eggs.")){
            Main.getInstance().eggs.set("MaxEggs",0);
            Main.getInstance().saveEggs();
            return;
        }
        ArrayList<String> maxEggs = new ArrayList<>(Main.getInstance().eggs.getConfigurationSection("Eggs.").getKeys(false));
        Main.getInstance().eggs.set("MaxEggs",maxEggs.size());
        Main.getInstance().saveEggs();
    }

    public boolean checkFoundAll(Player player){
        return getEggsFound(player) == getMaxEggs();
    }
    @Override
    public void spawnEggParticle(){
        new BukkitRunnable() {
            double time = 0;
            @Override
            public void run() {
                time += 0.025;
                if(!Main.getInstance().eggs.contains("Eggs.")){
                    return;
                }
                if(!Main.getInstance().getConfig().getBoolean("Particle.enabled")) return;
                for (String key: Main.getInstance().eggs.getConfigurationSection("Eggs.").getKeys(false)){
                    ConfigLocationUtil locationUtil = new ConfigLocationUtil(Main.getInstance(),"Eggs."+key);
                    if (locationUtil.loadBlockLocation() != null){
                        String world = Main.getInstance().eggs.getString("Eggs."+key+".World");
                        int x = Main.getInstance().eggs.getInt("Eggs."+key+".X");
                        int y = Main.getInstance().eggs.getInt("Eggs."+key+".Y");
                        int z = Main.getInstance().eggs.getInt("Eggs."+key+".Z");
                        Location startLocation = new Location(Bukkit.getWorld(world),x,y,z);
                        for (Entity e: startLocation.getWorld().getNearbyEntities(startLocation, 10,10,10)){
                            if (e instanceof Player){
                                Player p = (Player) e;
                                if (time > 3.0)
                                    time = 0;
                                if (time > 2.0) {
                                    double startX = startLocation.getX() - 1;
                                    double startY = startLocation.getY();
                                    double startZ = startLocation.getZ() + 1;
                                    startLocation.getWorld().spawnParticle(getParticle(p,key), (startX - 1) + time, (startY), (startZ), 0);
                                    startLocation.getWorld().spawnParticle(getParticle(p,key), (startX + 2), (startY), (startZ - 3) + time, 0);
                                    startLocation.getWorld().spawnParticle(getParticle(p,key), (startX + 2), (startY + 3) - time, (startZ), 0);
                                    continue;
                                }
                                if (time > 1.0) {
                                    double startX = startLocation.getX();
                                    double startY = startLocation.getY();
                                    double startZ = startLocation.getZ() - 1;
                                    startLocation.getWorld().spawnParticle(getParticle(p,key), (startX - 1) + time, startY, (startZ + 1), 0);
                                    startLocation.getWorld().spawnParticle(getParticle(p,key), startX, startY, startZ + time, 0);
                                    startLocation.getWorld().spawnParticle(getParticle(p,key), (startX), (startY + 2) - time, (startZ + 2), 0);
                                    startLocation.getWorld().spawnParticle(getParticle(p,key), (startX + 1), (startY + 2) - time, (startZ + 1), 0);
                                    startLocation.getWorld().spawnParticle(getParticle(p,key), (startX - 1) + time, (startY + 1), (startZ + 2), 0);
                                    startLocation.getWorld().spawnParticle(getParticle(p,key), (startX + 1), (startY + 1), (startZ) + time, 0);
                                    continue;
                                }

                                double startX = startLocation.getX();
                                double startY = startLocation.getY() + 1.0;
                                double startZ = startLocation.getZ();
                                startLocation.getWorld().spawnParticle(getParticle(p,key), startX + time, startY, startZ, 0);
                                startLocation.getWorld().spawnParticle(getParticle(p,key), startX, startY - time, startZ, 0);
                                startLocation.getWorld().spawnParticle(getParticle(p,key), startX, startY, startZ + time, 0);
                            }
                        }
                        int radius = Main.getInstance().getConfig().getInt("Settings.ShowEggsNearbyMessageRadius");
                        for(Entity e : startLocation.getWorld().getNearbyEntities(startLocation,radius,radius,radius)){
                            if(e instanceof Player){
                                Player p = (Player) e;
                                if(!VersionManager.getEggManager().hasFound(p,key)){
                                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Main.getInstance().getMessage("EggNearbyMessage")));
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(),0,3);
    }

    public Particle getParticle(Player p,String key){
        if(VersionManager.getEggManager().hasFound(p,key)){
            return Particle.valueOf(Main.getInstance().getConfig().getString("Particle.type.EggNotFound").toUpperCase());
        }else {
            return Particle.valueOf(Main.getInstance().getConfig().getString("Particle.type.EggFound").toUpperCase());
        }
    }

    public void resetStatsPlayer(String name){
        for(String uuids : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)){
            if(Main.getInstance().eggs.getString("FoundEggs."+uuids+".Name").equals(name)) {
                Main.getInstance().eggs.set("FoundEggs." + uuids, null);
                Main.getInstance().saveEggs();
            }
        }
    }
    public void resetStatsAll(){
        Main.getInstance().eggs.set("FoundEggs",null);
        Main.getInstance().saveEggs();
    }

    public boolean containsPlayer(String name){
        if(Main.getInstance().eggs.contains("FoundEggs.")){
            for(String uuids : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)){
                if(Main.getInstance().eggs.getString("FoundEggs."+uuids+".Name").equals(name)){
                    return true;
                }
            }
        }
        return false;
    }
    public void showAllEggs(){
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
                ArmorStand armorStand = loc.getWorld().spawn(loc, ArmorStand.class);
                armorStand.setGravity(false);
                armorStand.setInvulnerable(true);
                armorStand.setGlowing(true);
                armorStand.setCustomName("§dEgg #"+key);
                armorStand.setCustomNameVisible(true);
                armorStand.setSmall(true);
                armorStand.setInvisible(true);
                Main.getInstance().getShowedArmorstands().add(armorStand);
                new BukkitRunnable() {
                    int count = Main.getInstance().getConfig().getInt("Settings.ArmorstandGlow");
                    @Override
                    public void run() {
                        count --;
                        if(count == 0){
                            cancel();
                            for(ArmorStand a : Main.getInstance().getShowedArmorstands()){
                                a.remove();
                                Main.getInstance().getShowedArmorstands().remove(a);
                            }
                        }
                    }
                }.runTaskTimer(Main.getInstance(),0,20);
            }
        }
    }

    public String getTopPlayerName(){
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(!Main.getInstance().eggs.contains("FoundEggs.")){
            return "?????";
        }
        for(String uuid : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)) {
            leaderboard.put(Main.getInstance().eggs.getString("FoundEggs."+uuid+".Name"),Main.getInstance().eggs.getInt("FoundEggs."+uuid+".Count"));
        }
        if(leaderboard.size() < 1){
            return "?????";
        }
        List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());

        leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        return leaderList.get(0).getKey();
    }
    public int getTopPlayerEggsFound(){
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(!Main.getInstance().eggs.contains("FoundEggs.")){
            return -1;
        }
        for(String uuid : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)) {
            leaderboard.put(Main.getInstance().eggs.getString("FoundEggs."+uuid+".Name"),Main.getInstance().eggs.getInt("FoundEggs."+uuid+".Count"));
        }
        if(leaderboard.size() < 1){
            return -1;
        }
        List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());

        leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        return leaderList.get(0).getValue();
    }

    public String getSecondPlayerName(){
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(!Main.getInstance().eggs.contains("FoundEggs.")){
            return "?????";
        }
        for(String uuid : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)) {
            leaderboard.put(Main.getInstance().eggs.getString("FoundEggs."+uuid+".Name"),Main.getInstance().eggs.getInt("FoundEggs."+uuid+".Count"));
        }
        if(leaderboard.size() < 2){
            return "?????";
        }
        List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());

        leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        return leaderList.get(1).getKey();
    }
    public int getSecondPlayerEggsFound(){
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(!Main.getInstance().eggs.contains("FoundEggs.")){
            return -1;
        }
        for(String uuid : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)) {
            leaderboard.put(Main.getInstance().eggs.getString("FoundEggs."+uuid+".Name"),Main.getInstance().eggs.getInt("FoundEggs."+uuid+".Count"));
        }
        if(leaderboard.size() < 2){
            return -1;
        }
        List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());

        leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        return leaderList.get(1).getValue();
    }

    public String getThirdPlayerName(){
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(!Main.getInstance().eggs.contains("FoundEggs.")){
            return "?????";
        }
        for(String uuid : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)) {
            leaderboard.put(Main.getInstance().eggs.getString("FoundEggs."+uuid+".Name"),Main.getInstance().eggs.getInt("FoundEggs."+uuid+".Count"));
        }
        if(leaderboard.size() < 3){
            return "?????";
        }
        List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());

        leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        return leaderList.get(2).getKey();
    }
    public int getThirdPlayerEggsFound(){
        HashMap<String, Integer> leaderboard = new HashMap<>();
        if(!Main.getInstance().eggs.contains("FoundEggs.")){
            return -1;
        }
        for(String uuid : Main.getInstance().eggs.getConfigurationSection("FoundEggs.").getKeys(false)) {
            leaderboard.put(Main.getInstance().eggs.getString("FoundEggs."+uuid+".Name"),Main.getInstance().eggs.getInt("FoundEggs."+uuid+".Count"));
        }
        if(leaderboard.size() < 3){
            return -1;
        }
        List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());

        leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        return leaderList.get(2).getValue();
    }
}
