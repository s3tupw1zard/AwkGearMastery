package dev.s3tupw1zard.awkgearmastery.listeners;

import dev.s3tupw1zard.awkgearmastery.AwkGearMasteryPlugin;
import dev.s3tupw1zard.awkgearmastery.api.event.AwkGearMasteryLevelIncreaseEvent;
import dev.s3tupw1zard.awkgearmastery.api.event.AwkGearMasteryXPIncreaseEvent;
import dev.s3tupw1zard.awkgearmastery.api.item.AwkGearMasteryItem;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerSlot;
import dev.s3tupw1zard.awkgearmastery.config.ConfigManager;
import dev.s3tupw1zard.awkgearmastery.profile.ProfileManager;
import dev.s3tupw1zard.awkgearmastery.profile.display.DisplayProfile;
import dev.s3tupw1zard.awkgearmastery.profile.item.ItemProfile;
import dev.s3tupw1zard.awkgearmastery.profile.reward.RewardProfile;
import dev.s3tupw1zard.awkgearmastery.util.AwkGearMasteryUtil;
import dev.s3tupw1zard.awkgearmastery.util.Text;
import dev.s3tupw1zard.awkgearmastery.util.XPBooster;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

import static dev.s3tupw1zard.awkgearmastery.util.Text.formatMoney;

public final class XPHandler {
    private final ProfileManager profileManager;

