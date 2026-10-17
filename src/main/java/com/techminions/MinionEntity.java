package com.techminions;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Живой миньон. Стоит на месте (NoAI). Цикл как в Hypixel Skyblock:
 * 1) ставит по ОДНОМУ блоку временной руды за шаг, пока площадка 5x5 не построена целиком;
 * 2) затем ломает по ОДНОМУ блоку за шаг, пока площадка не исчезнет;
 * 3) повторяет. Длительность шага одна и та же для постройки и для добычи и зависит от уровня.
 */
public class MinionEntity extends Mob {
    public static final int MAX_LEVEL = 11;
    public static final int STORAGE_SIZE = 15;
    public static final int UPGRADE_SLOTS = 3;
    private static final int RADIUS = 2; // 5x5
    private static final String[] ROMAN = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"};

    private static final EntityDataAccessor<Integer> DATA_TYPE =
            SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LEVEL =
            SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PROGRESS =
            SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_MINED =
            SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> DATA_PLATE =
            SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_PLATE_MAX =
            SynchedEntityData.defineId(MinionEntity.class, EntityDataSerializers.INT);

    private final SimpleContainer storage = new SimpleContainer(STORAGE_SIZE);
    private final SimpleContainer upgrades = new SimpleContainer(UPGRADE_SLOTS);
    private final SimpleContainer eggs = new SimpleContainer(1); // слот яйца охоты (только охотники)
    // Позиции временной руды этого миньона. Чужие блоки сюда никогда не попадают.
    private final Set<BlockPos> ownedBlocks = new LinkedHashSet<>();

    // Фаза цикла: true = строим платформу, false = ломаем. plan = ещё не поставленные позиции.
    private boolean placing = true;
    private boolean huntArmed = false; // охотник: цикл «появление -> убийство» запущен
    private boolean huntKilling = false; // false = моб «появляется», true = моба «убивают»
    private final List<BlockPos> plan = new ArrayList<>();

    private int cooldown = 40;
    private int cooldownMax = 40;

    public MinionEntity(EntityType<? extends MinionEntity> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setPersistenceRequired();
        this.setCustomNameVisible(true);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    // ---------- Синхронизируемые данные ----------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_TYPE, 0);
        builder.define(DATA_LEVEL, 1);
        builder.define(DATA_STATE, 0);
        builder.define(DATA_PROGRESS, 0);
        builder.define(DATA_MINED, 0);
        builder.define(DATA_PLATE, 0);
        builder.define(DATA_PLATE_MAX, 0);
    }

    public MinionType getMinionType() {
        return MinionType.byOrdinal(entityData.get(DATA_TYPE));
    }

    public void setMinionType(MinionType type) {
        entityData.set(DATA_TYPE, type.ordinal());
    }

    public int getMinionLevel() {
        return Mth.clamp(entityData.get(DATA_LEVEL), 1, MAX_LEVEL);
    }

    public MinionState getState() {
        return MinionState.byOrdinal(entityData.get(DATA_STATE));
    }

    public int getProgress() {
        return entityData.get(DATA_PROGRESS);
    }

    public int getMined() {
        return entityData.get(DATA_MINED);
    }

    /** Сколько блоков платформы стоит сейчас. */
    public int getPlateBlocks() {
        return entityData.get(DATA_PLATE);
    }

    /** Сколько блоков в платформе в этом цикле. */
    public int getPlateMax() {
        return entityData.get(DATA_PLATE_MAX);
    }

    private void setState(MinionState state) {
        if (getState() != state) {
            entityData.set(DATA_STATE, state.ordinal());
            refreshName();
        }
    }

    private void setProgress(int value) {
        entityData.set(DATA_PROGRESS, Mth.clamp(value, 0, 100));
    }

    public static String roman(int level) {
        return ROMAN[Mth.clamp(level, 1, MAX_LEVEL)];
    }

    // ---------- Формулы ----------

    public static int capacityFor(int level, int storageUpgrades) {
        return Math.min(STORAGE_SIZE, 2 + level + 2 * storageUpgrades);
    }

