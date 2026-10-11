package dev.s3tupw1zard.awkgearmastery.api.trigger.impl;

import dev.s3tupw1zard.awkgearmastery.api.trigger.Trigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerContext;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerIds;
import dev.s3tupw1zard.awkgearmastery.profile.trigger.TriggerProfile;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ArmorDurabilityTrigger implements Trigger {
    @Override
    @NotNull
    public String getTriggerId() {
        return TriggerIds.ARMOR_DURABILITY;
    }

    @Override
    public boolean canHandle(@NotNull TriggerContext context) {
        Material armorMaterial = resolveArmorMaterial(context);
        if (armorMaterial == null) {
            return false;
        }

        TriggerProfile profile = context.getTriggerProfile();
        return profile.isSourceAllowed(armorMaterial.name());
    }

    @Override
    public double calculateXpModifier(@NotNull TriggerContext context) {
        Material armorMaterial = resolveArmorMaterial(context);
        if (armorMaterial == null) {
            return 0;
        }

        TriggerProfile profile = context.getTriggerProfile();
        return profile.calculateXpModifier(armorMaterial.name());
    }

    @Nullable
    private Material resolveArmorMaterial(@NotNull TriggerContext context) {
        Material sourceMaterial = context.getSourceAs(Material.class);
        if (sourceMaterial != null) {
            return sourceMaterial;
        }

        ItemStack sourceItem = context.getSourceAs(ItemStack.class);
        if (sourceItem != null) {
            return sourceItem.getType();
        }

        ItemStack contextItem = context.getItem();
        return contextItem.getType();
    }
}