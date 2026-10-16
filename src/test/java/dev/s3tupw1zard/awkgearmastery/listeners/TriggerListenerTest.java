package dev.s3tupw1zard.awkgearmastery.listeners;

import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerSlot;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TriggerListenerTest {
    @Test
    @SuppressWarnings("deprecation")
    void confirmedBlocksEarnXpEvenWhenNoHealthOrDurabilityIsLost() {
        final Player player = mock(Player.class);
        final ItemStack shield = mock(ItemStack.class);
        when(shield.getType()).thenReturn(Material.SHIELD);
        when(player.getActiveItem()).thenReturn(shield);
        final EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)).thenReturn(true);
        when(event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING)).thenReturn(-1.0);
        when(event.getFinalDamage()).thenReturn(0.0);
        when(player.getActiveItemHand()).thenReturn(EquipmentSlot.OFF_HAND);
        assertEquals(TriggerSlot.OFF_HAND, TriggerListener.blockedShieldSlot(event));
        when(player.getActiveItemHand()).thenReturn(EquipmentSlot.HAND);
        assertEquals(TriggerSlot.HAND, TriggerListener.blockedShieldSlot(event));

        when(event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING)).thenReturn(0.0);
        assertNull(TriggerListener.blockedShieldSlot(event));
        when(event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING)).thenReturn(-1.0);
        when(event.isCancelled()).thenReturn(true);
        assertNull(TriggerListener.blockedShieldSlot(event));
    }

    @Test
    void heldToolsDoNotReceiveDefensiveDurabilityXp() {
        final Player player = mock(Player.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        final ItemStack hoe = mock(ItemStack.class);
        when(hoe.getType()).thenReturn(Material.IRON_HOE);
        when(inventory.getItemInMainHand()).thenReturn(hoe);
        assertNull(TriggerListener.findDamagedEquipmentSlot(player, hoe));
    }
}
