package dev.s3tupw1zard.awkgearmastery.api.event;

import dev.s3tupw1zard.awkgearmastery.AwkGearMasteryPlugin;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerRegistry;
import dev.s3tupw1zard.awkgearmastery.profile.ProfileManager;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class AwkGearMasteryLoadEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final AwkGearMasteryPlugin plugin;
    private final LoadPhase phase;

    public AwkGearMasteryLoadEvent(@NotNull AwkGearMasteryPlugin plugin, @NotNull LoadPhase phase) {
        this.plugin = plugin;
        this.phase = phase;
    }

    @NotNull
    public AwkGearMasteryPlugin getPlugin() {
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
