**Support/Help Server:** https://discord.gg/G8BDgqsuyw

**Wiki:** https://github.com/s3tupw1zard/AwkGearMastery/wiki


## A plugin that adds a leveling system to any item.

## Features

- Targets Paper/Purpur 26.2 with Java 25
- Supports Folia.
- No required dependencies; PlaceholderAPI is optional.
- Profile-based configuration system.
- Any item can level up with custom triggers.
- Configurable global, item-profile, and permission-based XP formulas.
- Configurable milestone rewards; no automatic enchantment leveling.
- Supports blacklisting/whitelisting for blocks, entities, and items.
- ActionBar notifications.
- Item lore modification.
- Optional enchanted-book blocking for AwkGearMastery items.
- Farming trigger support for fully grown player-planted crops.

## AwkGearMastery 2026.1

- `ARMOR_DURABILITY` trigger awards XP when armor takes damage.
- Configurable XP formulas under XP formula profiles, selectable per player via `awkgearmastery.formula.<id>`.
- Toggle to block enchanted books from being applied to AwkGearMastery items.
- `block_data_storage` option (`SQLITE`) for persisting per-block placement data.
- Farming trigger gains `ignore_player_placed_blocks_for_fully_grown_crops` for fully-grown crop handling.
- Fresh AwkGearMastery config and item state; no support for old configurations.

## Profile System

AwkGearMastery uses a modular profile-based configuration system. Any item can be configured to level up.

### Profile Types

| Profile Type | Purpose | File |
|-------------|---------|------|
| **Trigger Profiles** | Define how XP is gained | `trigger_profiles.yml` |
| **Reward Profiles** | Define rewards per level | `reward_profiles.yml` |
| **Display Profiles** | Define name, lore, action bar | `display_profiles.yml` |
| **Item Profiles** | Tie everything together | `item_profiles.yml` |

### Trigger Types

- `BLOCK_BREAK` - XP when breaking blocks
- `ENTITY_KILL` - XP when killing entities
- `FISHING` - XP when catching items
- `RIGHT_CLICK` / `LEFT_CLICK` - XP on click
- `CONSUME` - XP when consuming items
- `FARMING` - XP when tilling soil and breaking fully-grown crops
- `ARMOR_DURABILITY` - XP when worn armor takes damage

### Default Supported Items

Out of the box, AwkGearMastery ships profiles for:
- Pickaxes, Axes, Shovels (block mining)
- Swords (combat)
- Bows, Crossbows (ranged)
- Fishing Rods (fishing)
- Tridents (combat / ranged)
- Hoes (farming)

Add any item by creating custom profiles. See the [Wiki](https://github.com/s3tupw1zard/AwkGearMastery/wiki) for details.


## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/awkgearmastery help [page]` | Paginated command help | None |
| `/awkgearmastery reload` | Reloads configuration | `awkgearmastery.admin` |
| `/awkgearmastery reset <player>` | Reset hand item for player | `awkgearmastery.admin` |
| `/awkgearmastery reset <player> --all` | Reset all items for player | `awkgearmastery.admin` |
| `/awkgearmastery xp <amount>` | Set hand item XP | `awkgearmastery.admin` |
| `/awkgearmastery level <level>` | Set hand item level | `awkgearmastery.admin` |
| `/awkgearmastery levelup` | Increase hand item level by 1 | `awkgearmastery.admin` |
| `/awkgearmastery debug` | Show debug information | `awkgearmastery.admin` |

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `awkgearmastery.admin` | Access to admin commands | op |
| `awkgearmastery.enabled` | Allow leveling for this player | true |
| `awkgearmastery.formula.<id>` | Selects an XP formula defined under `xp_formulas` | false |

## Developer API

**View detailed API usage [here](https://github.com/s3tupw1zard/AwkGearMastery/wiki/Developer-API).**

## PlaceholderAPI

| Placeholder | Description |
|-------------|-------------|
| `%awkgearmastery_level%` | Current item level (main hand) |
| `%awkgearmastery_xp%` | Current XP (main hand) |
| `%awkgearmastery_max_xp%` | XP required for the next level |
| `%awkgearmastery_progress%` | Progress percentage (0-100, 1 decimal) |
| `%awkgearmastery_progress_bar%` | Rendered progress bar |
| `%awkgearmastery_item_profile%` | Item profile id for the hand item |
| `%awkgearmastery_max_level%` | Max level for the current item profile |

## Documentation

- [Configuration](https://github.com/s3tupw1zard/AwkGearMastery/wiki/Configuration)
- [Trigger Profiles](https://github.com/s3tupw1zard/AwkGearMastery/wiki/Trigger-Profiles)
- [Reward Profiles](https://github.com/s3tupw1zard/AwkGearMastery/wiki/Reward-Profiles)
- [Display Profiles](https://github.com/s3tupw1zard/AwkGearMastery/wiki/Display-Profiles)
- [Item Profiles](https://github.com/s3tupw1zard/AwkGearMastery/wiki/Item-Profiles)

