package de.theredend2000.advancedhunt.managers;

public class TreasureWorldEdit {

    public enum Action {
        RESTORE,
        REMOVE
    }

    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private final String material;
    private final String blockState;
    private final String nbtData;
    private final Action action;

    public TreasureWorldEdit(String worldName, int x, int y, int z, String material, String blockState, String nbtData, Action action) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.material = material;
        this.blockState = blockState;
        this.nbtData = nbtData;
        this.action = action;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public String getMaterial() {
        return material;
    }

    public String getBlockState() {
        return blockState;
    }

    public String getNbtData() {
        return nbtData;
    }

    public Action getAction() {
        return action;
    }

    public int getChunkX() {
        return x >> 4;
    }

    public int getChunkZ() {
        return z >> 4;
    }
}
