package com.techminions;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Блок заказов. ПКМ Контрактной печатью принимает случайный контракт на ванильный ресурс (печать расходуется).
 * ПКМ нужными предметами сдаёт их; перед списанием проверяется точное количество. Награда выдаётся один раз.
 */
public class OrderBlock extends Block implements EntityBlock {
    /** {предмет, сколько, награда в осколках минионита} */
    private static final Object[][] ORDERS = {
            {Items.COBBLESTONE, 256, 1}, {Items.COAL, 128, 1}, {Items.SAND, 192, 1}, {Items.GRAVEL, 192, 1},
            {Items.CLAY_BALL, 96, 1}, {Items.OAK_LOG, 128, 1}, {Items.WHEAT, 128, 1}, {Items.SUGAR_CANE, 96, 1},
            {Items.IRON_INGOT, 64, 2}, {Items.REDSTONE, 128, 2}, {Items.LAPIS_LAZULI, 96, 2}, {Items.GOLD_INGOT, 32, 2},
            {Items.QUARTZ, 64, 2}, {Items.DIAMOND, 8, 3}, {Items.EMERALD, 8, 3}
    };

    public OrderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OrderBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof OrderBlockEntity order) || !(player instanceof ServerPlayer serverPlayer)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ModRegistry.CONTRACT_SEAL.get())) {
            if (order.active()) {
                player.displayClientMessage(Component.translatable("message.techminions.order.busy"), true);
                return ItemInteractionResult.CONSUME;
            }
            Object[] pick = ORDERS[level.random.nextInt(ORDERS.length)];
            Item item = (Item) pick[0];
            order.start(player.getUUID(), item, (Integer) pick[1], (Integer) pick[2]);
            stack.shrink(1);
            player.sendSystemMessage(Component.translatable("message.techminions.order.accepted",
                    pick[1], new ItemStack(item).getHoverName()).withStyle(ChatFormatting.GOLD));
            return ItemInteractionResult.CONSUME;
        }
        if (order.active() && stack.is(order.item())) {
            if (!player.getUUID().equals(order.owner())) {
                player.displayClientMessage(Component.translatable("message.techminions.order.not_yours"), true);
                return ItemInteractionResult.CONSUME;
            }
            // Сначала проверяем, сколько реально можно сдать, потом списываем ровно столько
            int give = Math.min(order.remaining(), stack.getCount());
            if (give > 0) {
                stack.shrink(give);
                order.deliver(give);
            }
            if (order.remaining() <= 0) {
                int reward = order.reward();
                order.clear();
                ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModRegistry.MINIONITE_SHARD.get(), reward));
                player.sendSystemMessage(Component.translatable("message.techminions.order.done", reward).withStyle(ChatFormatting.GREEN));
                ModAdvancements.award(serverPlayer, "contract_done");
            } else {
                player.displayClientMessage(Component.translatable("message.techminions.order.progress",
                        order.delivered(), order.need()), true);
            }
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OrderBlockEntity order) {
            if (order.active()) {
                player.displayClientMessage(Component.translatable("message.techminions.order.status",
                        order.delivered(), order.need(), new ItemStack(order.item()).getHoverName()), true);
            } else {
                player.displayClientMessage(Component.translatable("message.techminions.order.empty"), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
