package de.theredend2000.advancedhunt.model;

import java.util.UUID;

public class PlacePreset {
    private final UUID id;
    private String group;
    private String name;

    /**
     * Base64 serialized ItemStack via {@link de.theredend2000.advancedhunt.util.ItemSerializer}.
     */
    private String itemData;

    public PlacePreset(UUID id, String group, String name, String itemData) {
        this.id = id;
        this.group = group;
        this.name = name;
        this.itemData = itemData;
    }

    public UUID getId() {
        return id;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getItemData() {
        return itemData;
    }

    public void setItemData(String itemData) {
        this.itemData = itemData;
    }
}
