package com.carcass.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;

/** Кремінний матеріал для ножа (ванільні Tiers розширити не можна). */
public enum ModTiers implements Tier {
    FLINT(1, 80, 4.0F, 1.0F, 5);

    private final int level, uses, enchantValue;
    private final float speed, damage;

    ModTiers(int level, int uses, float speed, float damage, int enchantValue) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damage = damage;
        this.enchantValue = enchantValue;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return damage; }
    @Override public int getLevel() { return level; }
    @Override public int getEnchantmentValue() { return enchantValue; }
    @Override public Ingredient getRepairIngredient() { return Ingredient.of(Items.FLINT); }
}
