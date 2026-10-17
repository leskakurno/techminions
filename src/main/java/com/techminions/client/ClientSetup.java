package com.techminions.client;

import com.techminions.ModRegistry;
import com.techminions.TechMinions;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.techminions.HuntEggItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.core.registries.BuiltInRegistries;
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
            if (egg != null) {
                return 0xFF000000 | egg.getColor(tintIndex);
            }
            if (type == null) {
                return 0xFFFFFFFF;
            }
            // У существа нет ванильного яйца (боссы модов): стабильный цвет из его id, два оттенка как у обычных яиц.
            int h = BuiltInRegistries.ENTITY_TYPE.getKey(type).toString().hashCode() * 0x9E3779B1;
            int base = 0x404040 | ((h >> 8) & 0xBF7F7F);
            return 0xFF000000 | (tintIndex == 0 ? base : (~base & 0xFFFFFF) | 0x303030);
        }, ModRegistry.HUNT_EGG.get());
    }
}
