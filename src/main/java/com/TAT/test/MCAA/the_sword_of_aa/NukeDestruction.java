package com.TAT.test.MCAA.the_sword_of_aa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 特殊 TNT 的「挖空」任务。
 *
 * <p>半径 80 格的球体里大约有 <b>210 万个方块</b>。如果照原版爆炸那样在一个 tick 里
 * 全部处理完，服务器会直接冻结（甚至被看门狗判定超时）；而且原版 {@code Explosion}
 * 的射线算法要为每个被炸方块生成 {@code BlockPos} 与掉落物，这个量级下必然把游戏拖垮。</p>
 *
 * <p>所以这里换一套做法：</p>
 * <ul>
 *   <li><b>分批执行</b>——每 tick 只处理到时间预算用完为止，服务器不会冻结，
 *       视觉上像冲击波一层层把方块挖空（几秒完成）；</li>
 *   <li><b>不掉落物</b>——直接把方块置为空气，210 万个掉落物是不可接受的；</li>
 *   <li><b>不触发邻居更新</b>——用 {@link Block#UPDATE_CLIENTS} 而不是默认的
 *       {@code UPDATE_ALL}，避免水、岩浆、沙砾的连锁更新把服务器二次拖垮。</li>
 * </ul>
 *
 * <p>任务由 {@code The_sword_of_aa#onServerTick} 驱动，不做持久化：服务器如果在挖空
 * 途中重启，留下的就是一个"半个坑"（不影响存档正确性）。</p>
 */
public final class NukeDestruction {

    /** 每 tick 最多花在破坏上的时间（纳秒）。原版一 tick 是 50ms，留足余量 */
    private static final long TICK_BUDGET_NANOS = 30_000_000L;

    /**
     * 抗爆等级达到此值的方块炸不掉。
     * 黑曜石/铁砧/下界合金块/远古残骸是 1200，基岩等是 3600000；水和岩浆恰好是 100。
     * 取 100 意味着「除了液体和真正抗爆的东西，其余全部蒸发」。
     */
    private static final float MAX_BREAKABLE_RESISTANCE = 100.0F;

    private static final List<Job> JOBS = new ArrayList<>();

    private NukeDestruction() {
    }

    /** 启动一个挖空任务。必须在服务端主线程调用 */
    public static void start(ServerLevel level, double x, double y, double z, int radius) {
        JOBS.add(new Job(level, x, y, z, radius));
    }

    /** 由服务器 tick 事件驱动：推进所有未完成的任务 */
    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) {
            return;
        }
        Iterator<Job> iterator = JOBS.iterator();
        while (iterator.hasNext()) {
            Job job = iterator.next();
            // 附加条件 first：服务器重载后残留的旧任务（属于别的 server 实例）直接丢弃
            if (job.level.getServer() != server || job.step()) {
                iterator.remove();
            }
        }
    }

    /** 一次挖空任务。用「球体外接立方体的下标」当游标，不保存任何方块列表，内存占用恒定 */
    private static final class Job {

        private final ServerLevel level;
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        /** 外接立方体边长 = 2r + 1 */
        private final int side;
        private final int minX;
        private final int minY;
        private final int minZ;
        /** 下标的终点 = side³ */
        private final long total;
        private final double radiusSquared;
        private long cursor;

        Job(ServerLevel level, double centerX, double centerY, double centerZ, int radius) {
            this.level = level;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.side = radius * 2 + 1;
            this.minX = Mth.floor(centerX) - radius;
            this.minY = Mth.floor(centerY) - radius;
            this.minZ = Mth.floor(centerZ) - radius;
            this.total = (long) this.side * this.side * this.side;
            this.radiusSquared = (double) radius * radius;
        }

        /** @return true 表示整颗球都处理完了 */
        @SuppressWarnings("deprecation") // Block#getExplosionResistance()：这里不需要带上下文的版本
        boolean step() {
            long deadline = System.nanoTime() + TICK_BUDGET_NANOS;
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int counter = 0;

            while (this.cursor < this.total) {
                long index = this.cursor++;
                // 每 256 次查一次表：全是空气/抗爆方块时也要能被时间预算打断
                if ((++counter & 0xFF) == 0 && System.nanoTime() >= deadline) {
                    return false;
                }

                int dx = (int) (index % this.side);
                int dy = (int) (index / this.side % this.side);
                int dz = (int) (index / ((long) this.side * this.side));

                // 以方块中心到球心的距离判断是否落在球内
                double offX = this.minX + dx + 0.5D - this.centerX;
                double offY = this.minY + dy + 0.5D - this.centerY;
                double offZ = this.minZ + dz + 0.5D - this.centerZ;
                if (offX * offX + offY * offY + offZ * offZ > this.radiusSquared) {
                    continue;
                }

                pos.set(this.minX + dx, this.minY + dy, this.minZ + dz);
                BlockState state = this.level.getBlockState(pos);
                if (state.isAir() || state.getBlock().getExplosionResistance() >= MAX_BREAKABLE_RESISTANCE) {
                    continue;
                }
                this.level.setBlock(pos, air, Block.UPDATE_CLIENTS);
            }
            return true;
        }
    }
}
