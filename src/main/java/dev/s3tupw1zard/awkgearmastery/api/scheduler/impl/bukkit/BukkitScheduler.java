package dev.s3tupw1zard.awkgearmastery.api.scheduler.impl.bukkit;

import dev.s3tupw1zard.awkgearmastery.LevelToolsPlugin;
import dev.s3tupw1zard.awkgearmastery.api.scheduler.ScheduledTask;
import dev.s3tupw1zard.awkgearmastery.api.scheduler.Scheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public class BukkitScheduler implements Scheduler {
    private final LevelToolsPlugin plugin;

    public BukkitScheduler(LevelToolsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void asyncDelayed(Runnable runnable, long ticksDelay) {
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, runnable, ticksDelay);
    }

    @Override
    public void syncTimer(Runnable runnable, long ticksDelay, long ticksPeriod) {
        Bukkit.getScheduler().runTaskTimer(plugin, runnable, ticksDelay, ticksPeriod);
    }

    @Override
    public void locationDelayed(Runnable runnable, Location location, long ticksDelay) {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, ticksDelay);
    }

    @Override
    public void entityDelayed(Runnable runnable, Entity entity, long ticksDelay) {
        Bukkit.getScheduler().runTaskLater(plugin, runnable, Math.max(1L, ticksDelay));
    }

    @Override
    public ScheduledTask asyncTimer(Runnable runnable, long ticksDelay, long ticksPeriod) {
        return new BukkitScheduledTask(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, ticksDelay, ticksPeriod));
    }
}
