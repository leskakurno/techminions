package com.techminions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Улучшение «Ограничитель»: миньон останавливается, когда добытого (в хранилище и Сундуках миньонов) уже достаточно.
 * ПКМ по воздуху листает порог вперёд, с приседом - назад.
 */
public final class UpgradeLimit {
    public static final int[] LIMITS = {64, 256, 1024, 4096, 16384, 65536};
    private static final int DEFAULT_INDEX = 2;

    private UpgradeLimit() {
    }

    public static int index(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) {
            return DEFAULT_INDEX;
        }
        CompoundTag tag = custom.copyTag();
        return tag.contains("LimitIdx") ? Math.max(0, Math.min(LIMITS.length - 1, tag.getInt("LimitIdx"))) : DEFAULT_INDEX;
    }

    public static int target(ItemStack stack) {
        return LIMITS[index(stack)];
    }

    public static void cycle(Player player, ItemStack stack) {
        int next = (index(stack) + (player.isShiftKeyDown() ? LIMITS.length - 1 : 1)) % LIMITS.length;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("LimitIdx", next));
        player.displayClientMessage(Component.translatable("message.techminions.limiter.set", LIMITS[next]), true);
    }
}
