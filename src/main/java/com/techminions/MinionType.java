package com.techminions;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.Tags;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.world.entity.EntityType;

/**
 * Тип миньона. Порядок констант НЕЛЬЗЯ менять (ординал сохраняется в мире) — новые только в конец.
 *
 * Ресурс задаётся списком источников, берётся первый, который реально существует:
 *   "mod:item"   — конкретный предмет;
 *   "#ns:tag"    — любой предмет из тега (например #c:raw_materials/tin), поэтому поддерживаются
 *                  и другие моды, которые кладут свои руды в общие теги.
 * Предмет миньона регистрируется только если загружен хотя бы один мод из списка mods.
 * Если ресурс так и не найден, миньон просто не работает (состояние «Ресурс недоступен»).
 */
public enum MinionType implements StringRepresentable {
    STONE(1, 128, mods("minecraft"), "minecraft:cobblestone"),
    COAL(1, 96, mods("minecraft"), "minecraft:coal"),
    COPPER(1, 96, mods("minecraft"), "minecraft:raw_copper"),
    IRON(2, 80, mods("minecraft"), "minecraft:raw_iron"),
    GOLD(2, 64, mods("minecraft"), "minecraft:raw_gold"),
    REDSTONE(2, 96, mods("minecraft"), "minecraft:redstone"),
    LAPIS(2, 64, mods("minecraft"), "minecraft:lapis_lazuli"),
    DIAMOND(3, 24, mods("minecraft"), "minecraft:diamond"),
    EMERALD(3, 24, mods("minecraft"), "minecraft:emerald"),
    // --- Mekanism и другие ---
    OSMIUM(2, 64, mods("mekanism", "alltheores"), "mekanism:raw_osmium", "alltheores:raw_osmium", "#c:raw_materials/osmium"),
    TIN(1, 96, mods("mekanism", "thermal", "modern_industrialization", "alltheores"),
            "mekanism:raw_tin", "thermal:raw_tin", "modern_industrialization:raw_tin", "alltheores:raw_tin", "#c:raw_materials/tin"),
    LEAD(1, 96, mods("mekanism", "thermal", "immersiveengineering", "modern_industrialization", "alltheores"),
            "mekanism:raw_lead", "thermal:raw_lead", "immersiveengineering:raw_lead",
            "modern_industrialization:raw_lead", "alltheores:raw_lead", "#c:raw_materials/lead"),
    URANIUM(3, 16, mods("mekanism", "immersiveengineering", "modern_industrialization", "alltheores", "oritech"),
            "mekanism:raw_uranium", "immersiveengineering:raw_uranium",
            "modern_industrialization:raw_uranium", "alltheores:raw_uranium", "oritech:raw_uranium", "#c:raw_materials/uranium"),
    FLUORITE(2, 48, mods("mekanism"), "mekanism:fluorite_gem"),
    // --- Create ---
    ZINC(1, 96, mods("create", "alltheores"), "create:raw_zinc", "alltheores:raw_zinc", "#c:raw_materials/zinc"),
    // --- Powah ---
    URANINITE(2, 40, mods("powah"), "powah:uraninite_raw"),
    // --- Draconic Evolution ---
    DRACONIUM(3, 16, mods("draconicevolution"), "draconicevolution:raw_draconium", "draconicevolution:draconium_dust"),
    // --- Create: New Age ---
    THORIUM(3, 16, mods("create_new_age"), "create_new_age:raw_thorium", "create_new_age:thorium"),
    // --- Новые (v2.2): технические моды ---
    SILVER(2, 64, mods("thermal", "immersiveengineering", "modern_industrialization", "alltheores"),
            "thermal:raw_silver", "immersiveengineering:raw_silver", "alltheores:raw_silver", "#c:raw_materials/silver"),
    NICKEL(2, 64, mods("thermal", "immersiveengineering", "modern_industrialization", "alltheores", "oritech"),
            "thermal:raw_nickel", "immersiveengineering:raw_nickel", "alltheores:raw_nickel", "oritech:raw_nickel", "#c:raw_materials/nickel"),
    ALUMINUM(1, 96, mods("immersiveengineering", "alltheores"),
            "immersiveengineering:raw_aluminum", "alltheores:raw_aluminum", "#c:raw_materials/aluminum"),
    TITANIUM(3, 24, mods("modern_industrialization"),
            "modern_industrialization:raw_titanium", "#c:raw_materials/titanium"),
    TUNGSTEN(3, 24, mods("modern_industrialization"),
            "modern_industrialization:raw_tungsten", "#c:raw_materials/tungsten"),
    PLATINUM(3, 24, mods("modern_industrialization", "alltheores", "oritech"),
            "modern_industrialization:raw_platinum", "alltheores:raw_platinum", "oritech:raw_platinum", "#c:raw_materials/platinum"),
    CERTUS(2, 48, mods("ae2"), "ae2:certus_quartz_crystal"),
    CINNABAR(2, 48, mods("thermal"), "thermal:cinnabar"),
    SULFUR(1, 96, mods("thermal"), "thermal:sulfur"),
    NITER(1, 96, mods("thermal"), "thermal:niter"),
    // --- Новые (v2.2): ваниль ---
    QUARTZ(2, 64, mods("minecraft"), "minecraft:quartz"),
    OBSIDIAN(2, 48, mods("minecraft"), "minecraft:obsidian"),
    GLOWSTONE(2, 64, mods("minecraft"), "minecraft:glowstone_dust"),
    // --- Новые (v2.4): редкий материал и миньоны-охотники (охотники не строят платформу, а «убивают» моба) ---
    MINIONITE(3, 1, mods("techminions"), "techminions:minionite_shard"),
    HUNT_ZOMBIE(1, 6, mods("minecraft")),
    HUNT_SKELETON(1, 6, mods("minecraft")),
    HUNT_BLAZE(2, 6, mods("minecraft")),
    HUNT_ENDERMAN(2, 6, mods("minecraft")),
    HUNT_GUARDIAN(3, 1, mods("minecraft")),
    HUNT_NAGA(3, 1, mods("twilightforest")),
    HUNT_HYDRA(3, 1, mods("twilightforest")),
    // --- Новые (v2.8): иридий (AllTheOres) и самоцветы Thermal / ore-модов ---
    IRIDIUM(3, 16, mods("alltheores"), "alltheores:raw_iridium", "#c:raw_materials/iridium"),
    RUBY(2, 48, mods("thermal"), "thermal:ruby", "#c:gems/ruby"),
    SAPPHIRE(2, 48, mods("thermal"), "thermal:sapphire", "#c:gems/sapphire"),
    APATITE(1, 96, mods("thermal"), "thermal:apatite", "#c:gems/apatite"),
    // --- Новые (v2.9): универсальные охотники. Цель берётся из Яйца охоты, поэтому работают с мобами и боссами
    //     ЛЮБЫХ модов (Twilight Forest, Mowzie's Mobs, Cataclysm, Alex's Caves/Mobs, The Graveyard и т.д.) ---
    HUNT_ANY_MOB(2, 6, mods("minecraft")),
    HUNT_ANY_BOSS(3, 1, mods("minecraft")),
    // --- Новые (v2.10): остальные боссы Twilight Forest (Нага и Гидра уже есть выше) ---
    HUNT_LICH(3, 1, mods("twilightforest")),
    HUNT_MINOSHROOM(3, 1, mods("twilightforest")),
    HUNT_KNIGHT_PHANTOM(3, 1, mods("twilightforest")),
    HUNT_UR_GHAST(3, 1, mods("twilightforest")),
    HUNT_ALPHA_YETI(3, 1, mods("twilightforest")),
    HUNT_SNOW_QUEEN(3, 1, mods("twilightforest")),
    // --- Новые (v3.4): собиратели. Мир не ломают: проверяют окружение (деревья / посевы / вода) и «собирают» виртуально ---
    LUMBERJACK(1, 8, mods("minecraft")),
    FARMER(1, 8, mods("minecraft")),
    FISHER(2, 6, mods("minecraft")),
    // --- Универсальный добытчик: добывает то, что вставлено в слот образца (предмет из любого мода по тегам) ---
    UNIVERSAL_MINER(3, 6, mods("minecraft"));

