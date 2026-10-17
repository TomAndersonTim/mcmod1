package com.TAT.test.MCAA.the_sword_of_aa;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;

/**
 * 冒险协会之剑（Adventure Association Sword）。
 *
 * <p><b>攻击力无穷大</b>：设为 {@link Float#MAX_VALUE}，任何生物的血量都远小于它，
 * 因此持剑攻击必定一击必杀。原版 {@link SwordItem} 只能通过 {@code int} 传入攻击力
 * （上限约 21 亿），达不到「无穷大」，所以这里直接覆写
 * {@link #getDefaultAttributeModifiers} 挂上浮点上限的攻击力修饰符。手持时给予持有者
 * 生命恢复的逻辑见 {@code The_sword_of_aa#onLivingTick}。</p>
 *
 * <p><b>攻击速度极快</b>：见 {@link #ATTACK_SPEED}。玩家基础攻速为 4.0，加上修饰值后
 * 达到 100.0，攻击冷却 {@code 1/100 秒}（0.2 tick），已经低于引擎按 tick 计的最小粒度，
 * 即「每 tick 都能打出满蓄力一击」，实际就是原版的攻速上限。</p>
 *
 * <p><b>右键清场</b>：右键使用会在整个维度内清除除使用者以外的所有生物，见
 * {@link #purgeLivingEntities}。</p>
 */
public class AdventureSwordItem extends SwordItem {

    /**
     * 攻击速度修饰值。
     *
     * <p>玩家基础攻速 4.0 + 本值 96.0 = 100.0。原版剑是 -2.4（合 1.6），这里调到了「极快」。
     * 实际冷却是 {@code 20 / 攻速} 个 tick = 0.2 tick，小于 1 tick 后就不再构成限制，
     * 所以再往上加也不会更快了（可以理解为已经顶到引擎上限）。</p>
     */
    private static final float ATTACK_SPEED = 96.0F;

    public AdventureSwordItem(Tier tier, Properties properties) {
        // 攻击力参数传 0：实际数值由下面的 getDefaultAttributeModifiers 决定
        super(tier, 0, ATTACK_SPEED, properties);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        if (slot != EquipmentSlot.MAINHAND) {
            return super.getDefaultAttributeModifiers(slot);
        }
        return ImmutableMultimap.<Attribute, AttributeModifier>builder()
                // 无穷大攻击力
                .put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                        Float.MAX_VALUE, AttributeModifier.Operation.ADDITION))
                // 极快攻击速度
                .put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier",
                        ATTACK_SPEED, AttributeModifier.Operation.ADDITION))
                .build();
    }

    /**
     * 耐久无穷大：返回 false 表示该物品不可被消耗/损坏。
     *
     * <p>{@code ItemStack.isDamageableItem()} 与 Forge 的
     * {@code IForgeItem#isDamageable(ItemStack)} 都以此为准，返回 false 后
     * {@code ItemStack.hurtAndBreak} 不再扣耐久，物品也不会显示耐久条。</p>
     */
    @Override
    public boolean canBeDepleted() {
        return false;
    }

    /**
     * 原版 {@code Item#isEnchantable} 的判定依赖「可损坏」，上面关掉之后会连带
     * 失去附魔台附魔能力，这里显式保留（附魔值仍取下界合金级的 15）。
     */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    /**
     * 右键清场：清除当前维度内除使用者以外的所有生物。
     *
     * <p>只在逻辑服务端执行（客户端那边 {@code level} 是 {@link net.minecraft.client.multiplayer.ClientLevel}，
     * 会跳过 {@code instanceof ServerLevel} 的判断）。</p>
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            purgeLivingEntities(serverLevel, player);
        }
        // 客户端返回 SUCCESS（触发挥手动画），服务端返回 CONSUME
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    /**
     * 清除 {@code level} 中除 {@code user} 以外的所有 {@link LivingEntity}。
     *
     * <p>用的是 {@link LivingEntity#kill()}（内部就是
     * {@code hurt(genericKill, Float.MAX_VALUE)}）。选它而不是 {@code discard()}，
     * 是为了让生物「正常死亡」——掉落物、经验、死亡提示都照常。同时
     * {@code generic_kill} 在 {@code #minecraft:bypasses_invulnerability} 标签里，
     * 会穿过冒险协会套装（{@code onLivingHurt} 里对该标签直接放行）和一切无敌判定。</p>
     *
     * <p>注意：持有护身符的生物会被护身符救下来（一次），
     * 这和不死图腾的行为一致，是刻意保留的。</p>
     */
    public static void purgeLivingEntities(ServerLevel level, Entity user) {
        UUID userId = user.getUUID();
        // 先收集再处理：kill() 会把实体从实体列表里移除，边遍历边删会破坏迭代
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity living && !living.getUUID().equals(userId)) {
                targets.add(living);
            }
        }
        for (LivingEntity target : targets) {
            target.kill();
        }
    }
}
