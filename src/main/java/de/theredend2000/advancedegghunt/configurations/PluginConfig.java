package de.theredend2000.advancedegghunt.configurations;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.particles.XParticle;
import de.theredend2000.advancedegghunt.util.XHelper;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.*;

public class PluginConfig extends Configuration {
    private static TreeMap<Double, ConfigUpgrader> upgraders = new TreeMap<>();

    private static volatile PluginConfig instance;

    private PluginConfig(JavaPlugin plugin) {
        super(plugin, "config.yml");
    }

    public static PluginConfig getInstance(JavaPlugin plugin) {
        if (instance == null) {
            synchronized (PluginConfig.class) {
                if (instance == null) {
                    instance = new PluginConfig(plugin);
                }
            }
        }
        return instance;
    }

    @Override
    public TreeMap<Double, ConfigUpgrader> getUpgrader() {
        return upgraders;
    }

    public void saveData() {
        saveConfig();
    }

    public double getConfigVersion() {
        return getConfig().getDouble("config-version", -1);
    }
	public void setConfigVersion(double ConfigVersion) {
		getConfig().set("config-version", ConfigVersion);
	}

    public String getLanguage() {
        return getConfig().getString("messages-lang");
    }
    public void setLanguage(String language) {
        getConfig().set("messages-lang", language);
    }

    public String getPrefix() {
        return getConfig().getString("prefix");
    }
	public void setPrefix(String Prefix) {
		getConfig().set("prefix", Prefix);
	}

    public String getEdit(UUID playerUuid) {
        return getConfig().getString(MessageFormat.format("Edit.{0}.commandID", playerUuid));
    }

    public void setEdit(UUID playerUuid, String id) {
        getConfig().set(MessageFormat.format("Edit.{0}.commandID", playerUuid), id);
    }

    //region Settings
    public Integer getSoundVolume() {
        return getConfig().getInt("Settings.SoundVolume");
    }
	public void setSoundVolume(Integer SoundVolume) {
		getConfig().set("Settings.SoundVolume", SoundVolume);
	}

    public boolean getUpdater() {
        return getConfig().getBoolean("Settings.Updater");
    }
	public void setUpdater(boolean Updater) {
		getConfig().set("Settings.Updater", Updater);
	}

    public boolean getPlayerFoundOneEggRewards() {
        return getConfig().getBoolean("Settings.PlayerFoundOneEggRewards");
    }
	public void setPlayerFoundOneEggRewards(boolean PlayerFoundOneEggRewards) {
		getConfig().set("Settings.PlayerFoundOneEggRewards", PlayerFoundOneEggRewards);
	}

    public boolean getPlayerFoundAllEggsReward() {
        return getConfig().getBoolean("Settings.PlayerFoundAllEggsReward");
    }
	public void setPlayerFoundAllEggsReward(boolean PlayerFoundAllEggsReward) {
		getConfig().set("Settings.PlayerFoundAllEggsReward", PlayerFoundAllEggsReward);
	}

    public boolean getShowCoordinatesWhenEggFoundInProgressInventory() {
        return getConfig().getBoolean("Settings.ShowCoordinatesWhenEggFoundInProgressInventory");
    }
	public void setShowCoordinatesWhenEggFoundInProgressInventory(boolean ShowCoordinatesWhenEggFoundInProgressInventory) {
		getConfig().set("Settings.ShowCoordinatesWhenEggFoundInProgressInventory", ShowCoordinatesWhenEggFoundInProgressInventory);
	}

    public boolean getShowFireworkAfterEggFound() {
        return getConfig().getBoolean("Settings.ShowFireworkAfterEggFound");
    }
	public void setShowFireworkAfterEggFound(boolean ShowFireworkAfterEggFound) {
		getConfig().set("Settings.ShowFireworkAfterEggFound", ShowFireworkAfterEggFound);
	}

    public Integer getArmorstandGlow() {
        return getConfig().getInt("Settings.ArmorstandGlow");
    }
	public void setArmorstandGlow(Integer ArmorstandGlow) {
		getConfig().set("Settings.ArmorstandGlow", ArmorstandGlow);
	}

    public Integer getShowEggsNearbyMessageRadius() {
        return getConfig().getInt("Settings.ShowEggsNearbyMessageRadius");
    }
	public void setShowEggsNearbyMessageRadius(Integer ShowEggsNearbyMessageRadius) {
		getConfig().set("Settings.ShowEggsNearbyMessageRadius", ShowEggsNearbyMessageRadius);
	}

    public boolean getPluginPrefixEnabled() {
        return getConfig().getBoolean("Settings.PluginPrefixEnabled");
    }
	public void setPluginPrefixEnabled(boolean PluginPrefixEnabled) {
		getConfig().set("Settings.PluginPrefixEnabled", PluginPrefixEnabled);
	}

    public boolean getLeftClickEgg() {
        return getConfig().getBoolean("Settings.LeftClickEgg");
    }
	public void setLeftClickEgg(boolean LeftClickEgg) {
		getConfig().set("Settings.LeftClickEgg", LeftClickEgg);
	}

