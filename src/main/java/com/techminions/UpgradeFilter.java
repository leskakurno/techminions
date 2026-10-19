package com.techminions;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Фильтр улучшения «Умная логистика». Список предметов хранится в самом предмете-улучшении.
 * Настройка: фильтр в одной руке. В другой руке предмет - добавить/убрать его из списка;
 * пустая другая рука + присед - переключить «белый/чёрный список»; пустая другая рука без приседа - показать список.
 * Пустой список ничего не ограничивает.
 */
public final class UpgradeFilter {
    private UpgradeFilter() {
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom != null ? custom.copyTag() : new CompoundTag();
    }

    public static Set<String> ids(ItemStack filter) {
        Set<String> set = new LinkedHashSet<>();
        ListTag list = data(filter).getList("Filter", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            set.add(list.getString(i));
        }
        return set;
    }

    public static boolean isBlacklist(ItemStack filter) {
        return data(filter).getBoolean("Black");
    }

    /** Пропускает ли этот фильтр предмет. */
    public static boolean allows(ItemStack filter, ItemStack item) {
        Set<String> ids = ids(filter);
        if (ids.isEmpty()) {
            return true;
        }
        boolean listed = ids.contains(BuiltInRegistries.ITEM.getKey(item.getItem()).toString());
        return isBlacklist(filter) ? !listed : listed;
    }

    private static void save(ItemStack filter, Set<String> ids, boolean black) {
        ListTag list = new ListTag();
        for (String id : ids) {
            list.add(StringTag.valueOf(id));
        }
        CustomData.update(DataComponents.CUSTOM_DATA, filter, tag -> {
            tag.put("Filter", list);
            tag.putBoolean("Black", black);
        });
    }

    /** Настройка фильтра игроком (вызывается на сервере из UpgradeItem.use). */
    public static void interact(Player player, InteractionHand hand, ItemStack filter) {
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);
        Set<String> ids = ids(filter);
        boolean black = isBlacklist(filter);
        if (!other.isEmpty()) {
            String id = BuiltInRegistries.ITEM.getKey(other.getItem()).toString();
            if (!ids.remove(id)) {
                if (ids.size() >= 18) {
                    player.displayClientMessage(Component.translatable("message.techminions.filter.full"), true);
                    return;
                }
                ids.add(id);
                player.displayClientMessage(Component.translatable("message.techminions.filter.added", other.getHoverName()), true);
            } else {
                player.displayClientMessage(Component.translatable("message.techminions.filter.removed", other.getHoverName()), true);
            }
            save(filter, ids, black);
        } else if (player.isShiftKeyDown()) {
            save(filter, ids, !black);
            player.displayClientMessage(Component.translatable(!black ? "message.techminions.filter.mode_black"
                    : "message.techminions.filter.mode_white"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.techminions.filter.count",
                    ids.size(), Component.translatable(black ? "tooltip.techminions.filter.black" : "tooltip.techminions.filter.white")), true);
        }
    }
}
