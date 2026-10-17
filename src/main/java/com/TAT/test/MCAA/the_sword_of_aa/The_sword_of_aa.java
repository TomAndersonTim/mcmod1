package com.TAT.test.MCAA.the_sword_of_aa;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(The_sword_of_aa.MODID)
public class The_sword_of_aa {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "the_sword_of_aa";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "the_sword_of_aa" namespace
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "the_sword_of_aa" namespace
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "the_sword_of_aa" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // 护身符（Amulet）：一枚独立物品，拥有与不死图腾完全相同的效果，同时作为 MCAA 分类的图标
    // 堆叠上限 1、稀有度「少见」——与不死图腾保持一致
    public static final RegistryObject<Item> AMULET = ITEMS.register("amulet", () -> new AmuletItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    // 冒险协会之剑：攻击力「无穷大」（Float.MAX_VALUE）一击必杀；耐久无穷大（见 AdventureSwordItem）
    public static final RegistryObject<Item> ADVENTURE_SWORD = ITEMS.register("adventure_sword",
            () -> new AdventureSwordItem(Tiers.NETHERITE, new Item.Properties().rarity(Rarity.EPIC).fireResistant()));

    // The mod's own creative tab, with the id "the_sword_of_aa:mcaa" and the display name "MCAA"
    public static final RegistryObject<CreativeModeTab> MCAA_TAB = CREATIVE_MODE_TABS.register("mcaa", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.the_sword_of_aa.mcaa"))
            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
            .icon(() -> AMULET.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                // MCAA 分类：冒险协会之剑 + 护身符
                output.accept(ADVENTURE_SWORD.get());
                output.accept(AMULET.get());
            }).build());

    public The_sword_of_aa() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // 注册自定义网络数据包
        ModNetwork.register();

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // 护身符：主手或副手持有护身符时，濒死不会死亡，取而代之的是复刻不死图腾的效果
    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (AmuletItem.tryProtectFromDeath(event.getEntity(), event.getSource())) {
            event.setCanceled(true);
        }
    }

    // 冒险协会之剑：主手或副手手持时，持续给予持有者生命恢复
    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        // 只在逻辑服务端施加，客户端会自动通过网络同步
        if (entity.level().isClientSide()) {
            return;
        }
        boolean holding = entity.getMainHandItem().is(ADVENTURE_SWORD.get())
                || entity.getOffhandItem().is(ADVENTURE_SWORD.get());
        if (holding) {
            // 每 tick 刷新一次（每次 2 秒），因此离开手后最多 2 秒自动消失
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 255, false, false, true));
        }
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
