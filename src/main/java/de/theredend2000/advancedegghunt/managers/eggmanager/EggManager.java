package de.theredend2000.advancedegghunt.managers.eggmanager;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.particles.XParticle;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.ConfigLocationUtil;
import de.theredend2000.advancedegghunt.util.ItemBuilder;
import de.theredend2000.advancedegghunt.util.Updater;
import de.theredend2000.advancedegghunt.util.saveinventory.Config;
import de.theredend2000.advancedegghunt.util.saveinventory.Serialization;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

public class EggManager {

    private Main plugin;

    public EggManager(){
        this.plugin = Main.getInstance();
    }

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
        return new ItemBuilder(XMaterial.PLAYER_HEAD).setDisplayname("§6Easter Egg").setLore("§7Place this egg around the map","§7that everyone can search and find it.").setSkullOwner(getRandomEggTexture(id)).build();
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
        if(plugin.getEggDataManager().getPlacedEggs().contains("PlacedEggs.")){
            ConfigurationSection section = plugin.getEggDataManager().getPlacedEggs().getConfigurationSection("PlacedEggs.");
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
            new ConfigLocationUtil(plugin, location, "PlacedEggs."+ nextNumber).saveBlockLocation();
            player.sendMessage(Main.getInstance().getMessage("EggPlacedMessage").replaceAll("%ID%", String.valueOf(nextNumber)));
        }else {
            new ConfigLocationUtil(plugin, location, "PlacedEggs.0").saveBlockLocation();
            player.sendMessage(Main.getInstance().getMessage("EggPlacedMessage").replaceAll("%ID%","0"));
        }
        updateMaxEggs();
    }

    public void removeEgg(Player player, Block block){
        if(plugin.getEggDataManager().getPlacedEggs().contains("PlacedEggs.")){
            Set<String> keys = new HashSet<>();
            keys.clear();
            for (String key : plugin.getEggDataManager().getPlacedEggs().getConfigurationSection("PlacedEggs.").getKeys(false)) {
                keys.add(key);
                ConfigLocationUtil location = new ConfigLocationUtil(plugin, "PlacedEggs."+key);
                if (location.loadBlockLocation() != null) {
                    if (block.getX() == location.loadLocation().getBlockX() && block.getY() == location.loadLocation().getBlockY() && block.getZ() == location.loadLocation().getBlockZ()) {
                        plugin.getEggDataManager().getPlacedEggs().set("PlacedEggs."+key,null);
                        Main.getInstance().getEggDataManager().savePlacedEggs();
                        player.sendMessage(plugin.getMessage("EggBreakMessage").replaceAll("%ID%",key));
                        keys.remove(key);
                    }
                }
            }
            for (UUID uuids : plugin.getEggDataManager().savedPlayers()) {
                FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuids);
                if(playerConfig.contains("FoundEggs.")) {
                    for (String eggID : playerConfig.getConfigurationSection("FoundEggs.").getKeys(false)) {
                        if(eggID.equalsIgnoreCase("Count") || eggID.equalsIgnoreCase("Name")) continue;
                        ConfigLocationUtil location = new ConfigLocationUtil(plugin, "FoundEggs." + eggID);
                        if (location.loadBlockLocation(uuids) != null) {
                            if (block.getX() == location.loadLocation(player.getUniqueId()).getBlockX() && block.getY() == location.loadLocation(player.getUniqueId()).getBlockY() && block.getZ() == location.loadLocation(player.getUniqueId()).getBlockZ()) {
                                playerConfig.set("FoundEggs."+eggID, null);
                                playerConfig.set("FoundEggs.Count", getPlayerCount(uuids) - 1);
                                plugin.getPlayerEggDataManager().savePlayerData(uuids,playerConfig);
                            }
                        }
                    }
                }
            }
            if(keys.isEmpty()){
                plugin.getEggDataManager().getPlacedEggs().set("PlacedEggs",null);
                plugin.getEggDataManager().savePlacedEggs();
            }
            updateMaxEggs();
        }
    }

    public int getPlayerCount(UUID uuid){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
        return playerConfig.getInt("FoundEggs.Count");
    }

    public int getRandomNotFoundEgg(Player player){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        if(plugin.getEggDataManager().getPlacedEggs().contains("PlacedEggs.") && playerConfig.contains("FoundEggs.")) {
            for (int i = 0; i < getMaxEggs(); i++) {
                if (!hasFound(player, String.valueOf(i))) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean containsEgg(Block block){
        Main plugin = Main.getInstance();
        if(!plugin.getEggDataManager().getPlacedEggs().contains("PlacedEggs.")) return false;
        for (String key : plugin.getEggDataManager().getPlacedEggs().getConfigurationSection("PlacedEggs.").getKeys(false)) {
            ConfigLocationUtil location = new ConfigLocationUtil(plugin, "PlacedEggs."+key+".");
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
        if(!plugin.getEggDataManager().getPlacedEggs().contains("PlacedEggs.")) return null;
        for (String key : plugin.getEggDataManager().getPlacedEggs().getConfigurationSection("PlacedEggs.").getKeys(false)) {
            ConfigLocationUtil location = new ConfigLocationUtil(plugin, "PlacedEggs."+key+".");
            if (location.loadBlockLocation() != null) {
                if (block.getX() == location.loadLocation().getBlockX() && block.getY() == location.loadLocation().getBlockY() && block.getZ() == location.loadLocation().getBlockZ()) {
                    return key;
                }
            }
        }
        return null;
    }

    public void saveFoundEggs(Player player, Block block, String id){
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs();
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        placedEggs.set("PlacedEggs."+id+".TimesFound", placedEggs.contains("PlacedEggs."+id+".TimesFound") ? placedEggs.getInt("PlacedEggs."+id+".TimesFound")+1 : 1);
        Main.getInstance().getEggDataManager().savePlacedEggs();
        playerConfig.set("FoundEggs.Count", playerConfig.contains("FoundEggs.Count") ? playerConfig.getInt("FoundEggs.Count")+1 : 1);
        playerConfig.set("FoundEggs.Name", player.getName());
        new ConfigLocationUtil(plugin,block.getLocation(),"FoundEggs."+id).saveBlockLocation(player.getUniqueId(),playerConfig);
        plugin.getPlayerEggDataManager().savePlayerData(player.getUniqueId(),playerConfig);
        if(!Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundOneEggRewards") || !Main.getInstance().getConfig().getBoolean("Settings.PlayerFoundAllEggsReward"))
            player.sendMessage(Main.getInstance().getMessage("EggFoundMessage").replaceAll("%EGGS_FOUND%", String.valueOf(getEggsFound(player))).replaceAll("%EGGS_MAX%", String.valueOf(getMaxEggs())));
    }

    public int getTimesFound(String id) {
        return plugin.getEggDataManager().getPlacedEggs().contains("PlacedEggs."+id+".TimesFound") ? plugin.getEggDataManager().getPlacedEggs().getInt("PlacedEggs."+id+".TimesFound") : 0;
    }

    public String getEggDatePlaced(String id) {
        return plugin.getEggDataManager().getPlacedEggs().getString("PlacedEggs."+id+".Date");
    }

    public String getEggTimePlaced(String id) {
        return plugin.getEggDataManager().getPlacedEggs().getString("PlacedEggs."+id+".Time");
    }

    public String getEggDateCollected(String uuid,String id) {
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(UUID.fromString(uuid));
        return playerConfig.getString("FoundEggs."+id+".Date");
    }

    public String getEggTimeCollected(String uuid, String id) {
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(UUID.fromString(uuid));
        return playerConfig.getString("FoundEggs."+id+".Time");
    }

    public boolean hasFound(Player player, String id){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        return playerConfig.contains("FoundEggs."+id);
    }

    public int getMaxEggs(){
        return plugin.getEggDataManager().getPlacedEggs().getInt("MaxEggs");
    }

    public int getEggsFound(Player player){
        FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(player.getUniqueId());
        return playerConfig.getInt("FoundEggs.Count");
    }

    public void updateMaxEggs(){
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs();
        if(!placedEggs.contains("PlacedEggs.")){
            placedEggs.set("MaxEggs",0);
            Main.getInstance().getEggDataManager().savePlacedEggs();
            return;
        }
        ArrayList<String> maxEggs = new ArrayList<>(placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false));
        placedEggs.set("MaxEggs",maxEggs.size());
        Main.getInstance().getEggDataManager().savePlacedEggs();
    }

    public boolean checkFoundAll(Player player){
        return getEggsFound(player) == getMaxEggs();
    }

    public void spawnEggParticle(){
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs();
        new BukkitRunnable() {
            double time = 0;
            @Override
            public void run() {
                time += 0.025;
                if(!placedEggs.contains("PlacedEggs.")){
                    return;
                }
                if(!Main.getInstance().getConfig().getBoolean("Particle.enabled")) return;
                for (String key: placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false)){
                    ConfigLocationUtil locationUtil = new ConfigLocationUtil(plugin,"PlacedEggs."+key);
                    if (locationUtil.loadBlockLocation() != null){
                        String world = placedEggs.getString("PlacedEggs."+key+".World");
                        int x = placedEggs.getInt("PlacedEggs."+key+".X");
                        int y = placedEggs.getInt("PlacedEggs."+key+".Y");
                        int z = placedEggs.getInt("PlacedEggs."+key+".Z");
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
                                if(!hasFound(p,key)){
                                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(Main.getInstance().getMessage("EggNearbyMessage")));
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(Main.getInstance(),0,3);
    }

    public Particle getParticle(Player p, String key){
        if(hasFound(p,key)){
            return XParticle.getParticle(Main.getInstance().getConfig().getString("Particle.type.EggNotFound").toUpperCase());
        }else {
            return XParticle.getParticle(Main.getInstance().getConfig().getString("Particle.type.EggFound").toUpperCase());
        }
    }

    public void resetStatsPlayer(String name){
        ArrayList<String> eggID = new ArrayList<>();
        for(UUID uuids : plugin.getEggDataManager().savedPlayers()){
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuids);
            if(playerConfig.getString("FoundEggs.Name").equals(name)) {
                eggID.addAll(playerConfig.getConfigurationSection("FoundEggs.").getKeys(false));
                playerConfig.set("FoundEggs.", null);
                plugin.getPlayerEggDataManager().savePlayerData(uuids,playerConfig);
            }
        }
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs();
        for(String ids : placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false)){
            if(eggID.contains(ids)){
                placedEggs.set("PlacedEggs."+ids+".TimesFound",getTimesFound(ids)-1);
            }
        }
    }

    public boolean containsPlayer(String name){
        for(UUID uuids : plugin.getEggDataManager().savedPlayers()){
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuids);
            if(playerConfig.getString("FoundEggs.Name").equals(name)){
                return true;
            }
        }
        return false;
    }
    public void resetStatsAll(){
        for(UUID uuids : plugin.getEggDataManager().savedPlayers()){
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuids);
            playerConfig.set("FoundEggs",null);
            plugin.getPlayerEggDataManager().savePlayerData(uuids,playerConfig);
        }
    }
    public void showAllEggs(){
        FileConfiguration placedEggs = plugin.getEggDataManager().getPlacedEggs();
        if(!placedEggs.contains("PlacedEggs.")){
            return;
        }
        for (String key: placedEggs.getConfigurationSection("PlacedEggs.").getKeys(false)){
            ConfigLocationUtil locationUtil = new ConfigLocationUtil(plugin,"PlacedEggs."+key);
            if (locationUtil.loadBlockLocation() != null){
                String world = placedEggs.getString("PlacedEggs."+key+".World");
                int x = placedEggs.getInt("PlacedEggs."+key+".X");
                int y = placedEggs.getInt("PlacedEggs."+key+".Y");
                int z = placedEggs.getInt("PlacedEggs."+key+".Z");
                Location loc = new Location(Bukkit.getWorld(world),x,y,z).add(0.5,0.5,0.5);
                ArmorStand armorStand = loc.getWorld().spawn(loc, ArmorStand.class);
                armorStand.setGravity(false);
                armorStand.setInvulnerable(true);
                armorStand.setGlowing(true);
                armorStand.setCustomName("§dEgg #"+key);
                armorStand.setCustomNameVisible(true);
                armorStand.setSmall(true);
                armorStand.setInvisible(true);
                plugin.getShowedArmorstands().add(armorStand);
                new BukkitRunnable() {
                    int count = Main.getInstance().getConfig().getInt("Settings.ArmorstandGlow");
                    @Override
                    public void run() {
                        count --;
                        if(count == 0){
                            cancel();
                            for(ArmorStand a : Main.getInstance().getShowedArmorstands()){
                                a.remove();
                                plugin.getShowedArmorstands().remove(a);
                            }
                        }
                    }
                }.runTaskTimer(Main.getInstance(),0,20);
            }
        }
    }

    public String getTopPlayerName(){
        HashMap<String, Integer> leaderboard = new HashMap<>();
        for(UUID uuid : plugin.getEggDataManager().savedPlayers()) {
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
            leaderboard.put(playerConfig.getString("FoundEggs.Name"),playerConfig.getInt("FoundEggs.Count"));
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
        for(UUID uuid : plugin.getEggDataManager().savedPlayers()) {
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
            leaderboard.put(playerConfig.getString("FoundEggs.Name"),playerConfig.getInt("FoundEggs.Count"));
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
        for(UUID uuid : plugin.getEggDataManager().savedPlayers()) {
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
            leaderboard.put(playerConfig.getString("FoundEggs.Name"),playerConfig.getInt("FoundEggs.Count"));
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
        for(UUID uuid : plugin.getEggDataManager().savedPlayers()) {
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
            leaderboard.put(playerConfig.getString("FoundEggs.Name"),playerConfig.getInt("FoundEggs.Count"));
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
        for(UUID uuid : plugin.getEggDataManager().savedPlayers()) {
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
            leaderboard.put(playerConfig.getString("FoundEggs.Name"),playerConfig.getInt("FoundEggs.Count"));
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
        for(UUID uuid : plugin.getEggDataManager().savedPlayers()) {
            FileConfiguration playerConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
            leaderboard.put(playerConfig.getString("FoundEggs.Name"),playerConfig.getInt("FoundEggs.Count"));
        }
        if(leaderboard.size() < 3){
            return -1;
        }
        List<Map.Entry<String, Integer>> leaderList = new ArrayList<>(leaderboard.entrySet());

        leaderList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        return leaderList.get(2).getValue();
    }

    private boolean isVersionLessThan(String versionToCompare) {
        String pluginVersion = plugin.getDescription().getVersion();
        String[] currentVersionParts = pluginVersion.split("\\.");
        String[] targetVersionParts = versionToCompare.split("\\.");

        if (currentVersionParts.length == 3 && targetVersionParts.length == 3) {
            int currentMajor = Integer.parseInt(currentVersionParts[0]);
            int currentMinor = Integer.parseInt(currentVersionParts[1]);
            int currentPatch = Integer.parseInt(currentVersionParts[2]);

            int targetMajor = Integer.parseInt(targetVersionParts[0]);
            int targetMinor = Integer.parseInt(targetVersionParts[1]);
            int targetPatch = Integer.parseInt(targetVersionParts[2]);

            if (currentMajor < targetMajor) {
                return true;
            } else if (currentMajor == targetMajor && currentMinor < targetMinor) {
                return true;
            } else return currentMajor == targetMajor && currentMinor == targetMinor && currentPatch < targetPatch;
        }

        return false;
    }

    public void convertEggData() {
        File eggsFile = new File(plugin.getDataFolder(), "eggs.yml");


        if (eggsFile.exists() && isVersionLessThan("2.1.0")) {
            Bukkit.getConsoleSender().sendMessage("§cAdvancedEggHunt found the old configuration system.\n §cIt is now changing to the new one.\n§4THIS CAN TAKE A WHILE!");
            FileConfiguration eggsConfig = YamlConfiguration.loadConfiguration(eggsFile);

            Bukkit.broadcastMessage("§4AdvancedEggHunt IS UPDATING THEIR CONFIGURATION SYSTEM PLEASE DONT MOVE UTIL FINISHED");
            for(Player player : Bukkit.getOnlinePlayers()){
                if(player.isOp())
                    player.sendMessage("§cTHE BEST IS WHEN NOBODY IS ON THE SERVER DURING THE CONVERT. §4CONSOLE SHOWS THE PROGRESS");
            }
            if (eggsConfig.contains("Eggs")) {
                FileConfiguration placedEggsConfig = plugin.getEggDataManager().getPlacedEggs();

                placedEggsConfig.set("PlacedEggs", eggsConfig.getConfigurationSection("Eggs"));
                placedEggsConfig.set("MaxEggs", eggsConfig.getInt("MaxEggs"));
                plugin.getEggDataManager().savePlacedEggs();
                Bukkit.getConsoleSender().sendMessage("§aAdvancedEggHunt converted all egg locations!");

                ArrayList<UUID> convertPlayers = new ArrayList<>();

                for (String uuids : eggsConfig.getConfigurationSection("FoundEggs.").getKeys(false)) {
                    UUID uuid = UUID.fromString(uuids);
                    convertPlayers.add(uuid);
                }
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!convertPlayers.isEmpty()) {
                            List<UUID> playersToConvert = convertPlayers.subList(0, Math.min(5, convertPlayers.size()));

                            for (UUID uuid : playersToConvert) {
                                plugin.getPlayerEggDataManager().createPlayerFile(uuid);
                                FileConfiguration playerEggsConfig = plugin.getPlayerEggDataManager().getPlayerData(uuid);
                                ConfigurationSection foundEggsSection = eggsConfig.getConfigurationSection("FoundEggs." + uuid);
                                if (foundEggsSection != null) {
                                    playerEggsConfig.set("FoundEggs", foundEggsSection);
                                    plugin.getPlayerEggDataManager().savePlayerData(uuid, playerEggsConfig);
                                }
                                Bukkit.getConsoleSender().sendMessage("§eConverted player data of "+uuid+".");
                            }

                            convertPlayers.removeAll(playersToConvert);
                        }else{
                            cancel();
                            Bukkit.broadcastMessage("§aUPDATING DONE!");

                            try {
                                if (eggsFile.delete()) {
                                    Bukkit.getConsoleSender().sendMessage("§aeggs.yml was converted successfully.");
                                } else {
                                    Bukkit.getConsoleSender().sendMessage("§ceggs.yml can't be deleted.");
                                }
                            } catch (SecurityException e) {
                                Bukkit.getConsoleSender().sendMessage("§4There was an error while deleting the eggs.yml file: " + e.getMessage());
                            }
                            Bukkit.getConsoleSender().sendMessage("§aAdvancedEggHunt converted all data to the new system.\n§2The plugin is now running flawless again\n§4Please report bugs on the discord: §9https://discord.gg/qapvQMUt34");
                        }
                    }
                }.runTaskTimer(plugin,0,60);
            }
        }
    }
}
