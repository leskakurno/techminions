package com.techminions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Меню Сундука миньонов: сетка слотов зависит от размера сундука, ниже - инвентарь игрока. */
public class MinionChestMenu extends AbstractContainerMenu {
    private final MinionChestTier tier;
    private final ContainerLevelAccess access;
    private final int imageWidth;
    private final int imageHeight;

    /** Клиентская сторона: данные приходят из пакета (позиция и размер сундука). */
    public MinionChestMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, readTier(buf, inventory), null, ContainerLevelAccess.NULL);
    }

    private static MinionChestTier readTier(RegistryFriendlyByteBuf buf, Inventory inventory) {
        buf.readBlockPos();
        return MinionChestTier.byOrdinal(buf.readVarInt());
    }

    public MinionChestMenu(int id, Inventory inventory, MinionChestTier tier, IItemHandler handler, ContainerLevelAccess access) {
        super(ModRegistry.MINION_CHEST_MENU.get(), id);
        this.tier = tier;
        this.access = access;
        IItemHandler items = handler != null ? handler : new ItemStackHandler(tier.slots());

        int cols = tier.getColumns();
        int rows = tier.getRows();
        this.imageWidth = Math.max(cols * 18, 9 * 18) + 14;
        this.imageHeight = 114 + rows * 18;

        int chestX = (imageWidth - cols * 18) / 2 + 1;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                addSlot(new SlotItemHandler(items, c + r * cols, chestX + c * 18, 18 + r * 18));
            }
        }
        int invX = (imageWidth - 9 * 18) / 2 + 1;
        int invY = 18 + rows * 18 + 13;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                addSlot(new Slot(inventory, c + r * 9 + 9, invX + c * 18, invY + r * 18));
            }
        }
        for (int c = 0; c < 9; c++) {
            addSlot(new Slot(inventory, c, invX + c * 18, invY + 58));
        }
    }

    public MinionChestTier getTier() {
        return tier;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int chestSlots = tier.slots();
            if (index < chestSlots) {
                if (!moveItemStackTo(stack, chestSlots, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, chestSlots, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return access == ContainerLevelAccess.NULL
                || (stillValid(access, player, ModRegistry.MINION_CHEST_SMALL.get())
                || stillValid(access, player, ModRegistry.MINION_CHEST_MEDIUM.get())
                || stillValid(access, player, ModRegistry.MINION_CHEST_LARGE.get()));
    }
}
