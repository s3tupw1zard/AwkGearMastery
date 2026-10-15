package dev.s3tupw1zard.awkgearmastery;

import dev.s3tupw1zard.awkgearmastery.api.AnvilCombineMode;
import dev.s3tupw1zard.awkgearmastery.api.block.BlockDataManager;
import dev.s3tupw1zard.awkgearmastery.api.block.BlockDataManagerFactory;
import dev.s3tupw1zard.awkgearmastery.api.event.AwkGearMasteryLoadEvent;
import dev.s3tupw1zard.awkgearmastery.api.scheduler.Scheduler;
import dev.s3tupw1zard.awkgearmastery.api.trigger.TriggerRegistry;
import dev.s3tupw1zard.awkgearmastery.config.CombatXpConfig;
import dev.s3tupw1zard.awkgearmastery.config.ConfigManager;
import dev.s3tupw1zard.awkgearmastery.config.EnchantmentModifierRegistry;
import dev.s3tupw1zard.awkgearmastery.config.XpFormulaRegistry;
import dev.s3tupw1zard.awkgearmastery.config.XpSourceRegistry;
import dev.s3tupw1zard.awkgearmastery.listeners.anvil.AnvilListener;
import dev.s3tupw1zard.awkgearmastery.listeners.BlockPlacementListener;
import dev.s3tupw1zard.awkgearmastery.listeners.CombatProgressionListener;
import dev.s3tupw1zard.awkgearmastery.listeners.DefenseListener;
import dev.s3tupw1zard.awkgearmastery.listeners.TriggerListener;
import dev.s3tupw1zard.awkgearmastery.profile.ProfileManager;
import dev.s3tupw1zard.awkgearmastery.profile.stat.StatCalculator;
import dev.s3tupw1zard.awkgearmastery.util.AwkGearMasteryUtil;
import dev.s3tupw1zard.awkgearmastery.util.UpdateChecker;
import dev.s3tupw1zard.awkgearmastery.util.XPBooster;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import redempt.crunch.CompiledExpression;
import revxrsal.commands.Lamp;
import revxrsal.commands.bukkit.BukkitLamp;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static dev.s3tupw1zard.awkgearmastery.util.Text.colorize;

public final class AwkGearMasteryPlugin extends JavaPlugin {
    private static AwkGearMasteryPlugin instance;

    private Lamp<BukkitCommandActor> commandManager;
    private AnvilCombineMode anvilCombineMode;
    private UpdateChecker updateChecker;
    private volatile XpFormulaRegistry xpFormulaRegistry;
    private BlockDataManager blockDataManager;
    private String blockDataStorageType;
    private Scheduler scheduler;

    private ConfigManager configManager;
    private ProfileManager profileManager;
    private TriggerRegistry triggerRegistry;
    private EnchantmentModifierRegistry enchantmentModifierRegistry;
    private StatCalculator statCalculator;
    private CombatXpConfig combatXpConfig;
    private XpSourceRegistry xpSourceRegistry;

    public static AwkGearMasteryPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        sendStartupBanner();
        instance = this;

        scheduler = AwkGearMasteryUtil.createScheduler(this);
        updateChecker = new UpdateChecker(this, scheduler);

        configManager = new ConfigManager(this);
        configManager.loadAll();
        reloadConfig();

        setAnvilCombineMode();
        setXpFormulaRegistry();
        getLogger().info("Loaded configuration...");

        profileManager = new ProfileManager(getLogger());
        triggerRegistry = new TriggerRegistry();

        Bukkit.getPluginManager().callEvent(new AwkGearMasteryLoadEvent(this, AwkGearMasteryLoadEvent.LoadPhase.PRE_LOAD));

        triggerRegistry.registerDefaults();
        getLogger().info("Registered triggers...");

        Bukkit.getPluginManager().callEvent(new AwkGearMasteryLoadEvent(this, AwkGearMasteryLoadEvent.LoadPhase.POST_TRIGGERS));

