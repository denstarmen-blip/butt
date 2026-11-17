package com.carcass.client;

import com.carcass.entity.CorpseEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Малює труп як справжнього моба, що лежить на боці. */
public class CorpseRenderer extends EntityRenderer<CorpseEntity> {
    private final EntityRenderDispatcher dispatcher;

    public CorpseRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.dispatcher = ctx.getEntityRenderDispatcher();
        this.shadowRadius = 0.4F;
    }

    @Override
    public void render(CorpseEntity corpse, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffer, int light) {
        LivingEntity mob = corpse.getRenderEntity();
        if (mob == null) return;

        mob.setYRot(0F); mob.yRotO = 0F;
        mob.setXRot(0F); mob.xRotO = 0F;
        mob.yBodyRot = 0F; mob.yBodyRotO = 0F;
        mob.yHeadRot = 0F; mob.yHeadRotO = 0F;
        mob.hurtTime = 0;
        mob.deathTime = 0;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        pose.translate(0.0D, mob.getBbWidth() * 0.5D, 0.0D);
        pose.mulPose(Axis.ZP.rotationDegrees(90.0F));
        dispatcher.render(mob, 0.0D, 0.0D, 0.0D, 0.0F, partialTick, pose, buffer, light);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(CorpseEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
