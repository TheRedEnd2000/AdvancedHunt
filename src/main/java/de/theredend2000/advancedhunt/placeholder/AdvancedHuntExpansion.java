package de.theredend2000.advancedhunt.placeholder;

import de.theredend2000.advancedhunt.Main;
import de.theredend2000.advancedhunt.managers.CollectionManager;
import de.theredend2000.advancedhunt.managers.LeaderboardManager;
import de.theredend2000.advancedhunt.managers.PlayerManager;
import de.theredend2000.advancedhunt.managers.TreasureManager;
import de.theredend2000.advancedhunt.model.Collection;
import de.theredend2000.advancedhunt.model.PlayerData;
import de.theredend2000.advancedhunt.model.TreasureCore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * PlaceholderAPI expansion for AdvancedHunt.
 * <p>
 * Supported placeholders:
 * <ul>
 *     <li>%advancedhunt_found_count% - Total found treasures (global)</li>
 *     <li>%advancedhunt_selected_collection% - Name of the selected collection (Deprecated)</li>
 *     <li>%advancedhunt_collection_size% - Total number of collections</li>
 *     <li>%advancedhunt_max_treasures% - Max treasures in the selected collection (Deprecated)</li>
 *     <li>%advancedhunt_found_treasures% - Found treasures in the selected collection (Deprecated)</li>
 *     <li>%advancedhunt_remaining_treasures% - Remaining treasures in the selected collection (Deprecated)</li>
 *     <li>%advancedhunt_has_found_<treasure_id>% - "true"/"false" if found</li>
 *     <li>%advancedhunt_max_treasures_<collection>% - Max treasures in a specific collection</li>
 *     <li>%advancedhunt_found_treasures_<collection>% - Found treasures in a specific collection</li>
 *     <li>%advancedhunt_remaining_count_<collection>% - Remaining treasures in a specific collection</li>
 *     <li>%advancedhunt_remaining_treasures_<collection>% - Alias for remaining_count</li>
 *     <li>%advancedhunt_leaderboard_name_<collection>_<rank>% - Player name at rank</li>
 *     <li>%advancedhunt_top_player_<collection>_<rank>% - Alias for leaderboard_name</li>
 *     <li>%advancedhunt_leaderboard_score_<collection>_<rank>% - Score at rank</li>
 *     <li>%advancedhunt_top_score_<collection>_<rank>% - Alias for leaderboard_score</li>
 * </ul>
 */
public class AdvancedHuntExpansion extends PlaceholderExpansion {

    private final Main plugin;
    private final PlayerManager playerManager;
    private final TreasureManager treasureManager;
    private final CollectionManager collectionManager;
    private final LeaderboardManager leaderboardManager;

    public AdvancedHuntExpansion(Main plugin) {
        this.plugin = plugin;
        this.playerManager = plugin.getPlayerManager();
        this.treasureManager = plugin.getTreasureManager();
        this.collectionManager = plugin.getCollectionManager();
        this.leaderboardManager = plugin.getLeaderboardManager();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "advancedhunt";
    }

