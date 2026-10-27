package com.techminions;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Табличка статуса: ПКМ показывает состояние привязанного миньона (статус, заполнение, добыча за час). Ключом привязывается. */
public class StatusSignBlock extends Block implements EntityBlock {
    private static final VoxelShape SHAPE = Block.box(2, 0, 6, 14, 14, 10);

    public StatusSignBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatusLinkBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof ToolItems.BindingKeyItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof StatusLinkBlockEntity link) {
            ToolItems.BindingKeyItem.bindTo(serverPlayer, stack, link);
        }
        return level.isClientSide ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof StatusLinkBlockEntity link
                && serverPlayer.getServer() != null) {
            MinionRegistry.Entry entry = link.entry(serverPlayer.getServer());
            if (entry == null) {
                player.displayClientMessage(Component.translatable("message.techminions.sign.unlinked"), true);
            } else {
                int status = link.currentStatus(serverPlayer.getServer());
                int percent = entry.slotsTotal <= 0 ? 0 : Math.min(100, entry.slotsUsed * 100 / entry.slotsTotal);
                player.sendSystemMessage(Component.translatable("message.techminions.sign.report",
                        Component.translatable("type.techminions." + MinionType.byOrdinal(entry.type).getSerializedName()),
                        MinionEntity.roman(entry.level),
                        Component.translatable("message.techminions.lamp.status." + status),
                        percent, entry.hourly).withStyle(ChatFormatting.AQUA));
            }
        }
        return InteractionResult.CONSUME;
    }
}
