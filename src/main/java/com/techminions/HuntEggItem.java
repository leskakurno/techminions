package com.techminions;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * Яйцо охоты: выглядит как ванильное яйцо призыва (та же текстура, цвета берутся у ванильного яйца моба),
 * но НЕ призывает моба и не вставляется в спавнер (это обычный Item, а не SpawnEggItem).
 * Нужно миньону-охотнику: вставленное в его слот яйцо задаёт, на кого он охотится.
 */
public class HuntEggItem extends Item {
    private static final String KEY = "Entity";

    public HuntEggItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(EntityType<?> type) {
        ItemStack stack = new ItemStack(ModRegistry.HUNT_EGG.get());
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY, BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static EntityType<?> getEntityType(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag tag = data.copyTag();
        if (!tag.contains(KEY)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(tag.getString(KEY));
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    @Override
    public Component getName(ItemStack stack) {
        EntityType<?> type = getEntityType(stack);
        Component mob = type != null ? type.getDescription() : Component.literal("?");
        return Component.translatable(this.getDescriptionId(stack), mob);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.techminions.hunt_egg").withStyle(ChatFormatting.GRAY));
    }
}
