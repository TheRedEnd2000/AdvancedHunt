package de.theredend2000.advancedegghunt.util.messages;

public enum MenuMessageKey {
    NEXT_PAGE_BUTTON("next_page_button"),
    PREVIOUS_PAGE_BUTTON("previous_page_button"),
    CLOSE_BUTTON("close_button"),
    SORT_BUTTON("sort_button"),
    REFRESH_BUTTON("refresh_button"),

    SELECTED_COLLECTION_BUTTON("selected_collection_button"),
    NO_EGGS_AVAILABLE("no_eggs_available"),
    NO_PLAYER("no_player"),
    EGG_ITEM("egg_item");

    private final String path;

    MenuMessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
