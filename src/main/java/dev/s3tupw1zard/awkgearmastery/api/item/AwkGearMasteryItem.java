package dev.s3tupw1zard.awkgearmastery.api.item;

import dev.s3tupw1zard.awkgearmastery.AwkGearMasteryPlugin;
import dev.s3tupw1zard.awkgearmastery.util.AwkGearMasteryUtil;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import redempt.crunch.CompiledExpression;

public interface AwkGearMasteryItem {
    @NotNull
    ItemStack getItemStack();

    @NotNull
    default ItemStack getItemStack(double maxXp) {
        return getItemStack();
    }

    int getLevel();

    void setLevel(int level);

    double getXp();

    void setXp(double xp);

    int getLastHandledReward();

    void setLastHandledReward(int rewardKey);

    default double getMaxXp() {
        final CompiledExpression formula = AwkGearMasteryPlugin.getInstance().getLevelXpFormula();
        final double nextXpRequirement = AwkGearMasteryUtil.round(formula.evaluate(getLevel()), 1);

        if (nextXpRequirement <= 0.0) {
            throw new RuntimeException(
                    "The next XP requirement formula returned a value too small! Please optimize your formula.");
        }

        return nextXpRequirement;
    }

    void enchant(Enchantment enchantment, int level);

    void modifyAttribute(String attribute, double modifier);
}
