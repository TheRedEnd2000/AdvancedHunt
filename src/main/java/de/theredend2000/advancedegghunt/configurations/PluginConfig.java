package de.theredend2000.advancedegghunt.configurations;

import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.particles.XParticle;
import de.theredend2000.advancedegghunt.Main;
import de.theredend2000.advancedegghunt.util.enums.Permission;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.text.MessageFormat;
import java.util.Set;
import java.util.UUID;

public class PluginConfig extends Configuration {
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

    public void saveData() {
        saveConfig();
    }

    public double getConfigVersion() {
        return getConfig().getDouble("config-version");
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

    //region Permission
    public Boolean getPermissionEnabled(Permission permission) {
        return getConfig().getBoolean(MessageFormat.format("Permissions.{0}.use", permission.toString()));
    }
	public void setPermissionEnabled(Permission permission, String Permission) {
		getConfig().set(MessageFormat.format("Permissions.{0}.use", permission.toString()), Permission);
	}

    public String getPermission(Permission permission) {
        return getConfig().getString(MessageFormat.format("Permissions.{0}.permission", permission.toString()));
    }
    public void setPermission(Permission permission, String Permission) {
        getConfig().set(MessageFormat.format("Permissions.{0}.permission", permission.toString()), Permission);
    }

    public Boolean getPermissionEnabled(Permission.AdvancedEggHuntCommandPermissionCommand permission) {
        return getConfig().getBoolean(MessageFormat.format("Permissions.{0}.use", permission.toString()));
    }
    public void setPermissionEnabled(Permission.AdvancedEggHuntCommandPermissionCommand permission, String Permission) {
        getConfig().set(MessageFormat.format("Permissions.{0}.use", permission.toString()), Permission);
    }

    public String getPermission(Permission.AdvancedEggHuntCommandPermissionCommand permission) {
        return getConfig().getString(MessageFormat.format("Permissions.{0}.permission", permission.toString()));
    }
    public void setPermission(Permission.AdvancedEggHuntCommandPermissionCommand permission, String Permission) {
        getConfig().set(MessageFormat.format("Permissions.{0}.permission", permission.toString()), Permission);
    }
    //endregion

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

    public boolean getDisableCommandFeedback() {
        return getConfig().getBoolean("Settings.DisableCommandFeedback");
    }
	public void setDisableCommandFeedback(boolean DisableCommandFeedback) {
		getConfig().set("Settings.DisableCommandFeedback", DisableCommandFeedback);
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

    public XMaterial getRewardInventoryMaterial() {
        return Main.getMaterial(getConfig().getString("Settings.RewardInventoryMaterial"));
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

    public Integer getHintUpdateTime() {
        return getConfig().getInt("Settings.HintUpdateTime");
    }
	public void setHintUpdateTime(Integer HintUpdateTime) {
		getConfig().set("Settings.HintUpdateTime", HintUpdateTime);
	}

    //endregion

    //region Sounds
    public Sound getPlayerFindEggSound() {
        return XSound.valueOf(getConfig().getString("Sounds.PlayerFindEggSound")).parseSound();
    }
	public void setPlayerFindEggSound(XMaterial PlayerFindEggSound) {
		getConfig().set("Sounds.PlayerFindEggSound", PlayerFindEggSound.toString());
	}

    public Sound getEggAlreadyFoundSound() {
        return XSound.valueOf(getConfig().getString("Sounds.EggAlreadyFoundSound")).parseSound();
    }
	public void setEggAlreadyFoundSound(XMaterial EggAlreadyFoundSound) {
		getConfig().set("Sounds.EggAlreadyFoundSound", EggAlreadyFoundSound.toString());
	}

    public Sound getAllEggsFoundSound() {
        return XSound.valueOf(getConfig().getString("Sounds.AllEggsFoundSound")).parseSound();
    }
	public void setAllEggsFoundSound(XMaterial AllEggsFoundSound) {
		getConfig().set("Sounds.AllEggsFoundSound", AllEggsFoundSound.toString());
	}

    public Sound getEggBreakSound() {
        return XSound.valueOf(getConfig().getString("Sounds.EggBreakSound")).parseSound();
    }
	public void setEggBreakSound(XMaterial EggBreakSound) {
		getConfig().set("Sounds.EggBreakSound", EggBreakSound.toString());
	}

    public Sound getEggPlaceSound() {
        return XSound.valueOf(getConfig().getString("Sounds.EggPlaceSound")).parseSound();
    }
	public void setEggPlaceSound(XMaterial EggPlaceSound) {
		getConfig().set("Sounds.EggPlaceSound", EggPlaceSound.toString());
	}

    public Sound getErrorSound() {
        return XSound.valueOf(getConfig().getString("Sounds.ErrorSound")).parseSound();
    }
	public void setErrorSound(XMaterial ErrorSound) {
		getConfig().set("Sounds.ErrorSound", ErrorSound.toString());
	}

    public Sound getInventoryClickSuccess() {
        return XSound.valueOf(getConfig().getString("Sounds.InventoryClickSuccess")).parseSound();
    }
	public void setInventoryClickSuccess(XMaterial InventoryClickSuccess) {
		getConfig().set("Sounds.InventoryClickSuccess", InventoryClickSuccess.toString());
	}

    public Sound getInventoryClickFailed() {
        return XSound.valueOf(getConfig().getString("Sounds.InventoryClickFailed")).parseSound();
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
        return XParticle.getParticle(getConfig().getString("Particle.type.EggFound", "CRIT"));
    }
	public void setEggFoundParticle(Particle EggFoundParticle) {
		getConfig().set("Particle.type.EggFound", EggFoundParticle.toString());
	}

    @Nullable
    public Particle getEggNotFoundParticle() {
        return XParticle.getParticle(getConfig().getString("Particle.type.EggNotFound", "VILLAGER_HAPPY"));
    }
	public void setEggNotFoundParticle(Particle EggNotFoundParticle) {
		getConfig().set("Particle.type.EggNotFound", EggNotFoundParticle.toString());
	}
    //endregion

    //region PlaceholderAPI
    public String getCollection() {
        return getConfig().getString("PlaceholderAPI.collection");
    }
	public void setCollection(String Collection) {
		getConfig().set("PlaceholderAPI.collection", Collection);
	}

    public String getName() {
        return getConfig().getString("PlaceholderAPI.name");
    }
	public void setName(String Name) {
		getConfig().set("PlaceholderAPI.name", Name);
	}

    public String getCount() {
        return getConfig().getString("PlaceholderAPI.count");
    }
	public void setCount(String Count) {
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
	public void setPlaceEggTexture(int id, Integer PlaceEggTexture) {
		getConfig().set(MessageFormat.format("PlaceEggs.{0}.texture", id), PlaceEggTexture);
	}

    public String getPlaceEggType(String id) {
        return getConfig().getString(MessageFormat.format("PlaceEggs.{0}.type", id));
    }
    public void setPlaceEggType(int id, String PlaceEggType) {
        getConfig().set(MessageFormat.format("PlaceEggs.{0}.type", id), PlaceEggType);
    }
    //endregion
}
