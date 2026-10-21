package com.techminions;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

/**
 * Проверка конфига при запуске сервера: неверные id и теги попадают в лог понятным предупреждением, а не молча
 * ломают функцию. Ничего не меняет и не падает: только сообщает.
 */
public final class ConfigDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();

    private ConfigDiagnostics() {
    }

    public static void onServerStarting(ServerStartingEvent event) {
        try {
            run();
        } catch (Exception e) {
            LOGGER.warn("[TechMinions] Config check failed: {}", e.toString());
        }
    }

    private static void run() {
        int problems = 0;

        for (String entry : Config.machineBlocks()) {
            if (entry.contains(":")) {
                ResourceLocation id = ResourceLocation.tryParse(entry);
                if (id == null) {
                    LOGGER.warn("[TechMinions] machineBlocks: '{}' is not a valid block id", entry);
                    problems++;
                } else if (!BuiltInRegistries.BLOCK.containsKey(id) && ModList.get().isLoaded(id.getNamespace())) {
                    LOGGER.warn("[TechMinions] machineBlocks: block '{}' does not exist although mod '{}' is installed",
                            entry, id.getNamespace());
                    problems++;
                }
            } else if (!entry.matches("[a-z0-9_.-]+")) {
                LOGGER.warn("[TechMinions] machineBlocks: '{}' is not a valid mod id", entry);
                problems++;
            }
        }

        for (String tag : Config.csv(Config.UNIVERSAL_TAGS.get())) {
            ResourceLocation id = ResourceLocation.tryParse(tag);
            if (id == null) {
                LOGGER.warn("[TechMinions] universalMinerTags: '{}' is not a valid tag id", tag);
                problems++;
            } else if (BuiltInRegistries.ITEM.getTag(ItemTags.create(id)).isEmpty()) {
                LOGGER.info("[TechMinions] universalMinerTags: tag '{}' is empty or unknown (fine if the mod is not installed)", tag);
            }
        }

        for (String item : Config.csv(Config.UNIVERSAL_BLACKLIST.get())) {
            ResourceLocation id = ResourceLocation.tryParse(item);
            if (id == null || (!BuiltInRegistries.ITEM.containsKey(id) && ModList.get().isLoaded(id.getNamespace()))) {
                LOGGER.warn("[TechMinions] universalMinerBlacklist: unknown item '{}'", item);
                problems++;
            }
        }

        String raw = Config.HUNT_OVERRIDES.get();
        if (!raw.isBlank()) {
            for (String entry : raw.split(";")) {
                if (entry.isBlank()) {
                    continue;
                }
                String[] kv = entry.trim().split("=");
                String[] values = kv.length == 2 ? kv[1].trim().split(",") : new String[0];
                boolean formatOk = values.length == 3;
                if (formatOk) {
                    for (String v : values) {
                        try {
                            Integer.parseInt(v.trim());
                        } catch (NumberFormatException e) {
                            formatOk = false;
                        }
                    }
                }
                if (!formatOk) {
                    LOGGER.warn("[TechMinions] huntOverrides: bad entry '{}' (expected entity_id=spawnSeconds,killSeconds,rewardMultiplier)", entry.trim());
                    problems++;
                    continue;
                }
                ResourceLocation id = ResourceLocation.tryParse(kv[0].trim());
                if (id == null || (!BuiltInRegistries.ENTITY_TYPE.containsKey(id) && ModList.get().isLoaded(id.getNamespace()))) {
                    LOGGER.warn("[TechMinions] huntOverrides: unknown entity '{}'", kv[0].trim());
                    problems++;
                }
            }
        }

        if (problems == 0) {
            LOGGER.info("[TechMinions] Config check passed");
        } else {
            LOGGER.warn("[TechMinions] Config check found {} problem(s), see messages above", problems);
        }
    }
}
