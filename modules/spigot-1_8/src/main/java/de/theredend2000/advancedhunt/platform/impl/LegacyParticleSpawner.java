package de.theredend2000.advancedhunt.platform.impl;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

final class LegacyParticleSpawner {

    private static final Map<String, Effect> EFFECTS = new HashMap<>();

    static {
        // Common mappings from modern particle names to 1.8 Effect names.
        // This is intentionally small; unknown names are ignored.
        register("VILLAGER_HAPPY", "HAPPY_VILLAGER");
        register("SMOKE", "SMOKE");
        register("SMOKE_NORMAL", "SMOKE");
        register("SPLASH", "SPLASH");
        register("WATER_SPLASH", "SPLASH");
        register("WATER_BUBBLE", "BUBBLE");
        register("WATER_DROP", "RAIN");
        register("EXPLOSION_NORMAL", "EXPLOSION");
        register("EXPLOSION_LARGE", "EXPLOSION");
        register("SPELL_WITCH", "WITCH_MAGIC");
        register("SPELL_INSTANT", "INSTANT_SPELL");
        register("SPELL_MOB", "MOBSPAWNER_FLAMES");
        register("REDSTONE", "COLOURED_DUST");
        register("SNOWBALL", "SNOWBALL_BREAK");
        register("SLIME", "SLIME");
        register("BLOCK_CRACK", "TILE_BREAK");
        register("TOTEM", "MOBSPAWNER_FLAMES");
    }

    private static void register(String key, String effectName) {
        try {
            EFFECTS.put(key, Effect.valueOf(effectName));
        } catch (Throwable ignored) {
        }
    }

    void spawn(Location location, String particleName, int count,
               double offsetX, double offsetY, double offsetZ, double speed) {
        if (location == null) return;
        World world = location.getWorld();
        if (world == null) return;
        if (particleName == null || particleName.trim().isEmpty()) return;

        Effect effect = EFFECTS.get(particleName.trim().toUpperCase());
        if (effect == null) {
            // Best-effort: try direct Effect name.
            try {
                effect = Effect.valueOf(particleName.trim().toUpperCase());
            } catch (Throwable ignored) {
                return;
            }
        }

        try {
            // Use Spigot API directly (no reflection).
            world.spigot().playEffect(
                    location,
                    effect,
                    0,
                    0,
                    (float) offsetX,
                    (float) offsetY,
                    (float) offsetZ,
                    (float) speed,
                    count,
                    64
            );
        } catch (Throwable ignored) {
        }
    }
}
