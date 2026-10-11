package dev.s3tupw1zard.awkgearmastery.api.trigger.impl;

import dev.s3tupw1zard.awkgearmastery.api.trigger.Trigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerContext;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerIds;
import dev.s3tupw1zard.awkgearmastery.profile.trigger.TriggerProfile;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ConsumeTrigger implements Trigger {
    @Override
    @NotNull
    public String getTriggerId() {
        return TriggerIds.CONSUME;
    }

    @Override
    public boolean canHandle(@NotNull TriggerContext context) {
        ItemStack consumedItem = context.getSourceAs(ItemStack.class);
        if (consumedItem == null) {
            return false;
        }

        TriggerProfile profile = context.getTriggerProfile();
        String itemType = consumedItem.getType().name();

        return profile.isSourceAllowed(itemType);
    }

    @Override
    public double calculateXpModifier(@NotNull TriggerContext context) {
        ItemStack consumedItem = context.getSourceAs(ItemStack.class);
        if (consumedItem == null) {
            return 0;
        }

        TriggerProfile profile = context.getTriggerProfile();
        return profile.calculateXpModifier(consumedItem.getType().name());
    }

}