    private final int tier;
    private final int upgradeBase;
    private final String[] mods;
    private final String[] sources;
    private Item cached;
    private Item cachedCost;

    MinionType(int tier, int upgradeBase, String[] mods, String... sources) {
        this.tier = tier;
        this.upgradeBase = upgradeBase;
        this.mods = mods;
        this.sources = sources;
    }

    private static String[] mods(String... ids) {
        return ids;
    }

    /** 1 = базовое ядро, 2 = усиленное, 3 = элитное (см. рецепты). */
    public int getTier() {
        return tier;
    }

    /** Загружен ли хотя бы один мод, поддерживающий этот ресурс. */
    public boolean isModLoaded() {
        for (String id : mods) {
            if (id.equals("minecraft") || ModList.get().isLoaded(id)) {
                return true;
            }
        }
        // Типы, у которых есть источник-тег (#c:raw_materials/..., #c:gems/...), включаются и тогда,
        // когда стоит любой другой мод руд (Create Ores, Ores and Metals и т.п.), чьих id мы не знаем.
        return hasTagSource() && OreMods.anyLoaded();
    }

    private boolean hasTagSource() {
        for (String src : sources) {
            if (src.startsWith("#")) {
                return true;
            }
        }
        return false;
    }

    /** Определяет «моды руд» по id: alltheores, createores, ores_and_metals, fore_more_ores, oritech и т.д. */
    private static final class OreMods {
        private static Boolean cached;

