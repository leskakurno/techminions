package com.techminions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Хранилище Сундука миньонов. Доступно и воронкам/трубам через ItemHandler. */
public class MinionChestBlockEntity extends BlockEntity implements MenuProvider {
    private final MinionChestTier tier;
    private final ItemStackHandler items;
    // Вещи из старых (больших) сундуков, не поместившиеся после уменьшения размера: выбрасываются, а не пропадают.
    private final List<ItemStack> overflow = new ArrayList<>();
    private java.util.UUID owner; // кто поставил (нужно Центральному складу: привязывать миньонов может только владелец)

    public MinionChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModRegistry.MINION_CHEST_BE.get(), pos, state);
        this.tier = state.getBlock() instanceof MinionChestBlock block ? block.getTier() : MinionChestTier.SMALL;
        this.items = new ItemStackHandler(tier.slots()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    public MinionChestTier getTier() {
        return tier;
    }

    public IItemHandler getItemHandler() {
        return items;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack.copy());
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    /** Забирает всё содержимое (нужно при улучшении сундука до следующего размера). */
    public List<ItemStack> takeAllItems() {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                result.add(stack.copy());
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        return result;
    }

    /** Выбрасывает вещи, которые не влезли после уменьшения сундука (вызывается тикером блока). */
    public void dropOverflow(Level level, BlockPos pos) {
        if (overflow.isEmpty()) {
            return;
        }
        for (ItemStack stack : overflow) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, stack);
        }
        overflow.clear();
        setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        if (tag.contains("Items")) {
            // В старых сохранениях сундук мог быть больше: читаем во временный список и раскладываем по новым ячейкам.
            ItemStackHandler old = new ItemStackHandler(1);
            old.deserializeNBT(registries, tag.getCompound("Items"));
            for (int i = 0; i < items.getSlots(); i++) {
                items.setStackInSlot(i, ItemStack.EMPTY);
            }
            for (int i = 0; i < old.getSlots(); i++) {
                ItemStack stack = old.getStackInSlot(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (i < items.getSlots()) {
                    items.setStackInSlot(i, stack);
                } else {
                    ItemStack rest = ItemHandlerHelper.insertItem(items, stack, false);
                    if (!rest.isEmpty()) {
                        overflow.add(rest);
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
    }

    public java.util.UUID getOwner() {
        return owner;
    }

    public void setOwner(java.util.UUID owner) {
        this.owner = owner;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MinionChestMenu(id, inventory, tier, items, ContainerLevelAccess.create(level, worldPosition));
    }
}
