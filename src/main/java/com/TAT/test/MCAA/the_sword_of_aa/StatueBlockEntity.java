package com.TAT.test.MCAA.the_sword_of_aa;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 雕像方块实体。
 *
 * <p>纯装饰，不需要额外数据——朝向存在方块状态 {@link StatueBlock#FACING} 里，
 * 皮肤是打包好的固定资源。它的存在只是为了让
 * {@link StatueBlockEntityRenderer} 有机会把玩家模型画出来。</p>
 */
public class StatueBlockEntity extends BlockEntity {

    public StatueBlockEntity(BlockPos pos, BlockState state) {
        super(The_sword_of_aa.STATUE_BLOCK_ENTITY.get(), pos, state);
    }

    /**
     * 雕像本体站在底座上、比它高出约 1.3 格。默认的渲染包围盒只取碰撞箱（半砖），
     * 抬头看雕像时上半身会被视锥剔除、突然消失，所以这里把包围盒放大到整尊雕像。
     */
    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(this.getBlockPos()).expandTowards(0.0D, 2.0D, 0.0D).inflate(1.0D);
    }
}
