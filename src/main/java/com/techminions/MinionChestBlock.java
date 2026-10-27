package com.techminions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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

    /** Запоминает владельца (нужно Центральному складу). */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player && level.getBlockEntity(pos) instanceof MinionChestBlockEntity chest) {
            chest.setOwner(player.getUUID());
        }
    }

    /** Лёгкий тикер: только выбрасывает остатки из старых больших сундуков (обычно ничего не делает). */
    @Nullable
    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModRegistry.MINION_CHEST_BE.get()) {
            return null;
        }
        BlockEntityTicker<MinionChestBlockEntity> ticker = (lvl, pos, st, be) -> be.dropOverflow(lvl, pos);
        return (BlockEntityTicker<T>) ticker;
    }

    /** ПКМ улучшением сундука: маленький -> средний -> большой. Содержимое переносится в новый сундук. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (stack.is(ModRegistry.STORAGE_SEAL.get())) {
            // Пломба хранилища: владелец запирает или отпирает контейнер (открыть его сможет только он)
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MinionChestBlockEntity sealed) {
                if (sealed.getOwner() == null) {
                    sealed.setOwner(player.getUUID());
                }
                if (!player.getUUID().equals(sealed.getOwner())) {
                    player.displayClientMessage(Component.translatable("message.techminions.warehouse.not_yours"), true);
                } else {
                    sealed.setLocked(!sealed.isLocked());
                    player.displayClientMessage(Component.translatable(sealed.isLocked()
                            ? "message.techminions.seal.locked" : "message.techminions.seal.unlocked"), true);
                    if (sealed.isLocked() && player instanceof ServerPlayer serverPlayer) {
                        ModAdvancements.award(serverPlayer, "sealed");
                    }
                }
            }
            return level.isClientSide ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
        }
        if (stack.getItem() instanceof WarehouseKeyItem) {
            if (tier != MinionChestTier.WAREHOUSE) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof MinionChestBlockEntity warehouse) {
                if (warehouse.getOwner() == null) {
                    warehouse.setOwner(player.getUUID()); // старый склад без владельца достаётся первому, кто привяжет ключ
                }
                if (!player.getUUID().equals(warehouse.getOwner())) {
                    player.displayClientMessage(Component.translatable("message.techminions.warehouse.not_yours"), true);
                } else {
                    WarehouseKeyItem.setTarget(stack, pos, level.dimension().location().toString());
                    player.displayClientMessage(Component.translatable("message.techminions.warehouse.key_set"), true);
                }
            }
            return level.isClientSide ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
        }
        if (!(stack.getItem() instanceof ChestUpgradeItem upgrade)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (upgrade.getTarget().ordinal() != tier.ordinal() + 1) {
            player.displayClientMessage(Component.translatable("message.techminions.chest_upgrade.wrong"), true);
            return ItemInteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof MinionChestBlockEntity chest) {
            List<ItemStack> contents = chest.takeAllItems(); // сначала вынимаем, чтобы замена блока ничего не выбросила
            level.setBlock(pos, ModRegistry.chestBlock(upgrade.getTarget()).defaultBlockState(), Block.UPDATE_ALL);
            if (level.getBlockEntity(pos) instanceof MinionChestBlockEntity newChest) {
                for (ItemStack item : contents) {
                    ItemStack rest = ItemHandlerHelper.insertItem(newChest.getItemHandler(), item, false);
                    if (!rest.isEmpty()) {
                        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, rest);
                    }
                }
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                ModAdvancements.award(serverPlayer, "chest_upgrade");
            }
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof MinionChestBlockEntity chest) {
            if (chest.isLocked() && chest.getOwner() != null && !chest.getOwner().equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.techminions.seal.denied"), true);
                return InteractionResult.CONSUME; // запломбировано владельцем
            }
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
