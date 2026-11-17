package com.carcass.item;

import com.carcass.entity.CorpseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class KnifeItem extends SwordItem {
    public static final String TARGET_TAG = "carcass_target";

    private final int cutTicks;
    private final float bonusChance;

    /** cutTicks - скільки тіків треба тримати ПКМ, щоб повністю розрізати труп. */
    public KnifeItem(Tier tier, int cutTicks, float bonusChance, Properties props) {
        super(tier, 0, -1.8F, props);
        this.cutTicks = cutTicks;
        this.bonusChance = bonusChance;
    }

    public int getCutTicks() { return cutTicks; }
    public float getBonusChance() { return bonusChance; }

    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.CUSTOM; }

    /** Працює на сервері: рахує прогрес розрізання, поки гравець тримає ПКМ. */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining) {
        if (level.isClientSide || !(user instanceof Player player)) return;

        CompoundTag data = player.getPersistentData();
        Entity target = data.contains(TARGET_TAG) ? level.getEntity(data.getInt(TARGET_TAG)) : null;
        if (!(target instanceof CorpseEntity corpse) || !corpse.isAlive() || corpse.distanceToSqr(player) > 25.0D) {
            player.stopUsingItem();
            return;
        }

        int elapsed = getUseDuration(stack) - remaining;
        if (elapsed > 0 && elapsed < cutTicks && elapsed % 8 == 0) {
            corpse.cutStroke();
            player.displayClientMessage(Component.translatable("message.carcass.cutting",
                    Math.min(99, elapsed * 100 / cutTicks)), true);
        }
        if (elapsed >= cutTicks) {
            corpse.finishCut(player, stack);
            player.stopUsingItem();
        }
    }

    /** Анімація ножа від першої особи: піднятий ніж і рубальні рухи. */
    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public boolean applyForgeHandTransform(PoseStack pose, LocalPlayer player, HumanoidArm arm,
                                                   ItemStack stack, float partialTick, float equipProcess,
                                                   float swingProcess) {
                if (!player.isUsingItem() || player.getUseItem().getItem() != stack.getItem()) return false;

                float t = player.getTicksUsingItem() + partialTick;
                float in = Mth.clamp(t / 6.0F, 0F, 1F);        // плавний вхід в анімацію
                float s = Mth.sin(t * 0.7F) * in;              // рубальний рух
                int side = arm == HumanoidArm.RIGHT ? 1 : -1;

                pose.translate(side * 0.56F, -0.52F, -0.72F);
                pose.translate(-side * 0.22F * in, 0.28F * in + 0.06F * s, 0.10F * in);
                pose.mulPose(Axis.XP.rotationDegrees(-40.0F * in + 18.0F * s));
                pose.mulPose(Axis.YP.rotationDegrees(side * 20.0F * in));
                pose.mulPose(Axis.ZP.rotationDegrees(side * 12.0F * s));
                return true;
            }
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.carcass.bonus", Math.round(bonusChance * 100)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.carcass.speed", String.format("%.1f", cutTicks / 20.0)).withStyle(ChatFormatting.GRAY));
    }
}
