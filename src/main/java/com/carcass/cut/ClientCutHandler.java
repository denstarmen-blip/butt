package com.carcass.cut;

import com.carcass.Carcass;
import com.carcass.item.KnifeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Під час розрізання камера плавно нахиляється до трупа і наближається (зум). */
@Mod.EventBusSubscriber(modid = Carcass.MODID, value = Dist.CLIENT)
public class ClientCutHandler {
    private static float blend = 0F;
    private static float prevBlend = 0F;

    private static float ease(float x) {
        return x * x * (3.0F - 2.0F * x);
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean cutting = player != null && mc.level != null
                && player.isUsingItem()
                && player.getUseItem().getItem() instanceof KnifeItem
                && CutState.clientTarget != -1
                && mc.level.getEntity(CutState.clientTarget) != null;
        prevBlend = blend;
        blend = Mth.clamp(blend + (cutting ? 0.10F : -0.10F), 0F, 1F);
        if (!cutting && blend == 0F) CutState.clientTarget = -1;
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (blend <= 0F) return;
        event.setNewFovModifier(event.getNewFovModifier() * (1.0F - 0.40F * ease(blend)));
    }

    @SubscribeEvent
    public static void onCamera(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.options.getCameraType().isFirstPerson()) return;
        float b = Mth.lerp((float) event.getPartialTick(), prevBlend, blend);
        if (b <= 0.001F) return;
        Entity target = mc.level.getEntity(CutState.clientTarget);
        if (target == null) return;

        Vec3 eye = mc.player.getEyePosition((float) event.getPartialTick());
        Vec3 to = target.position().add(0, 0.2, 0).subtract(eye);
        double horiz = Math.sqrt(to.x * to.x + to.z * to.z);
        float wantYaw = (float) (Mth.atan2(-to.x, to.z) * 180.0 / Math.PI);
        float wantPitch = (float) (-(Mth.atan2(to.y, horiz) * 180.0 / Math.PI));

        float k = ease(b) * 0.85F;
        event.setYaw(event.getYaw() + Mth.wrapDegrees(wantYaw - event.getYaw()) * k);
        event.setPitch(event.getPitch() + (wantPitch - event.getPitch()) * k);
    }
}