        static boolean anyLoaded() {
            if (cached == null) {
                cached = ModList.get().getMods().stream().anyMatch(m -> {
                    String id = m.getModId();
                    return id.endsWith("ores") || id.contains("mekanism") || id.contains("_ores") || id.startsWith("ores")
                            || id.contains("oreexcavation") || id.contains("ore_excavation")
                            || id.equals("oritech") || id.equals("elementarium");
                });
            }
            return cached;
        }
    }

    /** Ресурс, который добывает миньон (он же платится за прокачку). AIR, если недоступен. */
    public Item getResource() {
        if (cached == null) {
            for (String src : sources) {
                Item found = src.startsWith("#") ? fromTag(src.substring(1)) : fromItem(src);
                if (found != Items.AIR) {
                    cached = found;
                    break;
                }
            }
        }
        return cached != null ? cached : Items.AIR;
    }

    /**
     * Слитки, которыми платится прокачка (и из которых крафтится миньон). Формат тот же, что у ресурса.
     * Если типа нет в таблице (уголь, алмаз, камень и т.п. — слитка не существует), берётся сам ресурс.
     */
    private static final class Ingots {
        static final Map<MinionType, String[]> MAP = new EnumMap<>(MinionType.class);

        static {
            MAP.put(COPPER, new String[]{"minecraft:copper_ingot"});
            MAP.put(IRON, new String[]{"minecraft:iron_ingot"});
            MAP.put(GOLD, new String[]{"minecraft:gold_ingot"});
            MAP.put(OSMIUM, new String[]{"mekanism:ingot_osmium", "alltheores:osmium_ingot", "#c:ingots/osmium"});
            MAP.put(TIN, new String[]{"mekanism:ingot_tin", "thermal:tin_ingot", "alltheores:tin_ingot", "#c:ingots/tin"});
            MAP.put(LEAD, new String[]{"mekanism:ingot_lead", "thermal:lead_ingot",
                    "immersiveengineering:ingot_lead", "alltheores:lead_ingot", "#c:ingots/lead"});
            MAP.put(URANIUM, new String[]{"mekanism:ingot_uranium", "immersiveengineering:ingot_uranium",
                    "alltheores:uranium_ingot", "oritech:uranium_ingot", "#c:ingots/uranium"});
            MAP.put(ZINC, new String[]{"create:zinc_ingot", "alltheores:zinc_ingot", "#c:ingots/zinc"});
            MAP.put(URANINITE, new String[]{"powah:uraninite"});
            MAP.put(DRACONIUM, new String[]{"draconicevolution:draconium_ingot", "#c:ingots/draconium"});
            MAP.put(THORIUM, new String[]{"create_new_age:thorium_ingot", "#c:ingots/thorium"});
            MAP.put(SILVER, new String[]{"thermal:silver_ingot", "immersiveengineering:ingot_silver", "alltheores:silver_ingot", "#c:ingots/silver"});
            MAP.put(NICKEL, new String[]{"thermal:nickel_ingot", "immersiveengineering:ingot_nickel", "alltheores:nickel_ingot",
                    "oritech:nickel_ingot", "#c:ingots/nickel"});
            MAP.put(ALUMINUM, new String[]{"immersiveengineering:ingot_aluminum", "alltheores:aluminum_ingot", "#c:ingots/aluminum"});
            MAP.put(TITANIUM, new String[]{"#c:ingots/titanium"});
            MAP.put(TUNGSTEN, new String[]{"#c:ingots/tungsten"});
            MAP.put(PLATINUM, new String[]{"alltheores:platinum_ingot", "oritech:platinum_ingot", "#c:ingots/platinum"});
            MAP.put(IRIDIUM, new String[]{"alltheores:iridium_ingot", "#c:ingots/iridium"});
            MAP.put(MINIONITE, new String[]{"minecraft:netherite_ingot"});
            MAP.put(HUNT_ZOMBIE, new String[]{"minecraft:iron_ingot"});
            MAP.put(HUNT_SKELETON, new String[]{"minecraft:iron_ingot"});
            MAP.put(HUNT_BLAZE, new String[]{"minecraft:gold_ingot"});
            MAP.put(HUNT_ENDERMAN, new String[]{"minecraft:gold_ingot"});
            // боссы: прокачка и крафт требуют редкий минионит
            MAP.put(HUNT_GUARDIAN, new String[]{"techminions:minionite_ingot"});
            MAP.put(HUNT_NAGA, new String[]{"techminions:minionite_ingot"});
            MAP.put(HUNT_HYDRA, new String[]{"techminions:minionite_ingot"});
            MAP.put(HUNT_ANY_MOB, new String[]{"minecraft:gold_ingot"});
            MAP.put(HUNT_ANY_BOSS, new String[]{"techminions:minionite_ingot"});
            MAP.put(HUNT_LICH, new String[]{"techminions:minionite_ingot"});
            MAP.put(HUNT_MINOSHROOM, new String[]{"techminions:minionite_ingot"});
            MAP.put(HUNT_KNIGHT_PHANTOM, new String[]{"techminions:minionite_ingot"});
            MAP.put(HUNT_UR_GHAST, new String[]{"techminions:minionite_ingot"});
            MAP.put(HUNT_ALPHA_YETI, new String[]{"techminions:minionite_ingot"});
            MAP.put(HUNT_SNOW_QUEEN, new String[]{"techminions:minionite_ingot"});
            MAP.put(LUMBERJACK, new String[]{"minecraft:iron_ingot"});
            MAP.put(FARMER, new String[]{"minecraft:iron_ingot"});
            MAP.put(FISHER, new String[]{"minecraft:gold_ingot"});
            MAP.put(UNIVERSAL_MINER, new String[]{"minecraft:gold_ingot"});
        }
    }

