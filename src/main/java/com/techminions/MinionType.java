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
    OSMIUM(2, 64, mods("mekanism"), "mekanism:raw_osmium", "#c:raw_materials/osmium"),
    TIN(1, 96, mods("mekanism", "thermal", "modern_industrialization"),
            "mekanism:raw_tin", "thermal:raw_tin", "modern_industrialization:raw_tin", "#c:raw_materials/tin"),
    LEAD(1, 96, mods("mekanism", "thermal", "immersiveengineering", "modern_industrialization"),
            "mekanism:raw_lead", "thermal:raw_lead", "immersiveengineering:raw_lead",
            "modern_industrialization:raw_lead", "#c:raw_materials/lead"),
    URANIUM(3, 16, mods("mekanism", "immersiveengineering", "modern_industrialization"),
            "mekanism:raw_uranium", "immersiveengineering:raw_uranium",
            "modern_industrialization:raw_uranium", "#c:raw_materials/uranium"),
    FLUORITE(2, 48, mods("mekanism"), "mekanism:fluorite_gem"),
    // --- Create ---
    ZINC(1, 96, mods("create"), "create:raw_zinc", "#c:raw_materials/zinc"),
    // --- Powah ---
    URANINITE(2, 40, mods("powah"), "powah:uraninite_raw"),
    // --- Draconic Evolution ---
    DRACONIUM(3, 16, mods("draconicevolution"), "draconicevolution:raw_draconium", "draconicevolution:draconium_dust"),
    // --- Create: New Age ---
    THORIUM(3, 16, mods("create_new_age"), "create_new_age:raw_thorium", "create_new_age:thorium"),
    // --- Новые (v2.2): технические моды ---
    SILVER(2, 64, mods("thermal", "immersiveengineering", "modern_industrialization"),
            "thermal:raw_silver", "immersiveengineering:raw_silver", "#c:raw_materials/silver"),
    NICKEL(2, 64, mods("thermal", "immersiveengineering", "modern_industrialization"),
            "thermal:raw_nickel", "immersiveengineering:raw_nickel", "#c:raw_materials/nickel"),
    ALUMINUM(1, 96, mods("immersiveengineering"),
            "immersiveengineering:raw_aluminum", "#c:raw_materials/aluminum"),
    TITANIUM(3, 24, mods("modern_industrialization"),
            "modern_industrialization:raw_titanium", "#c:raw_materials/titanium"),
    TUNGSTEN(3, 24, mods("modern_industrialization"),
            "modern_industrialization:raw_tungsten", "#c:raw_materials/tungsten"),
    PLATINUM(3, 24, mods("modern_industrialization"),
            "modern_industrialization:raw_platinum", "#c:raw_materials/platinum"),
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
    HUNT_HYDRA(3, 1, mods("twilightforest"));

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
        return false;
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
            MAP.put(OSMIUM, new String[]{"mekanism:ingot_osmium", "#c:ingots/osmium"});
            MAP.put(TIN, new String[]{"mekanism:ingot_tin", "thermal:tin_ingot", "#c:ingots/tin"});
            MAP.put(LEAD, new String[]{"mekanism:ingot_lead", "thermal:lead_ingot",
                    "immersiveengineering:ingot_lead", "#c:ingots/lead"});
            MAP.put(URANIUM, new String[]{"mekanism:ingot_uranium", "immersiveengineering:ingot_uranium",
                    "#c:ingots/uranium"});
            MAP.put(ZINC, new String[]{"create:zinc_ingot", "#c:ingots/zinc"});
            MAP.put(URANINITE, new String[]{"powah:uraninite"});
            MAP.put(DRACONIUM, new String[]{"draconicevolution:draconium_ingot", "#c:ingots/draconium"});
            MAP.put(THORIUM, new String[]{"create_new_age:thorium_ingot", "#c:ingots/thorium"});
            MAP.put(SILVER, new String[]{"thermal:silver_ingot", "immersiveengineering:ingot_silver", "#c:ingots/silver"});
            MAP.put(NICKEL, new String[]{"thermal:nickel_ingot", "immersiveengineering:ingot_nickel", "#c:ingots/nickel"});
            MAP.put(ALUMINUM, new String[]{"immersiveengineering:ingot_aluminum", "#c:ingots/aluminum"});
            MAP.put(TITANIUM, new String[]{"#c:ingots/titanium"});
            MAP.put(TUNGSTEN, new String[]{"#c:ingots/tungsten"});
            MAP.put(PLATINUM, new String[]{"#c:ingots/platinum"});
            MAP.put(MINIONITE, new String[]{"minecraft:netherite_ingot"});
            MAP.put(HUNT_ZOMBIE, new String[]{"minecraft:iron_ingot"});
            MAP.put(HUNT_SKELETON, new String[]{"minecraft:iron_ingot"});
            MAP.put(HUNT_BLAZE, new String[]{"minecraft:gold_ingot"});
            MAP.put(HUNT_ENDERMAN, new String[]{"minecraft:gold_ingot"});
            // боссы: прокачка и крафт требуют редкий минионит
            MAP.put(HUNT_GUARDIAN, new String[]{"techminions:minionite_ingot"});
            MAP.put(HUNT_NAGA, new String[]{"techminions:minionite_ingot"});
            MAP.put(HUNT_HYDRA, new String[]{"techminions:minionite_ingot"});
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
        }
    }

    /** Миньон-охотник: вместо платформы периодически «убивает» моба и кладёт его добычу в хранилище. */
    public boolean isHunter() {
        return Hunts.MAP.containsKey(this);
    }

    public boolean isBoss() {
        Hunt hunt = Hunts.MAP.get(this);
        return hunt != null && hunt.boss();
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
        return this == MINIONITE ? 4 : 1;
    }

    public String describeSource() {
        Hunt hunt = Hunts.MAP.get(this);
        return hunt != null ? "hunts " + hunt.entity() : String.valueOf(getResource());
    }

    public boolean isAvailable() {
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
