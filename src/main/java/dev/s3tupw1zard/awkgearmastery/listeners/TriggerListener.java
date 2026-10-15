package dev.s3tupw1zard.awkgearmastery.listeners;

import dev.s3tupw1zard.awkgearmastery.AwkGearMasteryPlugin;
import dev.s3tupw1zard.awkgearmastery.api.block.BlockPosition;
import dev.s3tupw1zard.awkgearmastery.api.item.AwkGearMasteryItem;
import dev.s3tupw1zard.awkgearmastery.api.trigger.*;
import dev.s3tupw1zard.awkgearmastery.api.trigger.impl.FarmingTrigger;
import dev.s3tupw1zard.awkgearmastery.config.ConfigManager;
import dev.s3tupw1zard.awkgearmastery.profile.ProfileManager;
import dev.s3tupw1zard.awkgearmastery.profile.item.ItemProfile;
import dev.s3tupw1zard.awkgearmastery.profile.trigger.TriggerProfile;
import dev.s3tupw1zard.awkgearmastery.util.AwkGearMasteryUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class TriggerListener implements Listener {
    private final ProfileManager profileManager;
    private final TriggerRegistry triggerRegistry;
    private final XPHandler xpHandler;
    private final Set<BlockPosition> pendingTilling = ConcurrentHashMap.newKeySet();

    public TriggerListener(
            @NotNull ProfileManager profileManager,
            @NotNull TriggerRegistry triggerRegistry
    ) {
        this.profileManager = profileManager;
        this.triggerRegistry = triggerRegistry;
        this.xpHandler = new XPHandler(profileManager);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!player.hasPermission("awkgearmastery.enabled")) {
            return;
        }

        ConfigManager.Settings settings = AwkGearMasteryPlugin.getInstance().getConfigManager().getSettings();
        if (!settings.isCountPlayerPlacedBlocks()
                && AwkGearMasteryPlugin.getInstance().getBlockDataManager().isPlacedBlock(BlockPosition.fromBukkit(block))) {
            if (settings.isIgnorePlayerPlacedBlocksForFullyGrownCrops() && FarmingTrigger.isMatureCropSource(block)) {
                handleTrigger(player, player.getInventory().getItemInMainHand(), TriggerSlot.HAND, block, event, TriggerIds.FARMING);
            }
            return;
        }

        handleTrigger(player, player.getInventory().getItemInMainHand(), TriggerSlot.HAND, block, event, TriggerIds.BLOCK_BREAK, TriggerIds.FARMING);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("awkgearmastery.enabled")) {
            return;
        }

        Entity caught = event.getCaught();
        ItemStack item;
        TriggerSlot slot;

        if (!AwkGearMasteryUtil.supportsDualWielding() || event.getHand() == null) {
            item = event.getPlayer().getInventory().getItemInMainHand();
            slot = TriggerSlot.HAND;
        } else {
            item = event.getPlayer().getInventory().getItem(event.getHand());
            slot = TriggerSlot.fromBukkit(event.getHand());
        }

        handleTrigger(player, item, slot, caught, event, TriggerIds.FISHING);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.PHYSICAL) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock != null && event.useInteractedBlock() == Event.Result.DENY) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || event.useItemInHand() == Event.Result.DENY) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("awkgearmastery.enabled")) {
            return;
        }

        TriggerSlot slot = !AwkGearMasteryUtil.supportsDualWielding() || event.getHand() == null
                ? TriggerSlot.HAND
                : TriggerSlot.fromBukkit(event.getHand());

        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
            // Do not replace the held hoe while vanilla is still processing its use.
            // XP is only awarded after a real soil conversion, never for a click alone.
            if (action == Action.RIGHT_CLICK_BLOCK) {
                scheduleTilling(event, player, item, slot, clickedBlock);
            }
            handleTrigger(player, item, slot, clickedBlock, event, TriggerIds.RIGHT_CLICK);
        } else if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            handleTrigger(player, item, slot, clickedBlock, event, TriggerIds.LEFT_CLICK);
        }
    }

    private void scheduleTilling(
            @NotNull PlayerInteractEvent event,
            @NotNull Player player,
            @NotNull ItemStack item,
            @NotNull TriggerSlot slot,
            @Nullable Block block
    ) {
        if (block == null || !item.getType().name().endsWith("_HOE")) {
            return;
        }
        final Material before = block.getType();
        if (!FarmingTrigger.isTillableSoil(before)) {
            return;
        }
        final BlockPosition position = BlockPosition.fromBukkit(block);
        if (!pendingTilling.add(position)) {
            return;
        }
        final ItemStack expected = item.clone();
        final var world = block.getWorld();
        final var plugin = AwkGearMasteryPlugin.getInstance();
        plugin.getScheduler().locationDelayed(() -> {
            try {
                if (event.useInteractedBlock() == Event.Result.DENY
                        || event.useItemInHand() == Event.Result.DENY
                        || !FarmingTrigger.isSuccessfulTill(before, block.getType())) {
                    return;
                }
                // On Folia the player may have moved to another region by now.
                plugin.getScheduler().entityDelayed(() -> {
                    if (!player.isOnline() || !player.getWorld().equals(world)
                            || !player.hasPermission("awkgearmastery.enabled")) {
                        return;
                    }
                    final ItemStack live = AwkGearMasteryUtil.getItemInSlot(player, slot);
                    if (live == null || !sameItemIgnoringWear(expected, live)) {
                        return;
                    }
                    handleTrigger(player, live, slot, "TILL", event, TriggerIds.FARMING);
                }, player, 1);
            } finally {
                pendingTilling.remove(position);
            }
        }, block.getLocation(), 1);
    }

    static boolean sameItemIgnoringWear(@NotNull ItemStack expected, @NotNull ItemStack live) {
        if (expected.getType() != live.getType() || expected.getAmount() != live.getAmount()) {
            return false;
        }
        final ItemMeta expectedMeta = expected.getItemMeta();
        final ItemMeta liveMeta = live.getItemMeta();
        if (expectedMeta != null && liveMeta != null) {
            final NamespacedKey idKey = NamespacedKey.fromString("awkgearmastery:item_id");
            if (idKey != null) {
                final String expectedId = expectedMeta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
                final String liveId = liveMeta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
                if (expectedId != null || liveId != null) {
                    return java.util.Objects.equals(expectedId, liveId);
                }
            }
        }
        if (expectedMeta instanceof Damageable expectedDamage && liveMeta instanceof Damageable liveDamage) {
            expectedDamage.setDamage(liveDamage.getDamage());
        }
        return java.util.Objects.equals(expectedMeta, liveMeta);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("awkgearmastery.enabled")) {
            return;
        }

        TriggerSlot slot = !AwkGearMasteryUtil.supportsDualWielding()
                ? TriggerSlot.HAND
                : TriggerSlot.fromBukkit(event.getHand());
        ItemStack consumedItem = event.getItem();
        handleTrigger(player, consumedItem, slot, consumedItem, event, TriggerIds.CONSUME);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("awkgearmastery.enabled")) {
            return;
        }

        ItemStack damagedItem = event.getItem();
        if (damagedItem.getType() == Material.AIR || damagedItem.getType() == Material.SHIELD
                || event.getDamage() <= 0) {
            return;
        }

        TriggerSlot armorSlot = findDamagedEquipmentSlot(player, damagedItem);
        if (armorSlot == null) {
            return;
        }

        handleTrigger(player, damagedItem, armorSlot, damagedItem.getType(), event, TriggerIds.ARMOR_DURABILITY);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onShieldBlock(EntityDamageByEntityEvent event) {
        final TriggerSlot slot = blockedShieldSlot(event);
        if (slot == null || !(event.getEntity() instanceof Player player)
                || !player.hasPermission("awkgearmastery.enabled")) {
            return;
        }
        final ItemStack expected = player.getInventory().getItem(
                slot == TriggerSlot.HAND ? EquipmentSlot.HAND : EquipmentSlot.OFF_HAND).clone();
        // Wait for vanilla shield wear/disable processing before updating its lore/PDC.
        AwkGearMasteryPlugin.getInstance().getScheduler().entityDelayed(() -> {
            if (!player.isOnline() || event.isCancelled() || !hasBlockedDamage(event)
                    || !player.hasPermission("awkgearmastery.enabled")) {
                return;
            }
            final ItemStack live = AwkGearMasteryUtil.getItemInSlot(player, slot);
            if (live == null || !sameItemIgnoringWear(expected, live)) {
                return;
            }
            // Support the previous shield profile type too, so existing rc.2 configs work.
            handleTrigger(player, live, slot, Material.SHIELD, event,
                    // Keep the live stack identity while the player continues blocking.
                    // Replacing the inventory stack can interrupt active item use.
                    updated -> live.setItemMeta(updated.getItemMeta()),
                    TriggerIds.SHIELD_BLOCK, TriggerIds.ARMOR_DURABILITY);
        }, player, 1);
    }

    @Nullable
    @SuppressWarnings("deprecation") // Paper exposes confirmed blocked damage through this modifier.
    static TriggerSlot blockedShieldSlot(@NotNull EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !(event.getEntity() instanceof Player player)
                || !hasBlockedDamage(event)
                || player.getActiveItem().getType() != Material.SHIELD) {
            return null;
        }
        final EquipmentSlot hand = player.getActiveItemHand();
        return hand == EquipmentSlot.HAND ? TriggerSlot.HAND
                : hand == EquipmentSlot.OFF_HAND ? TriggerSlot.OFF_HAND : null;
    }

    @SuppressWarnings("deprecation")
    private static boolean hasBlockedDamage(EntityDamageByEntityEvent event) {
        if (!event.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)) {
            return false;
        }
        final double blocked = event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING);
        return Double.isFinite(blocked) && blocked < 0.0;
    }

    public void awardVerifiedTilling(@NotNull Player player, @NotNull PlayerInteractEvent event) {
        final ItemStack hoe = player.getInventory().getItemInMainHand();
        if (hoe.getType().name().endsWith("_HOE")) {
            handleTrigger(player, hoe, TriggerSlot.HAND, "TILL", event, TriggerIds.FARMING);
        }
    }

    private void handleTrigger(
            @NotNull Player player,
            @NotNull ItemStack item,
            @Nullable TriggerSlot slot,
            @Nullable Object source,
            @NotNull Event event,
            @NotNull String... triggerIds
    ) {
        handleTrigger(player, item, slot, source, event, null, triggerIds);
    }

    private void handleTrigger(
            @NotNull Player player,
            @NotNull ItemStack item,
            @Nullable TriggerSlot slot,
            @Nullable Object source,
            @NotNull Event event,
            @Nullable Consumer<ItemStack> committer,
            @NotNull String... triggerIds
    ) {
        if (item.getType() == Material.AIR) {
            return;
        }

        ConfigManager.Settings settings = AwkGearMasteryPlugin.getInstance().getConfigManager().getSettings();
        if (settings.getDisabledWorlds().contains(player.getWorld().getName())) {
            return;
        }

        ItemProfile itemProfile = profileManager.getProfileForMaterial(item.getType());
        if (itemProfile == null) {
            return;
        }

        List<TriggerProfile> triggerProfiles = profileManager.getTriggerProfilesFor(itemProfile);
        if (triggerProfiles.isEmpty()) {
            return;
        }

        double totalModifier = 0.0;
        for (TriggerProfile triggerProfile : triggerProfiles) {
            String triggerId = triggerProfile.getTriggerId();
            if (!isApplicableTrigger(triggerId, triggerIds)) {
                continue;
            }

            if (!triggerProfile.getSlotFilter().matches(slot)) {
                continue;
            }

            Trigger trigger = triggerRegistry.get(triggerId);
            if (trigger == null) {
                continue;
            }

            TriggerContext context = TriggerContext.builder()
                    .player(player)
                    .item(item)
                    .triggerId(triggerId)
                    .slot(slot)
                    .source(source)
                    .originalEvent(event)
                    .itemProfile(itemProfile)
                    .triggerProfile(triggerProfile)
                    .build();

            if (!trigger.canHandle(context)) {
                continue;
            }

            if (AwkGearMasteryPlugin.getInstance().getXpSourceRegistry().hasSource(triggerProfile.getId())) {
                totalModifier += AwkGearMasteryPlugin.getInstance()
                        .getXpSourceRegistry()
                        .calculate(triggerProfile.getId(), context);
            } else {
                totalModifier += trigger.calculateXpModifier(context);
            }
        }

        if (totalModifier <= 0.0) {
            return;
        }

        AwkGearMasteryItem tool = AwkGearMasteryUtil.createAwkGearMasteryItem(item);
        if (committer != null) {
            xpHandler.handleDirectCommit(player, itemProfile, tool, totalModifier, committer);
        } else {
            xpHandler.handle(
                    player,
                    itemProfile,
                    slot,
                    tool,
                    totalModifier,
                    event instanceof PlayerItemDamageEvent
            );
        }
    }

    private boolean isApplicableTrigger(@NotNull String triggerId, @NotNull String[] triggerIds) {
        for (String id : triggerIds) {
            if (id.equals(triggerId)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    static TriggerSlot findDamagedEquipmentSlot(@NotNull Player player, @NotNull ItemStack itemStack) {
        if (matchesItem(player.getInventory().getHelmet(), itemStack)) {
            return TriggerSlot.HELMET;
        }

        if (matchesItem(player.getInventory().getChestplate(), itemStack)) {
            return TriggerSlot.CHESTPLATE;
        }

        if (matchesItem(player.getInventory().getLeggings(), itemStack)) {
            return TriggerSlot.LEGGINGS;
        }

        if (matchesItem(player.getInventory().getBoots(), itemStack)) {
            return TriggerSlot.BOOTS;
        }

        return null;
    }

    private static boolean matchesItem(@Nullable ItemStack item, @NotNull ItemStack check) {
        return item != null && item.equals(check);
    }
}
