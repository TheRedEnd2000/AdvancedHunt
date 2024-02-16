package de.theredend2000.advancedegghunt.util.messages;

public enum MessageKey {
    PERMISSION_ERROR("no-permission-error"),
    COMMAND_NOT_FOUND("command-not-found"),
    ONLY_PLAYER("only-player"),
    RELOAD_CONFIG("reload-config"),
    PLAYER_NOT_FOUND("player-not-found"),

    EGG_PLACED("egg-placed"),
    EGG_BROKEN("egg-broken"),
    TELEPORT_TO_EGG("teleport-to-egg"),

    EGG_FOUND("egg-found"),
    EGG_ALREADY_FOUND("egg-already-found"),
    ALL_EGGS_FOUND("all-eggs-found"),
    NO_EGGS("no-eggs"),
    EGG_HINT("egg-hint"),
    HINT_COOLDOWN("hint-cooldown"),
    CLICKED_SAME("clicked-same"),
    EGG_NOT_ACCESSED("egg-not-accessed"),

    ONLY_IN_PLACEMODE("only-in-placemode"),
    ENTER_PLACEMODE("enter-placemode"),
    LEAVE_PLACEMODE("leave-placemode"),

    FIRST_PAGE("first-page"),
    LAST_PAGE("last-page"),
    WAIT_REFRESH("wait-refresh"),
    BLOCK_LISTED("block-listed"),

    EGG_NEARBY("egg-nearby"),

    SOUND_VOLUME("sound-volume"),
    EGG_VISIBLE("egg-visible"),
    ARMORSTAND_GLOW("armorstand-glow"),
    EGG_RADIUS("egg-radius"),

    COMMAND_TYPE("command-type"),
    COMMAND_ENABLED("command-enabled"),
    COMMAND_DELETE("command-delete"),
    NEW_COMMAND("new-command"),
    COMMAND_CANCEL("command-cancel"),
    COMMAND_ADD("command-add"),
    COMMAND_EXPIRED("command-expired"),
    ONE_COMMAND("one-command"),
    COMMAND_CHANGED("command-changed"),

    FOUNDEGGS_RESET("foundeggs-reset"),
    FOUNDEGGS_PLAYER_RESET("foundeggs-player-reset"),

    ACTIVATE_REQUIREMENTS("activate-requirements"),
    DEACTIVATE_REQUIREMENTS("deactivate-requirements"),

    COLLECTION_DISABLED("collection-disabled"),
    COLLECTION_SELECTION("collection-selected"),
    COLLECTION_DELETED("collection-deleted");


    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}