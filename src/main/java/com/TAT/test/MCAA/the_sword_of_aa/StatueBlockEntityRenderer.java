package com.TAT.test.MCAA.the_sword_of_aa;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * 仅客户端：把完整的玩家模型（{@link #STATUE_SKIN} 这张皮肤）画在底座上方。
 *
 * <p>不注册 {@code ModelLayerLocation}，直接 {@code bakeRoot()} 出模型——雕像是一动不动的
 * 静物，不需要参与实体模型的烘焙流程；也从不调用 {@code setupAnim}，所以保持默认站姿。</p>
 */
public class StatueBlockEntityRenderer implements BlockEntityRenderer<StatueBlockEntity> {

    /** 打包进模组的皮肤（TomAndersonTim，64x64，classic / Steve 4px 手臂） */
    public static final ResourceLocation STATUE_SKIN =
            new ResourceLocation(The_sword_of_aa.MODID, "textures/entity/statue_tomanderson_tim.png");

    /** 底座高度（半砖，与 StatueBlock#SHAPE 一致） */
    private static final float BASE_HEIGHT = 0.5F;

    private final PlayerModel<LivingEntity> model;

    public StatueBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        // 64x64 皮肤；false = classic 模型（trues 则是 Alex 的 3px 手臂）
        ModelPart root = LayerDefinition
                .create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64)
                .bakeRoot();
        this.model = new PlayerModel<>(root, false);
        this.model.young = false;
    }

    @Override
    public void render(StatueBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Direction facing = blockEntity.getBlockState().getValue(StatueBlock.FACING);

        poseStack.pushPose();
        // 站在底座正中央
        poseStack.translate(0.5D, BASE_HEIGHT, 0.5D);
        // 下面两行与 LivingEntityRenderer 里的处理完全一致，模型才会正立、且朝向正确
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - facing.toYRot()));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0D, -1.501D, 0.0D);

        VertexConsumer buffer = bufferSource.getBuffer(this.model.renderType(STATUE_SKIN));
        this.model.renderToBuffer(poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);

        poseStack.popPose();
    }
}
