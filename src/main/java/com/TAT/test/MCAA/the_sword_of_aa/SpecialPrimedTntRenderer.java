package com.TAT.test.MCAA.the_sword_of_aa;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 仅客户端：像原版 TNT 那样渲染待爆的特殊 TNT——
 * 引信最后 1 秒会逐渐膨胀，并且每 5 tick 闪一次白光。
 *
 * <p>与 {@code TntRenderer} 唯一的区别是渲染的是我们自己的方块
 * （所以换成自定义贴图后，实体渲染出来的样子也会跟着变），
 * 白闪逻辑直接复用 public static 的
 * {@link TntMinecartRenderer#renderWhiteSolidBlock}。</p>
 */
public class SpecialPrimedTntRenderer extends EntityRenderer<SpecialPrimedTnt> {

    private final BlockRenderDispatcher blockRenderer;

    public SpecialPrimedTntRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(SpecialPrimedTnt entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);

        int fuse = entity.getFuse();
        if ((float) fuse - partialTick + 1.0F < 10.0F) {
            float swell = 1.0F - ((float) fuse - partialTick + 1.0F) / 10.0F;
            swell = Mth.clamp(swell, 0.0F, 1.0F);
            swell *= swell;
            swell *= swell;
            float scale = 1.0F + swell * 0.3F;
            poseStack.scale(scale, scale, scale);
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.translate(-0.5F, -0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        TntMinecartRenderer.renderWhiteSolidBlock(this.blockRenderer,
                The_sword_of_aa.SPECIAL_TNT.get().defaultBlockState(),
                poseStack, bufferSource, packedLight, fuse / 5 % 2 == 0);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SpecialPrimedTnt entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