    public Boolean getRightClickEgg() {
        return getConfig().getBoolean("Settings.RightClickEgg");
    }
	public void setRightClickEgg(Integer RightClickEgg) {
		getConfig().set("Settings.RightClickEgg", RightClickEgg);
	}

    public Integer getHintCount() {
        return getConfig().getInt("Settings.HintCount");
    }
	public void setHintCount(Integer HintCount) {
		getConfig().set("Settings.HintCount", HintCount);
	}

    public Integer getHintCooldownSeconds() {
        return getConfig().getInt("Settings.HintCooldownSeconds");
    }
	public void setHintCooldownSeconds(Integer HintCooldownSeconds) {
		getConfig().set("Settings.HintCooldownSeconds", HintCooldownSeconds);
	}

    public Boolean getHintApplyCooldownOnFail() {
        return getConfig().getBoolean("Settings.HintApplyCooldownOnFail");
    }
    public void setHintApplyCooldownOnFails(Boolean HintApplyCooldownOnFail) {
        getConfig().set("Settings.HintApplyCooldownOnFail", HintApplyCooldownOnFail);
    }

    public Integer getHintUpdateTime() {
        return getConfig().getInt("Settings.HintUpdateTime");
    }
	public void setHintUpdateTime(Integer HintUpdateTime) {
		getConfig().set("Settings.HintUpdateTime", HintUpdateTime);
	}

    //endregion

    //region Sounds
    public Sound getPlayerFindEggSound() {
        return XHelper.ParseSound(getConfig().getString("Sounds.PlayerFindEggSound"), XSound.ENTITY_PLAYER_LEVELUP).parseSound();
    }
	public void setPlayerFindEggSound(XMaterial PlayerFindEggSound) {
		getConfig().set("Sounds.PlayerFindEggSound", PlayerFindEggSound.toString());
	}

    public Sound getEggAlreadyFoundSound() {
        return XHelper.ParseSound(getConfig().getString("Sounds.EggAlreadyFoundSound"), XSound.ENTITY_VILLAGER_NO).parseSound();
    }
	public void setEggAlreadyFoundSound(XMaterial EggAlreadyFoundSound) {
		getConfig().set("Sounds.EggAlreadyFoundSound", EggAlreadyFoundSound.toString());
	}

    public Sound getAllEggsFoundSound() {
        return XHelper.ParseSound(getConfig().getString("Sounds.AllEggsFoundSound"), XSound.ENTITY_ENDER_DRAGON_DEATH).parseSound();
    }
	public void setAllEggsFoundSound(XMaterial AllEggsFoundSound) {
		getConfig().set("Sounds.AllEggsFoundSound", AllEggsFoundSound.toString());
	}

    public Sound getEggBreakSound() {
        return XHelper.ParseSound(getConfig().getString("Sounds.EggBreakSound"), XSound.BLOCK_NOTE_BLOCK_BELL).parseSound();
    }
	public void setEggBreakSound(XMaterial EggBreakSound) {
		getConfig().set("Sounds.EggBreakSound", EggBreakSound.toString());
	}

    public Sound getEggPlaceSound() {
        return XHelper.ParseSound(getConfig().getString("Sounds.EggPlaceSound"), XSound.BLOCK_NOTE_BLOCK_BELL).parseSound();
    }
	public void setEggPlaceSound(XMaterial EggPlaceSound) {
		getConfig().set("Sounds.EggPlaceSound", EggPlaceSound.toString());
	}

    public Sound getErrorSound() {
        return XHelper.ParseSound(getConfig().getString("Sounds.ErrorSound"), XSound.BLOCK_NOTE_BLOCK_BASEDRUM).parseSound();
    }
	public void setErrorSound(XMaterial ErrorSound) {
		getConfig().set("Sounds.ErrorSound", ErrorSound.toString());
	}

    public Sound getInventoryClickSuccess() {
        return XHelper.ParseSound(getConfig().getString("Sounds.InventoryClickSuccess"), XSound.BLOCK_NOTE_BLOCK_CHIME).parseSound();
    }
	public void setInventoryClickSuccess(XMaterial InventoryClickSuccess) {
		getConfig().set("Sounds.InventoryClickSuccess", InventoryClickSuccess.toString());
	}

    public Sound getInventoryClickFailed() {
        return XHelper.ParseSound(getConfig().getString("Sounds.InventoryClickFailed"), XSound.BLOCK_NOTE_BLOCK_HAT).parseSound();
    }
	public void setInventoryClickFailed(XMaterial InventoryClickFailed) {
		getConfig().set("Sounds.InventoryClickFailed", InventoryClickFailed.toString());
	}

    //endregion

    //region Particle
    public boolean getParticleEnabled() {
        return getConfig().getBoolean("Particle.enabled");
    }
	public void setParticleEnabled(boolean ParticleEnabled) {
		getConfig().set("Particle.enabled", ParticleEnabled);
	}

