package de.theredend2000.advancedegghunt.configurations.enums;

public enum Permission {
    BreakEggPermission,
    PlaceEggPermission,
    IgnoreCooldownPermission,
    ChangeCollectionsPermission;
    public enum AdvancedEggHuntCommandPermissionCommand {
        placeEggs,
        list,
        show,
        reload,
        help,
        settings,
        collection,
        progress,
        commands,
        leaderboard,
        hint,
        reset;

        @Override
        public String toString() {
            return "AdvancedEggHuntCommandPermission.commands." + this.name();
        }
    }
}
