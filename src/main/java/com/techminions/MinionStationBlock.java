package com.techminions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Станция разборки. ПКМ предметом-миньоном: миньон разбирается, игрок получает назад ядро и половину материалов,
 * потраченных на его прокачку (доля задаётся в конфиге). Операция одним действием: предмет забирается и сразу же
 * выдаются материалы, поэтому дублировать нечего.
 */
public class MinionStationBlock extends Block {
    public MinionStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof MinionItem minionItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        MinionType type = minionItem.getMinionType();
        int minionLevel = 1;
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom != null) {
            CompoundTag tag = custom.copyTag();
            if (tag.contains("Level")) {
                minionLevel = Math.max(1, Math.min(MinionEntity.MAX_LEVEL, tag.getInt("Level")));
            }
        }
        // Материалы, потраченные на уровни со II по текущий (как в MinionEntity.costForNext), и доля возврата.
        double spent = 0;
        for (int l = 1; l < minionLevel; l++) {
            spent += Math.max(1, Math.round(type.getUpgradeBase() * l * Config.UPGRADE_COST_MULTIPLIER.get()));
        }
        int refund = (int) Math.floor(spent * Config.DISMANTLE_REFUND.get());
        Item costItem = type.getCostItem();
        Item core = switch (type.getTier()) {
            case 1 -> ModRegistry.CORE_BASIC.get();
            case 2 -> ModRegistry.CORE_REINFORCED.get();
            default -> ModRegistry.CORE_ELITE.get();
        };
        stack.shrink(1); // сначала забираем миньона, потом выдаём материалы
        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(core));
        while (refund > 0 && costItem != net.minecraft.world.item.Items.AIR) {
            int n = Math.min(refund, new ItemStack(costItem).getMaxStackSize());
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(costItem, n));
            refund -= n;
        }
        player.displayClientMessage(Component.translatable("message.techminions.station.dismantled"), true);
        if (player instanceof ServerPlayer serverPlayer) {
            ModAdvancements.award(serverPlayer, "dismantle");
        }
        return ItemInteractionResult.CONSUME;
    }
}
