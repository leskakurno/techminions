package com.techminions;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.level.Level;

import java.util.List;

public class UpgradeItem extends Item {
    private final UpgradeType upgradeType;

    public UpgradeItem(Properties properties, UpgradeType upgradeType) {
        super(properties);
        this.upgradeType = upgradeType;
    }

    public UpgradeType getUpgradeType() {
        return upgradeType;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (upgradeType != UpgradeType.FILTER) {
            return super.use(level, player, hand);
        }
        if (!level.isClientSide) {
            UpgradeFilter.interact(player, hand, stack); // настройка фильтра «Умная логистика»
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.techminions.upgrade." + upgradeType.getSerializedName())
                .withStyle(ChatFormatting.GRAY));
        if (upgradeType == UpgradeType.FILTER) {
            boolean black = UpgradeFilter.isBlacklist(stack);
            tooltip.add(Component.translatable(black ? "tooltip.techminions.filter.black" : "tooltip.techminions.filter.white")
                    .withStyle(ChatFormatting.AQUA));
            for (String id : UpgradeFilter.ids(stack)) {
                tooltip.add(Component.literal(" - " + id).withStyle(ChatFormatting.DARK_GRAY));
            }
            tooltip.add(Component.translatable("tooltip.techminions.filter.how").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
