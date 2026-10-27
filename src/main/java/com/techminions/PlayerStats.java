package com.techminions;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Постоянные данные игроков и мира для новых механик v4: ежедневные цели, серии, флаги «получено один раз»,
 * серверные события. Хранится отдельно от самих игроков, поэтому не теряется при смерти и не дублируется.
 */
public class PlayerStats extends SavedData {
    private static final String NAME = "techminions_players";

    private final Map<UUID, CompoundTag> players = new HashMap<>();
    private CompoundTag global = new CompoundTag();

    public static PlayerStats get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PlayerStats::new, PlayerStats::load, null), NAME);
    }

    public static PlayerStats load(CompoundTag tag, HolderLookup.Provider registries) {
        PlayerStats stats = new PlayerStats();
        CompoundTag list = tag.getCompound("Players");
        for (String key : list.getAllKeys()) {
            try {
                stats.players.put(UUID.fromString(key), list.getCompound(key));
            } catch (IllegalArgumentException ignored) {
                // повреждённый ключ пропускаем
            }
        }
        stats.global = tag.getCompound("Global");
        return stats;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag list = new CompoundTag();
        for (Map.Entry<UUID, CompoundTag> e : players.entrySet()) {
            list.put(e.getKey().toString(), e.getValue());
        }
        tag.put("Players", list);
        tag.put("Global", global);
        return tag;
    }

    /** Изменяемые данные игрока (после изменений вызвать setDirty()). */
    public CompoundTag of(UUID player) {
        return players.computeIfAbsent(player, id -> new CompoundTag());
    }

    public CompoundTag global() {
        return global;
    }

    public void forget(UUID player) {
        if (players.remove(player) != null) {
            setDirty();
        }
    }

    public boolean flag(UUID player, String key) {
        return of(player).getBoolean(key);
    }

    /** Ставит флаг и возвращает true, если он ставится впервые (для наград «один раз»). */
    public boolean firstTime(UUID player, String key) {
        CompoundTag tag = of(player);
        if (tag.getBoolean(key)) {
            return false;
        }
        tag.putBoolean(key, true);
        setDirty();
        return true;
    }

    static boolean has(CompoundTag tag, String key) {
        return tag.contains(key, Tag.TAG_ANY_NUMERIC);
    }
}
