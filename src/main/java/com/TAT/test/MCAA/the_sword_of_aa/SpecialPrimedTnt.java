package com.TAT.test.MCAA.the_sword_of_aa;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 特殊 TNT 的待爆实体。
 *
 * <p>直接继承原版 {@link PrimedTnt}，复用它全部的物理与引信同步逻辑
 * （{@code tick()} 里倒计时归零后会调用可覆写的 {@code explode()}），
 * 只做两件事：</p>
 * <ul>
 *   <li>构造时把引信从原版的 80 tick（4 秒）改成 {@link #FUSE_TICKS} = 400 tick（20 秒）；</li>
 *   <li>覆写 {@link #explode()}：实体伤害/击退按原版公式结算，但半径取 2×80 = 160 格；
 *       方块破坏交给 {@link NukeDestruction} 分批处理（原版爆炸一次性处理 210 万个方块
 *       会把服务器卡死）。</li>
 * </ul>
 */
public class SpecialPrimedTnt extends PrimedTnt {

    /** 引信时长：20 秒（400 tick）。原版 TNT 是 4 秒（80 tick） */
    public static final int FUSE_TICKS = 400;

    /** 爆炸半径：5 区块 */
    public static final int RADIUS_CHUNKS = 5;

    /** 换算成方块：5 × 16 = 80 格 */
    public static final int RADIUS_BLOCKS = RADIUS_CHUNKS * 16;

    /** 实体注册用构造器（由 {@code EntityType} 的工厂调用） */
    public SpecialPrimedTnt(EntityType<? extends SpecialPrimedTnt> type, Level level) {
        super(type, level);
        this.setFuse(FUSE_TICKS);
    }

    /** 便捷构造器：直接在指定位置生成一个待爆体（带原版那种随机初速度） */
    public SpecialPrimedTnt(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this(The_sword_of_aa.SPECIAL_PRIMED_TNT.get(), level);
        this.setPos(x, y, z);
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        this.setDeltaMovement(-Math.sin(angle) * 0.02D, 0.2D, -Math.cos(angle) * 0.02D);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    @Override
    protected void explode() {
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        double x = this.getX();
        double y = this.getY(0.0625D);
        double z = this.getZ();

        // 1) 实体伤害与击退（半径内的一切都会被这一下清掉）
        this.damageEntities(level, x, y, z);

        // 2) 音效 + 粒子（原版只有"会破坏方块"的爆炸才放音效，这里自己做）
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F);
        level.addParticle(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0D, 0.0D, 0.0D);
        level.gameEvent(this, GameEvent.EXPLODE, new Vec3(x, y, z));

        // 3) 分批挖空半径 80 格的球体
        NukeDestruction.start(level, x, y, z, RADIUS_BLOCKS);
    }

    /**
     * 复刻原版 {@code Explosion} 对实体的结算公式，只是把「半径」换成
     * {@code 2 × 80 = 160} 格。中心处理论伤害约 1120 点，足以清空整个球体；
     * 边缘则衰减到几乎无害——和原版爆炸的手感一致，只是尺度大了 20 倍。
     */
    private void damageEntities(ServerLevel level, double x, double y, double z) {
        float spread = RADIUS_BLOCKS * 2.0F;
        AABB area = new AABB(x - spread - 1.0D, y - spread - 1.0D, z - spread - 1.0D,
                x + spread + 1.0D, y + spread + 1.0D, z + spread + 1.0D);
        DamageSource source = level.damageSources().explosion(this, this.getOwner());
        Vec3 center = new Vec3(x, y, z);

        List<Entity> entities = level.getEntities(this, area);
        for (Entity entity : entities) {
            if (entity.ignoreExplosion()) {
                continue;
            }
            double distanceRatio = Math.sqrt(entity.distanceToSqr(center)) / spread;
            if (distanceRatio > 1.0D) {
                continue;
            }
            double dx = entity.getX() - x;
            double dy = (entity instanceof PrimedTnt ? entity.getY() : entity.getEyeY()) - y;
            double dz = entity.getZ() - z;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length == 0.0D) {
                continue;
            }
            dx /= length;
            dy /= length;
            dz /= length;

            double seen = Explosion.getSeenPercent(center, entity);
            double exposure = (1.0D - distanceRatio) * seen;
            entity.hurt(source, (float) (int) ((exposure * exposure + exposure) / 2.0D * 7.0D * (double) spread + 1.0D));

            double knockback = entity instanceof LivingEntity living
                    ? ProtectionEnchantment.getExplosionKnockbackAfterDampener(living, exposure)
                    : exposure;
            entity.setDeltaMovement(entity.getDeltaMovement().add(dx * knockback, dy * knockback, dz * knockback));
        }
    }
}
