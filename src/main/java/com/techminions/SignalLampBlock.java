package com.techminions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Сигнальная лампа: светится зелёным, красным или серым в зависимости от статуса привязанного миньона
 * (работает / остановлен / нет связи). Привязывается Ключом привязки. Данные берёт из реестра: чанки не загружает.
 */
public class SignalLampBlock extends Block implements EntityBlock {
    public static final IntegerProperty STATUS = IntegerProperty.create("status", 0, 3);

    public SignalLampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STATUS, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATUS);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StatusLinkBlockEntity(pos, state);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModRegistry.LINK_BE.get()) {
            return null;
        }
        BlockEntityTicker<StatusLinkBlockEntity> ticker = (lvl, pos, st, be) -> be.serverTick(lvl, pos, st);
        return (BlockEntityTicker<T>) ticker;
    }

    void update(ServerLevel level, BlockPos pos, BlockState state, int status) {
        if (state.getValue(STATUS) != status) {
            level.setBlock(pos, state.setValue(STATUS, status), Block.UPDATE_ALL);
        }
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
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof StatusLinkBlockEntity link) {
            int status = state.getValue(STATUS);
            player.displayClientMessage(Component.translatable("message.techminions.lamp.status." + status), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
