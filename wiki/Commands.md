# Commands

All commands can be customized in the config.yml file. The first command listed will be the main command.
Default commands: `/treasurehunt`, `/treasures`, `/advancedhunt`, `/aeh`, `/easter`

## Player Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/treasurehunt collection` | `AdvancedHunt.command.collection` | Switch between and edit collections |
| `/treasurehunt help` | `AdvancedHunt.command.help` | Show help messages and command list |
| `/treasurehunt progress` | `AdvancedHunt.command.progress` | View your treasure hunting progress |
| `/treasurehunt hint` | `AdvancedHunt.command.hint` | Open hint menu to find treasures easier |

## Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/treasurehunt place` | `AdvancedHunt.command.place` | Enter place mode to add/remove treasures |
| `/treasurehunt list` | `AdvancedHunt.command.list` | List all placed treasures |
| `/treasurehunt show` | `AdvancedHunt.command.show` | Show glowing armor stands at treasure locations |
| `/treasurehunt reload` | `AdvancedHunt.command.reload` | Reload plugin configuration |
| `/treasurehunt settings` | `AdvancedHunt.command.settings` | Configure plugin settings |
| `/treasurehunt reset [player\|all] [collection\|all]` | `AdvancedHunt.command.reset` | Reset found treasures |
| `/treasurehunt import` | `AdvancedHunt.command.import` | Import custom player head textures |

## Permission Groups

### Admin Group
Permission: `AdvancedHunt.group.admin`
- Includes all admin permissions
- Includes default group permissions
- Default: op

### Default Group  
Permission: `AdvancedHunt.group.default`
- Basic player permissions
- Default: true

## Permission Summary

| Feature | Required Permission | Default |
|---------|-------------------|---------|
| Basic Finding | `AdvancedHunt.FindTreasures` | true |
| Break Treasures | `AdvancedHunt.BreakTreasure` | op |
| Place Treasures | `AdvancedHunt.PlaceTreasure` | op |
| Bypass Cooldowns | `AdvancedHunt.IgnoreCooldown` | op |
| Switch Collections | `AdvancedHunt.ChangeCollections` | true |
| Create Collections | `AdvancedHunt.CreateCollection` | op |
| Configure Rewards | `AdvancedHunt.OpenRewards` | op |

## Command Aliases
All commands can be used with these prefixes:
```
/treasurehunt
/treasures
/advancedhunt
/aeh
/easter
```
