package dev.s3tupw1zard.awkgearmastery.listeners.anvil;

import dev.s3tupw1zard.awkgearmastery.AwkGearMasteryPlugin;
import dev.s3tupw1zard.awkgearmastery.api.item.AwkGearMasteryItem;
import dev.s3tupw1zard.awkgearmastery.profile.item.ItemProfile;
import dev.s3tupw1zard.awkgearmastery.util.AwkGearMasteryUtil;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class AnvilHelper {
    private AnvilHelper() {}

    static ItemStack getResultItem(AwkGearMasteryItem item, HumanEntity viewer, ItemStack result) {
        final Player player = viewer instanceof Player ? (Player) viewer : null;
        final ItemProfile profile = AwkGearMasteryUtil.getItemProfile(result.getType());

        return AwkGearMasteryUtil.getItemStack(item, player, profile);
    }

    static boolean shouldBlockEnchantedBookResult(ItemStack firstItem, ItemStack secondItem, ItemStack result) {
        return AwkGearMasteryPlugin.getInstance().getConfigManager().getSettings().isPreventEnchantedBooksOnAwkGearMasteryItems()
            && result != null
            && firstItem != null
            && secondItem != null
            && AwkGearMasteryUtil.isSupportedTool(result.getType())
            && AwkGearMasteryUtil.isSupportedTool(firstItem.getType())
            && secondItem.getType() == Material.ENCHANTED_BOOK;
    }
}
