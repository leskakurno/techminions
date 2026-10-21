package com.techminions.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Данные одного миньона для окна просмотра (только чтение). */
public record MinionInfo(UUID id, String dimension, BlockPos pos, int type, int level, int state, int ageTicks,
                         int mined, int hourly, int slotsUsed, int slotsTotal, boolean locked, List<ItemCount> items, List<StopEvent> history) {

    /** Предмет и количество (без компонентов - только для показа). */
    public record ItemCount(String itemId, int count) {
    }

    /** Запись истории остановок: состояние и сколько тиков назад. */
    public record StopEvent(int state, int agoTicks) {
    }

    public static final int STALE_TICKS = 600; // миньон не сообщал о себе 30 с: чанк, скорее всего, не загружен

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUUID(id);
        buf.writeUtf(dimension);
        buf.writeBlockPos(pos);
        buf.writeVarInt(type);
        buf.writeVarInt(level);
        buf.writeVarInt(state);
        buf.writeVarInt(ageTicks);
        buf.writeVarInt(mined);
        buf.writeVarInt(hourly);
        buf.writeVarInt(slotsUsed);
        buf.writeVarInt(slotsTotal);
        buf.writeBoolean(locked);
        buf.writeVarInt(items.size());
        for (ItemCount item : items) {
            buf.writeUtf(item.itemId());
            buf.writeVarInt(item.count());
        }
        buf.writeVarInt(history.size());
        for (StopEvent event : history) {
            buf.writeVarInt(event.state());
            buf.writeVarInt(event.agoTicks());
        }
    }

    public static MinionInfo read(RegistryFriendlyByteBuf buf) {
        UUID id = buf.readUUID();
        String dimension = buf.readUtf();
        BlockPos pos = buf.readBlockPos();
        int type = buf.readVarInt();
        int level = buf.readVarInt();
        int state = buf.readVarInt();
        int age = buf.readVarInt();
        int mined = buf.readVarInt();
        int hourly = buf.readVarInt();
        int used = buf.readVarInt();
        int total = buf.readVarInt();
        boolean locked = buf.readBoolean();
        int n = buf.readVarInt();
        List<ItemCount> items = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            items.add(new ItemCount(buf.readUtf(), buf.readVarInt()));
        }
        int h = buf.readVarInt();
        List<StopEvent> history = new ArrayList<>(h);
        for (int i = 0; i < h; i++) {
            history.add(new StopEvent(buf.readVarInt(), buf.readVarInt()));
        }
        return new MinionInfo(id, dimension, pos, type, level, state, age, mined, hourly, used, total, locked, items, history);
    }
}