    public XPHandler(@NotNull ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    public void handle(
            @NotNull Player player,
            @NotNull ItemProfile itemProfile,
            @Nullable TriggerSlot slot,
            @NotNull AwkGearMasteryItem tool,
            double modifier,
            boolean deferCommit
    ) {
        modifier = Math.max(0, XPBooster.apply(player, modifier));
        double newXp = AwkGearMasteryUtil.round(tool.getXp() + modifier, 1);

        AwkGearMasteryXPIncreaseEvent xpEvent = new AwkGearMasteryXPIncreaseEvent(tool, player, newXp, newXp, false);
        Bukkit.getPluginManager().callEvent(xpEvent);

        if (xpEvent.isCancelled()) {
            return;
        }

        tool.setXp(xpEvent.getNewXp());

        double maxXp = advanceLevels(tool, player, itemProfile);
        handleReward(tool, player, itemProfile);

        if (deferCommit) {
            commitDeferred(player, slot, tool, maxXp);
        } else {
            updateItem(player, slot, tool.getItemStack(maxXp));
        }

        showActionBar(tool, player, itemProfile, maxXp, slot);
    }

    public void handleDirectCommit(
            @NotNull Player player,
            @NotNull ItemProfile itemProfile,
            @NotNull AwkGearMasteryItem tool,
            double modifier,
            @NotNull Consumer<ItemStack> committer
    ) {
        modifier = Math.max(0, XPBooster.apply(player, modifier));
        double newXp = AwkGearMasteryUtil.round(tool.getXp() + modifier, 1);

        AwkGearMasteryXPIncreaseEvent xpEvent =
                new AwkGearMasteryXPIncreaseEvent(tool, player, newXp, newXp, false);
        Bukkit.getPluginManager().callEvent(xpEvent);

        if (xpEvent.isCancelled()) {
            return;
        }

        tool.setXp(xpEvent.getNewXp());

        double maxXp = advanceLevels(tool, player, itemProfile);
        handleReward(tool, player, itemProfile);
        committer.accept(tool.getItemStack(maxXp));
        showActionBar(tool, player, itemProfile, maxXp, null);
    }

    private void commitDeferred(
            @NotNull Player player,
            @Nullable TriggerSlot slot,
            @NotNull AwkGearMasteryItem tool,
            double maxXp
    ) {
        ItemStack backing = AwkGearMasteryUtil.getBackingItemStack(tool);
        if (backing == null) {
            updateItem(player, slot, tool.getItemStack(maxXp));
            return;
        }

        Material expectedType = backing.getType();
        AwkGearMasteryPlugin.getInstance().getScheduler().entityDelayed(() -> {
            if (!player.isOnline()) {
                return;
            }

            ItemStack live = AwkGearMasteryUtil.getItemInSlot(player, slot);
            if (live == null || live.getType() != expectedType
                    || !TriggerListener.sameItemIgnoringWear(backing, live)) {
                return;
            }

            if (AwkGearMasteryUtil.rebindAwkGearMasteryItem(tool, live)) {
                updateItem(player, slot, tool.getItemStack(maxXp));
            }
        }, player, 1);
    }

    private double advanceLevels(
            @NotNull AwkGearMasteryItem tool,
            @NotNull Player player,
            @NotNull ItemProfile itemProfile
    ) {
        final int maxLevel = AwkGearMasteryUtil.getMaxLevel(itemProfile);
        double maxXp = getMaxXp(player, itemProfile, tool);

        while (tool.getLevel() < maxLevel && tool.getXp() >= maxXp) {
            if (!handleLevelUp(tool, player, itemProfile, maxXp)) {
                break;
            }
            maxXp = getMaxXp(player, itemProfile, tool);
        }

        if (tool.getLevel() >= maxLevel) {
            tool.setLevel(maxLevel);
            tool.setXp(0.0);
            maxXp = getMaxXp(player, itemProfile, tool);
        }

        return maxXp;
    }

    private boolean handleLevelUp(
            @NotNull AwkGearMasteryItem tool,
            @NotNull Player player,
            @NotNull ItemProfile itemProfile,
            double maxXp
    ) {
        int newLevel = tool.getLevel() + 1;
        int maxLevel = AwkGearMasteryUtil.getMaxLevel(itemProfile);

        if (newLevel > maxLevel) {
            tool.setXp(maxXp);
            return false;
        }

        AwkGearMasteryLevelIncreaseEvent levelEvent = new AwkGearMasteryLevelIncreaseEvent(tool, player, newLevel, false);
        Bukkit.getPluginManager().callEvent(levelEvent);

        if (levelEvent.isCancelled()) {
            return false;
        }

        tool.setXp(AwkGearMasteryUtil.round(Math.abs(tool.getXp() - maxXp), 1));
        tool.setLevel(levelEvent.getNewLevel());

        if (levelEvent.getNewLevel() == maxLevel) {
            tool.setXp(0);
            tool.setLevel(maxLevel);
        }

        playLevelUpSound(player);
        return true;
    }

    private void playLevelUpSound(@NotNull Player player) {
        ConfigManager.LevelUpSound sound = AwkGearMasteryPlugin.getInstance().getConfigManager().getSettings().getLevelUpSound();
        if (sound != null) {
            sound.play(player);
        }
    }

    private void showActionBar(
            @NotNull AwkGearMasteryItem tool,
            @NotNull Player player,
            @NotNull ItemProfile itemProfile,
            double maxXp,
            @Nullable TriggerSlot slot
    ) {
        DisplayProfile displayProfile = profileManager.getDisplayProfileFor(itemProfile);
        if (displayProfile == null) {
            return;
        }

        DisplayProfile.ActionBarDisplay actionBar = displayProfile.getActionBarDisplay();
        if (!actionBar.isEnabled()) {
            return;
        }

        final int maxLevel = AwkGearMasteryUtil.getMaxLevel(itemProfile);
        if (tool.getLevel() >= maxLevel) {
            return;
        }

        final double displayXp = tool.getXp();
        String progressBar = displayProfile.getProgressBar().buildProgressBar(displayXp, maxXp);
        String text = Text.colorize(actionBar.getText()
                .replace("{progress_bar}", progressBar)
                .replace("{xp}", String.valueOf(displayXp))
                .replace("{max_xp}", String.valueOf(maxXp))
                .replace("{level}", String.valueOf(tool.getLevel()))
                .replace("{slot}", AwkGearMasteryUtil.getReadableSlotName(slot))
                .replace("{item}", AwkGearMasteryUtil.getReadableItemName(tool.getItemStack()))
                .replace("{max_xp_formatted}", formatMoney(maxXp))
                .replace("{xp_formatted}", formatMoney(displayXp)));

        AwkGearMasteryUtil.sendActionBar(player, text);
    }

    private void handleReward(
            @NotNull AwkGearMasteryItem tool,
            @NotNull Player player,
            @NotNull ItemProfile itemProfile
    ) {
        RewardProfile rewardProfile = profileManager.getRewardProfileFor(itemProfile);
        if (rewardProfile == null) {
            return;
        }

        AwkGearMasteryUtil.applyRewards(tool, player, rewardProfile);
    }

    private double getMaxXp(@NotNull Player player, @NotNull ItemProfile itemProfile, @NotNull AwkGearMasteryItem tool) {
        return AwkGearMasteryUtil.getMaxXp(player, itemProfile, tool);
    }

    private void updateItem(@NotNull Player player, @Nullable TriggerSlot slot, @NotNull ItemStack itemStack) {
        AwkGearMasteryUtil.setItemInSlot(player, slot, itemStack);
    }
}
