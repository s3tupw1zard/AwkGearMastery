package dev.s3tupw1zard.awkgearmastery.listeners;

import dev.s3tupw1zard.awkgearmastery.api.scheduler.Scheduler;
import io.papermc.paper.event.player.PlayerItemGroupCooldownEvent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShieldAimCooldownTest {
    @Test
    void cooldownIsSentBeforeUseAndReleasedWhenLookingAway() {
        final Fixture f = new Fixture();
        f.guard.update(f.player, true, 100);
        verify(f.player).setCooldown(f.shield, 4);
        f.guard.update(f.player, false, 101);
        verify(f.player).setCooldown(f.shield, 0);
    }

    @Test
    void anAlreadyRaisedShieldIsLoweredWhenTheAimChangesToSoil() {
        final Fixture f = new Fixture();
        when(f.player.hasActiveItem()).thenReturn(true);
        when(f.player.getActiveItem()).thenReturn(f.shield);
        f.guard.update(f.player, true, 100);
        verify(f.player).clearActiveItem();
        verify(f.player).setCooldown(f.shield, 4);
    }

    @Test
    void existingCombatCooldownIsNeverShortened() {
        final Fixture f = new Fixture();
        f.remaining.set(100);
        f.guard.update(f.player, true, 100);
        f.guard.update(f.player, false, 101);
        verify(f.player, never()).setCooldown(any(ItemStack.class), anyInt());
    }

    @Test
    void externalCooldownReplacesOwnershipEvenWhenItIsShorter() {
        final Fixture f = new Fixture();
        f.guard.update(f.player, true, 100);
        final PlayerItemGroupCooldownEvent event = mock(PlayerItemGroupCooldownEvent.class);
        when(event.getPlayer()).thenReturn(f.player);
        when(event.getCooldownGroup()).thenReturn(NamespacedKey.minecraft("shield"));
        f.guard.onExternalCooldown(event);
        f.remaining.set(2);
        clearInvocations(f.player);
        f.guard.update(f.player, true, 101);
        f.guard.update(f.player, false, 101);
        verify(f.player, never()).setCooldown(any(ItemStack.class), anyInt());
    }

    private static final class Fixture {
        private final Player player = mock(Player.class);
        private final ItemStack shield = mock(ItemStack.class);
        private final AtomicInteger remaining = new AtomicInteger();
        private final ShieldAimCooldown guard = new ShieldAimCooldown(mock(Scheduler.class), p -> true, () -> 100);

        private Fixture() {
            final PlayerInventory inventory = mock(PlayerInventory.class);
            when(player.getUniqueId()).thenReturn(UUID.randomUUID());
            when(player.isOnline()).thenReturn(true);
            when(player.getInventory()).thenReturn(inventory);
            when(inventory.getItemInOffHand()).thenReturn(shield);
            when(shield.getType()).thenReturn(Material.SHIELD);
            when(shield.clone()).thenReturn(shield);
            when(player.getCooldown(shield)).thenAnswer(invocation -> remaining.get());
            doAnswer(invocation -> { remaining.set(invocation.getArgument(1)); return null; })
                    .when(player).setCooldown(eq(shield), anyInt());
        }
    }
}
