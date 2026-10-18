package com.techminions.jei;

import com.techminions.Config;
import com.techminions.EggDropHandler;
import com.techminions.TechMinions;
import com.techminions.ModRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.Locale;

/**
 * Подключение к JEI (необязательное: JEI сам находит этот класс, без JEI он не загружается).
 * Добавляет страницы «Информация» для яйца охоты и для книг «Коллекционер яиц» с реальными шансами выпадения,
 * посчитанными по значениям из конфига (eggChancePerLevel, eggLootingBonusPerLevel, eggBossChanceMultiplier).
 */
@JeiPlugin
public class TechMinionsJeiPlugin implements IModPlugin {
    private static final int LOOTING_MAX = 3;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(TechMinions.MOD_ID, "jei_plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        double perLevel = 0.001;
        double lootingBonus = 0.5;
        double bossMult = 5.0;
        try {
            perLevel = Config.EGG_CHANCE_PER_LEVEL.get();
            lootingBonus = Config.EGG_LOOTING_BONUS.get();
            bossMult = Config.EGG_BOSS_MULT.get();
        } catch (Exception ignored) {
            // конфиг ещё не загружен: показываем значения по умолчанию
        }
        double lootingMax = 1.0 + lootingBonus * LOOTING_MAX;

        registration.addIngredientInfo(new ItemStack(ModRegistry.HUNT_EGG.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.techminions.hunt_egg.how"),
                Component.translatable("jei.techminions.hunt_egg.chance",
                        pct(chance(perLevel, 1, 0, lootingBonus, false, bossMult)),
                        pct(chance(perLevel, 2, 0, lootingBonus, false, bossMult)),
                        pct(chance(perLevel, 3, 0, lootingBonus, false, bossMult))),
                Component.translatable("jei.techminions.hunt_egg.looting",
                        pct(lootingBonus), num(lootingMax)),
                Component.translatable("jei.techminions.hunt_egg.boss",
                        num(bossMult),
                        pct(chance(perLevel, 3, 0, lootingBonus, true, bossMult)),
                        pct(chance(perLevel, 3, LOOTING_MAX, lootingBonus, true, bossMult))));

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Holder<Enchantment> collector = mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(EggDropHandler.EGG_COLLECTOR).orElse(null);
        if (collector == null) {
            return;
        }
        for (int level = 1; level <= collector.value().getMaxLevel(); level++) {
            ItemStack book = EnchantedBookItem.createForEnchantment(new EnchantmentInstance(collector, level));
            registration.addIngredientInfo(book, VanillaTypes.ITEM_STACK,
                    Component.translatable("jei.techminions.egg_collector.info",
                            pct(chance(perLevel, level, 0, lootingBonus, false, bossMult)),
                            pct(chance(perLevel, level, 0, lootingBonus, true, bossMult)),
                            pct(lootingBonus)));
        }
    }

    /** Та же формула, что в EggDropHandler. */
    private static double chance(double perLevel, int level, int looting, double lootingBonus,
                                 boolean boss, double bossMult) {
        double c = perLevel * level * (1.0 + lootingBonus * looting);
        if (boss) {
            c *= bossMult;
        }
        return Math.min(1.0, c);
    }

    private static String pct(double fraction) {
        return num(fraction * 100.0) + "%";
    }

    private static String num(double value) {
        String s = String.format(Locale.ROOT, "%.2f", value);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }
}