    @Nullable
    public Particle getEggFoundParticle() {
        return XHelper.ParseParticle(getConfig().getString("Particle.type.EggFound", "CRIT"), XParticle.getParticle("CRIT"));
    }
	public void setEggFoundParticle(Particle EggFoundParticle) {
		getConfig().set("Particle.type.EggFound", EggFoundParticle.toString());
	}

    @Nullable
    public Particle getEggNotFoundParticle() {
        return XHelper.ParseParticle(getConfig().getString("Particle.type.EggNotFound", "VILLAGER_HAPPY"), XParticle.getParticle("VILLAGER_HAPPY"));
    }
	public void setEggNotFoundParticle(Particle EggNotFoundParticle) {
		getConfig().set("Particle.type.EggNotFound", EggNotFoundParticle.toString());
	}
    //endregion

    //region PlaceholderAPI
    public String getPlaceholderAPICollection() {
        return getConfig().getString("PlaceholderAPI.collection");
    }
	public void setPlaceholderAPICollection(String Collection) {
		getConfig().set("PlaceholderAPI.collection", Collection);
	}

    public String getPlaceholderAPIName() {
        return getConfig().getString("PlaceholderAPI.name");
    }
	public void setPlaceholderAPIName(String Name) {
		getConfig().set("PlaceholderAPI.name", Name);
	}

    public String getPlaceholderAPICount() {
        return getConfig().getString("PlaceholderAPI.count");
    }
	public void setPlaceholderAPICount(String Count) {
		getConfig().set("PlaceholderAPI.count", Count);
	}
    //endregion

    //region PlaceEggs
    public Boolean hasPlaceEggs() {
        return getConfig().contains("PlaceEggs.");
    }
    public Set<String> getPlaceEggIds() {
        return getConfig().getConfigurationSection("PlaceEggs.").getKeys(false);
    }
    public String getPlaceEggTexture(String id) {
        return getConfig().getString(MessageFormat.format("PlaceEggs.{0}.texture", id));
    }
	public void setPlaceEggTexture(int id, String PlaceEggTexture) {
		getConfig().set(MessageFormat.format("PlaceEggs.{0}.texture", id), PlaceEggTexture);
	}

    public String getPlaceEggType(String id) {
        return getConfig().getString(MessageFormat.format("PlaceEggs.{0}.type", id));
    }
    public void setPlaceEggType(int id, String PlaceEggType) {
        getConfig().set(MessageFormat.format("PlaceEggs.{0}.type", id), PlaceEggType);
    }

    public void setPlaceEggPlayerHead(String base64Texture) {
        List<String> ids = List.copyOf(getPlaceEggIds());
        int id = Integer.parseInt(ids.get(ids.size() - 1)) + 1;
        setPlaceEggTexture(id, base64Texture);
        setPlaceEggType(id, "PLAYER_HEAD");
    }

    public void setDefaultIndividualLoadingPreset(String preset){
        getConfig().set("Presets.DefaultIndividualPresetLoad", preset);
        saveConfig();
    }

    public String getDefaultIndividualLoadingPreset(){
        return getConfig().getString("Presets.DefaultIndividualPresetLoad");
    }

    public void setDefaultGlobalLoadingPreset(String preset){
        getConfig().set("Presets.DefaultGlobalPresetLoad", preset);
        saveConfig();
    }

    public String getDefaultGlobalLoadingPreset(){
        return getConfig().getString("Presets.DefaultGlobalPresetLoad");
    }

    public boolean isCommandBlacklisted(String command){
        return getConfig().getStringList("BlacklistedCommands").contains(command.split(" ")[0]);
    }

    @Override
    public void registerUpgrader() {
        upgraders.put(3.1, (oldConfig, NewConfig) -> {
            List<String> blacklistedCommands  = NewConfig.getStringList("BlacklistedCommands");
            LinkedHashSet<String> blacklistedCommandsSet = new LinkedHashSet<String>(blacklistedCommands);
            blacklistedCommandsSet.add("restart");
            blacklistedCommandsSet.add("minecraft:restart");
            blacklistedCommandsSet.add("execute");
            blacklistedCommandsSet.add("minecraft:execute");
            blacklistedCommandsSet.add("setblock");
            blacklistedCommandsSet.add("minecraft:setblock");
            blacklistedCommandsSet.add("fill");
            blacklistedCommandsSet.add("minecraft:fill");
            blacklistedCommandsSet.add("reload");
            blacklistedCommandsSet.add("minecraft:reload");
            blacklistedCommandsSet.add("rl");
            blacklistedCommandsSet.add("minecraft:rl");
            NewConfig.set("BlacklistedCommands", new ArrayList<>(blacklistedCommandsSet));
        });
    }

    //Downloader

    public boolean getAutoDownloadAdvancedEggHunt() {
        return getConfig().getBoolean("Download.AdvancedEggHunt");
    }

    public boolean getAutoDownloadNBTAPI() {
        return getConfig().getBoolean("Download.NBT-API");
    }
    //endregion
}
