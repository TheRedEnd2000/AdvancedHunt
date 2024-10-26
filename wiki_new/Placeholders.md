# PlaceholderAPI Integration

AdvancedHunt integrates with PlaceholderAPI to provide dynamic placeholders. Enable PlaceholderAPI integration in the config.

## Available Placeholders

| Placeholder | Description | Example |
|-------------|-------------|----------|
| `%advancedhunt_selected_collection%` | Currently selected collection | "Winter 2024" |
| `%advancedhunt_collection_size%` | Total number of collections | "5" |
| `%advancedhunt_max_treasures%` | Total treasures in current collection | "10" |
| `%advancedhunt_found_treasures%` | Found treasures in current collection | "7" |
| `%advancedhunt_remaining_treasures%` | Remaining treasures to find | "3" |

### Leaderboard Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%advancedhunt_player_name_[position]%` | Name of player at specified position |
| `%advancedhunt_player_count_[position]%` | Found count of player at position |

Add `_[collection]` to get stats for a specific collection:
- `%advancedhunt_player_name_1_winter2024%`
- `%advancedhunt_player_count_1_winter2024%`

## Configuration

Default placeholder values when data is unavailable:
```yaml
PlaceholderAPI:
  collection: 'N/A'  # When collection is null
  name: 'N/A'        # When player name is null  
  count: '-1'        # When count is null
```
