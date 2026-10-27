package com.techminions;

import net.minecraft.core.BlockPos;
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
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * Пьедестал (один предмет), Витрина (один предмет за стеклом), Стойка яиц (три Яйца охоты).
 * ПКМ предметом - выставить один; ПКМ пустой рукой или с приседом - забрать. Предметы только показываются.
 */
public class DisplayBlock extends Block implements EntityBlock {
    public enum Kind {
        PEDESTAL(1, 0.62, Block.box(3, 0, 3, 13, 13, 13)),
        CASE(1, 0.5, Block.box(1, 0, 1, 15, 15, 15)),
        RACK(3, 0.4, Block.box(1, 0, 1, 15, 6, 15));

        private final int slots;
        private final double height;
        private final VoxelShape shape;

        Kind(int slots, double height, VoxelShape shape) {
            this.slots = slots;
            this.height = height;
            this.shape = shape;
        }

        public int slots() {
            return slots;
        }

        public double itemHeight() {
            return height;
        }

        boolean accepts(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }
            if (this == RACK) {
                return stack.getItem() instanceof HuntEggItem;
            }
            // Контейнеры с содержимым не выставляем (шалкеры, мешки): чтобы не плодить огромные снимки предметов.
            return stack.get(net.minecraft.core.component.DataComponents.CONTAINER) == null
                    && stack.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS) == null;
        }
    }

    private final Kind kind;

    public DisplayBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return kind.shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (player.isShiftKeyDown() || !kind.accepts(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof DisplayBlockEntity display && display.put(stack)) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof DisplayBlockEntity display) {
            ItemStack taken = display.takeLast();
            if (!taken.isEmpty()) {
                ItemHandlerHelper.giveItemToPlayer(player, taken);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DisplayBlockEntity display) {
            display.dropAll(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
