package dev.s3tupw1zard.awkgearmastery.api.trigger.impl;

import dev.s3tupw1zard.awkgearmastery.api.trigger.Trigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerContext;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerIds;
import dev.s3tupw1zard.awkgearmastery.profile.trigger.TriggerProfile;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

public final class FarmingTrigger implements Trigger {
    private static final Set<Material> TILLABLE_BLOCKS = EnumSet.of(
            Material.DIRT,
            Material.GRASS_BLOCK,
            Material.DIRT_PATH,
            Material.COARSE_DIRT,
            Material.ROOTED_DIRT
    );

    @Override
    @NotNull
    public String getTriggerId() {
        return TriggerIds.FARMING;
    }

    @Override
    public boolean canHandle(@NotNull TriggerContext context) {
        final TriggerProfile profile = context.getTriggerProfile();
        if (context.getOriginalEventAs(PlayerInteractEvent.class) != null) {
            // Only the listener's verified post-interaction conversion uses this source.
            return "TILL".equals(context.getSourceAs(String.class)) && profile.isSourceAllowed("TILL");
        }
        final Block block = context.getSourceAs(Block.class);
        if (block == null) {
            return false;
        }
        final Material blockType = block.getType();

        if (context.getOriginalEventAs(BlockBreakEvent.class) != null) {
            if (!isMatureCropSource(block)) {
                return false;
            }

            return profile.isSourceAllowed(blockType.name());
        }

        return false;
    }

    @Override
    public double calculateXpModifier(@NotNull TriggerContext context) {
        final TriggerProfile profile = context.getTriggerProfile();
        if (context.getOriginalEventAs(PlayerInteractEvent.class) != null) {
            return "TILL".equals(context.getSourceAs(String.class)) ? profile.calculateXpModifier("TILL") : 0.0;
        }
        final Block block = context.getSourceAs(Block.class);
        return block == null ? 0.0 : profile.calculateXpModifier(block.getType().name());
    }

    public static boolean isTillableSoil(@NotNull Material material) {
        return TILLABLE_BLOCKS.contains(material);
    }

    public static boolean isSuccessfulTill(@NotNull Material before, @NotNull Material after) {
        return switch (before) {
            case DIRT, GRASS_BLOCK, DIRT_PATH -> after == Material.FARMLAND;
            case COARSE_DIRT, ROOTED_DIRT -> after == Material.DIRT;
            default -> false;
        };
    }

    public static boolean isMatureCropSource(@NotNull Block block) {
        final BlockData data = block.getBlockData();
        return data instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge();
    }
}
