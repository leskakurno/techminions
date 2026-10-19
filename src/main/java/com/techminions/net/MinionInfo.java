package com.techminions.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Данные одного миньона для окна просмотра (только чтение). */
public record MinionInfo(UUID id, String dimension, BlockPos pos, int type, int level, int state, int ageTicks,
                         int mined, int slotsUsed, int slotsTotal, boolean locked, List<ItemCount> items) {

    /** Предмет и количество (без компонентов - только для показа). */
    public record ItemCount(String itemId, int count) {
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
        buf.writeVarInt(slotsUsed);
        buf.writeVarInt(slotsTotal);
        buf.writeBoolean(locked);
        buf.writeVarInt(items.size());
        for (ItemCount item : items) {
            buf.writeUtf(item.itemId());
            buf.writeVarInt(item.count());
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
        int used = buf.readVarInt();
        int total = buf.readVarInt();
        boolean locked = buf.readBoolean();
        int n = buf.readVarInt();
        List<ItemCount> items = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            items.add(new ItemCount(buf.readUtf(), buf.readVarInt()));
        }
        return new MinionInfo(id, dimension, pos, type, level, state, age, mined, used, total, locked, items);
    }
}
