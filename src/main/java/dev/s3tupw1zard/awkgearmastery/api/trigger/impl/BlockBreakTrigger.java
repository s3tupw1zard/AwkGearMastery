package dev.s3tupw1zard.awkgearmastery.api.trigger.impl;

import dev.s3tupw1zard.awkgearmastery.api.trigger.Trigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerContext;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerIds;
import dev.s3tupw1zard.awkgearmastery.profile.trigger.TriggerProfile;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

public final class BlockBreakTrigger implements Trigger {
    @Override
    @NotNull
    public String getTriggerId() {
        return TriggerIds.BLOCK_BREAK;
    }

    @Override
    public boolean canHandle(@NotNull TriggerContext context) {
        Block block = context.getSourceAs(Block.class);
        if (block == null) {
            return false;
        }

        TriggerProfile profile = context.getTriggerProfile();
        String blockType = block.getType().name();

        return profile.isSourceAllowed(blockType);
    }

    @Override
    public double calculateXpModifier(@NotNull TriggerContext context) {
        Block block = context.getSourceAs(Block.class);
        if (block == null) {
            return 0;
        }

        TriggerProfile profile = context.getTriggerProfile();
        return profile.calculateXpModifier(block.getType().name());
    }

}
