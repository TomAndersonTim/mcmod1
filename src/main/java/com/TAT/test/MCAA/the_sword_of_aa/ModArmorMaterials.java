package com.TAT.test.MCAA.the_sword_of_aa;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * 冒险协会套装的盔甲材质。
 *
 * <p>「防御无穷」并不是靠 {@code ARMOR} 属性堆出来的：原版减伤公式
 * （{@code CombatRules#getDamageAfterAbsorb}）会把有效护甲值截断在 20 点，
 * 也就是说护甲值再高，最多也只能减伤 80%。所以这里把护甲值设成一个必定触顶的
 * 数值，真正的「无穷防御」由 {@code The_sword_of_aa#onLivingHurt} 实现：
 * 穿齐整套时直接免伤。</p>
 *
 * <p>贴图：{@link ArmorMaterial#getName()} 会被 {@code HumanoidArmorLayer} 拼成
 * {@code <命名空间>:textures/models/armor/<名字>_layer_1.png}（护腿用 _layer_2）。
 * 这里返回 {@code "the_sword_of_aa:adventure"}，对应
 * {@code src/main/resources/assets/the_sword_of_aa/textures/models/armor/adventure_layer_1.png}
 * 与 {@code adventure_layer_2.png}。这两张目前是原版下界合金盔甲的底板，等着替换成自绘贴图。</p>
 */
public final class ModArmorMaterials {

    /** 单件护甲值：远超原版 20 点上限，穿一件就能吃满 80% 减伤 */
    private static final int DEFENSE = 25;
    /** 盔甲韧性 */
    private static final float TOUGHNESS = 10.0F;
    /** 击退抗性（每件 +1.0，穿齐后完全免疫击退） */
    private static final float KNOCKBACK_RESISTANCE = 1.0F;

    public static final ArmorMaterial ADVENTURE = new ArmorMaterial() {

        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            // 耐久实际无穷（见 InfiniteArmorItem#canBeDepleted），这里只给个下界合金级的正常数值
            return switch (type) {
                case HELMET -> 407;
                case CHESTPLATE -> 592;
                case LEGGINGS -> 555;
                case BOOTS -> 481;
            };
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return DEFENSE;
        }

        @Override
        public int getEnchantmentValue() {
            return 30;
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_NETHERITE;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.NETHERITE_INGOT);
        }

        @Override
        public String getName() {
            // 对应 assets/the_sword_of_aa/textures/models/armor/adventure_layer_{1,2}.png
            return "the_sword_of_aa:adventure";
        }

        @Override
        public float getToughness() {
            return TOUGHNESS;
        }

        @Override
        public float getKnockbackResistance() {
            return KNOCKBACK_RESISTANCE;
        }
    };

    private ModArmorMaterials() {
    }
}
