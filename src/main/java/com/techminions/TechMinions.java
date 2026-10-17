package com.techminions;

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
        modEventBus.addListener(this::onLoadComplete);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /** Сундук миньонов виден воронкам, трубам и самим миньонам как обычный ItemHandler. */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ModRegistry.MINION_CHEST_BE.get(),
                (chest, side) -> chest.getItemHandler());
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
