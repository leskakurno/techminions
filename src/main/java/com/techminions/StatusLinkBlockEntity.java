package com.techminions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/** Хранит привязку Сигнальной лампы или Таблички статуса к миньону (по UUID). Читает только реестр, чанки не грузит. */
public class StatusLinkBlockEntity extends BlockEntity {
    private UUID minion;
    private UUID owner;

    public StatusLinkBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.LINK_BE.get(), pos, state);
    }

    public void bind(UUID minion, UUID owner) {
        this.minion = minion;
        this.owner = owner;
        setChanged();
    }

    public UUID getMinion() {
        return minion;
    }

    public UUID getOwner() {
        return owner;
    }

    /** 0 - нет привязки, 1 - работает, 2 - остановлен, 3 - нет связи (чанк не загружен). */
    public int currentStatus(MinecraftServer server) {
        MinionRegistry.Entry entry = entry(server);
        if (entry == null) {
            return 0;
        }
        long now = server.overworld().getGameTime();
        if (now - entry.lastSeen > 600) {
            return 3;
        }
        MinionState state = MinionState.byOrdinal(entry.state);
        return switch (state) {
            case PLACING, BREAKING, SPAWNING, KILLING, WORKING, PREPARING -> 1;
            default -> 2;
        };
    }

    public MinionRegistry.Entry entry(MinecraftServer server) {
        return minion == null ? null : MinionRegistry.get(server).get(minion);
    }

    public void serverTick(Level level, BlockPos pos, BlockState state) {
        if (!(level instanceof ServerLevel serverLevel) || level.getGameTime() % 20 != 3) {
            return;
        }
        if (state.getBlock() instanceof SignalLampBlock lamp) {
            lamp.update(serverLevel, pos, state, currentStatus(serverLevel.getServer()));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (minion != null) {
            tag.putUUID("Minion", minion);
        }
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        minion = tag.hasUUID("Minion") ? tag.getUUID("Minion") : null;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
}
