package dev.s3tupw1zard.awkgearmastery;

import dev.s3tupw1zard.awkgearmastery.api.item.AwkGearMasteryItem;
import dev.s3tupw1zard.awkgearmastery.profile.ProfileManager;
import dev.s3tupw1zard.awkgearmastery.profile.display.DisplayProfile;
import dev.s3tupw1zard.awkgearmastery.profile.item.ItemProfile;
import dev.s3tupw1zard.awkgearmastery.util.AwkGearMasteryUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class AwkGearMasteryPlaceholders extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "awkgearmastery";
    }

    @Override
    public @NotNull String getAuthor() {
        return "byteful, s3tupw1zard";
    }

    @Override
    public @NotNull String getVersion() {
        return AwkGearMasteryPlugin.getInstance().getPluginMeta().getVersion();
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return null;
        }

        final ItemStack hand = AwkGearMasteryUtil.getHand(player);

        if (!AwkGearMasteryUtil.isSupportedTool(hand.getType())) {
            return "N/A";
        }

        switch (params.toLowerCase(Locale.ROOT).replace(" ", "_")) {
            case "level": {
                return "" + AwkGearMasteryUtil.createAwkGearMasteryItem(hand).getLevel();
            }

            case "xp": {
                return "" + AwkGearMasteryUtil.createAwkGearMasteryItem(hand).getXp();
            }

            case "max_xp": {
                AwkGearMasteryItem item = AwkGearMasteryUtil.createAwkGearMasteryItem(hand);
                ItemProfile itemProfile = AwkGearMasteryUtil.getItemProfile(hand.getType());
                return "" + AwkGearMasteryUtil.getMaxXp(player, itemProfile, item);
            }

            case "progress_bar": {
                AwkGearMasteryItem item = AwkGearMasteryUtil.createAwkGearMasteryItem(hand);
                ItemProfile itemProfile = AwkGearMasteryUtil.getItemProfile(hand.getType());
                if (item.getLevel() >= AwkGearMasteryUtil.getMaxLevel(itemProfile)) {
                    return "";
                }
                double maxXp = AwkGearMasteryUtil.getMaxXp(player, itemProfile, item);
                return AwkGearMasteryUtil.createProgressBar(
                        item.getXp(),
                        maxXp,
                        getDisplayProfile(itemProfile)
                );
            }

            case "progress": {
                AwkGearMasteryItem item = AwkGearMasteryUtil.createAwkGearMasteryItem(hand);
                ItemProfile itemProfile = AwkGearMasteryUtil.getItemProfile(hand.getType());
                double maxXp = AwkGearMasteryUtil.getMaxXp(player, itemProfile, item);
                if (maxXp <= 0.0) {
                    return "0";
                }
                final double displayXp =
                        item.getLevel() >= AwkGearMasteryUtil.getMaxLevel(itemProfile) ? maxXp : item.getXp();
                return "" + AwkGearMasteryUtil.round((displayXp / maxXp) * 100.0, 1);
            }

            case "item_profile": {
                ItemProfile itemProfile = AwkGearMasteryUtil.getItemProfile(hand.getType());
                return itemProfile != null ? itemProfile.getId() : "N/A";
            }

            case "max_level": {
                ItemProfile itemProfile = AwkGearMasteryUtil.getItemProfile(hand.getType());
                return itemProfile != null ? String.valueOf(AwkGearMasteryUtil.getMaxLevel(itemProfile)) : "N/A";
            }

            default: {
                return "N/A";
            }
        }
    }

    @Nullable
    private DisplayProfile getDisplayProfile(@Nullable ItemProfile itemProfile) {
        if (itemProfile == null) {
            return null;
        }
        ProfileManager profileManager = AwkGearMasteryPlugin.getInstance().getProfileManager();
        if (profileManager == null) {
            return null;
        }
        return profileManager.getDisplayProfileFor(itemProfile);
    }
}
