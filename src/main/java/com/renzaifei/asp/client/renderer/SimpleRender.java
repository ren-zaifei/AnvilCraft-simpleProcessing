package com.renzaifei.asp.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.renzaifei.asp.entity.block.SimpleBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

public abstract class SimpleRender<T extends SimpleBlockEntity> implements BlockEntityRenderer<T> {

    private static final int DEFAULT_MAX_RENDERS = 5;
    private static final Vec3 DEFAULT_RENDER_POS = new Vec3(0.5, 0.8, 0.5);
    private static final int DEFAULT_ROT_ANGLE  = 90;
    private static final Vec3 DEFAULT_SCALE      = new Vec3(0.5, 0.5, 0.5);

    protected final ItemRenderer itemRenderer;
    protected final int maxRenders;
    protected final Vec3 renderPos;
    protected final int rotAngle;
    protected final Vec3 scale;

    protected SimpleRender(BlockEntityRendererProvider.Context ctx) {
        this(ctx, DEFAULT_MAX_RENDERS, DEFAULT_RENDER_POS, DEFAULT_ROT_ANGLE, DEFAULT_SCALE);
    }

    protected SimpleRender(BlockEntityRendererProvider.Context ctx, int maxRenders) {
        this(ctx, maxRenders, DEFAULT_RENDER_POS, DEFAULT_ROT_ANGLE, DEFAULT_SCALE);
    }

    protected SimpleRender(BlockEntityRendererProvider.Context ctx, Vec3 renderPos) {
        this(ctx, DEFAULT_MAX_RENDERS, renderPos, DEFAULT_ROT_ANGLE, DEFAULT_SCALE);
    }

    protected SimpleRender(BlockEntityRendererProvider.Context ctx, Vec3 renderPos, int rotAngle) {
        this(ctx, DEFAULT_MAX_RENDERS, renderPos, rotAngle, DEFAULT_SCALE);
    }

    protected SimpleRender(BlockEntityRendererProvider.Context ctx, int maxRenders,
                           Vec3 renderPos, int rotAngle, Vec3 scale) {
        this.itemRenderer = ctx.getItemRenderer();
        this.maxRenders = maxRenders;
        this.renderPos = renderPos;
        this.rotAngle = rotAngle;
        this.scale = scale;
    }

    /**
     * 生成随机位置
     * @param poseStack
     * @param random
     * @param i
     * @param facing
     */
    private void RandomArray(PoseStack poseStack , RandomSource random , int i ,@Nullable Direction facing) {
        if (facing != null) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - facing.toYRot()));
        }
        float dx = (random.nextFloat() - 0.5f) * 0.15f;
        float dz = (random.nextFloat() - 0.5f) * 0.15f;
        poseStack.translate(dx, i * 0.015625, dz);
        float rot = (random.nextFloat() - 0.5f) * 30f;
        poseStack.mulPose(Axis.YP.rotationDegrees(rot));
        poseStack.mulPose(Axis.XP.rotationDegrees(this.rotAngle));
    }

    @Override
    public void render(T be, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {

        int height = 0;
        height = renderHandlerItems(be.getInputItemHandler(null), be, height, poseStack, buffer, packedLight);
        height = renderHandlerItems(be.getOutputItemHandler(null), be, height, poseStack, buffer, packedLight);
    }

    /** 渲染一个 IItemHandler 中的所有物品，返回渲染后的累计高度 */
    private int renderHandlerItems(IItemHandler handler, T be, int height,
                                   PoseStack poseStack, MultiBufferSource buffer,
                                   int packedLight) {
        if (handler == null) return height;
        int slots = handler.getSlots();
        for (int i = 0; i < slots; i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            Direction facing = null;
            try {
                facing = be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            } catch (IllegalArgumentException e) {

            }
            int count = stack.getCount();
            int renders = Math.min(count, this.maxRenders);
            RandomSource random = RandomSource.create(stack.hashCode());
            for (int j = 0; j < renders; j++) {
                height++;
                poseStack.pushPose();
                poseStack.translate(this.renderPos.x, this.renderPos.y, this.renderPos.z);
                this.RandomArray(poseStack, random, height, facing);
                poseStack.scale((float) this.scale.x, (float) this.scale.y, (float) this.scale.z);
                this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED,
                        packedLight, OverlayTexture.NO_OVERLAY,
                        poseStack, buffer, be.getLevel(), 0);
                poseStack.popPose();
            }
        }
        return height;
    }
}
