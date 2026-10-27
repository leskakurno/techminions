package com.techminions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Ежедневная цель («Стабильная смена») и серия («Серия мастера»). Считаются циклы миньонов игрока, пока он в сети.
 * Награда за день выдаётся один раз; серия сбрасывается, если пропущен день. Рубежи серии: 3, 7 и 14 дней.
 */
public final class DailyGoals {
    private static final int[] MILESTONES = {3, 7, 14};

    private DailyGoals() {
    }

    public static void onCycle(ServerPlayer player) {
        if (!Config.DAILY_GOALS.get() || player.getServer() == null) {
            return;
        }
        PlayerStats stats = PlayerStats.get(player.getServer());
        CompoundTag tag = stats.of(player.getUUID());
        long today = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        if (tag.getLong("GoalDay") != today) {
            tag.putLong("GoalDay", today);
            tag.putInt("GoalCycles", 0);
            tag.putBoolean("GoalDone", false);
        }
        if (tag.getBoolean("GoalDone")) {
            return;
        }
        int cycles = tag.getInt("GoalCycles") + 1;
        tag.putInt("GoalCycles", cycles);
        if (cycles >= Config.DAILY_GOAL_CYCLES.get()) {
            tag.putBoolean("GoalDone", true);
            complete(player, tag, today);
        }
        stats.setDirty();
    }

    private static void complete(ServerPlayer player, CompoundTag tag, long today) {
        long last = tag.getLong("StreakDay");
        int streak = tag.getInt("Streak");
        if (last == today - 1) {
            streak++;
        } else if (last != today) {
            streak = 1;
        }
        tag.putLong("StreakDay", today);
        tag.putInt("Streak", streak);
        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModRegistry.MINIONITE_SHARD.get(), 2));
        player.sendSystemMessage(Component.translatable("message.techminions.goal.done", streak));
        ModAdvancements.award(player, "daily_goal");
        for (int milestone : MILESTONES) {
            if (streak == milestone) {
                ItemStack cert = new ItemStack(ModRegistry.EXPEDITION_CERTIFICATE.get());
                cert.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                        Component.translatable("item.techminions.expedition_certificate.streak", milestone));
                ItemHandlerHelper.giveItemToPlayer(player, cert);
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModRegistry.MINIONITE_SHARD.get(), milestone));
                player.sendSystemMessage(Component.translatable("message.techminions.streak", milestone));
                if (milestone >= 7) {
                    ModAdvancements.award(player, "streak_week");
                }
            }
        }
    }

    /** Сброс цели игрока (команда поддержки). */
    public static void reset(net.minecraft.server.MinecraftServer server, java.util.UUID player) {
        PlayerStats stats = PlayerStats.get(server);
        CompoundTag tag = stats.of(player);
        tag.putInt("GoalCycles", 0);
        tag.putBoolean("GoalDone", false);
        stats.setDirty();
    }
}
