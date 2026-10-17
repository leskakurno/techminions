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
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/** Хранилище Сундука миньонов. Доступно и воронкам/трубам через ItemHandler. */
public class MinionChestBlockEntity extends BlockEntity implements MenuProvider {
    private final MinionChestTier tier;
    private final ItemStackHandler items;

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

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Items")) {
            items.deserializeNBT(registries, tag.getCompound("Items"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
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
