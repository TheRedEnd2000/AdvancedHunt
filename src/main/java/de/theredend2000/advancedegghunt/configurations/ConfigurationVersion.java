package de.theredend2000.advancedegghunt.configurations;

import java.util.Map;

public class ConfigurationVersion {
    public final Map<String, String> Upgrade;
    public final Map<String, String> Downgrade;

    public ConfigurationVersion(Map<String, String> upgrade, Map<String, String> downgrade){
        this.Upgrade = upgrade;
        this.Downgrade = downgrade;
    }
}
