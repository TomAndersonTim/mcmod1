package com.TAT.test.MCAA.the_sword_of_aa;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

/**
 * 仅客户端代码。通过 {@link AmuletActivationPacket} 里的
 * {@code DistExecutor.unsafeRunWhenOn(Dist.CLIENT, ...)} 惰性引用，
 * 因此该类只会在客户端被加载，专用服务器不会触碰它。
 */
public final class ClientAmuletActivation {

    private ClientAmuletActivation() {
    }

    /** 在屏幕中央播放「护身符」的物品激活动画（原版图腾动画的同款效果）。 */
    public static void show() {
        Minecraft.getInstance().gameRenderer.displayItemActivation(new ItemStack(The_sword_of_aa.AMULET.get()));
    }
}
