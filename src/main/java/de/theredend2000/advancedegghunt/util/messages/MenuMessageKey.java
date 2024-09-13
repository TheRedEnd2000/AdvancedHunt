package de.theredend2000.advancedegghunt.util.messages;

public enum MenuMessageKey {

    // General buttons
    BACK_BUTTON("back_button"),
    NEXT_PAGE_BUTTON("next_page_button"),
    PREVIOUS_PAGE_BUTTON("previous_page_button"),
    CLOSE_BUTTON("close_button"),
    REFRESH_BUTTON("refresh_button"),

    // Collection related
    SELECTED_COLLECTION_BUTTON("selected_collection_button"),
    ADD_COLLECTION_BUTTON("add_collection_button"),
    COLLECTION_ITEM("collection_item"),

    // Collection editor
    COLLECTION_EDITOR_STATUS("collection_editor_status"),
    COLLECTION_EDITOR_DELETE("collection_editor_delete"),
    COLLECTION_EDITOR_REQUIREMENTS("collection_editor_requirements"),
    COLLECTION_EDITOR_RESET("collection_editor_reset"),
    COLLECTION_EDITOR_HIDE_FOR_PLAYER("collection_editor_hide_for_player"),
    COLLECTION_EDITOR_ONE_PLAYER("collection_editor_one_player"),
    COLLECTION_EDITOR_DELETE_TYPE("collection_editor_delete_type"),

    // Collection creator
    COLLECTION_CREATOR_NAME("collection_creator_name"),
    COLLECTION_CREATOR_STATUS("collection_creator_status"),
    COLLECTION_CREATOR_REQUIREMENTS("collection_creator_requirements"),
    COLLECTION_CREATOR_CREATE("collection_creator_create"),

    // Egg list related
    EGGSLIST_EGG("eggslist_egg"),
    EGGPLACE_INFORMATION("eggplace_information"),
    EGGPLACE_EGG("eggplace_egg"),
    EGGPROGRESS_LOCATION_FOUND("eggprogress_location_found"),
    EGGPROGRESS_FOUND("eggprogress_found"),
    EGGPROGRESS_NOT_FOUND("eggprogress_not_found"),

    // Player related

    INFORMATION_PLAYER("information_player"),

    // Leaderboard related
    LEADERBOARD_SORT("leaderboard_sort"),
    LEADERBOARD_PLAYER("leaderboard_player"),

    // Reward related
    REWARDS_CONFIRM_MENU_CONFIRM("rewards_confirm_menu_confirm"),
    REWARDS_CONFIRM_MENU_CANCEL("rewards_confirm_menu_cancel"),
    REWARDS_INDIVIDUAL_SAVE_PRESET("rewards_individual_save_preset"),
    REWARDS_INDIVIDUAL_LOAD_PRESET("rewards_individual_load_preset"),
    REWARDS_INDIVIDUAL_NEW_REWARD("rewards_individual_new_reward"),
    REWARDS_INDIVIDUAL_SWITCH_GLOBAL("rewards_individual_switch_global"),
    REWARDS_INDIVIDUAL_INFORMATION("rewards_individual_information"),
    REWARDS_INDIVIDUAL_REWARD("rewards_individual_reward"),

    REWARDS_GLOBAL_SAVE_PRESET("rewards_global_save_preset"),
    REWARDS_GLOBAL_LOAD_PRESET("rewards_global_load_preset"),
    REWARDS_GLOBAL_NEW_REWARD("rewards_global_new_reward"),
    REWARDS_GLOBAL_SWITCH_INDIVIDUAL("rewards_global_switch_individual"),
    REWARDS_GLOBAL_INFORMATION("rewards_global_information"),
    REWARDS_GLOBAL_REWARD("rewards_global_reward"),

    // Preset related

    // Requirement related
    REQUIREMENTS_SELECTION("requirements_selection"),
    REQUIREMENTS_ACTIVATE("requirements_activate"),
    REQUIREMENTS_DEACTIVATE("requirements_deactivate"),
    REQUIREMENTS_ORDER("requirements_order"),
    REQUIREMENTS_SEASON("requirements_season"),
    REQUIREMENTS_YEAR("requirements_year"),
    REQUIREMENTS_MONTH("requirements_month"),
    REQUIREMENTS_WEEKDAY("requirements_weekday"),
    REQUIREMENTS_DATE("requirements_date"),
    REQUIREMENTS_HOUR("requirements_hour"),

    // Settings related
    SETTINGS_ONE_EGG_REWARD("settings_one_egg_reward"),
    SETTINGS_ALL_EGG_REWARD("settings_all_eggs_reward"),
    SETTINGS_UPDATER("settings_updater"),
    SETTINGS_COMMAND_FEEDBACK("settings_command_feedback"),
    SETTINGS_SOUND_VOLUME("settings_sound_volume"),
    SETTINGS_SHOW_COORDINATES("settings_show_coordinates"),
    SETTINGS_ARMORSTAND_GLOW("settings_armorstand_glow"),
    SETTINGS_EGG_NEARBY_RADIUS("settings_egg_nearby_radius"),
    SETTINGS_PLUGIN_PREFIX("settings_plugin_prefix"),
    SETTINGS_FIREWORK("settings_firework"),
    SETTINGS_HINT_COOLDOWN("settings_hint_cooldown");

    // Miscellaneous

    private final String path;

    MenuMessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
