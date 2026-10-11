package dev.s3tupw1zard.awkgearmastery.api.scheduler.impl.folia;


import dev.s3tupw1zard.awkgearmastery.api.scheduler.ScheduledTask;

public class FoliaScheduledTask implements ScheduledTask {
    private final io.papermc.paper.threadedregions.scheduler.ScheduledTask task;

    public FoliaScheduledTask(io.papermc.paper.threadedregions.scheduler.ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void stop() {
        task.cancel();
    }
}
