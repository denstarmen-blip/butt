package com.carcass.entity;

import com.carcass.cut.CutState;
import com.carcass.item.KnifeItem;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CorpseEntity extends Entity {
    private static final EntityDataAccessor<String> MOB_TYPE =
            SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<CompoundTag> MOB_NBT =
            SynchedEntityData.defineId(CorpseEntity.class, EntityDataSerializers.COMPOUND_TAG);

    /** 10 хвилин, потім труп зникає, а предмети висипаються на землю. */
    private static final int LIFETIME = 12000;

    private final List<ItemStack> loot = new ArrayList<>();
    private int age;
    @Nullable private LivingEntity renderEntity; // тільки клієнт

    public CorpseEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public void setup(ResourceLocation mobId, CompoundTag mobNbt, List<ItemStack> drops) {
        entityData.set(MOB_TYPE, mobId.toString());
        entityData.set(MOB_NBT, mobNbt);
        loot.clear();
        loot.addAll(drops);
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(MOB_TYPE, "");
        entityData.define(MOB_NBT, new CompoundTag());
    }

    @Override
    public void tick() {
        super.tick();
        if (!onGround()) {
            setDeltaMovement(getDeltaMovement().add(0, -0.04, 0));
        }
        move(MoverType.SELF, getDeltaMovement());
        float f = onGround() ? 0.55F : 0.91F;
        setDeltaMovement(getDeltaMovement().multiply(f, 0.98, f));

        if (!level().isClientSide && ++age > LIFETIME) {
            for (ItemStack s : loot) spawnAtLocation(s);
            loot.clear();
            discard();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof KnifeItem)) {
            boolean hasKnife = player.getMainHandItem().getItem() instanceof KnifeItem
                    || player.getOffhandItem().getItem() instanceof KnifeItem;
            if (!hasKnife && !level().isClientSide && hand == InteractionHand.MAIN_HAND) {
                player.displayClientMessage(Component.translatable("message.carcass.need_knife"), true);
            }
            return InteractionResult.PASS;
        }
        if (player.isUsingItem()) return InteractionResult.CONSUME;

        if (level().isClientSide) {
            CutState.clientTarget = getId();
        } else {
            if (loot.isEmpty()) {
                discard();
                return InteractionResult.CONSUME;
            }
            player.getPersistentData().putInt(KnifeItem.TARGET_TAG, getId());
        }
        // Далі розрізання йде в KnifeItem#onUseTick, поки гравець тримає ПКМ
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    /** Один "рубальний рух": звук і трохи крові. */
    public void cutStroke() {
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.02F, 0.02F), 1.1F),
                    getX(), getY() + 0.3, getZ(), 6, 0.3, 0.1, 0.3, 0.02);
        }
        level().playSound(null, blockPosition(), SoundEvents.SHEEP_SHEAR, SoundSource.PLAYERS,
                0.9F, 0.8F + random.nextFloat() * 0.3F);
    }

    /** Розрізання завершено: вибух пікселів, усі предмети випадають, труп зникає. */
    public void finishCut(Player player, ItemStack knifeStack) {
        if (!(knifeStack.getItem() instanceof KnifeItem knife)) return;

        for (ItemStack s : loot) {
            ItemStack out = s.copy();
            if (random.nextFloat() < knife.getBonusChance() && out.getCount() < out.getMaxStackSize()) {
                out.grow(1);
            }
            spawnAtLocation(out, 0.3F);
        }
        loot.clear();

        if (level() instanceof ServerLevel sl) {
            double x = getX(), y = getY() + 0.3, z = getZ();
            sl.sendParticles(new DustParticleOptions(new Vector3f(0.7F, 0.02F, 0.02F), 1.4F), x, y, z, 30, 0.45, 0.25, 0.45, 0.05);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.RED_WOOL.defaultBlockState()), x, y, z, 35, 0.4, 0.25, 0.4, 0.12);
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.WHITE_WOOL.defaultBlockState()), x, y, z, 30, 0.4, 0.25, 0.4, 0.12);
        }
        level().playSound(null, blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.PLAYERS, 1.0F, 0.7F);

        InteractionHand hand = player.getUsedItemHand();
        knifeStack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
        discard();
    }

    @Override public boolean isPickable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public boolean isAttackable() { return false; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD)) {
            discard();
            return true;
        }
        return false;
    }

    /** Створює "муляж" моба для рендеру лежачого трупа (клієнт). */
    @Nullable
    public LivingEntity getRenderEntity() {
        if (renderEntity == null) {
            String s = entityData.get(MOB_TYPE);
            if (s.isEmpty()) return null;
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id == null || !ForgeRegistries.ENTITY_TYPES.containsKey(id)) return null;
            Entity e = ForgeRegistries.ENTITY_TYPES.getValue(id).create(level());
            if (!(e instanceof LivingEntity le)) return null;
            CompoundTag nbt = entityData.get(MOB_NBT);
            if (!nbt.isEmpty()) {
                try { le.load(nbt.copy()); } catch (Exception ignored) { }
            }
            renderEntity = le;
        }
        return renderEntity;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(MOB_TYPE, tag.getString("MobType"));
        entityData.set(MOB_NBT, tag.getCompound("MobNbt"));
        age = tag.getInt("Age");
        loot.clear();
        ListTag list = tag.getList("Loot", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) loot.add(ItemStack.of(list.getCompound(i)));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("MobType", entityData.get(MOB_TYPE));
        tag.put("MobNbt", entityData.get(MOB_NBT));
        tag.putInt("Age", age);
        ListTag list = new ListTag();
        for (ItemStack s : loot) list.add(s.save(new CompoundTag()));
        tag.put("Loot", list);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
