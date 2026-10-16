package com.techminions.client;

import com.techminions.ModRegistry;
import com.techminions.TechMinions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.techminions.HuntEggItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = TechMinions.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModRegistry.MINION.get(), MinionRenderer::new);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModRegistry.MINION_MENU.get(), MinionScreen::new);
    }

    /** Яйцо охоты красится цветами ванильного яйца того же моба (та же текстура, что и у яйца призыва). */
    @SubscribeEvent
    public static void registerColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> {
            EntityType<?> type = HuntEggItem.getEntityType(stack);
            SpawnEggItem egg = type != null ? SpawnEggItem.byId(type) : null;
            return egg != null ? (0xFF000000 | egg.getColor(tintIndex)) : 0xFFFFFFFF;
        }, ModRegistry.HUNT_EGG.get());
    }
}
