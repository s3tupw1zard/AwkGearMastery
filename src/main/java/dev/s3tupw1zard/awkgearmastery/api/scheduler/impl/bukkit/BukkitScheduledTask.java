package dev.s3tupw1zard.awkgearmastery.api.scheduler.impl.bukkit;

import dev.s3tupw1zard.awkgearmastery.api.scheduler.ScheduledTask;
import org.bukkit.scheduler.BukkitTask;

public class BukkitScheduledTask implements ScheduledTask {
    private final BukkitTask task;

    public BukkitScheduledTask(BukkitTask task) {
        this.task = task;
    }

    @Override
    public void stop() {
        task.cancel();
    }
}
