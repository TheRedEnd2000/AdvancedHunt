package de.theredend2000.advancedegghunt.util.messages;

public enum MenuMessageKey {
    MAIN_MENU_TITLE("main_menu_title"),
    SETTINGS_BUTTON("settings_button"),
    EGGS_BUTTON("eggs_button"),
    STATISTICS_BUTTON("statistics_button"),
    HELP_BUTTON("help_button"),

    SETTINGS_MENU_TITLE("settings_menu_title"),
    LANGUAGE_BUTTON("language_button"),
    SOUND_VOLUME_BUTTON("sound_volume_button"),
    EGG_VISIBILITY_BUTTON("egg_visibility_button"),
    ARMORSTAND_GLOW_BUTTON("armorstand_glow_button"),

    EGGS_MENU_TITLE("eggs_menu_title"),
    PLACE_EGG_BUTTON("place_egg_button"),
    EDIT_EGG_BUTTON("edit_egg_button"),
    REMOVE_EGG_BUTTON("remove_egg_button"),
    TELEPORT_TO_EGG_BUTTON("teleport_to_egg_button"),

    STATISTICS_MENU_TITLE("statistics_menu_title"),
    TOTAL_EGGS_FOUND("total_eggs_found"),
    EGGS_FOUND_TODAY("eggs_found_today"),
    TOP_FINDERS("top_finders"),

    CONFIRM_MENU_TITLE("confirm_menu_title"),
    CONFIRM_BUTTON("confirm_button"),
    CANCEL_BUTTON("cancel_button"),

    BACK_BUTTON("back_button"),
    NEXT_PAGE_BUTTON("next_page_button"),
    PREVIOUS_PAGE_BUTTON("previous_page_button"),
    CLOSE_BUTTON("close_button");

    private final String path;

    MenuMessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
