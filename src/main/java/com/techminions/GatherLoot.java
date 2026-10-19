package com.techminions;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Добыча миньонов-собирателей (лесоруб, агроном, рыбак). Блоки мира они НЕ ломают и НЕ меняют: окружение только
 * проверяется (есть ли рядом деревья, посевы, вода), а добыча «виртуальная». Поэтому они безопасны для построек.
 */
public final class GatherLoot {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MIN_TREES = 3;
    private static final int MIN_CROPS = 4;
    private static final int MIN_WATER = 9;

    /** problem != null - миньону не хватает окружения (деревьев, посевов, воды), добычи в этот раз нет. */
    public record Result(List<ItemStack> drops, MinionState problem) {
    }

    private GatherLoot() {
    }

    public static Result roll(ServerLevel level, MinionEntity minion, MinionType type) {
        try {
            return switch (type) {
                case LUMBERJACK -> lumber(level, minion);
                case FARMER -> farm(level, minion);
                case FISHER -> fish(level, minion);
                default -> new Result(List.of(), MinionState.UNAVAILABLE);
            };
        } catch (Exception e) {
            LOGGER.warn("[TechMinions] Gathering failed for {}: {}", type.getSerializedName(), e.toString());
            return new Result(List.of(), MinionState.UNAVAILABLE);
        }
    }

    private static Result lumber(ServerLevel level, MinionEntity minion) {
        BlockPos c = minion.blockPosition();
        List<BlockPos> logs = new ArrayList<>();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (int dy = -1; dy <= 8; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (level.hasChunkAt(p) && level.getBlockState(p).is(BlockTags.LOGS)) {
                        logs.add(p);
                    }
                }
            }
        }
        if (logs.size() < MIN_TREES) {
            return new Result(List.of(), MinionState.NEEDS_TREES);
        }
        BlockPos pick = logs.get(level.random.nextInt(logs.size()));
        Item logItem = level.getBlockState(pick).getBlock().asItem();
        List<ItemStack> drops = new ArrayList<>();
        if (logItem != Items.AIR) {
            drops.add(new ItemStack(logItem, 1 + level.random.nextInt(2)));
        }
        if (level.random.nextFloat() < 0.25F) {
            drops.add(new ItemStack(Items.STICK, 1 + level.random.nextInt(3)));
        }
        return new Result(drops, null);
    }

    private static Result farm(ServerLevel level, MinionEntity minion) {
        BlockPos c = minion.blockPosition();
        List<BlockPos> crops = new ArrayList<>();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (level.hasChunkAt(p) && level.getBlockState(p).getBlock() instanceof CropBlock) {
                        crops.add(p);
                    }
                }
            }
        }
        if (crops.size() < MIN_CROPS) {
            return new Result(List.of(), MinionState.NEEDS_CROPS);
        }
        BlockPos pick = crops.get(level.random.nextInt(crops.size()));
        CropBlock crop = (CropBlock) level.getBlockState(pick).getBlock();
        BlockState mature = crop.getStateForAge(crop.getMaxAge());
        // Берём настоящую таблицу выпадения спелой культуры, поэтому работают и культуры из модов.
        return new Result(new ArrayList<>(Block.getDrops(mature, level, pick, null)), null);
    }

    private static Result fish(ServerLevel level, MinionEntity minion) {
        BlockPos c = minion.blockPosition();
        int water = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                for (int dy = -3; dy <= 0; dy++) {
                    BlockPos p = c.offset(dx, dy, dz);
                    if (level.hasChunkAt(p) && level.getFluidState(p).is(FluidTags.WATER)) {
                        water++;
                    }
                }
            }
        }
        if (water < MIN_WATER) {
            return new Result(List.of(), MinionState.NEEDS_WATER);
        }
        LootTable table = level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, minion.position())
                .withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
                .create(LootContextParamSets.FISHING);
        return new Result(new ArrayList<>(table.getRandomItems(params)), null);
    }
}
