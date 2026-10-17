package com.TAT.test.MCAA.the_sword_of_aa;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;

/**
 * 护身符（Amulet）——一枚独立物品，拥有与不死图腾完全相同的效果。
 *
 * <p>原版 {@code LivingEntity#checkTotemDeathProtection} 写死了只识别
 * {@code Items.TOTEM_OF_UNDYING}，无法通过继承扩展，因此这里改由可取消的
 * {@code LivingDeathEvent} 来复刻同一套逻辑（见 {@link #tryProtectFromDeath}）。</p>
 */
public class AmuletItem extends Item {

    public AmuletItem(Properties properties) {
        super(properties);
    }

    /**
     * 复刻原版不死图腾的死亡保护：手上（主手/副手）持有护身符时消耗一个，
     * 把血量拉回 1 并施加与原版完全一致的增益。
     *
     * @param entity 濒死的实体
     * @param source 致死伤害来源
     * @return 是否成功触发保护（触发则调用方应取消死亡事件）
     */
    public static boolean tryProtectFromDeath(LivingEntity entity, DamageSource source) {
        // 与不死图腾一致：无视无敌的伤害（例如 /kill）无法被抵挡
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }

        // 与不死图腾一致：只检查主手与副手
        ItemStack amulet = null;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = entity.getItemInHand(hand);
            if (stack.is(The_sword_of_aa.AMULET.get())) {
                amulet = stack.copy();
                stack.shrink(1);
                break;
            }
        }
        if (amulet == null) {
            return false;
        }

        // 以下数值与原版不死图腾 checkTotemDeathProtection 完全一致
        entity.setHealth(1.0F);
        entity.removeAllEffects();
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 1));     // 生命恢复 II / 45 秒
        entity.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1));       // 伤害吸收 II / 5 秒
        entity.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));  // 抗火 I / 40 秒
        entity.level().broadcastEntityEvent(entity, (byte) 35);                        // 粒子 + 音效（与原版图腾一致）

        // 屏幕中央的激活大图标：原版写死显示不死图腾，
        // 这里在 35 事件之后补发一个包，让客户端把图标覆盖成护身符
        if (entity instanceof ServerPlayer serverPlayer) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new AmuletActivationPacket());
        }

        return true;
    }
}
