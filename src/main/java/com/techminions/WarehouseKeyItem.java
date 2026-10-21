package com.techminions;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Ключ склада. ПКМ по Центральному складу - запомнить его. ПКМ по своему миньону - привязать миньона к этому складу
 * (миньон будет сам отправлять туда добычу). Присед + ПКМ по миньону - отвязать. Ключ сам предметов не хранит и не выдаёт.
 */
public class WarehouseKeyItem extends Item {
    public WarehouseKeyItem(Properties properties) {
        super(properties);
    }

    private static CompoundTag data(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom != null ? custom.copyTag() : new CompoundTag();
    }

    public static boolean hasTarget(ItemStack stack) {
        return data(stack).contains("WhPos");
    }

    public static BlockPos getPos(ItemStack stack) {
        return BlockPos.of(data(stack).getLong("WhPos"));
    }

    public static String getDimension(ItemStack stack) {
        return data(stack).getString("WhDim");
    }

    public static void setTarget(ItemStack stack, BlockPos pos, String dimension) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            tag.putLong("WhPos", pos.asLong());
            tag.putString("WhDim", dimension);
        });
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.techminions.warehouse_key").withStyle(ChatFormatting.GRAY));
        if (hasTarget(stack)) {
            BlockPos p = getPos(stack);
            tooltip.add(Component.translatable("tooltip.techminions.warehouse_key.target",
                    p.getX() + " " + p.getY() + " " + p.getZ()).withStyle(ChatFormatting.AQUA));
        }
    }

    /** Привязка миньона к складу из ключа. Вызывается на сервере из MinionEntity.mobInteract. */
    public static void bindMinion(Player player, ItemStack key, MinionEntity minion) {
        if (!minion.isOwnedBy(player)) {
            player.displayClientMessage(Component.translatable("message.techminions.tablet.not_yours"), true);
            return;
        }
        if (player.isShiftKeyDown()) {
            minion.setWarehouse(null, "");
            player.displayClientMessage(Component.translatable("message.techminions.warehouse.unlinked"), true);
            return;
        }
        if (!hasTarget(key)) {
            player.displayClientMessage(Component.translatable("message.techminions.warehouse.key_empty"), true);
            return;
        }
        String dim = getDimension(key);
        if (!dim.equals(minion.level().dimension().location().toString())) {
            player.displayClientMessage(Component.translatable("message.techminions.warehouse.other_dimension"), true);
            return;
        }
        minion.setWarehouse(getPos(key), dim);
        player.displayClientMessage(Component.translatable("message.techminions.warehouse.linked"), true);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "warehouse_linked");
        }
    }
}
