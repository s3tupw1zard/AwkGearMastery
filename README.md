[![Java CI with Gradle](https://github.com/s3tupw1zard/AwkGearMastery/actions/workflows/gradle.yml/badge.svg?branch=main)](https://github.com/s3tupw1zard/AwkGearMastery/actions/workflows/gradle.yml)

<h5 align="center">Maintained fork: https://github.com/s3tupw1zard/AwkGearMastery</h5>
<h5 align="center">Issues / support: https://github.com/s3tupw1zard/AwkGearMastery/issues</h5>
<h5 align="center">Originally developed by byteful (AGPL-3.0)</h5>

<h3 align="center">AwkGearMastery — configurable RPG-style equipment progression for Paper and Purpur.</h3>

AwkGearMastery is based on open-source code originally authored by byteful; the original copyright notices and AGPL-3.0 licensing are retained.

## AwkGearMastery 2026.1 Direction

**Recommended and gameplay-tested:** Paper/Purpur 26.3 with Java 25.

**Minimum API:** Paper 1.21.11. The same JAR is compiled against that API and can
also load on newer compatible servers, including Paper/Purpur 26.1.x and 26.2.
Java 25 is required on every server version. Versions below 1.21.11 are not supported.
CI boots the distributed JAR on Paper 1.21.11, 26.1.2, 26.2 and 26.3; startup
checks do not replace in-game testing of combat, farming, shields and persistence.
Items absent from an older server are skipped by the material profile loader.

The released `2026.1.0-rc.1` remains the historical Minecraft 26.2 candidate;
subsequent candidates no longer intentionally exclude compatible older servers.
Versioning uses SemVer-compatible CalVer (`YYYY.RELEASE.PATCH`), with `2026.1.0`
as the first stable target and `2026.1.0-rc.2-SNAPSHOT` for current development builds.

The 2026.1 progression model deliberately separates item levels from enchantments:

- Level **1** is the vanilla/baseline item.
- Every level improves formula-derived stats; there are no hardcoded per-level stat tables.
- Default max level is 100 but the curve supports other caps without making large caps exponentially impractical.
- Leveling does **not** add enchantments automatically.
- Player-applied enchantments keep their vanilla behavior and can add small configurable AwkGearMastery stat bonuses.
- Item progression is stored directly in PersistentDataContainer using the `awkgearmastery` namespace.
- Java compilation is enforced with `-Xlint:all -Werror`.

## Formula-Based Stats

Stats use normalized level progress, so the same profile remains meaningful at max level 50, 100, 500 or 1000.

```text
progress = (level - 1) / (maxLevel - 1)

value = start + (max - start) * progress^exponent
```

Default stat types:

- Damage
- Critical hit chance
- Critical hit damage
- Defense
- Critical defense
- Max durability

Melee weapons currently default to a maximum **+300% AwkGearMastery damage bonus**
(4x the baseline before vanilla enchantment effects), 15% AwkGearMastery critical
chance and +75% AwkGearMastery critical damage at max level.

Armor defense is combined multiplicatively across pieces, avoiding accidental
100%+ damage reduction.

## XP Progression

Required XP is configured in `progression_profiles.yml`.

The default profile starts at **100 XP for level 1 -> 2** and ramps to
**50,000 XP for level 99 -> 100**, using a curve exponent of **2.2**. These are
per-level transition costs, not cumulative totals. The first few levels stay
accessible while later levels require sustained play.

Ordinary blocks grant 0.2–0.4 XP; ore rewards are reduced to roughly one quarter
of their previous values. Farming and armor XP are also reduced, fishing rewards
are halved, and the combat XP formula and extra boss bonuses are toned down.
Axes gain both mining and combat XP and use melee weapon stats. Shields gain XP
from confirmed blocked attacks in either hand, even when no durability is lost, and improve durability;
they do not grant passive armor defense or receive offensive kill XP.

Tilling XP is awarded only after the soil actually changes, including when a
shield is held in the other hand. Cancelled, blocked, or repeated clicks on
unchanged soil do not award tilling XP.

Minecraft 26.3 gives shields priority over hoes. With `farming.shield_priority`
enabled (default), aiming at valid soil prioritizes the hoe unless a mob within
8 blocks has you as its target. Sneaking deliberately prioritizes tilling even
under threat. The farming action is evaluated on interaction. While the hoe/shield pair is
equipped, an aim guard prevents predicted shield raising over soil and newly
tilled farmland. The soil
conversion respects interaction cancellation and a cancellable `BlockPlaceEvent`;
vanilla durability/Unbreaking processing still applies.

Existing configuration files are preserved when updating the JAR. See
[the rc.2 testing notes](docs/rc2-testing.md) to apply the new defaults and test
these changes without resetting item progress.

`max_level_influence` controls how total grind changes when the level cap changes:

- `0.0`: roughly the same total XP even with more/fewer level steps.
- `0.5`: sublinear total-XP growth (default).
- `1.0`: roughly linear total-XP growth.

At max level the display is capped at full progress instead of showing a fake next level.

## Combat XP and Shared Kills

Combat XP no longer uses a flat 1-2 XP reward. A mob produces an XP pool from its
max health and an optional entity multiplier. Difficult mobs ship with stronger
multipliers; for example Warden, Wither and Ender Dragon are explicitly weighted.

When multiple players/items contribute damage:

- XP is damage-weighted.
- Overkill is ignored by default.
- Contributions below 2% are ignored by default.
- The XP pool is not duplicated for every player.
- Contributions are tracked per player **and per AwkGearMastery item**, so switching
  weapons does not move all XP to the item held at death.
- Optional group scaling exists but is disabled by default.

Configured boss mobs can also grant a level-based bonus. This bonus can require
one or more AwkGearMastery critical hits and is protected by a persistent per-player,
per-mob claim interval.

## Configuration Layout

| File | Purpose |
| --- | --- |
| `config.yml` | Runtime/general plugin settings |
| `item_profiles.yml` | Materials and references to the profiles below |
| `progression_profiles.yml` | Level cap and non-linear required-XP curves |
| `stat_profiles.yml` | Damage, crit, defense and durability curves |
| `xp_sources.yml` | Mining, farming, fishing, armor and combat XP values |
| `enchantment_modifiers.yml` | Optional enchantment -> AwkGearMastery stat contributions |
| `trigger_profiles.yml` | When/where a trigger may fire; filters and slots |
| `display_profiles.yml` | Compact item lore, action bar and progress display |
| `reward_profiles.yml` | Optional milestone commands/cosmetics; no default auto-enchants |


## Default Supported Items

- Pickaxes, axes, shovels and hoes
- Swords
- Bows and crossbows
- Tridents
- Maces
- Wooden through Netherite spears, including Copper
- Shields (blocked-hit XP in either hand)
- Wearable armor, including Copper and Turtle Helmet
- Fishing rods

## Compact Item Display

Different display profiles keep tooltips readable instead of showing every
possible stat on every item.

A weapon can show:

```text
Level 73/100
[progress] 6.84K/7.52K

Attack: 24.61 (+207.6%)
Critical: 9.6% • +48.2%
Durability: +70.1%
```

Armor instead shows Defense, Critical Defense and Durability. A detailed stats
GUI is intentionally left for a later feature.

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/awkgearmastery help [page]` | Paginated command help | None |
| `/awkgearmastery reload` | Reload configuration | `awkgearmastery.admin` |
| `/awkgearmastery reset <player>` | Reset hand item for player | `awkgearmastery.admin` |
| `/awkgearmastery reset <player> --all` | Reset all AwkGearMastery items for player | `awkgearmastery.admin` |
| `/awkgearmastery xp <amount>` | Set hand item XP | `awkgearmastery.admin` |
| `/awkgearmastery level <level>` | Set hand item level within its progression cap | `awkgearmastery.admin` |
| `/awkgearmastery levelup` | Increase hand item level by one | `awkgearmastery.admin` |
| `/awkgearmastery debug` | Show debug information | `awkgearmastery.admin` |

## PlaceholderAPI

| Placeholder | Description |
| --- | --- |
| `%awkgearmastery_level%` | Current item level |
| `%awkgearmastery_xp%` | Current item XP |
| `%awkgearmastery_max_xp%` | XP threshold for the current level |
| `%awkgearmastery_progress%` | Progress percentage |
| `%awkgearmastery_progress_bar%` | Rendered progress bar |
| `%awkgearmastery_item_profile%` | Current item profile |
| `%awkgearmastery_max_level%` | Max level from the progression profile |

## Attribution

AwkGearMastery is an AGPL-3.0-licensed continuation of open-source work originally
created by byteful. Original authorship and copyright notices remain credited;
the current progression architecture and new development are maintained by s3tupw1zard.
