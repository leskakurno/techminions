package com.techminions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Хранит выставленные предметы (Пьедестал, Витрина, Стойка яиц). Только показ: предметы можно забрать обратно. */
public class DisplayBlockEntity extends BlockEntity {
    private final NonNullList<ItemStack> items;

    public DisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.DISPLAY_BE.get(), pos, state);
        int size = state.getBlock() instanceof DisplayBlock block ? block.getKind().slots() : 1;
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    public int size() {
        return items.size();
    }

    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    /** Кладёт один предмет в первую свободную ячейку. */
    public boolean put(ItemStack stack) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).isEmpty()) {
                items.set(i, stack.copyWithCount(1));
                sync();
                return true;
            }
        }
        return false;
    }

    /** Забирает последний выставленный предмет. */
    public ItemStack takeLast() {
        for (int i = items.size() - 1; i >= 0; i--) {
            if (!items.get(i).isEmpty()) {
                ItemStack stack = items.get(i);
                items.set(i, ItemStack.EMPTY);
                sync();
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public void dropAll(Level level, BlockPos pos) {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
            }
        }
        items.clear();
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
