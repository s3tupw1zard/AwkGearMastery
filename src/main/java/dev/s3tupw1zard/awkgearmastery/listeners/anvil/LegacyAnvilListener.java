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
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static dev.s3tupw1zard.awkgearmastery.listeners.anvil.AnvilHelper.getResultItem;
import static dev.s3tupw1zard.awkgearmastery.listeners.anvil.AnvilHelper.shouldBlockEnchantedBookResult;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class LegacyAnvilListener implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAnvilClick(InventoryClickEvent e) {
        if (!(e.getInventory() instanceof AnvilInventory)) return;

        final int rawSlot = e.getRawSlot();
        if (rawSlot != 0 && rawSlot != 1 && rawSlot != 2) return;

        final AnvilInventory inv = (AnvilInventory) e.getInventory();
        final ItemStack firstItem = inv.getItem(0);
        final ItemStack secondItem = inv.getItem(1);
        final ItemStack result = inv.getItem(2);

        if (shouldBlockEnchantedBookResult(firstItem, secondItem, result)) {
            if (rawSlot == 2) {
                e.setCancelled(true);
            }
            inv.setItem(2, null);
            return;
        }

        if (!isHandledAnvilAction(firstItem, secondItem, result)) {
            return;
        }

        inv.setItem(2, createUpdatedResult(firstItem, secondItem, result, e.getWhoClicked()));
    }

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

        // Modern reward modifiers are keyed in this plugin's namespace.
        if (key.getNamespace().equals(AwkGearMasteryPlugin.getInstance().getName().toLowerCase(Locale.ROOT))
                && key.getKey().startsWith("reward/")) {
            return true;
        }

        // Older builds used a name-derived UUID; Bukkit exposes it as a namespaced key.
        final UUID legacyId = UUID.nameUUIDFromBytes(
                modifier.getName().getBytes(StandardCharsets.UTF_8));
        return NamespacedKey.BUKKIT.equals(key.getNamespace())
                && legacyId.toString().equals(key.getKey());
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
