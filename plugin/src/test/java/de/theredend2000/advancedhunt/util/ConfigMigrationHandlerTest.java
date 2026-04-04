package de.theredend2000.advancedhunt.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ConfigMigrationHandlerTest {

    private static final String PROGRESS_TITLE_PATH = "gui.progress.title";

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

    @Test
    public void migrateMessagesUpdatesLegacyProgressTitleDefaults() {
        String[][] titlePairs = new String[][] {
            {
                "&8Progress: %collection% &7[&e%progress%&7/&e%total% &7- &e%percentage%&7%]",
                "&8Progress: %collection%"
            },
            {
                "&8Fortschritt: %collection% &7[&e%progress%&7/&e%total% &7- &e%percentage%&7%]",
                "&8Fortschritt: %collection%"
            },
            {
                "&8Tiến trình: %collection% &7[&e%progress%&7/&e%total% &7- &e%percentage%&7%]",
                "&8Tiến trình: %collection%"
            },
            {
                "&8进度：%collection% &7[&e%progress%&7/&e%total% &7- &e%percentage%&7%]",
                "&8进度：%collection%"
            }
        };

        for (String[] titlePair : titlePairs) {
            YamlConfiguration config = new YamlConfiguration();
            config.set(PROGRESS_TITLE_PATH, titlePair[0]);

            ConfigMigrationHandler.migrateMessages(config, "2.0", "2.1");

            assertEquals(titlePair[1], config.getString(PROGRESS_TITLE_PATH));
        }
    }

    @Test
    public void migrateMessagesLeavesCustomProgressTitleUntouched() {
        YamlConfiguration config = new YamlConfiguration();
        config.set(PROGRESS_TITLE_PATH, "&8Collection: %collection% &7(%progress%/%total%)");

        ConfigMigrationHandler.migrateMessages(config, "2.0", "2.1");

        assertEquals("&8Collection: %collection% &7(%progress%/%total%)", config.getString(PROGRESS_TITLE_PATH));
    }

    @Test
    public void bundledMessageFilesStayAtMigrationVersion() {
        for (String resourcePath : Arrays.asList(
            "messages/messages_en.yml",
            "messages/messages_de.yml",
            "messages/messages_vi.yml",
            "messages/messages_zh.yml",
            "messages/messages_bn.yml"
        )) {
            InputStream resource = ConfigMigrationHandlerTest.class.getClassLoader().getResourceAsStream(resourcePath);
            assertNotNull(resourcePath, resource);

            YamlConfiguration config = YamlConfiguration.loadConfiguration(
                new InputStreamReader(resource, StandardCharsets.UTF_8)
            );
            assertEquals(resourcePath, "2.1", String.valueOf(config.get("config-version")));
        }
    }
}