    /** Предмет, который показывается в цене прокачки (слиток, а если слитков нет — сам ресурс). */
    public Item getCostItem() {
        if (cachedCost == null) {
            String[] list = Ingots.MAP.get(this);
            if (list != null) {
                for (String src : list) {
                    Item found = src.startsWith("#") ? fromTag(src.substring(1)) : fromItem(src);
                    if (found != Items.AIR) {
                        cachedCost = found;
                        break;
                    }
                }
            }
            if (cachedCost == null) {
                Item res = getResource();
                if (res == Items.AIR) {
                    return Items.AIR;
                }
                cachedCost = res;
            }
        }
        return cachedCost;
    }

    /** Подходит ли предмет для оплаты прокачки: любой слиток из списка/тега (из любого мода), а без слитков — ресурс. */
    public boolean isCostItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String[] list = Ingots.MAP.get(this);
        if (list != null) {
            boolean anyExists = false;
            for (String src : list) {
                if (src.startsWith("#")) {
                    TagKey<Item> key = TagKey.create(Registries.ITEM, ResourceLocation.parse(src.substring(1)));
                    if (stack.is(key)) {
                        return true;
                    }
                    anyExists |= fromTag(src.substring(1)) != Items.AIR;
                } else {
                    Item item = fromItem(src);
                    if (item != Items.AIR) {
                        anyExists = true;
                        if (stack.is(item)) {
                            return true;
                        }
                    }
                }
            }
            if (anyExists) {
                return false;
            }
        }
        // Слитков нет (или ни один не загружен) -> платим самим ресурсом.
        Item res = getResource();
        return res != Items.AIR && stack.is(res);
    }

    private static Item fromItem(String id) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id));
        return item.orElse(Items.AIR);
    }

    /** Из тега берём предмет с наименьшим id, чтобы выбор был одинаковым на клиенте и сервере. */
    private static Item fromTag(String id) {
        TagKey<Item> key = TagKey.create(Registries.ITEM, ResourceLocation.parse(id));
        Optional<HolderSet.Named<Item>> tag = BuiltInRegistries.ITEM.getTag(key);
        if (tag.isEmpty()) {
            return Items.AIR;
        }
        Item best = Items.AIR;
        String bestId = null;
        for (Holder<Item> holder : tag.get()) {
            Item item = holder.value();
            if (item == Items.AIR) {
                continue;
            }
            String itemId = BuiltInRegistries.ITEM.getKey(item).toString();
            if (bestId == null || itemId.compareTo(bestId) < 0) {
                best = item;
                bestId = itemId;
            }
        }
        return best;
    }


    /** Описание цели охотника: id существа и является ли оно боссом (боссы убиваются в разы дольше). */
    private record Hunt(String entity, boolean boss) {
    }

    private static final class Hunts {
        static final Map<MinionType, Hunt> MAP = new EnumMap<>(MinionType.class);

        static {
            MAP.put(HUNT_ZOMBIE, new Hunt("minecraft:zombie", false));
            MAP.put(HUNT_SKELETON, new Hunt("minecraft:skeleton", false));
            MAP.put(HUNT_BLAZE, new Hunt("minecraft:blaze", false));
            MAP.put(HUNT_ENDERMAN, new Hunt("minecraft:enderman", false));
            MAP.put(HUNT_GUARDIAN, new Hunt("minecraft:elder_guardian", true));
            MAP.put(HUNT_NAGA, new Hunt("twilightforest:naga", true));
            MAP.put(HUNT_HYDRA, new Hunt("twilightforest:hydra", true));
            MAP.put(HUNT_LICH, new Hunt("twilightforest:lich", true));
            MAP.put(HUNT_MINOSHROOM, new Hunt("twilightforest:minoshroom", true));
            MAP.put(HUNT_KNIGHT_PHANTOM, new Hunt("twilightforest:knight_phantom", true));
            MAP.put(HUNT_UR_GHAST, new Hunt("twilightforest:ur_ghast", true));
            MAP.put(HUNT_ALPHA_YETI, new Hunt("twilightforest:alpha_yeti", true));
            MAP.put(HUNT_SNOW_QUEEN, new Hunt("twilightforest:snow_queen", true));
        }
    }

    /** Миньон-охотник: вместо платформы периодически «убивает» моба и кладёт его добычу в хранилище. */
    public boolean isHunter() {
        return Hunts.MAP.containsKey(this) || isUniversalHunter();
    }

    /** Собиратель (лесоруб, агроном, рыбак): без платформы, добыча зависит от окружения рядом. */
    public boolean isGatherer() {
        return this == LUMBERJACK || this == FARMER || this == FISHER;
    }

    /** Универсальный добытчик: добывает предмет-образец из слота (любой мод, допустимое задают теги в конфиге). */
    public boolean isUniversalMiner() {
        return this == UNIVERSAL_MINER;
    }

    /** Универсальный охотник: цель задаёт вставленное Яйцо охоты (любой моб любого мода). */
    public boolean isUniversalHunter() {
        return this == HUNT_ANY_MOB || this == HUNT_ANY_BOSS;
    }

    /** Боссовые тайминги (долгое появление и убийство). У универсального охотника на боссов - всегда. */
    public boolean isBoss() {
        Hunt hunt = Hunts.MAP.get(this);
        return (hunt != null && hunt.boss()) || this == HUNT_ANY_BOSS;
    }

    /** Босс ли это существо: тег c:bosses (его ставят Twilight Forest, Cataclysm и др.) или список bossEntities в конфиге. */
    public static boolean isBossEntity(EntityType<?> type) {
        if (type.is(Tags.EntityTypes.BOSSES)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return Config.bossEntityIds().contains(id.toString());
    }

    /** Подходит ли яйцо с этим существом данному охотнику. */
    public boolean acceptsEgg(EntityType<?> egg) {
        if (egg == null) {
            return false;
        }
        if (Config.disabledEntities().contains(BuiltInRegistries.ENTITY_TYPE.getKey(egg).toString())) {
            return false;
        }
        if (this == HUNT_ANY_MOB) {
            return !isBossEntity(egg);
        }
        if (this == HUNT_ANY_BOSS) {
            return isBossEntity(egg);
        }
        return getHuntEntity().map(t -> t == egg).orElse(false);
    }

    /** Существо-цель охотника; пусто, если мод с этим существом не установлен. */
    public Optional<EntityType<?>> getHuntEntity() {
        Hunt hunt = Hunts.MAP.get(this);
        if (hunt == null) {
            return Optional.empty();
        }
        return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(hunt.entity()));
    }

    /** Во сколько раз шаг добычи длиннее обычного: минионит - редкий ресурс, поэтому медленный. */
    public int getTimeMultiplier() {
        if (isGatherer() || isUniversalMiner()) {
            return 2; // собиратели берут за цикл целую порцию, поэтому медленнее рудных
        }
        return this == MINIONITE ? 4 : 1;
    }

    public String describeSource() {
        Hunt hunt = Hunts.MAP.get(this);
        if (isUniversalHunter()) {
            return "universal hunter (any hunt egg)";
        }
        return hunt != null ? "hunts " + hunt.entity() : String.valueOf(getResource());
    }

    /** Выключен ли тип в конфиге (по имени типа или потому, что все его моды отключены в disabledMods). */
    public boolean isDisabledByConfig() {
        if (Config.disabledMinions().contains(getSerializedName())) {
            return true;
        }
        java.util.Set<String> off = Config.disabledMods();
        if (off.isEmpty()) {
            return false;
        }
        boolean anyLoaded = false;
        for (String id : mods) {
            if (id.equals("minecraft") || ModList.get().isLoaded(id)) {
                anyLoaded = true;
                if (!off.contains(id)) {
                    return false;
                }
            }
        }
        return anyLoaded;
    }

    public boolean isAvailable() {
        if (isDisabledByConfig()) {
            return false;
        }
        if (isUniversalHunter() || isGatherer() || isUniversalMiner()) {
            return true;
        }
        if (isHunter()) {
            return getHuntEntity().isPresent();
        }
        return getResource() != Items.AIR;
    }

    /** Цена прокачки с уровня L на L+1 = upgradeBase * L * множитель из конфига. */
    public int getUpgradeBase() {
        return upgradeBase;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MinionType byOrdinal(int ordinal) {
        MinionType[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}
