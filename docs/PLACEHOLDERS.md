# AdvancedHunt Placeholders

This document lists all available PlaceholderAPI placeholders for AdvancedHunt.

## Global Placeholders

| Placeholder | Description |
| :--- | :--- |
| `%advancedhunt_found_count%` | Total number of treasures found by the player across all collections. |
| `%advancedhunt_selected_collection%` | The name of the collection currently selected by the player. Returns "None" if no collection is selected. |
| `%advancedhunt_collection_size%` | The total number of collections available on the server. |

## Context-Aware Placeholders (Selected Collection)

These placeholders return values based on the player's currently selected collection. If no collection is selected, they return "0".

| Placeholder | Description |
| :--- | :--- |
| `%advancedhunt_max_treasures%` | The total number of treasures in the selected collection. |
| `%advancedhunt_found_treasures%` | The number of treasures the player has found in the selected collection. |
| `%advancedhunt_remaining_treasures%` | The number of treasures remaining for the player to find in the selected collection. |

## Collection-Specific Placeholders

Replace `<collection>` with the exact name of the collection.

| Placeholder | Description |
| :--- | :--- |
| `%advancedhunt_max_treasures_<collection>%` | The total number of treasures in the specified collection. |
| `%advancedhunt_found_treasures_<collection>%` | The number of treasures the player has found in the specified collection. |
| `%advancedhunt_remaining_count_<collection>%` | The number of treasures remaining for the player to find in the specified collection. |
| `%advancedhunt_remaining_treasures_<collection>%` | Alias for `remaining_count`. |

## Treasure-Specific Placeholders

Replace `<treasure_id>` with the UUID of the treasure.

| Placeholder | Description |
| :--- | :--- |
| `%advancedhunt_has_found_<treasure_id>%` | Returns "true" if the player has found the treasure, "false" otherwise. |

## Leaderboard Placeholders

Replace `<collection>` with the collection name and `<rank>` with the position (1, 2, 3, etc.).

| Placeholder | Description |
| :--- | :--- |
| `%advancedhunt_leaderboard_name_<collection>_<rank>%` | The name of the player at the specified rank in the collection leaderboard. |
| `%advancedhunt_top_player_<collection>_<rank>%` | Alias for `leaderboard_name`. |
| `%advancedhunt_leaderboard_score_<collection>_<rank>%` | The score (treasures found) of the player at the specified rank. |
| `%advancedhunt_top_score_<collection>_<rank>%` | Alias for `leaderboard_score`. |

## Changes from V2

If you are migrating from AdvancedHunt V2, please note the following changes:

1.  **Global Leaderboards**: Global leaderboards (e.g., `%advancedhunt_top_player_<rank>%` without a collection) are **no longer supported**. You must specify a collection name.
2.  **Selected Collection**: `%advancedhunt_selected_collection%` now returns "None" if no collection is selected, whereas V2 might have defaulted to a config-defined collection.
3.  **Aliases**: We have added aliases to support V2 naming conventions (e.g., `remaining_treasures` works alongside `remaining_count`), so most existing setups should work without changes.
