# Configuration Guide

## Main Configuration (config.yml)

### Basic Settings
```yaml
messages-lang: en  # Available: en, de, fr, vn, bn, zh
plugin-name-singular: "treasure"  # Single item name
plugin-name-plural: "treasures"   # Plural item name
prefix: '&f[&eAdvancedHunt&f] &r' # Plugin prefix
```

### Core Settings
```yaml
Settings:
  SoundVolume: 3                    # Sound volume (0-15)
  Updater: true                     # Check for updates
  PlayerFoundOneEggRewards: true    # Single treasure rewards
  PlayerFoundAllEggsReward: true    # All treasures rewards
  ShowCoordinatesWhenEggFoundInProgressInventory: false
  ShowFireworkAfterEggFound: true   # Celebration effect
  ArmorstandGlow: 10               # Glow duration in seconds
  ShowEggsNearbyMessageRadius: 10   # Nearby notification radius
  PluginPrefixEnabled: true        # Show prefix in messages
```

### Hint System
```yaml
Settings:
  HintCount: 25                    # Required clicks
  HintCooldownSeconds: 1800        # Cooldown between hints
  HintUpdateTime: 20               # Update interval in ticks
  HintApplyCooldownOnFail: true    # Apply cooldown on failure
```

### Rarity System
```yaml
send-rarity-messages: true
Rarity:
  common:
    display: '&fCOMMON'
    min: 50
    max: 100
  uncommon:
    display: '&aUNCOMMON' 
    min: 10
    max: 50
  # ... more rarities
```

### Command Blacklist
```yaml
BlacklistedCommands:
  - op
  - kill
  - summon
  # ... more commands
```

## Collection Files (eggs/*.yml)

Each collection has its own configuration file with:
- Placed treasures
- Requirements (time/date restrictions)
- Rewards
- Reset settings

Example structure:
```yaml
PlacedEggs:
  '0':
    Rewards:
      '0':
        command: "..."
        enabled: true
        chance: 100.0
    World: world
    X: -146
    Y: 71 
    Z: -306
Requirements:
  Hours:
    '0': true
  # ... more requirements
Reset:
  Year: 0
  Month: 0
  Day: 0
Enabled: true
MaxEggs: 3
```

## Player Data (playerdata/*.yml)

Stores per-player progress:
```yaml
FoundEggs:
  collection_name:
    Count: 3
    Name: PlayerName
    '0':
      World: world
      X: -146
      Y: 71
      Z: -306
      Date: 06.10.2024
      Time: '17:32:05'
```