    @Override
    public @NotNull String getAuthor() {
        return "TheRedEnd2000";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        PlayerData data = playerManager.getPlayerData(player.getUniqueId());

        // %advancedhunt_found_count%
        if (params.equalsIgnoreCase("found_count")) {
            return String.valueOf(data.getFoundTreasures().size());
        }

        // %advancedhunt_collection_size%
        if (params.equalsIgnoreCase("collection_size")) {
            return String.valueOf(collectionManager.getAllCollections().size());
        }

        // %advancedhunt_selected_collection% (Deprecated)
        if (params.equalsIgnoreCase("selected_collection")) {
            UUID selectedId = data.getSelectedCollectionId();
            if (selectedId != null) {
                return collectionManager.getCollectionById(selectedId)
                        .map(Collection::getName)
                        .orElse("Unknown");
            }
            return "None";
        }

        // Context-aware placeholders (Selected Collection) (Deprecated)
        if (params.equalsIgnoreCase("max_treasures") ||
            params.equalsIgnoreCase("found_treasures") ||
            params.equalsIgnoreCase("remaining_treasures")) {
            
            UUID selectedId = data.getSelectedCollectionId();
            if (selectedId == null) return "0";

            List<TreasureCore> allCores = treasureManager.getTreasureCoresInCollection(selectedId);
            if (allCores.isEmpty()) return "0";

            int total = allCores.size();
            int foundCount = treasureManager.countFoundInCollection(data.getFoundTreasures(), selectedId);

            if (params.equalsIgnoreCase("max_treasures")) {
                return String.valueOf(total);
            }
            if (params.equalsIgnoreCase("found_treasures")) {
                return String.valueOf(foundCount);
            }
            return String.valueOf(total - foundCount);
        }

        // %advancedhunt_has_found_<treasure_id>%
        if (params.startsWith("has_found_")) {
            String idStr = params.substring("has_found_".length());
            try {
                UUID id = UUID.fromString(idStr);
                return data.hasFound(id) ? "true" : "false";
            } catch (IllegalArgumentException e) {
                return "invalid_id";
            }
        }

        // %advancedhunt_max_treasures_<collection>%
        if (params.startsWith("max_treasures_")) {
            String collectionName = params.substring("max_treasures_".length());
            Optional<Collection> collectionOpt = collectionManager.getCollectionByName(collectionName);
            if (collectionOpt.isEmpty()) return "0";
            return String.valueOf(treasureManager.getTreasureCoresInCollection(collectionOpt.get().getId()).size());
        }

        // %advancedhunt_found_treasures_<collection>%
        if (params.startsWith("found_treasures_")) {
            String collectionName = params.substring("found_treasures_".length());
            Optional<Collection> collectionOpt = collectionManager.getCollectionByName(collectionName);
            if (collectionOpt.isEmpty()) return "0";

            UUID collectionId = collectionOpt.get().getId();
            return String.valueOf(treasureManager.countFoundInCollection(data.getFoundTreasures(), collectionId));
        }

        // %advancedhunt_remaining_count_<collection>% OR %advancedhunt_remaining_treasures_<collection>%
        if (params.startsWith("remaining_count_") || params.startsWith("remaining_treasures_")) {
            String prefix = params.startsWith("remaining_count_") ? "remaining_count_" : "remaining_treasures_";
            String collectionName = params.substring(prefix.length());
            Optional<Collection> collectionOpt =
                collectionManager.getCollectionByName(collectionName);
                
            if (collectionOpt.isEmpty()) return "0";

            UUID collectionId = collectionOpt.get().getId();
            List<TreasureCore> allCores = treasureManager.getTreasureCoresInCollection(collectionId);
            if (allCores.isEmpty()) return "0";
            int foundCount = treasureManager.countFoundInCollection(data.getFoundTreasures(), collectionId);
            return String.valueOf(allCores.size() - foundCount);
        }

        // %advancedhunt_leaderboard_name_<collection>_<position>% OR %advancedhunt_top_player_<collection>_<position>%
        if (params.startsWith("leaderboard_name_") || params.startsWith("top_player_")) {
            String prefix = params.startsWith("leaderboard_name_") ? "leaderboard_name_" : "top_player_";
            String remaining = params.substring(prefix.length());
            // Expected format: <collection>_<rank>
            // Since collection names might contain underscores, we need to be careful.
            // However, the previous implementation assumed simple split. 
            // Let's try to find the last underscore to separate rank.
            int lastUnderscore = remaining.lastIndexOf('_');
            if (lastUnderscore != -1 && lastUnderscore < remaining.length() - 1) {
                String collectionName = remaining.substring(0, lastUnderscore);
                String rankStr = remaining.substring(lastUnderscore + 1);
                try {
                    int rank = Integer.parseInt(rankStr);
                    LeaderboardManager.LeaderboardEntry entry = leaderboardManager.getEntry(collectionName, rank);
                    return entry != null ? entry.getPlayerName() : "---";
                } catch (NumberFormatException e) {
                    return "invalid_rank";
                }
            }
        }

        // %advancedhunt_leaderboard_score_<collection>_<position>% OR %advancedhunt_top_score_<collection>_<position>%
        if (params.startsWith("leaderboard_score_") || params.startsWith("top_score_")) {
            String prefix = params.startsWith("leaderboard_score_") ? "leaderboard_score_" : "top_score_";
            String remaining = params.substring(prefix.length());
            int lastUnderscore = remaining.lastIndexOf('_');
            if (lastUnderscore != -1 && lastUnderscore < remaining.length() - 1) {
                String collectionName = remaining.substring(0, lastUnderscore);
                String rankStr = remaining.substring(lastUnderscore + 1);
                try {
                    int rank = Integer.parseInt(rankStr);
                    LeaderboardManager.LeaderboardEntry entry = leaderboardManager.getEntry(collectionName, rank);
                    return entry != null ? String.valueOf(entry.getScore()) : "0";
                } catch (NumberFormatException e) {
                    return "invalid_rank";
                }
            }
        }

        return null;
    }
}
