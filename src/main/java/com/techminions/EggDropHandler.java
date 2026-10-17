package com.techminions;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/**
 * Зачарование «Коллекционер яиц» (data/techminions/enchantment/egg_collector.json, уровни I-III):
 * моб, убитый игроком с таким мечом/топором, с ОЧЕНЬ малым шансом роняет своё «яйцо охоты» (HuntEggItem).
 * Шанс = уровень * eggChancePerLevel (по умолчанию 0.1% / 0.2% / 0.3%), умножается на (1 + бонус * уровень «Добычи»)
 * на том же оружии (по умолчанию +50% за уровень), у боссов умножается на eggBossChanceMultiplier.
 */
public final class EggDropHandler {
    public static final ResourceKey<Enchantment> EGG_COLLECTOR = ResourceKey.create(
            Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(TechMinions.MOD_ID, "egg_collector"));

    private EggDropHandler() {
    }

    public static void onDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide() || !(victim instanceof Mob)) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty()) {
            return;
        }
        var enchantments = victim.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> collector = enchantments.get(EGG_COLLECTOR).orElse(null);
        if (collector == null) {
            return;
        }
        int level = EnchantmentHelper.getItemEnchantmentLevel(collector, weapon);
        if (level <= 0) {
            return;
        }
        double chance = Config.EGG_CHANCE_PER_LEVEL.get() * level;
        Holder<Enchantment> looting = enchantments.get(Enchantments.LOOTING).orElse(null);
        if (looting != null) {
            int lootingLevel = EnchantmentHelper.getItemEnchantmentLevel(looting, weapon);
            chance *= 1.0 + Config.EGG_LOOTING_BONUS.get() * lootingLevel; // «Добыча» повышает шанс
        }
        if (MinionType.isBossEntity(victim.getType())) {
            chance *= Config.EGG_BOSS_MULT.get();
        }
        if (victim.getRandom().nextDouble() < chance) {
            event.getDrops().add(new ItemEntity(victim.level(), victim.getX(), victim.getY(), victim.getZ(),
                    HuntEggItem.create(victim.getType())));
        }
    }
}
