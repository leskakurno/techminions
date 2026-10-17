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
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.WOOD));
    public static final DeferredBlock<MinionChestBlock> MINION_CHEST_MEDIUM = BLOCKS.registerBlock(
            "minion_chest_medium",
            props -> new MinionChestBlock(props, MinionChestTier.MEDIUM),
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(3.0F).sound(SoundType.WOOD));
    public static final DeferredBlock<MinionChestBlock> MINION_CHEST_LARGE = BLOCKS.registerBlock(
            "minion_chest_large",
            props -> new MinionChestBlock(props, MinionChestTier.LARGE),
            BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(4.0F).sound(SoundType.WOOD));

    public static final DeferredItem<BlockItem> MINION_CHEST_SMALL_ITEM = ITEMS.registerSimpleBlockItem(MINION_CHEST_SMALL);
    public static final DeferredItem<BlockItem> MINION_CHEST_MEDIUM_ITEM = ITEMS.registerSimpleBlockItem(MINION_CHEST_MEDIUM);
    public static final DeferredItem<BlockItem> MINION_CHEST_LARGE_ITEM = ITEMS.registerSimpleBlockItem(MINION_CHEST_LARGE);

    public static final Supplier<BlockEntityType<MinionChestBlockEntity>> MINION_CHEST_BE = BLOCK_ENTITIES.register(
            "minion_chest",
            () -> BlockEntityType.Builder.of(MinionChestBlockEntity::new,
                    MINION_CHEST_SMALL.get(), MINION_CHEST_MEDIUM.get(), MINION_CHEST_LARGE.get()).build(null));

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
                    () -> new UpgradeItem(new Item.Properties().stacksTo(16), type)));
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
                    .sized(0.5F, 1.2F)
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
                    })
                    .build());
}
