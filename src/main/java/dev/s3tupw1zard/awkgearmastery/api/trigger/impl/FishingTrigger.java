package dev.s3tupw1zard.awkgearmastery.api.trigger.impl;

import dev.s3tupw1zard.awkgearmastery.api.trigger.Trigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerContext;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerIds;
import dev.s3tupw1zard.awkgearmastery.profile.trigger.TriggerProfile;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class FishingTrigger implements Trigger {
    @Override
    @NotNull
    public String getTriggerId() {
        return TriggerIds.FISHING;
    }

    @Override
    public boolean canHandle(@NotNull TriggerContext context) {
        Entity caught = context.getSourceAs(Entity.class);
        if (!(caught instanceof Item)) {
            return false;
        }

        ItemStack itemStack = ((Item) caught).getItemStack();
        Material material = itemStack.getType();

        TriggerProfile profile = context.getTriggerProfile();
        return profile.isSourceAllowed(material.name());
    }

    @Override
    public double calculateXpModifier(@NotNull TriggerContext context) {
        Entity caught = context.getSourceAs(Entity.class);
        if (!(caught instanceof Item)) {
            return 0;
        }

        ItemStack itemStack = ((Item) caught).getItemStack();
        Material material = itemStack.getType();

        TriggerProfile profile = context.getTriggerProfile();
        return profile.calculateXpModifier(material.name());
    }
}
