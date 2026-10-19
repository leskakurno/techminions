package com.techminions;

import com.techminions.client.ClientNetwork;
import com.techminions.net.MinionsPayload;
import com.techminions.net.RequestMinionsPayload;
import com.techminions.net.ServerNetwork;
import com.techminions.net.TakeItemPayload;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(TechMinions.MOD_ID)
public class TechMinions {
    public static final String MOD_ID = "techminions";

    public TechMinions(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistry.BLOCKS.register(modEventBus);
        ModRegistry.ARMOR_MATERIALS.register(modEventBus);
        ModRegistry.ITEMS.register(modEventBus);
        ModRegistry.ENTITY_TYPES.register(modEventBus);
        ModRegistry.BLOCK_ENTITIES.register(modEventBus);
        ModRegistry.MENUS.register(modEventBus);
        ModRegistry.CREATIVE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.addListener(EggDropHandler::onDrops);
        modEventBus.addListener(this::registerAttributes);
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(this::registerPayloads);
        modEventBus.addListener(this::onLoadComplete);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /** Сундук миньонов виден воронкам, трубам и самим миньонам как обычный ItemHandler. */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModRegistry.MINION_CHEST_BE.get(),
                (chest, side) -> chest.getItemHandler());
    }

    /** Сеть: запрос списка миньонов (клиент -> сервер) и ответ-снимок (сервер -> клиент). Предметы по сети не ходят. */
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(RequestMinionsPayload.TYPE, RequestMinionsPayload.CODEC, ServerNetwork::handleRequest);
        registrar.playToServer(TakeItemPayload.TYPE, TakeItemPayload.CODEC, ServerNetwork::handleTake);
        registrar.playToClient(MinionsPayload.TYPE, MinionsPayload.CODEC, ClientNetwork::handleMinions);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModRegistry.MINION.get(), Mob.createMobAttributes().build());
    }

    /** Пишет в лог, какие типы миньонов включены (удобно проверять поддержку модов). */
    private void onLoadComplete(FMLLoadCompleteEvent event) {
        for (MinionType type : MinionType.values()) {
            LogUtils.getLogger().info("[TechMinions] {}: {}", type.getSerializedName(),
                    type.isAvailable() ? "enabled (" + type.describeSource() + ")"
                            : (type.isModLoaded() ? "DISABLED: resource item not found" : "mod not installed"));
        }
    }
}
