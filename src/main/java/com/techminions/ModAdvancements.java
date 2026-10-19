package com.techminions;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Выдача достижений мода, которые нельзя поймать обычными условиями (поставил миньона, макс. уровень и т.д.). */
public final class ModAdvancements {
    private ModAdvancements() {
    }

    /** Выдаёт достижение techminions:<id> (у него одно условие "done"). Если такого нет - ничего не делает. */
    public static void award(ServerPlayer player, String id) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        AdvancementHolder holder = server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(TechMinions.MOD_ID, id));
        if (holder != null) {
            player.getAdvancements().award(holder, "done");
        }
    }
}