        loadProfiles();
        enchantmentModifierRegistry =
                EnchantmentModifierRegistry.load(configManager.getEnchantmentModifiersConfig());
        statCalculator = new StatCalculator(profileManager, enchantmentModifierRegistry);
        combatXpConfig = CombatXpConfig.load(configManager.getXpSourcesConfig());
        xpSourceRegistry = XpSourceRegistry.load(configManager.getXpSourcesConfig());
        getLogger().info("Loaded profiles...");

        Bukkit.getPluginManager().callEvent(new AwkGearMasteryLoadEvent(this, AwkGearMasteryLoadEvent.LoadPhase.POST_PROFILES));

        blockDataManager = BlockDataManagerFactory.createBlockDataManager(
                getDataFolder().toPath(),
                getConfig(),
                scheduler
        );
        blockDataManager.load();
        blockDataStorageType = getBlockDataStorageType();
        getLogger().info("Loaded block data manager...");

        if (getConfig().getBoolean("update.start")) {
            updateChecker.check();
        }

        if (getConfig().getBoolean("update.periodically")) {
            final long delay = 20L * TimeUnit.DAYS.toSeconds(1);
            scheduler.syncTimer(() -> updateChecker.check(), delay, delay);
        }

        registerListeners();
        getLogger().info("Registered listeners...");

