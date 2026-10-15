# rc.2 configuration and testing

The JAR keeps existing configuration files. Installing it alone does not activate
changed defaults or add shield/axe profile settings to existing files.

## Apply the new defaults

1. Stop the server and back up `plugins/AwkGearMastery/`.
2. To use all new defaults, move the following files out of that directory:
   `item_profiles.yml`, `trigger_profiles.yml`, `xp_sources.yml`,
   `progression_profiles.yml`, and `display_profiles.yml`.
3. Install the new JAR and start the server. Missing files are recreated from the
   bundled defaults. Keep the backups to reapply intentional customizations.

`config.yml`, stat profiles, and item PDC progress do not need resetting.
Alternatively, manually merge the changed profiles from this PR: axes now reference
`[block_mining, combat]`, the weapon display and melee weapon stats; shields add an
item profile, a hand/offhand durability trigger, a source table and a display profile.
Merge the new XP and progression values if you want the revised balance.
Existing item XP is retained and evaluated against the new curve on subsequent
progression; the level cap remains 100.

## Progression defaults

- `max_level: 100`, `reference_max_level: 100`
- `first_level_xp: 100.0`, `final_level_xp: 50000.0`
- `curve_exponent: 2.2`, `max_level_influence: 0.50`

`final_level_xp` is the last transition cost, not total XP to reach level 100.
At the default cap, the entire curve costs approximately 1.56 million XP.
Keeping the reference cap at 100 and influence at 0.50 preserves sublinear
scaling when changing the level cap. First/final transition costs are scaled by
that setting when the cap differs from the reference.

## Server regression checks

- Kill a mob using an axe: the axe gains combat XP; break natural wood: mining XP
  still works. Confirm weapon stats appear after an item refresh.
- Till dirt and grass with a hoe, both with and without an offhand shield. Soil
  must change and only the hoe receives tilling XP.
- Click obstructed soil, existing farmland, and protected/cancelled interactions:
  no tilling XP. Spam-click one dirt block: only its real conversion earns XP.
- Till coarse/rooted dirt: conversion to dirt is eligible; coarse dirt's second
  conversion to farmland is a separate legitimate action.
- Block attacks with a shield in either hand: confirmed blocked damage earns
  shield XP, even when weak hits or Unbreaking cause no durability loss. Merely holding or raising it does not, nor does attacking with a
  sword while carrying a shield. Verify Unbreaking/cancelled durability events.
- Switch/remove a shield or hoe during a delayed update: a different item must
  not be overwritten. Verify shield durability decreases normally and persists.
- Compare ordinary block, ore, crop, armor, fishing, and mob XP against the bundled
  tables with XP booster permissions disabled.
- Verify level 1 -> 2 costs 100 XP, late levels are much slower, and reload/restart
  preserves progression.

Compilation and automated tests do not replace these in-game checks.

## Hoe/offhand-shield priority in Minecraft 26.3

The vanilla game now deliberately prioritizes raising a shield over tilling.
AwkGearMastery can override this only for a supported main-hand hoe, offhand
shield and valid soil actually under the crosshair, within interaction reach.
With no nearby mob targeting the player, right-click tills; when a mob within
8 blocks targets the player, normal shield behavior wins. Sneaking forces farming.

Configure `farming.shield_priority.enabled`, `threat_radius` and
`sneak_to_force_tilling` in `config.yml`. Missing entries use the bundled defaults;
regenerating existing files is not needed for this fix. Existing rc.2 shield
profiles using `ARMOR_DURABILITY` remain compatible, but the new default type is
`SHIELD_BLOCK`. Shields no longer also gain XP from their durability event.

Verify weak and strong melee hits, arrows, Unbreaking, rear/unblocked hits,
cancelled damage, main/offhand shields, and rapid consecutive blocked attacks.
Verify protected/obstructed soil and repeated clicks never yield farming XP;
shield priority returns when a nearby mob targets the player, while a passive or
uninterested mob does not block farming. Confirm sneak override and disabled mode.
On Folia, unfamiliar-region mob AI is treated conservatively as a threat and soil
outside the current player region is not modified.

### Shield animation while aiming

A short, client-visible shield cooldown is maintained while the hoe targets
eligible soil (and the farmland immediately after tilling). This prevents the
normal raise-then-cancel animation instead of waiting to stop an active shield.
The guard updates on movement/rotation and while the hoe/shield combination is
equipped. Looking away releases only its own cooldown; real combat or other
plugin cooldowns are left intact. Threat/sneak priority settings still apply.
Verify both a single click and holding right-click, aiming away after tilling,
switching hotbar slots, swapping hands, and an externally applied shield cooldown.
Client prediction before a newly equipped/aimed state reaches the server can
still depend on latency; this is not a client mod.
