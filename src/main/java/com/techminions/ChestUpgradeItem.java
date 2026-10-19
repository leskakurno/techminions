package com.techminions;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** Улучшение сундука: ПКМ по Сундуку миньонов превращает его в следующий размер, содержимое сохраняется. */
public class ChestUpgradeItem extends Item {
    private final MinionChestTier target;

    public ChestUpgradeItem(Properties properties, MinionChestTier target) {
        super(properties);
        this.target = target;
    }

    /** Размер, в который превращается сундук. */
    public MinionChestTier getTarget() {
        return target;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.techminions.chest_upgrade." + target.id()).withStyle(ChatFormatting.GRAY));
    }
}
