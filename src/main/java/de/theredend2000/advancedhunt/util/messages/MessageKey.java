package de.theredend2000.advancedhunt.util.messages;

public enum MessageKey {
    PERMISSION_ERROR("no-permission-error"),
    COMMAND_NOT_FOUND("command-not-found"),
    ONLY_PLAYER("only-player"),
    RELOAD_CONFIG("reload-config"),
    PLAYER_NOT_FOUND("player-not-found"),
    NOT_NUMBER("not-number"),

    EGG_PLACED("treasure-placed"),
    EGG_BROKEN("treasure-broken"),
    TELEPORT_TO_EGG("teleport-to-treasure"),

    EGG_ALREADY_FOUND("treasure-already-found"),
    EGG_ALREADY_FOUND_BY_PLAYER("treasure-already-found-by-player"),
    ALL_EGGS_FOUND("all-treasures-found"),
    NO_EGGS("no-treasures"),

    EGG_HINT("treasure-hint"),
    HINT_COOLDOWN("hint-cooldown"),
    CLICKED_SAME("clicked-same"),
    EGG_HINT_TIMEOUT("hint-timeout"),
    EGG_HINT_CANCELLED("hint-cancelled"),

    EGG_NOT_ACCESSED("treasure-not-accessed"),

    ONLY_IN_PLACEMODE("only-in-placemode"),
    ENTER_PLACEMODE("enter-placemode"),
    LEAVE_PLACEMODE("leave-placemode"),

    FIRST_PAGE("first-page"),
    LAST_PAGE("last-page"),
    WAIT_REFRESH("wait-refresh"),
    BLOCK_LISTED("block-listed"),

    EGG_NEARBY("treasure-nearby"),

    SOUND_VOLUME("sound-volume"),
    EGG_VISIBLE("treasure-visible"),
    EGG_SHOW_WARNING("treasure-show-warning"),
    ARMORSTAND_GLOW("armorstand-glow"),
    EGG_RADIUS("treasure-radius"),

    COMMAND_DELETE("command-delete"),
    NEW_COMMAND("new-command"),
    COMMAND_CANCEL("command-cancel"),
    COMMAND_ADD("command-add"),
    COMMAND_EXPIRED("command-expired"),
    ONE_COMMAND("one-command"),
    COMMAND_BLACKLISTED("command-blacklisted"),

    FOUNDEGGS_RESET("foundtreasures-reset"),
    FOUNDEGGS_PLAYER_RESET("foundtreasures-player-reset"),
    FOUNDEGGS_PLAYER_RESET_COLLECTION("foundtreasures-player-reset-collection"),

    ACTIVATE_REQUIREMENTS("activate-requirements"),
    DEACTIVATE_REQUIREMENTS("deactivate-requirements"),

    COLLECTION_DISABLED("collection-disabled"),
    COLLECTION_SELECTION("collection-selected"),
    COLLECTION_DELETED("collection-deleted"),
    CHANCED_CHANCE("chanced-chance"),
    RARITY_MESSAGE("rarity-message"),

    EGGIMPORT_HAND("treasureimport-hand"),
    EGGIMPORT_FAILED_PROFILE("treasureimport-failed-profile"),
    EGGIMPORT_SUCCESS("treasureimport-success"),
    EGGIMPORT_FAILED("treasureimport-failed"),

    PRESET_ALREADY_EXISTS("preset-already-exists"),
    PRESET_SAVED("preset-saved"),
    PRESET_FAILED_COMMANDS("preset-failed-commands"),
    PRESET_LOADED("preset-loaded"),
    PRESET_DELETE("preset-delete"),
    PRESET_DEFAULT("preset-default"),
    PRESET_NOT_DELETE_DEFAULT("preset-not-delete-default"),
    PRESET_IS_DEFAULT_1("preset-is-default-1"),
    PRESET_IS_DEFAULT_2("preset-is-default-2"),

    SETTING_COMMANDFEEDBACK("setting-commandfeedback"),

    REQUIREMENTS_NAME_HOUR("requirements-name-hour"),
    REQUIREMENTS_NAME_DATE("requirements-name-date"),
    REQUIREMENTS_NAME_WEEKDAY("requirements-name-weekday"),
    REQUIREMENTS_NAME_MONTH("requirements-name-month"),
    REQUIREMENTS_NAME_YEAR("requirements-name-year"),
    REQUIREMENTS_NAME_SEASON("requirements-name-season"),
    REQUIREMENTS_MORE("requirements-more"),
    REQUIREMENTS_CLICK_TO_CHANGE("requirements-click_to_change"),

    REQUIREMENTS_SEASON_WINTER("requirements-season-winter"),
    REQUIREMENTS_SEASON_FALL("requirements-season-fall"),
    REQUIREMENTS_SEASON_SPRING("requirements-season-spring"),
    REQUIREMENTS_SEASON_SUMMER("requirements-season-summer"),

