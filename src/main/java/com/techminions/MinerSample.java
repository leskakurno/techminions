package com.techminions;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * Образец для Универсального добытчика: вставленный в слот предмет (сырьё, самоцвет, пыль, культура, бревно...) из ЛЮБОГО
 * мода, который миньон начинает добывать. Допустимое задаётся тегами и чёрным списком в конфиге, поэтому работает со всеми
 * модами, которые кладут свои предметы в общие теги (c:raw_materials, c:gems, c:dusts, c:crops...).
 */
public final class MinerSample {
    private MinerSample() {
    }

    public static boolean isValid(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        String id = BuiltInRegistries.ITEM.getKey(item).toString();
        if (Config.csv(Config.UNIVERSAL_BLACKLIST.get()).contains(id)) {
            return false;
        }
        Set<String> tags = Config.csv(Config.UNIVERSAL_TAGS.get());
        for (String tag : tags) {
            ResourceLocation location = ResourceLocation.tryParse(tag);
            if (location == null) {
                continue;
            }
            TagKey<Item> key = ItemTags.create(location);
            if (stack.is(key)) {
                return true;
            }
        }
        return false;
    }
}
