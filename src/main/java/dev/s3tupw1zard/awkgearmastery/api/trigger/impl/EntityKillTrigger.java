package dev.s3tupw1zard.awkgearmastery.api.trigger.impl;

import dev.s3tupw1zard.awkgearmastery.api.trigger.Trigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerContext;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerIds;
import dev.s3tupw1zard.awkgearmastery.profile.trigger.TriggerProfile;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

public final class EntityKillTrigger implements Trigger {
    @Override
    @NotNull
    public String getTriggerId() {
        return TriggerIds.ENTITY_KILL;
    }

    @Override
    public boolean canHandle(@NotNull TriggerContext context) {
        Entity entity = context.getSourceAs(Entity.class);
        if (entity == null) {
            return false;
        }

        TriggerProfile profile = context.getTriggerProfile();
        String entityType = entity.getType().name();

        return profile.isSourceAllowed(entityType);
    }

    @Override
    public double calculateXpModifier(@NotNull TriggerContext context) {
        Entity entity = context.getSourceAs(Entity.class);
        if (entity == null) {
            return 0;
        }

        TriggerProfile profile = context.getTriggerProfile();
        return profile.calculateXpModifier(entity.getType().name());
    }

}