    public static int intervalFor(int level, int speedUpgrades) {
        double ticks = Config.BASE_TICKS.get() * Math.pow(0.88, level - 1) * Math.pow(0.8, speedUpgrades);
        return (int) Math.max(Config.MIN_TICKS.get(), Math.round(ticks));
    }

    /** Длительность шага с учётом типа: минионит медленнее, охотники ждут «убийство» (боссы - намного дольше). */
    public static int intervalFor(MinionType type, int level, int speedUpgrades) {
        return intervalFor(level, speedUpgrades) * type.getTimeMultiplier();
    }

    public int costForNext() {
        return Math.max(1, (int) Math.round(getMinionType().getUpgradeBase() * getMinionLevel() * Config.UPGRADE_COST_MULTIPLIER.get()));
    }

    private int upgradeCount(UpgradeType type) {
        int count = 0;
        for (int i = 0; i < upgrades.getContainerSize(); i++) {
            if (upgrades.getItem(i).getItem() instanceof UpgradeItem item && item.getUpgradeType() == type) {
                count++;
            }
        }
        return count;
    }

    private int capacity() {
        return capacityFor(getMinionLevel(), upgradeCount(UpgradeType.STORAGE));
    }

    private int interval() {
        return intervalFor(getMinionType(), getMinionLevel(), upgradeCount(UpgradeType.SPEED));
    }

    // ---------- Внешний вид ----------

    public void refreshVisuals() {
        updateTool();
        refreshName();
    }

