package com.TAT.test.MCAA.the_sword_of_aa;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

/**
 * 冒险协会套装的一件。
 *
 * <p>耐久无穷大（见 {@link #canBeDepleted()}）。「防御无穷」的判定见
 * {@link #isWearingFullSet(LivingEntity)}，实际免伤逻辑在
 * {@code The_sword_of_aa#onLivingHurt}。</p>
 */
public class InfiniteArmorItem extends ArmorItem {

    /** 头、胸、腿、脚四个槽位，用于判断是否穿齐整套 */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public InfiniteArmorItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    /**
     * 耐久无穷大：返回 false 表示该物品不可被消耗/损坏，
     * {@code hurtAndBreak} 不再扣耐久，也不会显示耐久条。
     */
    @Override
    public boolean canBeDepleted() {
        return false;
    }

    /**
     * 原版 {@code Item#isEnchantable} 的判定依赖「可损坏」，上面关掉之后会连带
     * 失去附魔台附魔能力，这里显式保留。
     */
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    /** 头、胸、腿、脚是否全部穿着本套装。 */
    public static boolean isWearingFullSet(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (!(entity.getItemBySlot(slot).getItem() instanceof InfiniteArmorItem)) {
                return false;
            }
        }
        return true;
    }
}
