package dev.s3tupw1zard.awkgearmastery.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class DefaultProfilesTest {
    @Test
    void axesSupportCombatAndShieldsHaveTheirOwnDefensiveSource() {
        final var items = resource("item_profiles.yml");
        final var triggers = resource("trigger_profiles.yml");
        final var sources = resource("xp_sources.yml");
        assertTrue(items.getStringList("profiles.axes.trigger_profiles").contains("combat"));
        assertTrue(items.getStringList("profiles.axes.trigger_profiles").contains("block_mining"));
        assertEquals("melee_weapon", items.getString("profiles.axes.stat_profile"));
        assertEquals("weapon", items.getString("profiles.axes.display_profile"));
        assertEquals(java.util.List.of("shield_block"), items.getStringList("profiles.shields.trigger_profiles"));
        assertEquals("ARMOR_DURABILITY", triggers.getString("profiles.shield_block.type"));
        assertEquals(java.util.List.of("HAND", "OFF_HAND"), triggers.getStringList("profiles.shield_block.slots"));
        assertTrue(sources.getDouble("sources.shield_block.default.min") > 0);
        assertTrue(sources.getDouble("sources.block_mining.default.max") <= 0.4);
    }

    @Test
    void progressionLoaderUsesTheBundledRebalancedDefaults() {
        final var profiles = new dev.s3tupw1zard.awkgearmastery.profile.progression.ProgressionProfileLoader(
                java.util.logging.Logger.getAnonymousLogger()).load(resource("progression_profiles.yml"));
        final var profile = Objects.requireNonNull(profiles.get("default"));
        assertEquals(100, profile.getMaxLevel());
        assertEquals(25.0, profile.xpRequiredForLevel(1), 0.01);
        assertTrue(profile.xpRequiredForLevel(5) < 30.0);
        assertEquals(50000.0, profile.xpRequiredForLevel(99), 0.01);
    }

    private static YamlConfiguration resource(String name) {
        return YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(DefaultProfilesTest.class.getClassLoader().getResourceAsStream(name)),
                StandardCharsets.UTF_8));
    }
}
