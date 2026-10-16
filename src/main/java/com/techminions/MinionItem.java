package com.techminions;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/** Предмет-миньон: ПКМ по земле ставит живого миньона. Уровень сохраняется в предмете. */
public class MinionItem extends Item {
    private final MinionType type;

    public MinionItem(Properties properties, MinionType type) {
        super(properties);
        this.type = type;
    }

    public MinionType getMinionType() {
        return type;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

        boolean spaceFree = level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
        boolean hasFloor = level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
        if (!spaceFree || !hasFloor) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("message.techminions.need_floor"), true);
            }
            return InteractionResult.FAIL;
        }

        MinionEntity minion = ModRegistry.MINION.get().create(serverLevel);
        if (minion == null) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = context.getItemInHand();
        minion.setMinionType(type);
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            minion.applyItemData(data.copyTag());
        }
        minion.refreshVisuals();

        float yaw = player != null ? player.getYRot() + 180.0F : 0.0F;
        minion.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
        minion.setYHeadRot(yaw);
        minion.yBodyRot = yaw;
        serverLevel.addFreshEntity(minion);
        level.playSound(null, pos, SoundEvents.ARMOR_STAND_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int lvl = 1;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            CompoundTag tag = data.copyTag();
            if (tag.contains("Level")) {
                lvl = Math.max(1, Math.min(MinionEntity.MAX_LEVEL, tag.getInt("Level")));
            }
        }
        tooltip.add(Component.translatable("tooltip.techminions.level", MinionEntity.roman(lvl))
                .withStyle(ChatFormatting.GRAY));
    }
}
