package de.theredend2000.advancedhunt.util.updater;

public enum UpdateChannel {
    RELEASE(1),
    BETA(2),
    ALPHA(3);

    private final int level;

    UpdateChannel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Checks if a candidate channel is accepted given the required minimum channel.
     * For example, if required is BETA, both RELEASE and BETA are accepted (level <= required.level).
     */
    public boolean accepts(UpdateChannel candidate) {
        if (candidate == null) return false;
        return candidate.level <= this.level;
    }

    public static UpdateChannel fromString(String str) {
        if (str == null) return RELEASE;
        try {
            return UpdateChannel.valueOf(str.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return RELEASE;
        }
    }
}
