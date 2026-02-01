package de.theredend2000.advancedhunt.managers;

public enum TreasureDeletionType {

    /**
     * After deleting the collection, all treasure blocks stay (or are restored if currently hidden).
     */
    KEEP_ALL,

    /**
     * After deleting the collection, only head/skull treasures are removed.
     * Non-head treasures stay (or are restored if currently hidden).
     */
    REMOVE_HEADS,

    /**
     * After deleting the collection, all treasure blocks and ItemsAdder furniture are removed.
     */
    REMOVE_BLOCKS_AND_FURNITURE
}
