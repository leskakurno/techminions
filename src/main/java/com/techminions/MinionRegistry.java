package com.techminions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Реестр миньонов мира: каждый миньон периодически сообщает сюда своё состояние и содержимое хранилища.
 * Отсюда читают вкладка «Миньоны» в инвентаре и Планшет миньонов - им не нужно, чтобы чанк миньона был загружен.
 * Это только СНИМОК для просмотра: достать из него предметы невозможно.
 */
public class MinionRegistry extends SavedData {
    private static final String NAME = "techminions_registry";
    private static final int MAX_ITEM_KINDS = 54;

    /** Снимок одного миньона. */
    public static class Entry {
        public final UUID id;
        public UUID owner;
        public String ownerName = "";
        public String dimension = "";
        public BlockPos pos = BlockPos.ZERO;
        public int type;
        public int level = 1;
        public int state;
        public int mined;
        public int hourly;
        public final List<long[]> history = new ArrayList<>(); // {состояние, игровое время}
        public int slotsUsed;
        public int slotsTotal;
        public long lastSeen;
        public final Map<String, Integer> items = new LinkedHashMap<>();

        public Entry(UUID id) {
            this.id = id;
        }

        public void addItem(String itemId, int count) {
            if (count <= 0) {
                return;
            }
            if (items.size() >= MAX_ITEM_KINDS && !items.containsKey(itemId)) {
                return;
            }
            items.merge(itemId, count, Integer::sum);
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            tag.putUUID("Id", id);
            if (owner != null) {
                tag.putUUID("Owner", owner);
            }
            tag.putString("OwnerName", ownerName);
            tag.putString("Dim", dimension);
            tag.putLong("Pos", pos.asLong());
            tag.putInt("Type", type);
            tag.putInt("Level", level);
            tag.putInt("State", state);
            tag.putInt("Mined", mined);
            tag.putInt("Hourly", hourly);
            long[] flat = new long[history.size() * 2];
            for (int i = 0; i < history.size(); i++) {
                flat[i * 2] = history.get(i)[0];
                flat[i * 2 + 1] = history.get(i)[1];
            }
            tag.putLongArray("Hist", flat);
            tag.putInt("Used", slotsUsed);
            tag.putInt("Total", slotsTotal);
            tag.putLong("Seen", lastSeen);
            ListTag list = new ListTag();
            for (Map.Entry<String, Integer> e : items.entrySet()) {
                CompoundTag item = new CompoundTag();
                item.putString("id", e.getKey());
                item.putInt("n", e.getValue());
                list.add(item);
            }
            tag.put("Items", list);
            return tag;
        }

        static Entry load(CompoundTag tag) {
            Entry e = new Entry(tag.getUUID("Id"));
            if (tag.hasUUID("Owner")) {
                e.owner = tag.getUUID("Owner");
            }
            e.ownerName = tag.getString("OwnerName");
            e.dimension = tag.getString("Dim");
            e.pos = BlockPos.of(tag.getLong("Pos"));
            e.type = tag.getInt("Type");
            e.level = tag.getInt("Level");
            e.state = tag.getInt("State");
            e.mined = tag.getInt("Mined");
            e.hourly = tag.getInt("Hourly");
            long[] flat = tag.getLongArray("Hist");
            for (int i = 0; i + 1 < flat.length; i += 2) {
                e.history.add(new long[]{flat[i], flat[i + 1]});
            }
            e.slotsUsed = tag.getInt("Used");
            e.slotsTotal = tag.getInt("Total");
            e.lastSeen = tag.getLong("Seen");
            ListTag list = tag.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag item = list.getCompound(i);
                e.items.put(item.getString("id"), item.getInt("n"));
            }
            return e;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public static MinionRegistry get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MinionRegistry::new, MinionRegistry::load, null), NAME);
    }

    public static MinionRegistry load(CompoundTag tag, HolderLookup.Provider registries) {
        MinionRegistry registry = new MinionRegistry();
        ListTag list = tag.getList("Minions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Entry e = Entry.load(list.getCompound(i));
            registry.entries.put(e.id, e);
        }
        return registry;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            list.add(e.save());
        }
        tag.put("Minions", list);
        return tag;
    }

    public void update(Entry entry) {
        entries.put(entry.id, entry);
        setDirty();
    }

    public void remove(UUID id) {
        if (entries.remove(id) != null) {
            setDirty();
        }
    }

    public Entry get(UUID id) {
        return entries.get(id);
    }

    public List<Entry> ownedBy(UUID owner) {
        List<Entry> result = new ArrayList<>();
        for (Entry e : entries.values()) {
            if (owner.equals(e.owner)) {
                result.add(e);
            }
        }
        result.sort((a, b) -> Long.compare(a.pos.asLong(), b.pos.asLong()));
        return result;
    }
}