    private void updateTool() {
        if (getMinionType().isHunter()) {
            Item sword = switch ((getMinionLevel() - 1) / 2) {
                case 0 -> Items.WOODEN_SWORD;
                case 1 -> Items.STONE_SWORD;
                case 2 -> Items.IRON_SWORD;
                case 3 -> Items.GOLDEN_SWORD;
                case 4 -> Items.DIAMOND_SWORD;
                default -> Items.NETHERITE_SWORD;
            };
            if (!getItemBySlot(EquipmentSlot.MAINHAND).is(sword)) {
                setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(sword));
            }
            return;
        }
        Item pickaxe = switch ((getMinionLevel() - 1) / 2) {
            case 0 -> Items.WOODEN_PICKAXE;
            case 1 -> Items.STONE_PICKAXE;
            case 2 -> Items.IRON_PICKAXE;
            case 3 -> Items.GOLDEN_PICKAXE;
            case 4 -> Items.DIAMOND_PICKAXE;
            default -> Items.NETHERITE_PICKAXE;
        };
        if (!getItemBySlot(EquipmentSlot.MAINHAND).is(pickaxe)) {
            setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(pickaxe));
        }
    }

    private void refreshName() {
        ChatFormatting color = switch (getState()) {
            case PLACING -> ChatFormatting.AQUA;
            case BREAKING, WORKING -> ChatFormatting.GREEN;
            case FULL -> ChatFormatting.RED;
            case WAITING -> ChatFormatting.GOLD;
            default -> ChatFormatting.YELLOW;
        };
        setCustomName(Component.translatable(
                        "name.techminions.minion",
                        Component.translatable("type.techminions." + getMinionType().getSerializedName()),
                        roman(getMinionLevel()))
                .withStyle(color));
    }

    // ---------- Основной цикл ----------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        if (tickCount % 40 == 0) {
            pushOutput();
        }
        if (cooldown > 0) {
            cooldown--;
            if (tickCount % 5 == 0 && cooldownMax > 0
                    && (getState() == MinionState.PLACING || getState() == MinionState.BREAKING
                    || getState() == MinionState.SPAWNING || getState() == MinionState.KILLING)) {
                setProgress(100 - cooldown * 100 / cooldownMax);
            }
            return;
        }
        runStep();
    }

    private void runStep() {
        pruneOwned();
        syncPlate();
        MinionType type = getMinionType();
        if (!type.isAvailable()) {
            setState(MinionState.UNAVAILABLE);
            setProgress(0);
            cooldown = 100;
            return;
        }
        if (type.isHunter()) {
            stepHunt(type);
            return;
        }
        // Платформа полностью сломана -> начинаем новый цикл со стройки.
        if (!placing && ownedBlocks.isEmpty()) {
            placing = true;
            plan.clear();
        }
        if (placing) {
            stepPlace();
        } else {
            stepBreak(producedStack(type));
        }
        syncPlate();
    }

    /**
     * Что миньон кладёт в хранилище. Обычно это сам ресурс (руда/сырьё), но с улучшением «Плавильня»
     * сырьё сразу переплавляется по обычному рецепту печки (raw_iron -> слиток железа и т.д.).
     * Если у ресурса нет рецепта плавки (уголь, алмаз...), кладётся как есть.
     */
    private ItemStack producedStack(MinionType type) {
        ItemStack raw = new ItemStack(type.getResource());
        if (upgradeCount(UpgradeType.SMELTER) > 0) {
            SingleRecipeInput input = new SingleRecipeInput(raw);
            Optional<RecipeHolder<SmeltingRecipe>> recipe =
                    level().getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level());
            if (recipe.isPresent()) {
                ItemStack result = recipe.get().value().assemble(input, level().registryAccess());
                if (!result.isEmpty()) {
                    return result;
                }
            }
        }
        return raw;
    }

    /** Яйцо охоты в слоте совпадает с целью этого охотника? */
    private boolean hasMatchingEgg(MinionType type) {
        ItemStack egg = eggs.getItem(0);
        if (egg.isEmpty() || !(egg.getItem() instanceof HuntEggItem)) {
            return false;
        }
        EntityType<?> eggType = HuntEggItem.getEntityType(egg);
        return type.acceptsEgg(eggType);
    }

    /** Цель охоты: у обычных охотников фиксированный моб, у универсальных - моб из вставленного яйца. */
    private EntityType<?> huntTarget(MinionType type) {
        if (type.isUniversalHunter()) {
            ItemStack egg = eggs.getItem(0);
            return egg.getItem() instanceof HuntEggItem ? HuntEggItem.getEntityType(egg) : null;
        }
        return type.getHuntEntity().orElse(null);
    }

    /** Длительность фазы охоты в тиках: «появление» моба и затем его «убийство». У боссов намного дольше. */
    public static int huntTicks(MinionType type, int level, int speedUpgrades, boolean killing) {
        long base;
        if (type.isBoss()) {
            base = 1200L * (killing ? Config.HUNT_BOSS_KILL_MIN.get() : Config.HUNT_BOSS_SPAWN_MIN.get());
        } else {
            base = 20L * (killing ? Config.HUNT_MOB_KILL_SEC.get() : Config.HUNT_MOB_SPAWN_SEC.get());
        }
        double scale = 1.0;
        if (Config.HUNT_SCALE.get()) {
            scale = (double) intervalFor(level, speedUpgrades) / Config.BASE_TICKS.get();
        }
        return (int) Math.max(20L, Math.round(base * scale));
    }

    private void scheduleHunt(MinionType type) {
        setProgress(0);
        cooldownMax = huntTicks(type, getMinionLevel(), upgradeCount(UpgradeType.SPEED), huntKilling);
        cooldown = cooldownMax;
    }

    /**
     * Охотник: без платформы и без настоящего моба (моб не появляется в мире, поэтому никому не может навредить).
     * Цикл: «моб появляется из яйца» (босс 45 мин) -> «моба убивают» (босс 15 мин) -> добыча из таблицы лута.
     * Яйцо охоты из слота при этом не тратится. Без подходящего яйца охотник стоит.
     */
    private void stepHunt(MinionType type) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!hasMatchingEgg(type)) {
            huntArmed = false;
            huntKilling = false;
            setState(MinionState.NO_EGG);
            setProgress(0);
            cooldown = 40;
            return;
        }
        if (!huntArmed) { // яйцо только что вставили: начинаем с появления моба
            huntArmed = true;
            huntKilling = false;
            setState(MinionState.SPAWNING);
            scheduleHunt(type);
            return;
        }
        if (!huntKilling) { // моб «появился» -> начинаем убивать
            huntKilling = true;
            setState(MinionState.KILLING);
            scheduleHunt(type);
            return;
        }
        // Убийство завершено.
        if (!storageHasRoom()) {
            setState(MinionState.FULL);
            setProgress(0);
            cooldown = 40;
            return;
        }
        EntityType<?> target = huntTarget(type);
        if (target == null) {
            setState(MinionState.UNAVAILABLE);
            setProgress(0);
            cooldown = 100;
            return;
        }
        swing(InteractionHand.MAIN_HAND);
        List<ItemStack> drops = new ArrayList<>(HuntLoot.roll(serverLevel, target, position()));
        int fortune = upgradeCount(UpgradeType.FORTUNE);
        if (fortune > 0 && random.nextFloat() < 0.3F * fortune) {
            drops.addAll(HuntLoot.roll(serverLevel, target, position())); // бонус как у добычи
        }
        for (ItemStack drop : drops) {
            ItemStack rest = insertIntoStorage(drop, false);
            if (!rest.isEmpty()) {
                spawnAtLocation(rest); // не теряем предметы, если хранилище заполнилось посреди выдачи
            }
        }
        level().playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_STRONG, SoundSource.NEUTRAL, 0.6F, 1.0F);
        entityData.set(DATA_MINED, getMined() + 1);
        huntKilling = false; // следующий моб «появляется» заново
        setState(MinionState.SPAWNING);
        scheduleHunt(type);
    }

    private boolean storageHasRoom() {
        int cap = capacity();
        for (int i = 0; i < cap; i++) {
            ItemStack slot = storage.getItem(i);
            if (slot.isEmpty() || slot.getCount() < Math.min(slot.getMaxStackSize(), storage.getMaxStackSize())) {
                return true;
            }
        }
        return false;
    }

    private void scheduleNext() {
        setProgress(0);
        cooldownMax = interval();
        cooldown = cooldownMax;
    }

    /** Ставит РОВНО ОДИН блок платформы. */
    private void stepPlace() {
        if (plan.isEmpty() && ownedBlocks.isEmpty()) {
            buildPlan();
        }
        BlockState ore = ModRegistry.TEMP_ORE.get().defaultBlockState().setValue(TempOreBlock.TYPE, getMinionType());
        while (!plan.isEmpty()) {
            BlockPos p = plan.remove(0);
            if (!canPlace(p)) {
                continue; // позицию заняли после планирования
            }
            faceTarget(p);
            swing(InteractionHand.MAIN_HAND);
            level().setBlock(p, ore, Block.UPDATE_ALL);
            level().playSound(null, p, ore.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8F, 1.0F);
            ownedBlocks.add(p);
            if (plan.isEmpty()) {
                placing = false; // платформа построена, со следующего шага ломаем
            }
            setState(MinionState.PLACING);
            scheduleNext();
            return;
        }
        // Больше нечего ставить.
        if (ownedBlocks.isEmpty()) {
            setState(MinionState.WAITING); // вокруг всё занято
            setProgress(0);
            cooldown = 100;
        } else {
            placing = false;
            setState(MinionState.BREAKING);
            scheduleNext();
        }
    }

    /** Ломает РОВНО ОДИН блок платформы (в порядке постройки). */
    private void stepBreak(ItemStack sample) {
        if (!insertIntoStorage(sample, true).isEmpty()) {
            setState(MinionState.FULL);
            setProgress(0);
            cooldown = 40;
            return;
        }
        BlockPos target = ownedBlocks.iterator().next();
        faceTarget(target);
        swing(InteractionHand.MAIN_HAND);

        level().destroyBlock(target, false); // частицы и звук, без дропа
        ownedBlocks.remove(target);

        insertIntoStorage(sample.copy(), false);
        int fortune = upgradeCount(UpgradeType.FORTUNE);
        if (fortune > 0 && random.nextFloat() < 0.3F * fortune) {
            insertIntoStorage(sample.copy(), false); // бонус, если есть место
        }
        entityData.set(DATA_MINED, getMined() + 1);
        if (ownedBlocks.isEmpty()) {
            placing = true; // следующий шаг начнёт новую стройку
            plan.clear();
        }
        setState(MinionState.BREAKING);
        scheduleNext();
    }

    private void faceTarget(BlockPos target) {
        double dx = target.getX() + 0.5 - getX();
        double dz = target.getZ() + 0.5 - getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        setYRot(yaw);
        yBodyRot = yaw;
        setYHeadRot(yaw);
    }

    private void syncPlate() {
        if (getPlateBlocks() != ownedBlocks.size()) {
            entityData.set(DATA_PLATE, ownedBlocks.size());
        }
    }

    private boolean canPlace(BlockPos p) {
        Level lvl = level();
        return lvl.isInWorldBounds(p)
                && lvl.hasChunkAt(p) // чанки не подгружаем
                && lvl.getBlockState(p).isAir() // чужие блоки не заменяем
                && lvl.getEntities((Entity) null, new AABB(p)).isEmpty(); // не запираем игроков и предметы
    }

    /** Составляет порядок постройки 5x5: от центра к краям по кругу. Занятые клетки пропускаются. */
    private void buildPlan() {
        plan.clear();
        int yOff = Config.PLATFORM_Y_OFFSET.get();
        BlockPos center = blockPosition().offset(0, yOff, 0);
        List<int[]> offsets = new ArrayList<>();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                if (dx == 0 && dz == 0 && yOff == 0) {
                    continue; // здесь стоит сам миньон
                }
                offsets.add(new int[]{dx, dz});
            }
        }
        offsets.sort((a, b) -> {
            int da = a[0] * a[0] + a[1] * a[1];
            int db = b[0] * b[0] + b[1] * b[1];
            if (da != db) {
                return Integer.compare(da, db);
            }
            return Double.compare(Math.atan2(a[1], a[0]), Math.atan2(b[1], b[0]));
        });
        for (int[] o : offsets) {
            BlockPos p = center.offset(o[0], 0, o[1]);
            if (canPlace(p)) {
                plan.add(p);
            }
        }
        entityData.set(DATA_PLATE_MAX, plan.size());
    }

    private void pruneOwned() {
        Level lvl = level();
        ownedBlocks.removeIf(p -> lvl.hasChunkAt(p) && !lvl.getBlockState(p).is(ModRegistry.TEMP_ORE.get()));
    }

    /** Убирает только временную руду этого миньона. */
    private void releaseZone() {
        Level lvl = level();
        for (BlockPos p : ownedBlocks) {
            if (lvl.hasChunkAt(p) && lvl.getBlockState(p).is(ModRegistry.TEMP_ORE.get())) {
                lvl.removeBlock(p, false);
            }
        }
        ownedBlocks.clear();
    }

    // ---------- Хранилище ----------

    private ItemStack insertIntoStorage(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        int cap = capacity();
        for (int i = 0; i < cap && !remaining.isEmpty(); i++) {
            ItemStack slot = storage.getItem(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
                int space = Math.min(slot.getMaxStackSize(), storage.getMaxStackSize()) - slot.getCount();
                if (space > 0) {
                    int moved = Math.min(space, remaining.getCount());
                    if (!simulate) {
                        slot.grow(moved);
                        storage.setChanged();
                    }
                    remaining.shrink(moved);
                }
            }
        }
        for (int i = 0; i < cap && !remaining.isEmpty(); i++) {
            if (storage.getItem(i).isEmpty()) {
                int moved = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                if (!simulate) {
                    storage.setItem(i, remaining.copyWithCount(moved));
                }
                remaining.shrink(moved);
            }
        }
        return remaining;
    }

    /**
     * Выгрузка добычи. Сундук миньонов (под миньоном или рядом с ним) принимает добычу сам, без улучшений.
     * Улучшение OUTPUT по-прежнему перекладывает добычу в любой контейнер (сундук, бочка), на котором стоит миньон.
     */
    private void pushOutput() {
        if (storageIsEmpty()) {
            return;
        }
        BlockPos pos = blockPosition();
        BlockPos below = pos.below();
        if (upgradeCount(UpgradeType.OUTPUT) > 0 || isMinionChest(below)) {
            pushInto(below, Direction.UP);
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(dir);
            if (isMinionChest(side)) {
                pushInto(side, dir.getOpposite());
            }
        }
    }

    private boolean isMinionChest(BlockPos pos) {
        return level().hasChunkAt(pos) && level().getBlockState(pos).getBlock() instanceof MinionChestBlock;
    }

    private boolean storageIsEmpty() {
        for (int i = 0; i < storage.getContainerSize(); i++) {
            if (!storage.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private void pushInto(BlockPos target, Direction side) {
        IItemHandler handler = level().getCapability(Capabilities.ItemHandler.BLOCK, target, side);
        if (handler == null) {
            return;
        }
        for (int i = 0; i < storage.getContainerSize(); i++) {
            ItemStack stack = storage.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack rest = ItemHandlerHelper.insertItem(handler, stack.copy(), false);
            int moved = stack.getCount() - rest.getCount();
            if (moved > 0) {
                stack.shrink(moved);
                storage.setChanged();
            }
        }
    }

    public void takeAll(ServerPlayer player) {
        for (int i = 0; i < storage.getContainerSize(); i++) {
            ItemStack stack = storage.getItem(i);
            if (!stack.isEmpty()) {
                storage.setItem(i, ItemStack.EMPTY);
                ItemHandlerHelper.giveItemToPlayer(player, stack);
            }
        }
    }

    // ---------- Прокачка ----------

    public void tryLevelUp(ServerPlayer player) {
        int lvl = getMinionLevel();
        if (lvl >= MAX_LEVEL) {
            player.displayClientMessage(Component.translatable("message.techminions.max_level"), true);
            return;
        }
        int cost = costForNext();
        MinionType type = getMinionType();
        Predicate<ItemStack> isCost = type::isCostItem; // слитки (любые из тега), без слитков - сам ресурс
        int have = countIn(player.getInventory().items, isCost) + countIn(storage, isCost);
        if (have < cost) {
            player.displayClientMessage(Component.translatable(
                    "message.techminions.not_enough", cost, new ItemStack(type.getCostItem()).getHoverName()), true);
            return;
        }
        int left = cost;
        left -= takeFrom(player.getInventory().items, isCost, left);
        if (left > 0) {
            takeFrom(storage, isCost, left);
        }
        entityData.set(DATA_LEVEL, lvl + 1);
        refreshVisuals();
        level().playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6F, 1.2F);
    }

    private static int countIn(List<ItemStack> list, Predicate<ItemStack> match) {
        int n = 0;
        for (ItemStack s : list) {
            if (match.test(s)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static int countIn(Container container, Predicate<ItemStack> match) {
        int n = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack s = container.getItem(i);
            if (match.test(s)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static int takeFrom(List<ItemStack> list, Predicate<ItemStack> match, int amount) {
        int taken = 0;
        for (ItemStack s : list) {
            if (taken >= amount) {
                break;
            }
            if (match.test(s)) {
                int n = Math.min(s.getCount(), amount - taken);
                s.shrink(n);
                taken += n;
            }
        }
        return taken;
    }

    private static int takeFrom(Container container, Predicate<ItemStack> match, int amount) {
        int taken = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (taken >= amount) {
                break;
            }
            ItemStack s = container.getItem(i);
            if (match.test(s)) {
                int n = Math.min(s.getCount(), amount - taken);
                s.shrink(n);
                taken += n;
            }
        }
        container.setChanged();
        return taken;
    }

    // ---------- Снять миньона ----------

    public void pickUp(ServerPlayer player) {
        var holder = ModRegistry.MINION_ITEMS.get(getMinionType());
        if (holder == null) {
            return; // мод ресурса удалён: предмета нет
        }
        ItemStack item = new ItemStack(holder.get());
        CompoundTag tag = new CompoundTag();
        tag.putInt("Level", getMinionLevel());
        tag.putInt("Mined", getMined());
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        ItemHandlerHelper.giveItemToPlayer(player, item);
        discard(); // remove(): убирает временную руду и выбрасывает содержимое
    }

    public void applyItemData(CompoundTag tag) {
        if (tag.contains("Level")) {
            entityData.set(DATA_LEVEL, Mth.clamp(tag.getInt("Level"), 1, MAX_LEVEL));
        }
        if (tag.contains("Mined")) {
            entityData.set(DATA_MINED, Math.max(0, tag.getInt("Mined")));
        }
    }

    // ---------- Взаимодействие ----------

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (id, inventory, p) -> new MinionMenu(id, inventory, this, storage, upgrades, eggs),
                            getDisplayName()),
                    buf -> buf.writeVarInt(getId()));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // Миньон неуязвим (кроме /kill и пустоты).
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(source, amount);
        }
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        // Чистим только при реальном удалении, а не при выгрузке чанка.
        if (!level().isClientSide && reason.shouldDestroy()) {
            releaseZone();
            dropContents();
        }
        super.remove(reason);
    }

    private void dropContents() {
        for (Container container : new Container[]{storage, upgrades, eggs}) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) {
                    container.setItem(i, ItemStack.EMPTY);
                    Containers.dropItemStack(level(), getX(), getY() + 0.5, getZ(), stack);
                }
            }
        }
    }

    // ---------- Сохранение ----------

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("MinionType", getMinionType().ordinal());
        tag.putInt("MinionLevel", getMinionLevel());
        tag.putInt("Mined", getMined());
        tag.putInt("Cooldown", cooldown);
        tag.put("Storage", saveContainer(storage));
        tag.put("Upgrades", saveContainer(upgrades));
        long[] owned = new long[ownedBlocks.size()];
        int i = 0;
        for (BlockPos p : ownedBlocks) {
            owned[i++] = p.asLong();
        }
        tag.putLongArray("Owned", owned);
        tag.putBoolean("Placing", placing);
        tag.putBoolean("HuntArmed", huntArmed);
        tag.putBoolean("HuntKilling", huntKilling);
        tag.put("Eggs", saveContainer(eggs));
        long[] planArr = new long[plan.size()];
        for (int k = 0; k < planArr.length; k++) {
            planArr[k] = plan.get(k).asLong();
        }
        tag.putLongArray("Plan", planArr);
        tag.putInt("PlateMax", getPlateMax());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DATA_TYPE, tag.getInt("MinionType"));
        entityData.set(DATA_LEVEL, Math.max(1, tag.getInt("MinionLevel")));
        entityData.set(DATA_MINED, tag.getInt("Mined"));
        cooldown = tag.getInt("Cooldown");
        cooldownMax = Math.max(1, cooldown);
        loadContainer(tag.getList("Storage", Tag.TAG_COMPOUND), storage);
        loadContainer(tag.getList("Upgrades", Tag.TAG_COMPOUND), upgrades);
        ownedBlocks.clear();
        for (long l : tag.getLongArray("Owned")) {
            ownedBlocks.add(BlockPos.of(l));
        }
        huntArmed = tag.getBoolean("HuntArmed");
        huntKilling = tag.getBoolean("HuntKilling");
        loadContainer(tag.getList("Eggs", Tag.TAG_COMPOUND), eggs);
        plan.clear();
        for (long l : tag.getLongArray("Plan")) {
            plan.add(BlockPos.of(l));
        }
        // Старые сохранения (v2.0) без фазы: есть руда -> ломаем, нет -> строим.
        placing = tag.contains("Placing") ? tag.getBoolean("Placing") : ownedBlocks.isEmpty();
        entityData.set(DATA_PLATE_MAX, tag.contains("PlateMax") ? tag.getInt("PlateMax") : ownedBlocks.size());
        entityData.set(DATA_PLATE, ownedBlocks.size());
        if (getMinionType().isHunter() && huntArmed) {
            entityData.set(DATA_STATE, (huntKilling ? MinionState.KILLING : MinionState.SPAWNING).ordinal());
        }
        setNoAi(true);
        refreshVisuals();
    }

    private ListTag saveContainer(Container container) {
        ListTag list = new ListTag();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Item", stack.save(registryAccess()));
                list.add(entry);
            }
        }
        return list;
    }

    private void loadContainer(ListTag list, Container container) {
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            final int slot = entry.getByte("Slot") & 255;
            if (slot < container.getContainerSize() && entry.contains("Item")) {
                ItemStack.parse(registryAccess(), entry.get("Item")).ifPresent(s -> container.setItem(slot, s));
            }
        }
    }
}
