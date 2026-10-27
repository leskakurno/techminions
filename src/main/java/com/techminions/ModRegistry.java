package com.techminions;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class ModRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TechMinions.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TechMinions.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TechMinions.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TechMinions.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, TechMinions.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, TechMinions.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TechMinions.MOD_ID);

    // Временная руда: без дропа, поршни её не двигают.
    public static final DeferredBlock<TempOreBlock> TEMP_ORE = BLOCKS.registerBlock(
            "temp_ore",
            TempOreBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(1.5F)
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.BLOCK)
                    .noLootTable());

    // ---------- Сундуки миньонов: три размера, крафтятся отдельно (не улучшаются друг в друга) ----------
    public static final DeferredBlock<MinionChestBlock> MINION_CHEST_SMALL = BLOCKS.registerBlock(
            "minion_chest_small",
            props -> new MinionChestBlock(props, MinionChestTier.SMALL),
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(7.0F).sound(SoundType.WOOD));
    public static final DeferredBlock<MinionChestBlock> MINION_CHEST_MEDIUM = BLOCKS.registerBlock(
            "minion_chest_medium",
            props -> new MinionChestBlock(props, MinionChestTier.MEDIUM),
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(10.0F).sound(SoundType.WOOD));
    public static final DeferredBlock<MinionChestBlock> MINION_CHEST_LARGE = BLOCKS.registerBlock(
            "minion_chest_large",
            props -> new MinionChestBlock(props, MinionChestTier.LARGE),
            BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(13.0F).sound(SoundType.WOOD));

    // Центральный склад: большой ограниченный склад, миньоны отправляют сюда добычу по привязке (Ключ склада)
    public static final DeferredBlock<MinionChestBlock> MINION_WAREHOUSE = BLOCKS.registerBlock(
            "minion_warehouse",
            props -> new MinionChestBlock(props, MinionChestTier.WAREHOUSE),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(25.0F).sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> MINION_WAREHOUSE_ITEM = ITEMS.registerSimpleBlockItem(MINION_WAREHOUSE);
    public static final DeferredHolder<Item, WarehouseKeyItem> WAREHOUSE_KEY = ITEMS.register("warehouse_key",
            () -> new WarehouseKeyItem(new Item.Properties().stacksTo(1)));

    // Дроблёные руды для колёс дробления Create (текстуры свои): из руд Powah и Mekanism
    public static final DeferredItem<Item> CRUSHED_URANINITE = ITEMS.registerSimpleItem("crushed_uraninite");
    public static final DeferredItem<Item> CRUSHED_OSMIUM = ITEMS.registerSimpleItem("crushed_osmium");
    public static final DeferredItem<Item> CRUSHED_TIN = ITEMS.registerSimpleItem("crushed_tin");
    public static final DeferredItem<Item> CRUSHED_LEAD = ITEMS.registerSimpleItem("crushed_lead");
    public static final DeferredItem<Item> CRUSHED_URANIUM = ITEMS.registerSimpleItem("crushed_uranium");

    // --- v4.0: ящик улучшений, пьедесталы, лампы, таблички, заказы ---
    public static final DeferredBlock<MinionChestBlock> UPGRADE_CRATE = BLOCKS.registerBlock(
            "upgrade_crate",
            props -> new MinionChestBlock(props, MinionChestTier.UPGRADE_CRATE),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(5.0F).sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> UPGRADE_CRATE_ITEM = ITEMS.registerSimpleBlockItem(UPGRADE_CRATE);

    public static final DeferredBlock<DisplayBlock> PEDESTAL = BLOCKS.registerBlock("pedestal",
            props -> new DisplayBlock(props, DisplayBlock.Kind.PEDESTAL),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(4.0F).sound(SoundType.STONE).noOcclusion());
    public static final DeferredBlock<DisplayBlock> DISPLAY_CASE = BLOCKS.registerBlock("display_case",
            props -> new DisplayBlock(props, DisplayBlock.Kind.CASE),
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(2.0F).sound(SoundType.GLASS).noOcclusion());
    public static final DeferredBlock<DisplayBlock> EGG_RACK = BLOCKS.registerBlock("egg_rack",
            props -> new DisplayBlock(props, DisplayBlock.Kind.RACK),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> PEDESTAL_ITEM = ITEMS.registerSimpleBlockItem(PEDESTAL);
    public static final DeferredItem<BlockItem> DISPLAY_CASE_ITEM = ITEMS.registerSimpleBlockItem(DISPLAY_CASE);
    public static final DeferredItem<BlockItem> EGG_RACK_ITEM = ITEMS.registerSimpleBlockItem(EGG_RACK);

    public static final DeferredBlock<SignalLampBlock> SIGNAL_LAMP = BLOCKS.registerBlock("signal_lamp", SignalLampBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(2.0F).sound(SoundType.GLASS)
                    .lightLevel(state -> state.getValue(SignalLampBlock.STATUS) == 0 ? 0 : state.getValue(SignalLampBlock.STATUS) == 3 ? 4 : 12));
    public static final DeferredBlock<StatusSignBlock> STATUS_SIGN = BLOCKS.registerBlock("status_sign", StatusSignBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(2.0F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<ManualSwitchBlock> MANUAL_SWITCH = BLOCKS.registerBlock("manual_switch", ManualSwitchBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.0F).sound(SoundType.METAL));
    public static final DeferredBlock<OrderBlock> ORDER_BLOCK = BLOCKS.registerBlock("order_block", OrderBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(4.0F).sound(SoundType.METAL));
    public static final DeferredBlock<LineSignBlock> LINE_SIGN = BLOCKS.registerBlock("line_sign", LineSignBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5F).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<Block> CORE_BENCH = BLOCKS.registerBlock("core_bench",
            props -> new net.minecraft.world.level.block.CraftingTableBlock(props),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(3.5F).sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> SIGNAL_LAMP_ITEM = ITEMS.registerSimpleBlockItem(SIGNAL_LAMP);
    public static final DeferredItem<BlockItem> STATUS_SIGN_ITEM = ITEMS.registerSimpleBlockItem(STATUS_SIGN);
    public static final DeferredItem<BlockItem> MANUAL_SWITCH_ITEM = ITEMS.registerSimpleBlockItem(MANUAL_SWITCH);
    public static final DeferredItem<BlockItem> ORDER_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(ORDER_BLOCK);
    public static final DeferredItem<BlockItem> LINE_SIGN_ITEM = ITEMS.registerSimpleBlockItem(LINE_SIGN);
    public static final DeferredItem<BlockItem> CORE_BENCH_ITEM = ITEMS.registerSimpleBlockItem(CORE_BENCH);

    // Трофеи и инструменты v4
    public static final DeferredHolder<Item, Item> ANCIENT_CRUMB = ITEMS.register("ancient_crumb",
            () -> new ToolItems.Plain(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), "tooltip.techminions.ancient_crumb"));
    public static final DeferredHolder<Item, Item> SEEKER_NOTE = ITEMS.register("seeker_note",
            () -> new ToolItems.Plain(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE), "tooltip.techminions.seeker_note"));
    public static final DeferredHolder<Item, Item> ENGINEER_RELIC = ITEMS.register("engineer_relic",
            () -> new ToolItems.Plain(new Item.Properties().rarity(net.minecraft.world.item.Rarity.EPIC), "tooltip.techminions.engineer_relic"));
    public static final DeferredHolder<Item, Item> FIRST_SHIFT_MEDAL = ITEMS.register("first_shift_medal",
            () -> new ToolItems.Plain(new Item.Properties().stacksTo(1), "tooltip.techminions.first_shift_medal"));
    public static final DeferredHolder<Item, Item> ARCHIVE_TOKEN = ITEMS.register("archive_token",
            () -> new ToolItems.Plain(new Item.Properties(), "tooltip.techminions.archive_token"));
    public static final DeferredHolder<Item, Item> EXPEDITION_CERTIFICATE = ITEMS.register("expedition_certificate",
            () -> new ToolItems.Plain(new Item.Properties().stacksTo(1), "tooltip.techminions.expedition_certificate"));
    public static final DeferredHolder<Item, Item> STORAGE_SEAL = ITEMS.register("storage_seal",
            () -> new ToolItems.Plain(new Item.Properties(), "tooltip.techminions.storage_seal"));
    public static final DeferredHolder<Item, Item> CONTRACT_SEAL = ITEMS.register("contract_seal",
            () -> new ToolItems.Plain(new Item.Properties(), "tooltip.techminions.contract_seal"));
    public static final DeferredHolder<Item, Item> UNPREDICTABLE_CUBE = ITEMS.register("unpredictable_cube",
            () -> new ToolItems.CubeItem(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> SERVICE_VOUCHER = ITEMS.register("service_voucher",
            () -> new ToolItems.VoucherItem(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<Item, Item> MINION_PASSPORT = ITEMS.register("minion_passport",
            () -> new ToolItems.PassportItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> BLACK_BOX = ITEMS.register("black_box",
            () -> new ToolItems.BlackBoxItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> BINDING_KEY = ITEMS.register("binding_key",
            () -> new ToolItems.BindingKeyItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> MASTER_COMPASS = ITEMS.register("master_compass",
            () -> new ToolItems.CompassItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> SHIFT_CLOCK = ITEMS.register("shift_clock",
            () -> new ToolItems.ClockItem(new Item.Properties().stacksTo(1)));

    public static final Supplier<BlockEntityType<DisplayBlockEntity>> DISPLAY_BE = BLOCK_ENTITIES.register("display",
            () -> BlockEntityType.Builder.of(DisplayBlockEntity::new, PEDESTAL.get(), DISPLAY_CASE.get(), EGG_RACK.get()).build(null));
    public static final Supplier<BlockEntityType<StatusLinkBlockEntity>> LINK_BE = BLOCK_ENTITIES.register("status_link",
            () -> BlockEntityType.Builder.of(StatusLinkBlockEntity::new, SIGNAL_LAMP.get(), STATUS_SIGN.get()).build(null));
    public static final Supplier<BlockEntityType<OrderBlockEntity>> ORDER_BE = BLOCK_ENTITIES.register("order",
            () -> BlockEntityType.Builder.of(OrderBlockEntity::new, ORDER_BLOCK.get()).build(null));

    // Станция разборки миньонов (возврат ядра и части материалов)
    public static final DeferredBlock<MinionStationBlock> MINION_STATION = BLOCKS.registerBlock(
            "minion_station",
            MinionStationBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0F).sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> MINION_STATION_ITEM = ITEMS.registerSimpleBlockItem(MINION_STATION);

    // --- Декор и предметы (пакет 1) ---
    public static final DeferredBlock<Block> MINIONITE_PANEL = BLOCKS.registerBlock("minionite_panel", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(6.0F).sound(SoundType.METAL));
    public static final DeferredBlock<Block> HARDENED_MINIONITE = BLOCKS.registerBlock("hardened_minionite", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(14.0F, 40.0F).sound(SoundType.NETHERITE_BLOCK));
    public static final DeferredBlock<StairBlock> HARDENED_MINIONITE_STAIRS = BLOCKS.registerBlock("hardened_minionite_stairs",
            props -> new StairBlock(HARDENED_MINIONITE.get().defaultBlockState(), props),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(14.0F, 40.0F).sound(SoundType.NETHERITE_BLOCK));
    public static final DeferredBlock<SlabBlock> HARDENED_MINIONITE_SLAB = BLOCKS.registerBlock("hardened_minionite_slab", SlabBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(14.0F, 40.0F).sound(SoundType.NETHERITE_BLOCK));
    public static final DeferredBlock<Block> WARNING_BLOCK = BLOCKS.registerBlock("warning_block", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(4.0F).sound(SoundType.METAL));
    public static final DeferredBlock<Block> LAB_LAMP = BLOCKS.registerBlock("lab_lamp", Block::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(2.0F).sound(SoundType.GLASS).lightLevel(state -> 15));
    public static final DeferredItem<BlockItem> MINIONITE_PANEL_ITEM = ITEMS.registerSimpleBlockItem(MINIONITE_PANEL);
    public static final DeferredItem<BlockItem> HARDENED_MINIONITE_ITEM = ITEMS.registerSimpleBlockItem(HARDENED_MINIONITE);
    public static final DeferredItem<BlockItem> HARDENED_MINIONITE_STAIRS_ITEM = ITEMS.registerSimpleBlockItem(HARDENED_MINIONITE_STAIRS);
    public static final DeferredItem<BlockItem> HARDENED_MINIONITE_SLAB_ITEM = ITEMS.registerSimpleBlockItem(HARDENED_MINIONITE_SLAB);
    public static final DeferredItem<BlockItem> WARNING_BLOCK_ITEM = ITEMS.registerSimpleBlockItem(WARNING_BLOCK);
    public static final DeferredItem<BlockItem> LAB_LAMP_ITEM = ITEMS.registerSimpleBlockItem(LAB_LAMP);
    // Полевой паёк: сытнее хлеба, но слабее стейка
    public static final DeferredItem<Item> FIELD_RATION = ITEMS.registerSimpleItem("field_ration",
            new Item.Properties().food(new FoodProperties.Builder().nutrition(7).saturationModifier(0.8F).build()));
    public static final DeferredItem<Item> EMPTY_TAG = ITEMS.registerSimpleItem("empty_tag");

    public static final DeferredItem<BlockItem> MINION_CHEST_SMALL_ITEM = ITEMS.registerSimpleBlockItem(MINION_CHEST_SMALL);
    public static final DeferredItem<BlockItem> MINION_CHEST_MEDIUM_ITEM = ITEMS.registerSimpleBlockItem(MINION_CHEST_MEDIUM);
    public static final DeferredItem<BlockItem> MINION_CHEST_LARGE_ITEM = ITEMS.registerSimpleBlockItem(MINION_CHEST_LARGE);

    /** Улучшения сундука: маленький -> средний -> большой (ПКМ по сундуку, содержимое сохраняется). */
    public static final DeferredHolder<Item, ChestUpgradeItem> CHEST_UPGRADE_MEDIUM = ITEMS.register("chest_upgrade_medium",
            () -> new ChestUpgradeItem(new Item.Properties().stacksTo(16), MinionChestTier.MEDIUM));
    public static final DeferredHolder<Item, ChestUpgradeItem> CHEST_UPGRADE_LARGE = ITEMS.register("chest_upgrade_large",
            () -> new ChestUpgradeItem(new Item.Properties().stacksTo(16), MinionChestTier.LARGE));

    public static MinionChestBlock chestBlock(MinionChestTier tier) {
        return switch (tier) {
            case SMALL -> MINION_CHEST_SMALL.get();
            case MEDIUM -> MINION_CHEST_MEDIUM.get();
            case LARGE -> MINION_CHEST_LARGE.get();
            case WAREHOUSE -> MINION_WAREHOUSE.get();
            case UPGRADE_CRATE -> UPGRADE_CRATE.get();
        };
    }

    public static final Supplier<BlockEntityType<MinionChestBlockEntity>> MINION_CHEST_BE = BLOCK_ENTITIES.register(
            "minion_chest",
            () -> BlockEntityType.Builder.of(MinionChestBlockEntity::new,
                    MINION_CHEST_SMALL.get(), MINION_CHEST_MEDIUM.get(), MINION_CHEST_LARGE.get(), MINION_WAREHOUSE.get(), UPGRADE_CRATE.get()).build(null));

    public static final Map<MinionType, DeferredHolder<Item, MinionItem>> MINION_ITEMS = new EnumMap<>(MinionType.class);
    public static final Map<UpgradeType, DeferredHolder<Item, UpgradeItem>> UPGRADE_ITEMS = new EnumMap<>(UpgradeType.class);

    static {
        for (MinionType type : MinionType.values()) {
            if (!type.isModLoaded()) {
                continue; // мод ресурса не установлен: предмет не регистрируем
            }
            MINION_ITEMS.put(type, ITEMS.register(
                    "minion_" + type.getSerializedName(),
                    () -> new MinionItem(new Item.Properties().stacksTo(1), type)));
        }
        for (UpgradeType type : UpgradeType.values()) {
            UPGRADE_ITEMS.put(type, ITEMS.register(
                    "upgrade_" + type.getSerializedName(),
                    () -> new UpgradeItem(new Item.Properties().stacksTo(type == UpgradeType.FILTER || type == UpgradeType.LIMITER ? 1 : 16), type)));
        }
    }

    // ---------- Планшет миньонов: просмотр добычи издалека (только просмотр, забрать предметы нельзя) ----------
    public static final DeferredHolder<Item, TabletItem> TABLET = ITEMS.register("tablet",
            () -> new TabletItem(new Item.Properties().stacksTo(1), false));
    // Элитный планшет: очень дорогой, всё встроено, принимает улучшение удалённого доступа.
    public static final DeferredHolder<Item, TabletItem> TABLET_ELITE = ITEMS.register("tablet_elite",
            () -> new TabletItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC).fireResistant(), true));
    public static final Map<TabletUpgradeItem.Kind, DeferredHolder<Item, TabletUpgradeItem>> TABLET_UPGRADES =
            new EnumMap<>(TabletUpgradeItem.Kind.class);

    static {
        for (TabletUpgradeItem.Kind kind : TabletUpgradeItem.Kind.values()) {
            TABLET_UPGRADES.put(kind, ITEMS.register(
                    "tablet_upgrade_" + kind.getSerializedName(),
                    () -> new TabletUpgradeItem(new Item.Properties().stacksTo(16), kind)));
        }
    }

    // Яйцо охоты: как яйцо призыва по виду, но моба не призывает (см. HuntEggItem).
    public static final DeferredHolder<Item, HuntEggItem> HUNT_EGG = ITEMS.register("hunt_egg",
            () -> new HuntEggItem(new Item.Properties().stacksTo(16)));

    public static final DeferredHolder<Item, Item> CORE_BASIC = ITEMS.register("core_basic", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CORE_REINFORCED = ITEMS.register("core_reinforced", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> CORE_ELITE = ITEMS.register("core_elite", () -> new Item(new Item.Properties()));

    // ---------- Минионит: редкий материал (добывается только особым миньоном, см. MinionType.MINIONITE) ----------
    public static final DeferredHolder<Item, Item> MINIONITE_SHARD = ITEMS.register("minionite_shard", () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> MINIONITE_INGOT = ITEMS.register("minionite_ingot", () -> new Item(new Item.Properties().fireResistant()));

    // Броня лучше незеритовой: защита 24 (у незерита 20), прочность x50, стойкость 5, сопротивление отбрасыванию 0.15 на деталь.
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MINIONITE_ARMOR = ARMOR_MATERIALS.register("minionite",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                        m.put(ArmorItem.Type.BOOTS, 4);
                        m.put(ArmorItem.Type.LEGGINGS, 7);
                        m.put(ArmorItem.Type.CHESTPLATE, 9);
                        m.put(ArmorItem.Type.HELMET, 4);
                        m.put(ArmorItem.Type.BODY, 13);
                    }),
                    25,
                    SoundEvents.ARMOR_EQUIP_NETHERITE,
                    () -> Ingredient.of(MINIONITE_INGOT.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(TechMinions.MOD_ID, "minionite"))),
                    5.0F,
                    0.15F));

    public static final DeferredHolder<Item, ArmorItem> MINIONITE_HELMET = ITEMS.register("minionite_helmet",
            () -> new ArmorItem(MINIONITE_ARMOR, ArmorItem.Type.HELMET,
                    new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(50)).fireResistant()));
    public static final DeferredHolder<Item, ArmorItem> MINIONITE_CHESTPLATE = ITEMS.register("minionite_chestplate",
            () -> new ArmorItem(MINIONITE_ARMOR, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(50)).fireResistant()));
    public static final DeferredHolder<Item, ArmorItem> MINIONITE_LEGGINGS = ITEMS.register("minionite_leggings",
            () -> new ArmorItem(MINIONITE_ARMOR, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().durability(ArmorItem.Type.LEGGINGS.getDurability(50)).fireResistant()));
    public static final DeferredHolder<Item, ArmorItem> MINIONITE_BOOTS = ITEMS.register("minionite_boots",
            () -> new ArmorItem(MINIONITE_ARMOR, ArmorItem.Type.BOOTS,
                    new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(50)).fireResistant()));

    public static final DeferredHolder<Item, SwordItem> MINIONITE_SWORD = ITEMS.register("minionite_sword",
            () -> new SwordItem(ModTiers.MINIONITE, new Item.Properties().fireResistant()
                    .attributes(SwordItem.createAttributes(ModTiers.MINIONITE, 5, -2.4F))));
    public static final DeferredHolder<Item, PickaxeItem> MINIONITE_PICKAXE = ITEMS.register("minionite_pickaxe",
            () -> new PickaxeItem(ModTiers.MINIONITE, new Item.Properties().fireResistant()
                    .attributes(PickaxeItem.createAttributes(ModTiers.MINIONITE, 1.0F, -2.8F))));
    public static final DeferredHolder<Item, AxeItem> MINIONITE_AXE = ITEMS.register("minionite_axe",
            () -> new AxeItem(ModTiers.MINIONITE, new Item.Properties().fireResistant()
                    .attributes(AxeItem.createAttributes(ModTiers.MINIONITE, 6.0F, -3.0F))));
    public static final DeferredHolder<Item, ShovelItem> MINIONITE_SHOVEL = ITEMS.register("minionite_shovel",
            () -> new ShovelItem(ModTiers.MINIONITE, new Item.Properties().fireResistant()
                    .attributes(ShovelItem.createAttributes(ModTiers.MINIONITE, 1.5F, -3.0F))));
    public static final DeferredHolder<Item, HoeItem> MINIONITE_HOE = ITEMS.register("minionite_hoe",
            () -> new HoeItem(ModTiers.MINIONITE, new Item.Properties().fireResistant()
                    .attributes(HoeItem.createAttributes(ModTiers.MINIONITE, -6.0F, 0.0F))));

    public static final Supplier<EntityType<MinionEntity>> MINION = ENTITY_TYPES.register(
            "minion",
            () -> EntityType.Builder.of(MinionEntity::new, MobCategory.MISC)
                    .sized(0.5F, 1.45F)
                    .clientTrackingRange(8)
                    .build("minion"));

    public static final Supplier<MenuType<MinionMenu>> MINION_MENU = MENUS.register(
            "minion",
            () -> IMenuTypeExtension.create(MinionMenu::new));

    public static final Supplier<MenuType<MinionChestMenu>> MINION_CHEST_MENU = MENUS.register(
            "minion_chest",
            () -> IMenuTypeExtension.create(MinionChestMenu::new));

    public static final Supplier<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.techminions"))
                    .icon(() -> new ItemStack(MINION_ITEMS.get(MinionType.DIAMOND).get()))
                    .displayItems((params, output) -> {
                        for (DeferredHolder<Item, MinionItem> holder : MINION_ITEMS.values()) {
                            if (holder.get().getMinionType().isAvailable()) {
                                output.accept(holder.get());
                            }
                        }
                        output.accept(CORE_BASIC.get());
                        output.accept(CORE_REINFORCED.get());
                        output.accept(CORE_ELITE.get());
                        for (MinionType type : MinionType.values()) {
                            if (type.getHuntEntity().isPresent()) {
                                output.accept(HuntEggItem.create(type.getHuntEntity().get()));
                            }
                        }
                        output.accept(MINION_CHEST_SMALL_ITEM.get());
                        output.accept(MINION_CHEST_MEDIUM_ITEM.get());
                        output.accept(MINION_CHEST_LARGE_ITEM.get());
                        output.accept(MINION_WAREHOUSE_ITEM.get());
                        output.accept(WAREHOUSE_KEY.get());
                        output.accept(CRUSHED_URANINITE.get());
                        output.accept(CRUSHED_OSMIUM.get());
                        output.accept(CRUSHED_TIN.get());
                        output.accept(CRUSHED_LEAD.get());
                        output.accept(CRUSHED_URANIUM.get());
                        output.accept(MINION_STATION_ITEM.get());
                        output.accept(MINIONITE_PANEL_ITEM.get());
                        output.accept(HARDENED_MINIONITE_ITEM.get());
                        output.accept(HARDENED_MINIONITE_STAIRS_ITEM.get());
                        output.accept(HARDENED_MINIONITE_SLAB_ITEM.get());
                        output.accept(WARNING_BLOCK_ITEM.get());
                        output.accept(LAB_LAMP_ITEM.get());
                        output.accept(FIELD_RATION.get());
                        output.accept(EMPTY_TAG.get());
                        output.accept(UPGRADE_CRATE_ITEM.get());
                        output.accept(PEDESTAL_ITEM.get());
                        output.accept(DISPLAY_CASE_ITEM.get());
                        output.accept(EGG_RACK_ITEM.get());
                        output.accept(SIGNAL_LAMP_ITEM.get());
                        output.accept(STATUS_SIGN_ITEM.get());
                        output.accept(MANUAL_SWITCH_ITEM.get());
                        output.accept(ORDER_BLOCK_ITEM.get());
                        output.accept(LINE_SIGN_ITEM.get());
                        output.accept(CORE_BENCH_ITEM.get());
                        output.accept(STORAGE_SEAL.get());
                        output.accept(CONTRACT_SEAL.get());
                        output.accept(BINDING_KEY.get());
                        output.accept(MASTER_COMPASS.get());
                        output.accept(SHIFT_CLOCK.get());
                        output.accept(MINION_PASSPORT.get());
                        output.accept(BLACK_BOX.get());
                        output.accept(UNPREDICTABLE_CUBE.get());
                        output.accept(SERVICE_VOUCHER.get());
                        output.accept(ANCIENT_CRUMB.get());
                        output.accept(SEEKER_NOTE.get());
                        output.accept(ENGINEER_RELIC.get());
                        output.accept(FIRST_SHIFT_MEDAL.get());
                        output.accept(ARCHIVE_TOKEN.get());
                        output.accept(EXPEDITION_CERTIFICATE.get());
                        output.accept(MINIONITE_SHARD.get());
                        output.accept(MINIONITE_INGOT.get());
                        output.accept(MINIONITE_HELMET.get());
                        output.accept(MINIONITE_CHESTPLATE.get());
                        output.accept(MINIONITE_LEGGINGS.get());
                        output.accept(MINIONITE_BOOTS.get());
                        output.accept(MINIONITE_SWORD.get());
                        output.accept(MINIONITE_PICKAXE.get());
                        output.accept(MINIONITE_AXE.get());
                        output.accept(MINIONITE_SHOVEL.get());
                        output.accept(MINIONITE_HOE.get());
                        for (DeferredHolder<Item, UpgradeItem> holder : UPGRADE_ITEMS.values()) {
                            output.accept(holder.get());
                        }
                        output.accept(CHEST_UPGRADE_MEDIUM.get());
                        output.accept(CHEST_UPGRADE_LARGE.get());
                        output.accept(TABLET.get());
                        output.accept(TABLET_ELITE.get());
                        for (DeferredHolder<Item, TabletUpgradeItem> holder : TABLET_UPGRADES.values()) {
                            output.accept(holder.get());
                        }
                    })
                    .build());
}
