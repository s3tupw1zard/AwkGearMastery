package dev.s3tupw1zard.awkgearmastery.listeners;

import dev.s3tupw1zard.awkgearmastery.AwkGearMasteryPlugin;
import dev.s3tupw1zard.awkgearmastery.api.trigger.impl.FarmingTrigger;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/** Configurable farming priority over the shield-first behavior introduced in Minecraft 26.3. */
public final class ShieldFarmingListener implements Listener {
    private final AwkGearMasteryPlugin plugin;
    private final BiConsumer<Player, PlayerInteractEvent> awardXp;
    private final Set<UUID> pending = ConcurrentHashMap.newKeySet();

    public ShieldFarmingListener(
            @NotNull AwkGearMasteryPlugin plugin,
            @NotNull BiConsumer<Player, PlayerInteractEvent> awardXp
    ) {
        this.plugin = plugin;
        this.awardXp = awardXp;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        final var config = plugin.getConfigManager().getMainConfig();
        if (!config.getBoolean("farming.shield_priority.enabled", true)
                || event.getAction() != Action.RIGHT_CLICK_BLOCK
                || event.useInteractedBlock() == Event.Result.DENY
                || event.useItemInHand() == Event.Result.DENY) {
            return;
        }
        final Player player = event.getPlayer();
        final Block block = event.getClickedBlock();
        final ItemStack hoe = player.getInventory().getItemInMainHand();
        if (block == null || !eligiblePlayer(player)
                || !hoe.getType().name().endsWith("_HOE")
                || player.getInventory().getItemInOffHand().getType() != Material.SHIELD
                || plugin.getProfileManager().getProfileForMaterial(hoe.getType()) == null
                || !validSoil(block, event.getBlockFace()) || !isAimedAt(player, block)
                || shouldKeepShield(player)) {
            return;
        }

        // Deny only item use, retaining block/protection cancellation from other plugins.
        // Vanilla HoeItem now deliberately passes to the shield; implement just this soil
        // action below, with the same BlockPlaceEvent gate used by vanilla item use.
        event.setUseItemInHand(Event.Result.DENY);
        if (!pending.add(player.getUniqueId())) {
            return;
        }
        final ItemStack expected = hoe.clone();
        final Material before = block.getType();
        final var world = block.getWorld();
        if (player.hasActiveItem() && player.getActiveItem().getType() == Material.SHIELD) {
            player.clearActiveItem();
        }
        plugin.getScheduler().entityDelayed(() -> {
            try {
                if (!player.isOnline() || !eligiblePlayer(player) || !player.getWorld().equals(world)
                        || !Bukkit.isOwnedByCurrentRegion(block)
                        || event.useInteractedBlock() == Event.Result.DENY
                        || block.getType() != before || !validSoil(block, event.getBlockFace())
                        || !isAimedAt(player, block) || shouldKeepShield(player)
                        || player.getInventory().getItemInOffHand().getType() != Material.SHIELD) {
                    return;
                }
                final ItemStack live = player.getInventory().getItemInMainHand();
                if (!TriggerListener.sameItemIgnoringWear(expected, live)) {
                    return;
                }
                if (convertSoil(player, block, live)) {
                    awardXp.accept(player, event);
                }
            } finally {
                pending.remove(player.getUniqueId());
            }
        }, player, 1);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }

    private boolean eligiblePlayer(Player player) {
        return !player.isDead() && (player.getGameMode() == GameMode.SURVIVAL
                || player.getGameMode() == GameMode.CREATIVE)
                && player.hasPermission("awkgearmastery.enabled")
                && !plugin.getConfigManager().getSettings().getDisabledWorlds()
                        .contains(player.getWorld().getName());
    }

    private boolean shouldKeepShield(Player player) {
        final var config = plugin.getConfigManager().getMainConfig();
        if (player.isSneaking() && config.getBoolean("farming.shield_priority.sneak_to_force_tilling", true)) {
            return false;
        }
        final double configured = config.getDouble("farming.shield_priority.threat_radius", 8.0);
        final double radius = Double.isFinite(configured) ? Math.max(1.0, Math.min(32.0, configured)) : 8.0;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            // Be conservative at Folia region boundaries rather than reading another region's mob AI.
            if (!Bukkit.isOwnedByCurrentRegion(mob) || targetsPlayer(mob, player)) {
                return true;
            }
        }
        return false;
    }

    static boolean targetsPlayer(Mob mob, Player player) {
        final var target = mob.getTarget();
        return target != null && target.getUniqueId().equals(player.getUniqueId());
    }

    static boolean validSoil(Block block, BlockFace face) {
        return face != BlockFace.DOWN && FarmingTrigger.isTillableSoil(block.getType())
                && (block.getType() == Material.ROOTED_DIRT || block.getRelative(BlockFace.UP).isEmpty());
    }

    static boolean isAimedAt(Player player, Block block) {
        final var attribute = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        final double reach = attribute == null ? 4.5 : attribute.getValue();
        return isAimedAt(player, block, reach);
    }

    static boolean isAimedAt(Player player, Block block, double reach) {
        final var hit = player.rayTraceBlocks(reach, FluidCollisionMode.NEVER);
        return hit != null && block.equals(hit.getHitBlock());
    }

    boolean convertSoil(Player player, Block block, ItemStack hoe) {
        final Material before = block.getType();
        if (!FarmingTrigger.isTillableSoil(before)) {
            return false;
        }
        final Material hoeType = hoe.getType();
        final Material after = before == Material.COARSE_DIRT || before == Material.ROOTED_DIRT
                ? Material.DIRT : Material.FARMLAND;
        final var previous = block.getState();
        block.setType(after, false);
        final BlockPlaceEvent place = new BlockPlaceEvent(
                block, previous, block, hoe, player, true, EquipmentSlot.HAND);
        plugin.getServer().getPluginManager().callEvent(place);
        if (place.isCancelled() || !place.canBuild()) {
            previous.update(true, false);
            return false;
        }
        // Do not grant XP or consume wear if another plugin replaced the proposed soil.
        if (block.getType() != after) {
            return false;
        }
        if (before == Material.ROOTED_DIRT) {
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(Material.HANGING_ROOTS));
        }
        if (player.getGameMode() != GameMode.CREATIVE) {
            player.damageItemStack(EquipmentSlot.HAND, 1);
        }
        block.getWorld().playSound(block.getLocation(), "minecraft:item.hoe.till", SoundCategory.BLOCKS, 1.0f, 1.0f);
        player.swingMainHand();
        player.incrementStatistic(Statistic.USE_ITEM, hoeType);
        return true;
    }
}
