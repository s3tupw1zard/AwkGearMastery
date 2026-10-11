package dev.s3tupw1zard.awkgearmastery.api.trigger.impl;

import dev.s3tupw1zard.awkgearmastery.api.trigger.Trigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerContext;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerIds;
import dev.s3tupw1zard.awkgearmastery.profile.trigger.TriggerProfile;
import dev.s3tupw1zard.awkgearmastery.profile.trigger.TriggerSettings;
import org.bukkit.block.Block;
import org.bukkit.event.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public final class RightClickTrigger implements Trigger {
    @Override
    @NotNull
    public String getTriggerId() {
        return TriggerIds.RIGHT_CLICK;
    }

    @Override
    public boolean canHandle(@NotNull TriggerContext context) {
        TriggerProfile profile = context.getTriggerProfile();
        TriggerSettings settings = profile.getSettings();
        TriggerSettings.ClickMode clickMode = settings.getClickMode();

        if (clickMode == null || clickMode == TriggerSettings.ClickMode.ANY) {
            return true;
        }

        PlayerInteractEvent event = context.getOriginalEventAs(PlayerInteractEvent.class);
        if (event == null) {
            return false;
        }
        Block clickedBlock = event.getClickedBlock();

        switch (clickMode) {
            case ON_BLOCK:
                return clickedBlock != null;
            case ON_AIR:
                return clickedBlock == null;
            case ON_ENTITY:
                return false;
            default:
                return true;
        }
    }

    @Override
    public double calculateXpModifier(@NotNull TriggerContext context) {
        TriggerProfile profile = context.getTriggerProfile();
        Block block = context.getSourceAs(Block.class);

        if (block != null) {
            return profile.calculateXpModifier(block.getType().name());
        }

        return profile.calculateXpModifier("AIR");
    }

}
