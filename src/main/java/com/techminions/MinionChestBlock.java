package com.techminions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Сундук миньонов (маленький / средний / большой). Миньон, стоящий на нём (или рядом),
 * сам складывает в него добычу.
 */
public class MinionChestBlock extends Block implements EntityBlock {
    public static final MapCodec<MinionChestBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            propertiesCodec(),
            com.mojang.serialization.Codec.INT.fieldOf("tier").forGetter(b -> b.tier.ordinal())
    ).apply(instance, (properties, tier) -> new MinionChestBlock(properties, MinionChestTier.byOrdinal(tier))));

    private final MinionChestTier tier;

    public MinionChestBlock(Properties properties, MinionChestTier tier) {
        super(properties);
        this.tier = tier;
    }

    public MinionChestTier getTier() {
        return tier;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MinionChestBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MinionChestBlockEntity chest) {
            serverPlayer.openMenu(chest, buf -> {
                buf.writeBlockPos(pos);
                buf.writeVarInt(tier.ordinal());
            });
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof MinionChestBlockEntity chest) {
            chest.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
