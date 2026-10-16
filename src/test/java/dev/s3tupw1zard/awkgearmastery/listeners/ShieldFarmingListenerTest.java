package dev.s3tupw1zard.awkgearmastery.listeners;

import dev.s3tupw1zard.awkgearmastery.AwkGearMasteryPlugin;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.RayTraceResult;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShieldFarmingListenerTest {
    @Test
    void onlyMobsActuallyTargetingThePlayerAreThreats() {
        final Player player = mock(Player.class);
        final Player other = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        final Mob mob = mock(Mob.class);
        assertFalse(ShieldFarmingListener.targetsPlayer(mob, player));
        when(mob.getTarget()).thenReturn(other);
        assertFalse(ShieldFarmingListener.targetsPlayer(mob, player));
        when(mob.getTarget()).thenReturn(player);
        assertTrue(ShieldFarmingListener.targetsPlayer(mob, player));
    }

    @Test
    void invalidObstructedOrBottomClickedSoilIsNotConverted() {
        final Block soil = mock(Block.class);
        final Block above = mock(Block.class);
        when(soil.getType()).thenReturn(Material.DIRT);
        when(soil.getRelative(BlockFace.UP)).thenReturn(above);
        assertFalse(ShieldFarmingListener.validSoil(soil, BlockFace.UP));
        when(above.isEmpty()).thenReturn(true);
        assertTrue(ShieldFarmingListener.validSoil(soil, BlockFace.UP));
        assertFalse(ShieldFarmingListener.validSoil(soil, BlockFace.DOWN));
        when(soil.getType()).thenReturn(Material.FARMLAND);
        assertFalse(ShieldFarmingListener.validSoil(soil, BlockFace.UP));
    }

    @Test
    void farmlandKeepsTheShieldSuppressedAfterTheHoeActionCompletes() {
        final Block farmland = mock(Block.class);
        when(farmland.getType()).thenReturn(Material.FARMLAND);
        assertTrue(ShieldFarmingListener.aimUsesHoe(farmland, BlockFace.UP));
        assertFalse(ShieldFarmingListener.aimUsesHoe(farmland, BlockFace.DOWN));
        when(farmland.getType()).thenReturn(Material.STONE);
        assertFalse(ShieldFarmingListener.aimUsesHoe(farmland, BlockFace.UP));
    }

    @Test
    void crosshairMustActuallyPointAtTheClickedSoil() {
        final Player player = mock(Player.class);
        final Block soil = mock(Block.class);
        assertFalse(ShieldFarmingListener.isAimedAt(player, soil, 4.5));
        final RayTraceResult hit = mock(RayTraceResult.class);
        when(player.rayTraceBlocks(4.5, FluidCollisionMode.NEVER)).thenReturn(hit);
        when(hit.getHitBlock()).thenReturn(mock(Block.class));
        assertFalse(ShieldFarmingListener.isAimedAt(player, soil, 4.5));
        when(hit.getHitBlock()).thenReturn(soil);
        assertTrue(ShieldFarmingListener.isAimedAt(player, soil, 4.5));
    }

    @Test
    void protectionCancellationRestoresSoilAndDoesNotConsumeDurability() {
        conversionTest(true);
    }

    @Test
    void acceptedConversionUsesVanillaDurabilityApiExactlyOnce() {
        conversionTest(false);
    }

    private void conversionTest(boolean cancel) {
        final AwkGearMasteryPlugin plugin = mock(AwkGearMasteryPlugin.class);
        final Server server = mock(Server.class);
        final PluginManager pm = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pm);
        final Player player = mock(Player.class);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        final ItemStack hoe = mock(ItemStack.class);
        when(hoe.getType()).thenReturn(Material.IRON_HOE);
        final Block block = mock(Block.class);
        final BlockState previous = mock(BlockState.class);
        final World world = mock(World.class);
        when(block.getState()).thenReturn(previous);
        when(block.getWorld()).thenReturn(world);
        final AtomicReference<Material> type = new AtomicReference<>(Material.DIRT);
        when(block.getType()).thenAnswer(invocation -> type.get());
        doAnswer(invocation -> { type.set(invocation.getArgument(0)); return null; })
                .when(block).setType(any(Material.class), eq(false));
        if (cancel) {
            doAnswer(invocation -> {
                ((BlockPlaceEvent) invocation.getArgument(0)).setCancelled(true);
                return null;
            }).when(pm).callEvent(any(BlockPlaceEvent.class));
        }
        final ShieldFarmingListener listener = new ShieldFarmingListener(plugin, (p, e) -> fail("XP callback not used here"));
        assertEquals(!cancel, listener.convertSoil(player, block, hoe));
        if (cancel) {
            verify(previous).update(true, false);
            verify(player, never()).damageItemStack(any(EquipmentSlot.class), anyInt());
        } else {
            assertEquals(Material.FARMLAND, type.get());
            verify(player).damageItemStack(EquipmentSlot.HAND, 1);
            // The same block can no longer be converted repeatedly for XP.
            assertFalse(listener.convertSoil(player, block, hoe));
        }
    }
}
