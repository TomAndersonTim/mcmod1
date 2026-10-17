package com.TAT.test.MCAA.the_sword_of_aa;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * 服务端 -&gt; 客户端：让触发护身符的玩家在屏幕中央看到「护身符」的激活大图标。
 *
 * <p>原版使用不死图腾时，客户端 {@code ClientPacketListener#handleEntityEvent} 的
 * {@code case 35} 会调用写死了 {@code Items.TOTEM_OF_UNDYING} 的 {@code findTotem}，
 * 所以屏幕中央永远是不死图腾。护身符触发时服务端依旧是先广播原版的 35 事件
 * （粒子 / 音效 / 跟拍效果与原版完全一致），紧接着再补发本包，把激活图标覆盖成护身符。</p>
 */
public class AmuletActivationPacket {

    public AmuletActivationPacket() {
    }

    public AmuletActivationPacket(FriendlyByteBuf buf) {
        // 无字段
    }

    public void encode(FriendlyByteBuf buf) {
        // 无字段
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientAmuletActivation::show));
        context.setPacketHandled(true);
    }
}
