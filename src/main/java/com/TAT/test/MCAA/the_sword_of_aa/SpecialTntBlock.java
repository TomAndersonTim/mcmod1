package com.TAT.test.MCAA.the_sword_of_aa;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 特殊 TNT（Special TNT）。
 *
 * <p>行为与机制跟原版 TNT 完全一致（打火石/火焰弹点燃、红石信号引爆、被爆炸波及时
 * 变成短引信待爆体、{@code UNSTABLE} 属性……），唯一的区别是它生成的是
 * {@link SpecialPrimedTnt}——<b>引信 20 秒、爆炸半径 5 区块（80 格）</b>。</p>
 *
 * <p>继承 {@code TntBlock} 而不是自己写一个 Block，是为了兼容原版/其他模组里
 * {@code instanceof TntBlock} 的判断（例如发射器打火石引爆后要用它决定是否消耗方块），
 * 以及继承 {@code dropFromExplosion() → false}（被炸掉时不掉落自身）。</p>
 */
public class SpecialTntBlock extends TntBlock {

    public SpecialTntBlock(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------
    // 所有点火入口。原版 TntBlock 里这些方法都指向私有的静态 explode()，无法覆写，
    // 所以这里逐个改写，统一指向 prime()。
    // ------------------------------------------------------------------

    /** 被火焰/岩浆点燃（Forge 的入口，FireBlock 与发射器都会走这里） */
    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face,
                             @Nullable LivingEntity igniter) {
        prime(level, pos, igniter);
    }

    /** 放置时如果旁边就有红石信号，立即引爆 */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock()) && level.hasNeighborSignal(pos)) {
            onCaughtFire(state, level, pos, null, null);
            level.removeBlock(pos, false);
        }
    }

    /** 红石信号变化时引爆 */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block,
                                BlockPos fromPos, boolean isMoving) {
        if (level.hasNeighborSignal(pos)) {
            onCaughtFire(state, level, pos, null, null);
            level.removeBlock(pos, false);
        }
    }

    /** 生存模式挖掉处于 UNSTABLE 状态的 TNT 会引爆（创造模式不会） */
    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && !player.isCreative() && state.getValue(UNSTABLE)) {
            onCaughtFire(state, level, pos, null, null);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    /** 被别的爆炸波及时，像原版 TNT 一样变成引信随机的待爆体 */
    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level.isClientSide) {
            return;
        }
        SpecialPrimedTnt tnt = new SpecialPrimedTnt(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, null);
        int fuse = tnt.getFuse();
        tnt.setFuse(level.random.nextInt(fuse / 4) + fuse / 8);
        level.addFreshEntity(tnt);
    }

    /** 打火石 / 火焰弹右键点燃 */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return super.use(state, level, pos, player, hand, hit);
        }
        this.onCaughtFire(state, level, pos, hit.getDirection(), player);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
        Item item = stack.getItem();
        if (!player.isCreative()) {
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, (broken) -> broken.broadcastBreakEvent(hand));
            } else {
                stack.shrink(1);
            }
        }
        player.awardStat(Stats.ITEM_USED.get(item));
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 着火的箭/火球命中时点燃 */
    @Override
    public void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level.isClientSide) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        Entity owner = projectile.getOwner();
        if (projectile.isOnFire() && projectile.mayInteract(level, pos)) {
            this.onCaughtFire(state, level, pos, null, owner instanceof LivingEntity living ? living : null);
            level.removeBlock(pos, false);
        }
    }

    /** 生成待爆体并播放点火音效（对应原版 TntBlock 的私有静态 explode） */
    private static void prime(Level level, BlockPos pos, @Nullable LivingEntity igniter) {
        if (level.isClientSide) {
            return;
        }
        SpecialPrimedTnt tnt = new SpecialPrimedTnt(level, pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, igniter);
        level.addFreshEntity(tnt);
        level.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
    }
}