        commandManager = BukkitLamp.builder(this).build();
        commandManager.register(new AwkGearMasteryCommand());
        getLogger().info("Registered commands...");

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new AwkGearMasteryPlaceholders().register();
        }

        Bukkit.getPluginManager().callEvent(new AwkGearMasteryLoadEvent(this, AwkGearMasteryLoadEvent.LoadPhase.COMPLETE));

        getLogger().info("Successfully started " + getPluginMeta().getName() + " v" + getPluginMeta().getVersion() + "!");
    }

    @Override
    public void onDisable() {
        closeBlockDataManager(blockDataManager);

        instance = null;

        getLogger().info("Successfully stopped " + getPluginMeta().getName() + " v" + getPluginMeta().getVersion() + ".");
    }

    private void sendStartupBanner() {
        Bukkit.getConsoleSender().sendMessage(colorize(" &b         _____"));
        Bukkit.getConsoleSender().sendMessage(colorize(" &d|          &b|     &8Based on original work by &2byteful"));
        Bukkit.getConsoleSender().sendMessage(colorize(" &d|          &b|     &8Extended by &2s3tupw1zard"));
        Bukkit.getConsoleSender().sendMessage(colorize(format(
                " &d|          &b|     &8Running &6%s &8on &6MC %s",
                getPluginMeta().getName() + " v" + getPluginMeta().getVersion(),
                AwkGearMasteryUtil.getServerVersion())));
        Bukkit.getConsoleSender().sendMessage(colorize(
                " &d|_____     &b|     &8Project: &9&nhttps://github.com/s3tupw1zard/AwkGearMastery"));
        Bukkit.getConsoleSender().sendMessage("");
    }

    private void loadProfiles() {
        profileManager.load(
                configManager.getTriggerProfilesConfig(),
                configManager.getRewardProfilesConfig(),
                configManager.getDisplayProfilesConfig(),
                configManager.getItemProfilesConfig(),
                configManager.getProgressionProfilesConfig(),
                configManager.getStatProfilesConfig()
        );
    }

    private void registerListeners() {
        final PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new BlockPlacementListener(scheduler), this);
        final TriggerListener triggers = new TriggerListener(profileManager, triggerRegistry);
        pm.registerEvents(triggers, this);
        final var shieldFarming = new dev.s3tupw1zard.awkgearmastery.listeners.ShieldFarmingListener(
                this, triggers::awardVerifiedTilling);
        pm.registerEvents(shieldFarming, this);
        pm.registerEvents(new dev.s3tupw1zard.awkgearmastery.listeners.ShieldAimCooldown(
                scheduler, shieldFarming::prefersHoeAim, Bukkit::getCurrentTick), this);
        pm.registerEvents(new CombatProgressionListener(profileManager), this);
        pm.registerEvents(new DefenseListener(profileManager), this);
        pm.registerEvents(new XPBooster(), this);
        pm.registerEvents(new AnvilListener(), this);
    }

    public void reloadPlugin() {
        Bukkit.getPluginManager().callEvent(new AwkGearMasteryLoadEvent(this, AwkGearMasteryLoadEvent.LoadPhase.PRE_LOAD));

        configManager.reload();
        reloadConfig();

        XpFormulaRegistry newFormulaRegistry = XpFormulaRegistry.load(getConfig());
        AnvilCombineMode newAnvilCombineMode =
                AnvilCombineMode.fromName(Objects.requireNonNull(getConfig().getString("anvil_combine")));
        BlockDataManager newBlockDataManager = null;
        String newBlockDataStorageType = getBlockDataStorageType();

        if (!Objects.equals(blockDataStorageType, newBlockDataStorageType)) {
            newBlockDataManager = BlockDataManagerFactory.createBlockDataManager(
                    getDataFolder().toPath(),
                    getConfig(),
                    scheduler
            );
            newBlockDataManager.load();
        }

        Bukkit.getPluginManager().callEvent(new AwkGearMasteryLoadEvent(this, AwkGearMasteryLoadEvent.LoadPhase.POST_TRIGGERS));

        try {
            loadProfiles();
            enchantmentModifierRegistry =
                    EnchantmentModifierRegistry.load(configManager.getEnchantmentModifiersConfig());
            statCalculator = new StatCalculator(profileManager, enchantmentModifierRegistry);
            combatXpConfig = CombatXpConfig.load(configManager.getXpSourcesConfig());
            xpSourceRegistry = XpSourceRegistry.load(configManager.getXpSourcesConfig());
        } catch (RuntimeException e) {
            closeBlockDataManager(newBlockDataManager);
            throw e;
        }

        anvilCombineMode = newAnvilCombineMode;
        xpFormulaRegistry = newFormulaRegistry;
        if (newBlockDataManager != null) {
            BlockDataManager oldBlockDataManager = blockDataManager;
            blockDataManager = newBlockDataManager;
            blockDataStorageType = newBlockDataStorageType;
            closeBlockDataManager(oldBlockDataManager);
        }

        Bukkit.getPluginManager().callEvent(new AwkGearMasteryLoadEvent(this, AwkGearMasteryLoadEvent.LoadPhase.POST_PROFILES));
        Bukkit.getPluginManager().callEvent(new AwkGearMasteryLoadEvent(this, AwkGearMasteryLoadEvent.LoadPhase.COMPLETE));

        getLogger().info("Reloaded AwkGearMastery!");
    }

    public void setAnvilCombineMode() {
        anvilCombineMode =
                AnvilCombineMode.fromName(Objects.requireNonNull(getConfig().getString("anvil_combine")));
    }

    public void setLevelXpFormula() {
        setXpFormulaRegistry();
    }

    public void setXpFormulaRegistry() {
        xpFormulaRegistry = XpFormulaRegistry.load(getConfig());
    }

    public AnvilCombineMode getAnvilCombineMode() {
        return anvilCombineMode;
    }

    public CompiledExpression getLevelXpFormula() {
        return xpFormulaRegistry.getGlobalFormula();
    }

    public XpFormulaRegistry getXpFormulaRegistry() {
        return xpFormulaRegistry;
    }

    public Lamp<BukkitCommandActor> getCommandManager() {
        return commandManager;
    }

    public UpdateChecker getUpdateChecker() {
        return updateChecker;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public BlockDataManager getBlockDataManager() {
        return blockDataManager;
    }

    private String getBlockDataStorageType() {
        return getConfig().getString("block_data_storage.type", "SQLITE").toUpperCase();
    }

    private void closeBlockDataManager(BlockDataManager manager) {
        if (manager == null) {
            return;
        }

        try {
            manager.close();
        } catch (IOException e) {
            getLogger().warning("Failed to close block data manager: " + e.getMessage());
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public TriggerRegistry getTriggerRegistry() {
        return triggerRegistry;
    }

    public StatCalculator getStatCalculator() {
        return statCalculator;
    }

    public CombatXpConfig getCombatXpConfig() {
        return combatXpConfig;
    }

    public XpSourceRegistry getXpSourceRegistry() {
        return xpSourceRegistry;
    }
}
