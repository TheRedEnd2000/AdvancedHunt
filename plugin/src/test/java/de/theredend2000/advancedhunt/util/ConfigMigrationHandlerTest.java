package de.theredend2000.advancedhunt.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class ConfigMigrationHandlerTest {

    @Test
    public void applyMigrationsRunsMinorVersionsInOrder() {
        YamlConfiguration config = new YamlConfiguration();
        List<String> applied = new ArrayList<>();

        NavigableMap<String, Consumer<org.bukkit.configuration.file.FileConfiguration>> migrations = new TreeMap<>(new VersionComparator());
        migrations.put("4.1", cfg -> applied.add("4.1"));
        migrations.put("4.2", cfg -> applied.add("4.2"));

        ConfigMigrationHandler.applyMigrations(
            config,
            "4.0",
            "4.2",
            migrations
        );

        assertEquals(2, applied.size());
        assertEquals("4.1", applied.get(0));
        assertEquals("4.2", applied.get(1));
    }

    @Test
    public void applyMigrationsSkipsEquivalentTrailingZeroRange() {
        YamlConfiguration config = new YamlConfiguration();
        List<String> applied = new ArrayList<>();

        NavigableMap<String, Consumer<org.bukkit.configuration.file.FileConfiguration>> migrations = new TreeMap<>(new VersionComparator());
        migrations.put("4.0", cfg -> applied.add("4.0"));
        migrations.put("4.1", cfg -> applied.add("4.1"));

        ConfigMigrationHandler.applyMigrations(
            config,
            "4",
            "4.0",
            migrations
        );

        assertEquals(0, applied.size());
    }
}