package dev.s3tupw1zard.awkgearmastery.listeners;

import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerSlot;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TriggerListenerTest {
    @Test
    void shieldDurabilityIsAttributedToTheActualHand() {
        final Player player = mock(Player.class);
        final PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        final ItemStack shield = mock(ItemStack.class);
        when(shield.getType()).thenReturn(Material.SHIELD);
        when(inventory.getItemInOffHand()).thenReturn(shield);
        assertEquals(TriggerSlot.OFF_HAND, TriggerListener.findDamagedEquipmentSlot(player, shield));

        when(inventory.getItemInOffHand()).thenReturn(null);
        when(inventory.getItemInMainHand()).thenReturn(shield);
        assertEquals(TriggerSlot.HAND, TriggerListener.findDamagedEquipmentSlot(player, shield));

        when(inventory.getItemInMainHand()).thenReturn(null);
        assertNull(TriggerListener.findDamagedEquipmentSlot(player, shield));
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
