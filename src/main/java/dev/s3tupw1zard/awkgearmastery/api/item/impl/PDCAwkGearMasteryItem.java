package dev.s3tupw1zard.awkgearmastery.api.item.impl;

import dev.s3tupw1zard.awkgearmastery.api.item.AwkGearMasteryItem;
import dev.s3tupw1zard.awkgearmastery.util.AwkGearMasteryUtil;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class PDCAwkGearMasteryItem implements AwkGearMasteryItem {
    @NotNull
    public static final NamespacedKey
            LEVEL_KEY = Objects.requireNonNull(NamespacedKey.fromString("awkgearmastery:level")),
            XP_KEY = Objects.requireNonNull(NamespacedKey.fromString("awkgearmastery:xp")),
            LAST_REWARD_KEY = Objects.requireNonNull(NamespacedKey.fromString("awkgearmastery:last_reward")),
            ITEM_ID_KEY = Objects.requireNonNull(NamespacedKey.fromString("awkgearmastery:item_id"));

    @NotNull
    private ItemStack stack;
    @NotNull
    private Map<Enchantment, Integer> enchantments;
    @NotNull
    private Map<String, Double> attributes;
    private int level;
    private double xp;
    private int lastHandledReward;
    @NotNull
    private String itemId;

    public PDCAwkGearMasteryItem(@NotNull ItemStack stack) {
        this(stack, stack.getItemMeta());
    }

    public PDCAwkGearMasteryItem(@NotNull ItemStack stack, @Nullable ItemMeta meta) {
        this.stack = stack;
        this.enchantments = new HashMap<>();
        this.attributes = new HashMap<>();
        readState(meta);
    }

    private void readState(@Nullable ItemMeta meta) {
        if (meta == null) {
            level = 1;
            xp = 0.0D;
            lastHandledReward = -1;
            itemId = UUID.randomUUID().toString();

            return;
        }

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        final Integer storedLevel = pdc.get(LEVEL_KEY, PersistentDataType.INTEGER);
        final Double storedXp = pdc.get(XP_KEY, PersistentDataType.DOUBLE);
        final Integer storedLastReward = pdc.get(LAST_REWARD_KEY, PersistentDataType.INTEGER);
        final String storedItemId = pdc.get(ITEM_ID_KEY, PersistentDataType.STRING);
        level = storedLevel == null ? 1 : Math.max(storedLevel, 1);
        xp = storedXp == null ? 0.0D : Math.max(storedXp, 0.0D);
        lastHandledReward = storedLastReward == null ? -1 : storedLastReward;
        itemId = storedItemId == null ? UUID.randomUUID().toString() : storedItemId;
    }

    @Override
    public @NotNull ItemStack getItemStack() {
        return getItemStack(getMaxXp());
    }

    @Override
    public @NotNull ItemStack getItemStack(double maxXp) {
        final ItemStack built = stack.clone();
        final ItemMeta meta = built.getItemMeta();
        assert meta != null : "ItemMeta is null! Should not happen.";

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(LEVEL_KEY, PersistentDataType.INTEGER, level);
        pdc.set(XP_KEY, PersistentDataType.DOUBLE, xp);
        pdc.set(LAST_REWARD_KEY, PersistentDataType.INTEGER, lastHandledReward);
        pdc.set(ITEM_ID_KEY, PersistentDataType.STRING, itemId);

        AwkGearMasteryUtil.applyCalculatedDurability(built, meta, level);
        AwkGearMasteryUtil.applyDisplay(built, meta, enchantments, level, xp, maxXp);
        applyAttributes(meta);
        built.setItemMeta(meta);

        return built;
    }

    private void applyAttributes(@NotNull ItemMeta meta) {
        for (Map.Entry<String, Double> entry : attributes.entrySet()) {
            final String name = entry.getKey();
            final Registry<Attribute> attributeRegistry =
                    RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE);
            final Attribute attribute = resolveAttribute(attributeRegistry, name);
            final NamespacedKey attributeKey = attributeRegistry.getKeyOrThrow(attribute);
            final NamespacedKey modifierKey =
                    Objects.requireNonNull(NamespacedKey.fromString("awkgearmastery:reward/" + attributeKey.getKey()));

            final Collection<AttributeModifier> existing = meta.getAttributeModifiers(attribute);
            if (existing != null) {
                for (AttributeModifier modifier : existing) {
                    if (modifierKey.equals(modifier.getKey())) {
                        meta.removeAttributeModifier(attribute, modifier);
                    }
                }
            }

            meta.addAttributeModifier(
                    attribute,
                    new AttributeModifier(
                            modifierKey,
                            entry.getValue(),
                            AttributeModifier.Operation.ADD_NUMBER));
        }
    }

    private static Attribute resolveAttribute(
            @NotNull Registry<Attribute> attributeRegistry, @NotNull String configuredName) {
        String normalized = configuredName.trim().toLowerCase(Locale.ROOT);
        final NamespacedKey key;

        if (normalized.contains(":")) {
            key = NamespacedKey.fromString(normalized);
        } else {
            final int legacyPrefixSeparator = normalized.indexOf('.');
            if (legacyPrefixSeparator >= 0) {
                normalized = normalized.substring(legacyPrefixSeparator + 1);
            }
            key = NamespacedKey.minecraft(normalized.replace('.', '_'));
        }

        if (key == null) {
            throw new IllegalArgumentException("Invalid attribute key: " + configuredName);
        }

        final Attribute attribute = attributeRegistry.get(key);
        if (attribute == null) {
            throw new IllegalArgumentException("Unknown attribute: " + configuredName);
        }

        return attribute;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public void setLevel(int level) {
        this.level = Math.max(level, 1);
    }

    @Override
    public double getXp() {
        return xp;
    }

    @Override
    public void setXp(double xp) {
        this.xp = Math.max(xp, 0.0D);
    }

    @Override
    public int getLastHandledReward() {
        return lastHandledReward;
    }

    @Override
    public void setLastHandledReward(int rewardKey) {
        this.lastHandledReward = rewardKey;
    }

    @Override
    public void enchant(Enchantment enchantment, int level) {
        enchantments.put(enchantment, level);
    }

    @Override
    public void modifyAttribute(String attribute, double modifier) {
        attributes.put(attribute, modifier);
    }

    @NotNull
    public String getItemId() {
        return itemId;
    }

    public void setItemId(@NotNull String itemId) {
        this.itemId = itemId;
    }

    @NotNull
    public ItemStack getStack() {
        return stack;
    }

    public void setStack(@NotNull ItemStack stack) {
        this.stack = stack;
        readState(stack.getItemMeta());
    }

    @NotNull
    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    public void setEnchantments(@NotNull Map<Enchantment, Integer> enchantments) {
        this.enchantments = enchantments;
    }

    public @NotNull Map<String, Double> getAttributes() {
        return attributes;
    }

    public void setAttributes(@NotNull Map<String, Double> attributes) {
        this.attributes = attributes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PDCAwkGearMasteryItem that = (PDCAwkGearMasteryItem) o;
        return level == that.level
                && Double.compare(that.xp, xp) == 0
                && lastHandledReward == that.lastHandledReward
                && stack.equals(that.stack)
                && enchantments.equals(that.enchantments)
                && attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack, enchantments, attributes, level, xp, lastHandledReward);
    }

    @Override
    public String toString() {
        return "PDCAwkGearMasteryItem{"
                + "stack="
                + stack
                + ", enchantments="
                + enchantments
                + ", attributes="
                + attributes
                + ", level="
                + level
                + ", xp="
                + xp
                + ", lastHandledReward="
                + lastHandledReward
                + '}';
    }
}
