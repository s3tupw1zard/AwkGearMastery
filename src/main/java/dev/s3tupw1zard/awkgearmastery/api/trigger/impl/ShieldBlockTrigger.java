package dev.s3tupw1zard.awkgearmastery.api.trigger.impl;

import dev.s3tupw1zard.awkgearmastery.api.trigger.Trigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerContext;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerIds;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.jetbrains.annotations.NotNull;

/** The listener only dispatches this trigger for confirmed blocked attacks. */
public final class ShieldBlockTrigger implements Trigger {
    @Override
    public @NotNull String getTriggerId() {
        return TriggerIds.SHIELD_BLOCK;
    }

    @Override
    public boolean canHandle(@NotNull TriggerContext context) {
        return context.getOriginalEventAs(EntityDamageByEntityEvent.class) != null
                && context.getItem().getType() == Material.SHIELD
                && context.getTriggerProfile().isSourceAllowed("SHIELD");
    }

    @Override
    public double calculateXpModifier(@NotNull TriggerContext context) {
        return context.getTriggerProfile().calculateXpModifier("SHIELD");
    }
}
