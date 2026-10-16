package dev.s3tupw1zard.awkgearmastery.listeners;

import dev.s3tupw1zard.awkgearmastery.api.scheduler.Scheduler;
import io.papermc.paper.event.player.PlayerItemGroupCooldownEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/** Sends a short cooldown before clicking so the client does not predict shield use. */
public final class ShieldAimCooldown implements Listener {
    private static final int DURATION = 4;
    private final Scheduler scheduler;
    private final Predicate<Player> preferHoe;
    private final IntSupplier tick;
    private final Set<UUID> tracking = ConcurrentHashMap.newKeySet();
    private final Set<UUID> writing = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Lease> leases = new ConcurrentHashMap<>();

    public ShieldAimCooldown(Scheduler scheduler, Predicate<Player> preferHoe, IntSupplier tick) {
        this.scheduler = scheduler;
        this.preferHoe = preferHoe;
        this.tick = tick;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) { track(event.getPlayer()); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) { track(event.getPlayer()); }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) { later(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeld(PlayerItemHeldEvent event) { later(event.getPlayer()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) { later(event.getPlayer()); }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) { later(player); }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        tracking.remove(event.getPlayer().getUniqueId());
        leases.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExternalCooldown(PlayerItemGroupCooldownEvent event) {
        final UUID id = event.getPlayer().getUniqueId();
        final Lease lease = leases.get(id);
        if (lease != null && !writing.contains(id) && lease.group().equals(event.getCooldownGroup())) {
            // Never remove, shorten or continually refresh a real combat/plugin cooldown.
            leases.remove(id);
        }
    }

    private void later(Player player) {
        scheduler.entityDelayed(() -> track(player), player, 1);
    }

    private void track(Player player) {
        if (!player.isOnline()) { return; }
        final boolean equipped = equipped(player);
        update(player, equipped && preferHoe.test(player), tick.getAsInt());
        if (equipped && tracking.add(player.getUniqueId())) { next(player); }
    }

    private void next(Player player) {
        scheduler.entityDelayed(() -> {
            if (!player.isOnline() || !equipped(player)) {
                update(player, false, tick.getAsInt());
                tracking.remove(player.getUniqueId());
                return;
            }
            update(player, preferHoe.test(player), tick.getAsInt());
            next(player);
        }, player, 1);
    }

    private static boolean equipped(Player player) {
        return player.getInventory().getItemInMainHand().getType().name().endsWith("_HOE")
                && player.getInventory().getItemInOffHand().getType() == Material.SHIELD;
    }

    void update(Player player, boolean prefer, int now) {
        final UUID id = player.getUniqueId();
        final Lease lease = leases.get(id);
        if (!prefer) {
            leases.remove(id);
            if (lease != null && player.isOnline()) {
                final int remaining = player.getCooldown(lease.item());
                if (remaining > 0 && remaining <= Math.max(0, lease.until() - now) + 1) {
                    write(player, lease.item(), 0);
                }
            }
            return;
        }
        if (player.hasActiveItem() && player.getActiveItem().getType() == Material.SHIELD) {
            player.clearActiveItem();
        }
        final ItemStack shield = player.getInventory().getItemInOffHand();
        final int remaining = player.getCooldown(shield);
        if (lease == null && remaining > 0) { return; }
        if (lease != null && remaining > Math.max(0, lease.until() - now) + 1) {
            leases.remove(id);
            return;
        }
        if (remaining > 2) { return; }
        final var meta = shield.getItemMeta();
        final NamespacedKey custom = meta != null && meta.hasUseCooldown()
                ? meta.getUseCooldown().getCooldownGroup() : null;
        final NamespacedKey group = custom == null ? shield.getType().getKey() : custom;
        leases.put(id, new Lease(shield.clone(), group, now + DURATION));
        write(player, shield, DURATION);
        if (player.getCooldown(shield) != DURATION) { leases.remove(id); }
    }

    private void write(Player player, ItemStack item, int ticks) {
        final UUID id = player.getUniqueId();
        writing.add(id);
        try { player.setCooldown(item, ticks); }
        finally { writing.remove(id); }
    }

    private record Lease(ItemStack item, NamespacedKey group, int until) {}
}
