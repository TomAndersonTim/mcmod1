package com.TAT.test.MCAA.the_sword_of_aa;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 本模组的网络通道。
 */
public final class ModNetwork {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(The_sword_of_aa.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private ModNetwork() {
    }

    /** 在模组构造期调用：注册所有自定义数据包 */
    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, AmuletActivationPacket.class,
                AmuletActivationPacket::encode,
                AmuletActivationPacket::new,
                AmuletActivationPacket::handle);
    }
}
