package dev.s3tupw1zard.awkgearmastery.api.event;

import dev.s3tupw1zard.awkgearmastery.LevelToolsPlugin;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerRegistry;
import dev.s3tupw1zard.awkgearmastery.profile.ProfileManager;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class LevelToolsLoadEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final LevelToolsPlugin plugin;
    private final LoadPhase phase;

    public LevelToolsLoadEvent(@NotNull LevelToolsPlugin plugin, @NotNull LoadPhase phase) {
        this.plugin = plugin;
        this.phase = phase;
    }

    @NotNull
    public LevelToolsPlugin getPlugin() {
        return plugin;
    }

    @NotNull
    public LoadPhase getPhase() {
        return phase;
    }

    @NotNull
    public TriggerRegistry getTriggerRegistry() {
        return plugin.getTriggerRegistry();
    }

    @NotNull
    public ProfileManager getProfileManager() {
        return plugin.getProfileManager();
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    public enum LoadPhase {
        PRE_LOAD,
        POST_TRIGGERS,
        POST_PROFILES,
        COMPLETE
    }
}
