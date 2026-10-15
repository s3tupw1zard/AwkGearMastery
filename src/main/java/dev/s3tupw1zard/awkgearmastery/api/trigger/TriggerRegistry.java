package dev.s3tupw1zard.awkgearmastery.api.trigger;

import dev.s3tupw1zard.awkgearmastery.api.trigger.impl.BlockBreakTrigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.impl.ShieldBlockTrigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.impl.ConsumeTrigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.impl.EntityKillTrigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.impl.FarmingTrigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.impl.FishingTrigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.impl.ArmorDurabilityTrigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.impl.LeftClickTrigger;
import dev.s3tupw1zard.awkgearmastery.api.trigger.impl.RightClickTrigger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class TriggerRegistry {
    private final Map<String, Trigger> triggers = new ConcurrentHashMap<>();

    public void register(@NotNull Trigger trigger) {
        String id = TriggerIds.normalize(trigger.getTriggerId());
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Trigger ID cannot be null or empty");
        }
        triggers.put(id, trigger);
    }

    public boolean unregister(@NotNull String triggerId) {
        return triggers.remove(TriggerIds.normalize(triggerId)) != null;
    }

    @Nullable
    public Trigger get(@NotNull String triggerId) {
        return triggers.get(TriggerIds.normalize(triggerId));
    }

    @NotNull
    public Optional<Trigger> getOptional(@NotNull String triggerId) {
        return Optional.ofNullable(get(triggerId));
    }

    public boolean has(@NotNull String triggerId) {
        return triggers.containsKey(TriggerIds.normalize(triggerId));
    }

    @NotNull
    public Set<String> getRegisteredIds() {
        return Collections.unmodifiableSet(triggers.keySet());
    }

    @NotNull
    public Collection<Trigger> getAll() {
        return Collections.unmodifiableCollection(triggers.values());
    }

    public void clear() {
        triggers.clear();
    }

    public void registerDefaults() {
        register(new BlockBreakTrigger());
        register(new EntityKillTrigger());
        register(new FishingTrigger());
        register(new RightClickTrigger());
        register(new LeftClickTrigger());
        register(new ConsumeTrigger());
        register(new FarmingTrigger());
        register(new ArmorDurabilityTrigger());
        register(new ShieldBlockTrigger());
    }
}
