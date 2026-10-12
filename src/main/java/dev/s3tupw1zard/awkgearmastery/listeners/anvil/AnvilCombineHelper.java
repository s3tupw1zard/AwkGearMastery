package dev.s3tupw1zard.awkgearmastery.listeners.anvil;

import dev.s3tupw1zard.awkgearmastery.AwkGearMasteryPlugin;
import dev.s3tupw1zard.awkgearmastery.api.AnvilCombineMode;
import dev.s3tupw1zard.awkgearmastery.api.item.AwkGearMasteryItem;
import dev.s3tupw1zard.awkgearmastery.model.LevelAndXPModel;
import dev.s3tupw1zard.awkgearmastery.util.AwkGearMasteryUtil;
import com.google.common.collect.Multimap;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static dev.s3tupw1zard.awkgearmastery.listeners.anvil.AnvilHelper.getResultItem;

import java.util.Collection;
import java.util.Map;

final class AnvilCombineHelper {
    private static ItemStack preserveBaseProgression(ItemStack baseItem, ItemStack vanillaResult) {
        final ItemStack protectedResult = vanillaResult.clone();

        for (Map.Entry<Enchantment, Integer> entry : baseItem.getEnchantments().entrySet()) {
            if (protectedResult.getEnchantmentLevel(entry.getKey()) < entry.getValue()) {
                protectedResult.addUnsafeEnchantment(entry.getKey(), entry.getValue());
            }
        }

        final ItemMeta sourceMeta = baseItem.getItemMeta();
        final ItemMeta targetMeta = protectedResult.getItemMeta();
        if (sourceMeta == null || targetMeta == null) {
            return protectedResult;
        }

        final Multimap<Attribute, AttributeModifier> sourceModifiers = sourceMeta.getAttributeModifiers();
        if (sourceModifiers == null || sourceModifiers.isEmpty()) {
            return protectedResult;
        }

        for (Map.Entry<Attribute, AttributeModifier> entry : sourceModifiers.entries()) {
            final AttributeModifier sourceModifier = entry.getValue();
            if (!isAwkGearMasteryAttributeModifier(sourceModifier)) {
                continue;
            }

            final Collection<AttributeModifier> targetModifiers =
                    targetMeta.getAttributeModifiers(entry.getKey());
            if (targetModifiers != null) {
                for (AttributeModifier targetModifier : targetModifiers) {
                    if (sourceModifier.getKey().equals(targetModifier.getKey())) {
                        targetMeta.removeAttributeModifier(entry.getKey(), targetModifier);
                    }
                }
            }

            targetMeta.addAttributeModifier(entry.getKey(), sourceModifier);
        }

        protectedResult.setItemMeta(targetMeta);
        return protectedResult;
    }

    private static boolean isAwkGearMasteryAttributeModifier(AttributeModifier modifier) {
        final NamespacedKey key = modifier.getKey();
        return key.getNamespace().equals("awkgearmastery") && key.getKey().startsWith("reward/");
    }

    static boolean isHandledAnvilAction(ItemStack firstItem, ItemStack secondItem, ItemStack result) {
        return result != null
                && AwkGearMasteryUtil.isSupportedTool(result.getType())
                && firstItem != null
                && secondItem != null
                && AwkGearMasteryUtil.isSupportedTool(firstItem.getType());
    }

    static ItemStack createUpdatedResult(
            ItemStack firstItem, ItemStack secondItem, ItemStack result, HumanEntity viewer) {
        final ItemStack protectedResult = preserveBaseProgression(firstItem, result);
        final AwkGearMasteryItem finalItem = AwkGearMasteryUtil.createAwkGearMasteryItem(protectedResult);

        if (AwkGearMasteryUtil.isSupportedTool(secondItem.getType())) {
            final AnvilCombineMode mode = AwkGearMasteryPlugin.getInstance().getAnvilCombineMode();
            final LevelAndXPModel first =
                    LevelAndXPModel.fromItem(AwkGearMasteryUtil.createAwkGearMasteryItem(firstItem));
            final LevelAndXPModel second =
                    LevelAndXPModel.fromItem(AwkGearMasteryUtil.createAwkGearMasteryItem(secondItem));
            final LevelAndXPModel finished = mode.getHandler().apply(first, second);
            finalItem.setLevel(finished.getLevel());
            finalItem.setXp(finished.getXp());
        } else {
            final AwkGearMasteryItem original = AwkGearMasteryUtil.createAwkGearMasteryItem(firstItem);
            finalItem.setLevel(original.getLevel());
            finalItem.setXp(original.getXp());
            finalItem.setLastHandledReward(original.getLastHandledReward());
        }

        return getResultItem(finalItem, viewer, result);
    }
}
