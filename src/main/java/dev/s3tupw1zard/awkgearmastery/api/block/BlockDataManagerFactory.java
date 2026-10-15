package dev.s3tupw1zard.awkgearmastery.api.block;

import dev.s3tupw1zard.awkgearmastery.api.block.impl.FileBlockDataManager;
import dev.s3tupw1zard.awkgearmastery.api.block.impl.SqliteBlockDataManager;
import dev.s3tupw1zard.awkgearmastery.api.scheduler.Scheduler;
import org.bukkit.configuration.ConfigurationSection;

import java.nio.file.Path;

public class BlockDataManagerFactory {
    public static BlockDataManager createBlockDataManager(Path dataFolder, ConfigurationSection config, Scheduler scheduler) {
        String storageType = config.getString("block_data_storage.type", "SQLITE");

        switch (storageType.toUpperCase()) {
            case "LEGACY_TEXT":
                return new FileBlockDataManager(dataFolder.resolve("placed_blocks.txt"), scheduler);
            case "SQLITE":
            default:
                return new SqliteBlockDataManager(dataFolder.resolve("placed_blocks.db"), scheduler);
        }
    }
}