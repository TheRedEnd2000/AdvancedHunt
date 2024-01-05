package de.theredend2000.advancedegghunt.managers;

import de.theredend2000.advancedegghunt.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;

public class CooldownManager {

    private final Main plugin;

    public CooldownManager(Main plugin){
        this.plugin = plugin;
    }

    public File getRewardFile() {
        return new File(plugin.getDataFolder(), "cooldown.yml");
    }

    public void setCooldown(Player player) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(getRewardFile());
        long toSet = System.currentTimeMillis()+(Main.getInstance().getConfig().getLong("Settings.HintCooldownSeconds") * 1000);
        cfg.set(player.getUniqueId() + ".cooldownTime", toSet);
        try {
            cfg.save(getRewardFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getCooldown(Player player) {
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(getRewardFile());
        return cfg.getLong(player.getUniqueId() + ".cooldownTime");
    }

    public String getRemainingTime(long millis) {
        long seconds = millis/1000;
        long minutes = 0;
        while(seconds > 60) {
            seconds-=60;
            minutes++;
        }
        long hours = 0;
        while(minutes > 60) {
            minutes-=60;
            hours++;
        }
        long days = 0;
        while(hours > 24) {
            hours-=24;
            days++;
        }
        return Main.getInstance().getMessage("OnCooldown").replaceAll("%DAYS%", String.valueOf(days)).replaceAll("%HOURS%", String.valueOf(hours)).replaceAll("%MINUTES%", String.valueOf(minutes)).replaceAll("%SECONDS%", String.valueOf(seconds));
    }


    public boolean isAllowReward(Player player) {
        long current = System.currentTimeMillis();
        long millis = getCooldown(player);
        return current > millis;
    }
}
