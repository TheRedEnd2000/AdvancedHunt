package de.theredend2000.advancedegghunt.util.messages;

public enum MenuMessageKey {
    // General buttons
    NEXT_PAGE_BUTTON("next_page_button"),
    PREVIOUS_PAGE_BUTTON("previous_page_button"),
    CLOSE_BUTTON("close_button"),
    SORT_BUTTON("sort_button"),
    REFRESH_BUTTON("refresh_button");

    // Collection related

    // Egg related

    // Player related

    // Leaderboard related

    // Reward related

    // Preset related

    // Requirement related

    // Settings related

    // Miscellaneous

    private final String path;

    MenuMessageKey(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }
}
