package com.techminions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MinionMenu extends AbstractContainerMenu {
    private static final int STORAGE_START = 0;
    private static final int UPGRADE_START = MinionEntity.STORAGE_SIZE;
    private static final int EGG_START = UPGRADE_START + MinionEntity.UPGRADE_SLOTS;
    private static final int PLAYER_START = EGG_START + 1;
    private static final int PLAYER_END = PLAYER_START + 36;

    private final MinionEntity minion;
    private final Container storage;
    private final Container upgrades;
    private final Container eggs;

    /** Клиентский конструктор (контейнеры заполнятся пакетами слотов). */
    public MinionMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory,
                (MinionEntity) inventory.player.level().getEntity(buf.readVarInt()),
                new SimpleContainer(MinionEntity.STORAGE_SIZE),
                new SimpleContainer(MinionEntity.UPGRADE_SLOTS),
                new SimpleContainer(1));
    }

    /** Серверный конструктор. */
    public MinionMenu(int id, Inventory inventory, MinionEntity minion, Container storage, Container upgrades, Container eggs) {
        super(ModRegistry.MINION_MENU.get(), id);
        this.minion = minion;
        this.storage = storage;
        this.upgrades = upgrades;
        this.eggs = eggs;

        for (int i = 0; i < MinionEntity.STORAGE_SIZE; i++) {
            addSlot(new StorageSlot(storage, i, 80 + (i % 5) * 18, 94 + (i / 5) * 18));
        }
        for (int i = 0; i < MinionEntity.UPGRADE_SLOTS; i++) {
            addSlot(new UpgradeSlot(upgrades, i, 8 + (i % 3) * 18, 94 + (i / 3) * 18)); // 3 столбца x 2 ряда
        }
        addSlot(new EggSlot(eggs, 0, 62, 94));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 180 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 238));
        }
    }

    public MinionEntity getMinion() {
        return minion;
    }

    public int countUpgrades(UpgradeType type) {
        int count = 0;
        for (int i = 0; i < upgrades.getContainerSize(); i++) {
            if (upgrades.getItem(i).getItem() instanceof UpgradeItem item && item.getUpgradeType() == type) {
                count++;
            }
        }
        return count;
    }

    private int activeStorageSlots() {
        return MinionEntity.capacityFor(minion.getMinionLevel(), countUpgrades(UpgradeType.STORAGE));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer serverPlayer) || minion == null || !minion.isAlive()) {
            return false;
        }
        switch (id) {
            case 0 -> minion.tryLevelUp(serverPlayer);
            case 1 -> minion.takeAll(serverPlayer);
            case 2 -> minion.pickUp(serverPlayer);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return minion != null && minion.isAlive() && player.distanceToSqr(minion) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack copy = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            copy = stack.copy();
            if (index < PLAYER_START) {
                if (!moveItemStackTo(stack, PLAYER_START, PLAYER_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof HuntEggItem) {
                if (!moveItemStackTo(stack, EGG_START, EGG_START + 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (stack.getItem() instanceof UpgradeItem) {
                if (!moveItemStackTo(stack, UPGRADE_START, PLAYER_START, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == copy.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, stack);
        }
        return copy;
    }

    /** Слот хранилища: можно только забирать. Лишние слоты (сверх вместимости) скрыты. */
    private class StorageSlot extends Slot {
        StorageSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean isActive() {
            return getContainerSlot() < activeStorageSlots();
        }
    }

    /** Слот улучшения: только предметы-улучшения, по одному. */
    private class UpgradeSlot extends Slot {
        UpgradeSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof UpgradeItem;
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean mayPickup(Player player) {
            ItemStack current = getItem();
            if (current.getItem() instanceof UpgradeItem item && item.getUpgradeType() == UpgradeType.STORAGE) {
                // Нельзя убрать улучшение вместимости, если в скрытых слотах лежат предметы.
                int newCap = MinionEntity.capacityFor(minion.getMinionLevel(), countUpgrades(UpgradeType.STORAGE) - 1);
                for (int i = newCap; i < storage.getContainerSize(); i++) {
                    if (!storage.getItem(i).isEmpty()) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    /** Слот яйца охоты: виден только у охотников, принимает только яйцо их цели. */
    private class EggSlot extends Slot {
        EggSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (minion == null || !(stack.getItem() instanceof HuntEggItem)) {
                return false;
            }
            var eggType = HuntEggItem.getEntityType(stack);
            return minion.getMinionType().acceptsEgg(eggType);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean isActive() {
            return minion != null && minion.getMinionType().isHunter();
        }
    }
}
