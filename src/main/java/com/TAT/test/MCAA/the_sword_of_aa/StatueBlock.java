package com.TAT.test.MCAA.the_sword_of_aa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 雕像方块（Statue）。
 *
 * <p>由两部分组成：</p>
 * <ul>
 *   <li><b>底座</b> —— 普通方块模型（半砖大小，见 {@code models/block/statue.json}），
 *       同时是碰撞箱；</li>
 *   <li><b>雕像本体</b> —— 完整玩家模型（{@code TomAndersonTim} 的皮肤），
 *       站在底座上，由 {@code StatueBlockEntityRenderer} 绘制。</li>
 * </ul>
 *
 * <p>{@link #getRenderShape} 特意返回 {@link RenderShape#MODEL}（而不是
 * {@code BaseEntityBlock} 默认的 {@code ENTITYBLOCK_ANIMATED}），这样底座能正常
 * 走方块光照渲染，雕像本体依旧由 BlockEntityRenderer 单独绘制，两者互不冲突。</p>
 */
public class StatueBlock extends BaseEntityBlock {

    /** 朝向：雕像正面朝哪边 */
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** 底座（半砖）：与 block/statue.json 的模型尺寸保持一致 */
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 8.0D, 16.0D);

    public StatueBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0F, 6.0F)
                .sound(SoundType.STONE)
                .noOcclusion());
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 正面朝向放置者（与箱子、告示牌一致）
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatueBlockEntity(pos, state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
