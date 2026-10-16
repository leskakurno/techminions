package com.techminions;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Добыча охотника: «убийство» моба без самого моба. Берём стандартную таблицу лута существа и бросаем её так,
 * как будто моба убил игрок (чтобы работали условия вроде «убит игроком»). Работает для любых модов,
 * у которых у существа есть обычная таблица лута.
 */
public final class HuntLoot {
    private static final Logger LOGGER = LogUtils.getLogger();

    private HuntLoot() {
    }

    public static List<ItemStack> roll(ServerLevel level, EntityType<?> type, Vec3 origin) {
        List<ItemStack> result = new ArrayList<>();
        try {
            Entity entity = type.create(level); // в мир не добавляется
            if (entity == null) {
                return result;
            }
            entity.moveTo(origin.x, origin.y, origin.z);
            ResourceKey<LootTable> key = type.getDefaultLootTable();
            LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
            FakePlayer killer = FakePlayerFactory.getMinecraft(level);
            DamageSource source = level.damageSources().playerAttack(killer);
            LootParams params = new LootParams.Builder(level)
                    .withParameter(LootContextParams.THIS_ENTITY, entity)
                    .withParameter(LootContextParams.ORIGIN, origin)
                    .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                    .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, killer)
                    .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, killer)
                    .withOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER, killer)
                    .create(LootContextParamSets.ENTITY);
            for (ItemStack stack : table.getRandomItems(params)) {
                if (!stack.isEmpty()) {
                    result.add(stack);
                }
            }
            entity.discard();
        } catch (Exception e) {
            LOGGER.warn("[TechMinions] Could not roll loot for {}: {}",
                    BuiltInRegistries.ENTITY_TYPE.getKey(type), e.toString());
        }
        return result;
    }
}
