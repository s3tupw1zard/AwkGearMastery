package dev.s3tupw1zard.awkgearmastery;

import dev.s3tupw1zard.awkgearmastery.api.item.AwkGearMasteryItem;
import dev.s3tupw1zard.awkgearmastery.profile.item.ItemProfile;
import dev.s3tupw1zard.awkgearmastery.util.AwkGearMasteryUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.*;
import revxrsal.commands.bukkit.actor.BukkitCommandActor;
import revxrsal.commands.command.ExecutableCommand;
import revxrsal.commands.help.Help;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static dev.s3tupw1zard.awkgearmastery.util.Text.colorize;

@Command("awkgearmastery")
public class AwkGearMasteryCommand {
    @Dependency
    private AwkGearMasteryPlugin plugin;

    @CommandPlaceholder
    public void onDefaultHelp(CommandSender sender, Help.RelatedCommands<BukkitCommandActor> help) {
        sendHelp(sender, help, 1);
    }

    @Subcommand("help")
    @Description("Shows the list of AwkGearMastery commands.")
    public void onHelp(CommandSender sender, Help.RelatedCommands<BukkitCommandActor> help, @Default("1") int page) {
        sendHelp(sender, help, page);
    }

    private void sendHelp(CommandSender sender, Help.RelatedCommands<BukkitCommandActor> help, int page) {
        final List<ExecutableCommand<BukkitCommandActor>> entries = new ArrayList<>();
        for (ExecutableCommand<BukkitCommandActor> entry : help) {
            if (entry.description() != null) {
                entries.add(entry);
            }
        }
        sender.sendMessage(colorize("&6&lAwkGearMastery Command Help:"));
        for (ExecutableCommand<BukkitCommandActor> entry : Help.paginate(entries, page, 7)) {
            sender.sendMessage(colorize(format("&7- &b/%s&7: &e%s", entry.usage(), entry.description())));
        }
    }

    @Subcommand("reload")
    @Description("Reloads AwkGearMastery' plugin configuration.")
    public void onReload(CommandSender sender) {
        if (!checkPerm(sender)) {
            return;
        }

        plugin.reloadPlugin();
        sender.sendMessage(
                colorize(
                        Objects.requireNonNull(plugin.getConfig().getString("messages.successful_reload"))));
    }

    @Subcommand("reset")
    @Description("Resets all XP/Levels for all the items in the target player.")
    public void onReset(CommandSender sender, Player target, @Switch("all") boolean all) {
        if (!checkPerm(sender)) {
            return;
        }

        final ItemStack hand = AwkGearMasteryUtil.getHand(target);
        if (!all) {
            if (!AwkGearMasteryUtil.isSupportedTool(hand.getType())) {
                sender.sendMessage(colorize(plugin.getConfig().getString("messages.item_not_tool")));

                return;
            }

            final AwkGearMasteryItem tool = AwkGearMasteryUtil.createAwkGearMasteryItem(hand);
            final ItemProfile itemProfile = AwkGearMasteryUtil.getItemProfile(hand.getType());
            tool.setLevel(1);
            tool.setXp(0);
            AwkGearMasteryUtil.setHand(target, AwkGearMasteryUtil.getItemStack(tool, target, itemProfile));
            sender.sendMessage(colorize(message(
                    "messages.successfully_reset_hand_tool",
                    "&aReset {item} for {player} to level 1 with 0 XP."
            )
                    .replace("{item}", AwkGearMasteryUtil.getReadableItemName(hand))
                    .replace("{player}", target.getName())));

            return;
        }

        final PlayerInventory inv = target.getInventory();
        final ItemStack[] contents = inv.getContents();
        int resetCount = 0;
        for (int i = 0; i < contents.length; i++) {
            final ItemStack item = contents[i];
            if (item == null || !AwkGearMasteryUtil.isSupportedTool(item.getType())) {
                continue;
            }
            final AwkGearMasteryItem tool = AwkGearMasteryUtil.createAwkGearMasteryItem(item);
            final ItemProfile itemProfile = AwkGearMasteryUtil.getItemProfile(item.getType());
            tool.setLevel(1);
            tool.setXp(0);
            inv.setItem(i, AwkGearMasteryUtil.getItemStack(tool, target, itemProfile));
            resetCount++;
        }
        sender.sendMessage(colorize(message(
                "messages.successfully_reset_tools",
                "&aReset {count} AwkGearMastery item(s) for {player} to level 1 with 0 XP."
        )
                .replace("{count}", String.valueOf(resetCount))
                .replace("{player}", target.getName())));
    }

    @Subcommand("xp")
    @Description("Sets the item in hand's XP to provided XP.")
    public void onXP(Player player, double xp) {
        if (!checkPerm(player)) {
            return;
        }

        final ItemStack item = AwkGearMasteryUtil.getHand(player);

        if (AwkGearMasteryUtil.isSupportedTool(item.getType())) {
            final AwkGearMasteryItem tool = AwkGearMasteryUtil.createAwkGearMasteryItem(item);
            final ItemProfile itemProfile = AwkGearMasteryUtil.getItemProfile(item.getType());
            tool.setXp(xp);
            AwkGearMasteryUtil.setHand(player, AwkGearMasteryUtil.getItemStack(tool, player, itemProfile));
            player.sendMessage(colorize(message(
                    "messages.successfully_set_xp",
                    "&aSet {item} XP to {xp}."
            )
                    .replace("{item}", AwkGearMasteryUtil.getReadableItemName(item))
                    .replace("{xp}", String.valueOf(tool.getXp()))));
        } else {
            player.sendMessage(
                    colorize(Objects.requireNonNull(plugin.getConfig().getString("messages.item_not_tool"))));
        }
    }

