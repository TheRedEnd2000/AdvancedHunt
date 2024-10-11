package de.theredend2000.advancedhunt.util.enums;

import de.theredend2000.advancedhunt.Main;

import java.text.MessageFormat;
import java.util.logging.Level;

public enum Permission {
    BreakEgg,
    PlaceEgg,
    IgnoreCooldown,
    ChangeCollections,
    CreateCollection,
    OpenRewards,
    FindEggs;
    @Override
    public String toString() {
        return "AdvancedEggHunt." + this.name();
    }

    public static Permission getEnum(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, MessageFormat.format("Failed to convert {0} to Enum.", value), e);
            return null;
        }
    }
    public enum Command {
        PLACE,
        IMPORT,
        LIST,
        SHOW,
        RELOAD,
        HELP,
        SETTINGS,
        COLLECTION,
        PROGRESS,
        LEADERBOARD,
        HINT,
        RESET;

        @Override
        public String toString() {
            return "AdvancedEggHunt.Command." + this.name().toLowerCase();
        }

        public static Command getEnum(String value) {
            try {
                return valueOf(value.toUpperCase());
            } catch (IllegalArgumentException e) {
                Main.getInstance().getLogger().log(Level.SEVERE, MessageFormat.format("Failed to convert {0} to Enum.", value), e);
                return null;
            }
        }
    }
}
