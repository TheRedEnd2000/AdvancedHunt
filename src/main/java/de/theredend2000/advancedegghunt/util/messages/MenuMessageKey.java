package de.theredend2000.advancedegghunt.util.messages;

public enum MenuMessageKey {
    // General buttons
    NEXT_PAGE_BUTTON("next_page_button"),
    PREVIOUS_PAGE_BUTTON("previous_page_button"),
    CLOSE_BUTTON("close_button"),
    SORT_BUTTON("sort_button"),
    REFRESH_BUTTON("refresh_button"),
    BACK_BUTTON("back_button"),
    CREATE_BUTTON("create_button"),

    // Collection related
    SELECTED_COLLECTION_BUTTON("selected_collection_button"),
    ADD_COLLECTION_BUTTON("add_collection_button"),
    COLLECTION_ITEM("collection_item"),

    // Egg related
    NO_EGGS_AVAILABLE("no_eggs_available"),
    EGG_ITEM("egg_item"),
    EGG_TYPES_BUTTON("egg_types_button"),
    FINISH_SETUP_BUTTON("finish_setup_button"),
    EASTER_EGG_ITEM("easter_egg_item"),

    // Player related
    NO_PLAYER("no_player"),
    PLAYER_ITEM("player_item"),

    // Leaderboard related
    LEADERBOARD_SORT_ALL("leaderboard_sort_all"),
    LEADERBOARD_SORT_TOP10("leaderboard_sort_top10"),
    LEADERBOARD_SORT_TOP3("leaderboard_sort_top3"),
    LEADERBOARD_SORT_YOU("leaderboard_sort_you"),

    // Reward related
    NO_REWARDS("no_rewards"),
    SAVE_PRESET_BUTTON("save_preset_button"),
    LOAD_PRESETS_BUTTON("load_presets_button"),
    CREATE_REWARD_BUTTON("create_reward_button"),
    SWITCH_TO_INDIVIDUAL_BUTTON("switch_to_individual_button"),
    SWITCH_TO_GLOBAL_BUTTON("switch_to_global_button"),
    REWARD_INFORMATION("reward_information"),
    REWARD_ITEM("reward_item"),

    // Preset related
    NO_PRESETS("no_presets"),
    PRESET_ITEM("preset_item"),

    // Requirement related
    REQUIREMENT_HOURS("requirement_hours"),
    REQUIREMENT_DATE("requirement_date"),
    REQUIREMENT_WEEKDAY("requirement_weekday"),
    REQUIREMENT_MONTH("requirement_month"),
    REQUIREMENT_YEAR("requirement_year"),
    REQUIREMENT_SEASON("requirement_season"),
    ACTIVATE_ALL_BUTTON("activate_all_button"),
    DEACTIVATE_ALL_BUTTON("deactivate_all_button"),
    REQUIREMENTS_ORDER_BUTTON("requirements_order_button"),

    // Settings related
    ONE_EGG_FOUND_REWARD("one_egg_found_reward"),
    ALL_EGGS_FOUND_REWARD("all_eggs_found_reward"),
    UPDATER_SETTING("updater_setting"),
    COMMAND_FEEDBACK_SETTING("command_feedback_setting"),
    SOUND_VOLUME_SETTING("sound_volume_setting"),
    SHOW_COORDINATES_SETTING("show_coordinates_setting"),
    ARMORSTAND_GLOW_SETTING("armorstand_glow_setting"),
    NEARBY_TITLE_RADIUS_SETTING("nearby_title_radius_setting"),
    SHOW_PLUGIN_PREFIX_SETTING("show_plugin_prefix_setting"),
    FIREWORK_SETTING("firework_setting"),
    HINT_COOLDOWN_SETTING("hint_cooldown_setting"),

    // Miscellaneous
    INFORMATION_BUTTON("information_button"),
    DELETION_TYPES_BUTTON("deletion_types_button");

    private final String path;

    MenuMessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