    @Subcommand("level")
    @Description("Sets the item in hand's level to provided level.")
    public void onLevel(Player player, int level) {
        if (!checkPerm(player)) {
            return;
        }

        final ItemStack item = AwkGearMasteryUtil.getHand(player);

        if (AwkGearMasteryUtil.isSupportedTool(item.getType())) {
            final AwkGearMasteryItem tool = AwkGearMasteryUtil.createAwkGearMasteryItem(item);
            final ItemProfile itemProfile = AwkGearMasteryUtil.getItemProfile(item.getType());
            final int initial = tool.getLevel();
            final int maxLevel = AwkGearMasteryUtil.getMaxLevel(itemProfile);
            final int targetLevel = Math.max(1, Math.min(level, maxLevel));
            tool.setLevel(targetLevel);
            if (targetLevel >= maxLevel) {
                tool.setXp(0.0);
            }
            AwkGearMasteryUtil.setHand(player, AwkGearMasteryUtil.getItemStack(tool, player, itemProfile));
            if (initial != tool.getLevel()) {
                AwkGearMasteryUtil.handleReward(tool, player);
            }
            player.sendMessage(colorize(message(
                    "messages.successfully_set_level",
                    "&aSet {item} to level {level}."
            )
                    .replace("{item}", AwkGearMasteryUtil.getReadableItemName(item))
                    .replace("{level}", String.valueOf(tool.getLevel()))));
        } else {
            player.sendMessage(
                    colorize(Objects.requireNonNull(plugin.getConfig().getString("messages.item_not_tool"))));
        }
    }

    @Subcommand("levelup")
    @Description("Increases the item in hand's level to next level.")
    public void onLevelUp(Player player) {
        if (!checkPerm(player)) {
            return;
        }

        final ItemStack item = AwkGearMasteryUtil.getHand(player);

        if (AwkGearMasteryUtil.isSupportedTool(item.getType())) {
            final AwkGearMasteryItem tool = AwkGearMasteryUtil.createAwkGearMasteryItem(item);
            final ItemProfile itemProfile = AwkGearMasteryUtil.getItemProfile(item.getType());
            final int initial = tool.getLevel();
            final int maxLevel = AwkGearMasteryUtil.getMaxLevel(itemProfile);
            if (initial >= maxLevel) {
                player.sendMessage(colorize(message(
                        "messages.already_max_level",
                        "&e{item} is already at the maximum level ({level})."
                )
                        .replace("{item}", AwkGearMasteryUtil.getReadableItemName(item))
                        .replace("{level}", String.valueOf(maxLevel))));
                return;
            }

            tool.setLevel(initial + 1);
            if (tool.getLevel() >= maxLevel) {
                tool.setXp(0.0);
            }
            AwkGearMasteryUtil.setHand(player, AwkGearMasteryUtil.getItemStack(tool, player, itemProfile));
            AwkGearMasteryUtil.handleReward(tool, player);
            player.sendMessage(colorize(message(
                    "messages.successfully_level_up",
                    "&aIncreased {item} to level {level}."
            )
                    .replace("{item}", AwkGearMasteryUtil.getReadableItemName(item))
                    .replace("{level}", String.valueOf(tool.getLevel()))));
        } else {
            player.sendMessage(
                    colorize(Objects.requireNonNull(plugin.getConfig().getString("messages.item_not_tool"))));
        }
    }

    @Subcommand("debug")
    @Description("Shows debug information about the server and plugin.")
    public void onDebug(CommandSender sender) {
        if (!checkPerm(sender)) {
            return;
        }

        plugin.getUpdateChecker().check();
        sender.sendMessage("AwkGearMastery Debug Information:");
        sender.sendMessage("- Server Version: " + Bukkit.getVersion());
        sender.sendMessage("- Server Type: " + Bukkit.getBukkitVersion());
        sender.sendMessage("- Plugin Version: " + plugin.getPluginMeta().getVersion());
        sender.sendMessage("- Latest Version: " + plugin.getUpdateChecker().getLastCheckedVersion());
        sender.sendMessage("{!} Please include your configuration when opening an issue at https://github.com/s3tupw1zard/AwkGearMastery/issues. {!}");
    }

    private String message(@NotNull String path, @NotNull String fallback) {
        return plugin.getConfig().getString(path, fallback);
    }

    private boolean checkPerm(CommandSender sender) {
        if (!sender.hasPermission("awkgearmastery.admin")) {
            sender.sendMessage(
                    colorize(Objects.requireNonNull(plugin.getConfig().getString("messages.no_permission"))));

            return false;
        }

        return true;
    }
}
