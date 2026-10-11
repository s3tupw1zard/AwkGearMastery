package dev.s3tupw1zard.awkgearmastery.listeners;

import dev.s3tupw1zard.awkgearmastery.LevelToolsPlugin;
import dev.s3tupw1zard.awkgearmastery.api.item.LevelToolsItem;
import dev.s3tupw1zard.awkgearmastery.profile.ProfileManager;
import dev.s3tupw1zard.awkgearmastery.profile.item.ItemProfile;
import dev.s3tupw1zard.awkgearmastery.profile.stat.CalculatedStats;
import dev.s3tupw1zard.awkgearmastery.util.LevelToolsUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class DefenseListener implements Listener {
    private final ProfileManager profileManager;

    public DefenseListener(@NotNull ProfileManager profileManager) {
        this.profileManager = profileManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(@NotNull EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (LevelToolsPlugin.getInstance()
                .getConfigManager()
                .getSettings()
                .getDisabledWorlds()
                .contains(player.getWorld().getName())) {
            return;
        }

        double remainingDamage = 1.0;
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            if (armor == null || armor.getType() == Material.AIR) {
                continue;
            }

            final ItemProfile profile = profileManager.getProfileForMaterial(armor.getType());
            if (profile == null) {
                continue;
            }

            final LevelToolsItem item = LevelToolsUtil.createLevelToolsItem(armor);
            final CalculatedStats stats =
                    LevelToolsPlugin.getInstance()
                            .getStatCalculator()
                            .calculate(armor, profile, item.getLevel());
            remainingDamage *= 1.0 - stats.defense();
        }

        event.setDamage(Math.max(0.0, event.getDamage() * remainingDamage));
    }
}
