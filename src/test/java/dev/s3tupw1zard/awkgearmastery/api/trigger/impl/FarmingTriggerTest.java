package dev.s3tupw1zard.awkgearmastery.api.trigger.impl;

import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerContext;
import dev.s3tupw1zard.awkgearmastery.profile.trigger.TriggerProfile;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FarmingTriggerTest {
    @Test
    void unchangedOrUnrelatedSoilCannotEarnTillingXp() {
        for (Material soil : new Material[]{Material.DIRT, Material.GRASS_BLOCK, Material.DIRT_PATH,
                Material.COARSE_DIRT, Material.ROOTED_DIRT}) {
            assertFalse(FarmingTrigger.isSuccessfulTill(soil, soil));
            assertFalse(FarmingTrigger.isSuccessfulTill(soil, Material.AIR));
        }
        assertFalse(FarmingTrigger.isSuccessfulTill(Material.FARMLAND, Material.FARMLAND));
        assertFalse(FarmingTrigger.isSuccessfulTill(Material.STONE, Material.FARMLAND));
    }

    @Test
    void realVanillaSoilConversionsAreEligible() {
        assertTrue(FarmingTrigger.isSuccessfulTill(Material.DIRT, Material.FARMLAND));
        assertTrue(FarmingTrigger.isSuccessfulTill(Material.GRASS_BLOCK, Material.FARMLAND));
        assertTrue(FarmingTrigger.isSuccessfulTill(Material.DIRT_PATH, Material.FARMLAND));
        assertTrue(FarmingTrigger.isSuccessfulTill(Material.COARSE_DIRT, Material.DIRT));
        assertTrue(FarmingTrigger.isSuccessfulTill(Material.ROOTED_DIRT, Material.DIRT));
    }

    @Test
    void rawInteractionCannotAwardXpBeforeTheSoilChanges() {
        final TriggerContext context = mock(TriggerContext.class);
        final TriggerProfile profile = mock(TriggerProfile.class);
        when(context.getTriggerProfile()).thenReturn(profile);
        when(context.getOriginalEventAs(PlayerInteractEvent.class)).thenReturn(mock(PlayerInteractEvent.class));
        when(context.getSourceAs(Block.class)).thenReturn(mock(Block.class));
        when(profile.isSourceAllowed("TILL")).thenReturn(true);
        final FarmingTrigger trigger = new FarmingTrigger();
        assertFalse(trigger.canHandle(context));
        assertEquals(0.0, trigger.calculateXpModifier(context));

        when(context.getSourceAs(String.class)).thenReturn("TILL");
        assertTrue(trigger.canHandle(context));
        when(profile.isSourceAllowed("TILL")).thenReturn(false);
        assertFalse(trigger.canHandle(context));
    }
}