    MONTH_JANUARY("month-january"),
    MONTH_FEBRUARY("month-february"),
    MONTH_MARCH("month-march"),
    MONTH_APRIL("month-april"),
    MONTH_MAY("month-may"),
    MONTH_JUNE("month-june"),
    MONTH_JULY("month-july"),
    MONTH_AUGUST("month-august"),
    MONTH_SEPTEMBER("month-september"),
    MONTH_OCTOBER("month-october"),
    MONTH_NOVEMBER("month-november"),
    MONTH_DECEMBER("month-december"),

    DAY_MONDAY("day-monday"),
    DAY_TUESDAY("day-tuesday"),
    DAY_WEDNESDAY("day-wednesday"),
    DAY_THURSDAY("day-thursday"),
    DAY_FRIDAY("day-friday"),
    DAY_SATURDAY("day-saturday"),
    DAY_SUNDAY("day-sunday"),


    HOUR_FORMAT("hour-format"),

    HELP_MESSAGE("help-message"),

    EGG_DATA_CONVERTED_SUCCESS("treasure-data-converted-success"),
    EGG_DATA_CONVERTED_BROADCAST("treasure-data-converted-broadcast"),
    EGG_DATA_CONVERTED_FAILED("treasure-data-converted-failed"),
    EGG_DATA_CONVERT_START("treasure-data-convert-start"),
    EGG_DATA_CONVERT_BROADCAST("treasure-data-convert-broadcast"),
    EGG_DATA_CONVERT_OP_MESSAGE("treasure-data-convert-op-message"),
    EGG_DATA_CONVERT_LOCATIONS("treasure-data-convert-locations"),
    EGG_DATA_CONVERT_PLAYER("treasure-data-convert-player"),
    EGG_DATA_CONVERT_DONE("treasure-data-convert-done"),
    EGG_DATA_CONVERT_SUCCESS("treasure-data-convert-success"),
    EGG_DATA_CONVERT_DELETE_FAIL("treasure-data-convert-delete-fail"),
    EGG_DATA_CONVERT_DELETE_ERROR("treasure-data-convert-delete-error"),
    EGG_DATA_CONVERT_COMPLETE("treasure-data-convert-complete"),
    PLACEHOLDERAPI_DETECTED("placeholderapi-detected"),
    PLACEHOLDERAPI_ENABLED("placeholderapi-enabled"),
    INIT_DATA_PLAYERS_LOADED("init-data-players-loaded"),
    INIT_DATA_COLLECTIONS_LOADED("init-data-collections-loaded"),
    MATERIAL_ERROR_CONSOLE("material-error-console"),
    SUCCESSFULLY_CHANGED_BLOCK("successfully-changed-block"),
    REQUIREMENT_SECTION_UNAVAILABLE("requirement-section-unavailable"),
    REQUIREMENTS_ORDER_COMING_SOON("requirements-order-coming-soon"),
    COLLECTION_SELECT_ERROR("collection-select-error"),
    COLLECTION_REFRESH_COOLDOWN("collection-refresh-cooldown"),
    COLLECTION_REFRESH_SUCCESS("collection-refresh-success"),
    COLLECTION_EDIT_NO_PERMISSION("collection-edit-no-permission"),
    COLLECTION_NAME_REQUIRED("collection-name-required"),
    COLLECTION_NAME_EXISTS("collection-name-exists"),
    COLLECTION_DEFAULT_UNDELETABLE("collection-default-undeletable"),
    FEATURE_COMING_SOON("feature-coming-soon"),
    ITEM_ADDED_SUCCESS("item-added-success"),
    INVALID_CHANCE("invalid-chance"),
    PLACEHOLDERS_HOVER_TEXT("placeholders-hover-text"),
    PLACEHOLDERS_HOVER_CONTENT("placeholders-hover-content"),
    PRESET_LOADING_NO("preset-loading-no"),
    PRESET_LOADING_SUCCESS("preset-loading-success"),
    PRESET_LOADING_PROGRESS("preset-loading-progress"),
    LANGUAGE_DETECTED("language-detected"),
    COMMAND_SYSTEM_UPDATED("command-system-updated"),
    COMMAND_SYSTEM_CHECK("command-system-check"),
    COMMAND_SYSTEM_CHANCES_UPDATED("command-system-chances-updated"),
    COMMAND_SYSTEM_CHANCES_CONTAINS("command-system-chances-contains"),
    UPDATE_AVAILABLE("update-available"),
    UPDATE_ERROR("update-error"),
    CONSOLE_UPDATE_AVAILABLE("console-update-available"),
    CONSOLE_UPDATE_ERROR("console-update-error");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
