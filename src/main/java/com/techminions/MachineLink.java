package com.techminions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Какие блоки рядом считаются «механизмами», с которыми миньон может обмениваться предметами
 * (улучшения «Подача в механизм» и «Забор из механизма»).
 * Сундуки, бочки, воронки, ящики-хранилища и транспортные блоки (трубы, кабели) НЕ подходят никогда:
 * миньон работает только со своими Сундуками миньонов и с настоящими машинами из списка в конфиге.
 */
public final class MachineLink {
    private static final Set<String> BLOCKED_WORDS = Set.of(
            "chest", "barrel", "crate", "drawer", "drawers", "bin", "shulker", "locker", "vault", "cabinet", "box",
            "hopper", "pipe", "conduit", "tube", "duct", "funnel", "tunnel", "interface", "bus", "terminal",
            "drive", "controller", "cable", "wire", "relay", "portal", "qio");

    private MachineLink() {
    }

    public static boolean isSupportedMachine(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }
        Block block = state.getBlock();
        if (block instanceof MinionChestBlock || block instanceof ChestBlock || block instanceof BarrelBlock
                || block instanceof HopperBlock || block instanceof ShulkerBoxBlock || block instanceof EnderChestBlock) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        Set<String> allowed = Config.machineBlocks();
        boolean listed = allowed.contains(id.toString())
                || (!id.getNamespace().equals("minecraft") && allowed.contains(id.getNamespace()));
        if (!listed) {
            return false;
        }
        for (String word : id.getPath().split("_")) {
            if (BLOCKED_WORDS.contains(word)) {
                return false;
            }
        }
        return level.getBlockEntity(pos) != null;
    }
}
