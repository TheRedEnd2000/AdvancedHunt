package de.theredend2000.advancedhunt.managers;

import com.cryptomorin.xseries.XEntityType;
import de.theredend2000.advancedhunt.Main;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;

import java.util.*;

public class FireworkManager {

    private final Main plugin;
    private final List<UUID> fireworkUUIDs = new ArrayList<>();
    private final Random random;

    public FireworkManager(Main plugin) {
        this.plugin = plugin;
        this.random = plugin.getRandom();
    }

    public void spawnFireworkRocket(Location location) {
        if (!plugin.getConfig().getBoolean("fireworks.enabled", true)) return;
        if (location.getWorld() == null) return;

        Firework firework = (Firework) location.getWorld().spawnEntity(
                location,
                Objects.requireNonNull(XEntityType.FIREWORK_ROCKET.get())
        );

        firework.setSilent(plugin.getConfig().getBoolean("fireworks.silent", false));

        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(getRandomPower());
        meta.addEffect(buildEffect());

        firework.setFireworkMeta(meta);
        fireworkUUIDs.add(firework.getUniqueId());
    }

    private FireworkEffect buildEffect() {
        ConfigurationSection effectsSec = plugin.getConfig().getConfigurationSection("fireworks.effects");

        FireworkEffect.Builder builder = FireworkEffect.builder()
                .flicker(effectsSec.getBoolean("flicker", true))
                .trail(effectsSec.getBoolean("trail", true))
                .with(getRandomType(effectsSec));

        // Add main colors
        builder.withColor(getColors("fireworks.colors"));

        // Add fade if active
        if (plugin.getConfig().getBoolean("fireworks.fade-colors.enabled", true)) {
            builder.withFade(getColors("fireworks.fade-colors"));
        }

        return builder.build();
    }

    private int getRandomPower() {
        int min = plugin.getConfig().getInt("fireworks.power.min", 1);
        int max = plugin.getConfig().getInt("fireworks.power.max", 2);
        return random.nextInt(Math.max(1, max - min + 1)) + min;
    }

    private FireworkEffect.Type getRandomType(ConfigurationSection effectsSec) {
        List<String> types = effectsSec.getStringList("types");

        if (types.isEmpty()) {
            return FireworkEffect.Type.BALL;
        }

        String randomType = types.get(random.nextInt(types.size()));

        try {
            return FireworkEffect.Type.valueOf(randomType.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid typ: " + randomType + " - using BALL!");
            return FireworkEffect.Type.BALL;
        }
    }

    private List<Color> getColors(String path) {
        ConfigurationSection colorSec = plugin.getConfig().getConfigurationSection(path);

        if (colorSec == null) {
            return List.of(Color.WHITE);
        }

        List<String> colorNames = colorSec.getStringList("list");
        List<Color> colors = new ArrayList<>();

        for (String colorName : colorNames) {
            try {
                Color color = (Color) Color.class.getField(colorName.toUpperCase()).get(null);
                colors.add(color);
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid color: " + colorName + " - skipped!");
            }
        }

        // if there's no color
        if (colors.isEmpty()) {
            colors.add(Color.WHITE);
        }

        // return random color
        return List.of(colors.get(random.nextInt(colors.size())));
    }

    public List<UUID> getFireworkUUIDs() {
        return fireworkUUIDs;
    }
}