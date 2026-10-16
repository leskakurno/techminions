package com.techminions;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Временная руда миньона. Ничего не выбрасывает, принадлежит конкретному миньону. */
public class TempOreBlock extends Block {
    public static final MapCodec<TempOreBlock> CODEC = simpleCodec(TempOreBlock::new);
    public static final EnumProperty<MinionType> TYPE = EnumProperty.create("type", MinionType.class);

    public TempOreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TYPE, MinionType.STONE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TYPE);
    }
}
