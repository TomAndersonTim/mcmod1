package com.TAT.test.MCAA.the_sword_of_aa;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * 冒险协会之剑（Adventure Association Sword）。
 *
 * <p>攻击力被设为 {@link Float#MAX_VALUE}，即「无穷大」——任何生物的血量都远小于它，
 * 因此持剑攻击必定一击必杀。</p>
 *
 * <p>原版 {@link SwordItem} 只能通过 {@code int} 传入攻击力（上限约 21 亿），
 * 达不到「无穷大」，所以这里直接覆写 {@link #getDefaultAttributeModifiers} 挂上
 * 浮点上限的攻击力修饰符。手持时给予持有者生命恢复的逻辑见
 * {@code The_sword_of_aa#onLivingTick}。</p>
 */
public class AdventureSwordItem extends SwordItem {

    /** 与原版剑一致的攻击速度修饰值 */
    private static final float ATTACK_SPEED = -2.4F;

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
}
