package com.carcass;

import com.carcass.entity.CorpseEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/** Замість того, щоб викинути предмети, моб залишає труп, який треба розрізати ножем. */
public class DeathHandler {

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        LivingEntity mob = event.getEntity();
        if (mob.level().isClientSide) return;
        if (mob instanceof Player || mob instanceof ArmorStand
                || mob instanceof EnderDragon || mob instanceof WitherBoss) return;
        if (event.getDrops().isEmpty()) return;

        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        if (id == null) return;

        List<ItemStack> drops = new ArrayList<>();
        for (ItemEntity ie : event.getDrops()) drops.add(ie.getItem().copy());

        CorpseEntity corpse = Carcass.CORPSE.get().create(mob.level());
        if (corpse == null) return;

        CompoundTag nbt = mob.saveWithoutId(new CompoundTag());
        corpse.setup(id, nbt, drops);
        corpse.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0F);
        mob.level().addFreshEntity(corpse);

        event.setCanceled(true); // предмети тепер лежать у трупі
    }
}
