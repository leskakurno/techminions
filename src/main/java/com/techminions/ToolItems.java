package com.techminions;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Предметы-инструменты и трофеи версии 4.0. Все действия выполняются на сервере и ничего не выдают сверх написанного. */
public final class ToolItems {
    private ToolItems() {
    }

    static CompoundTag data(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom != null ? custom.copyTag() : new CompoundTag();
    }

    /** Предмет с одной строкой подсказки из перевода tooltip.techminions.<id>. Трофеи, печати, пломбы. */
    public static class Plain extends Item {
        private final String tipKey;

        public Plain(Properties properties, String tipKey) {
            super(properties);
            this.tipKey = tipKey;
        }

        @Override
        public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(tipKey).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Кубик непредсказуемости: следующий цикл миньона даёт один малый случайный эффект (без редкостей). */
    public static class CubeItem extends Plain implements MinionInteractable {
        public CubeItem(Properties properties) {
            super(properties, "tooltip.techminions.unpredictable_cube");
        }

        @Override
        public void onMinion(ServerPlayer player, ItemStack stack, MinionEntity minion) {
            if (!minion.isOwnedBy(player)) {
                player.displayClientMessage(Component.translatable("message.techminions.tablet.not_yours"), true);
                return;
            }
            if (minion.isCubeArmed()) {
                player.displayClientMessage(Component.translatable("message.techminions.cube.already"), true);
                return;
            }
            minion.armCube();
            stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.techminions.cube.armed"), true);
        }
    }

    /** Талон обслуживания: сбрасывает временные бонусы миньона (кубик, импульс, калибратор). Уровень не трогает. */
    public static class VoucherItem extends Plain implements MinionInteractable {
        public VoucherItem(Properties properties) {
            super(properties, "tooltip.techminions.service_voucher");
        }

        @Override
        public void onMinion(ServerPlayer player, ItemStack stack, MinionEntity minion) {
            if (!minion.isOwnedBy(player)) {
                player.displayClientMessage(Component.translatable("message.techminions.tablet.not_yours"), true);
                return;
            }
            minion.clearBonuses();
            stack.shrink(1);
            player.displayClientMessage(Component.translatable("message.techminions.voucher.used"), true);
        }
    }

    /** Паспорт миньона: неизменяемый снимок типа, уровня, добычи и даты. Миньона не восстанавливает. */
    public static class PassportItem extends Plain implements MinionInteractable {
        public PassportItem(Properties properties) {
            super(properties, "tooltip.techminions.minion_passport");
        }

        @Override
        public void onMinion(ServerPlayer player, ItemStack stack, MinionEntity minion) {
            if (!minion.isOwnedBy(player)) {
                player.displayClientMessage(Component.translatable("message.techminions.tablet.not_yours"), true);
                return;
            }
            if (stack.get(DataComponents.LORE) != null) {
                player.displayClientMessage(Component.translatable("message.techminions.passport.filled"), true);
                return;
            }
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("name.techminions.minion",
                    Component.translatable("type.techminions." + minion.getMinionType().getSerializedName()),
                    MinionEntity.roman(minion.getMinionLevel())).withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("gui.techminions.mined", minion.getMined()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("message.techminions.passport.cycles", minion.getCycleCount()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal(LocalDate.now(ZoneOffset.UTC).toString()).withStyle(ChatFormatting.DARK_GRAY));
            stack.set(DataComponents.LORE, new ItemLore(lines));
            player.displayClientMessage(Component.translatable("message.techminions.passport.written"), true);
        }
    }

    /** Чёрный ящик смены: один снимок последних остановок миньона (без бесконечного журнала). */
    public static class BlackBoxItem extends Plain implements MinionInteractable {
        public BlackBoxItem(Properties properties) {
            super(properties, "tooltip.techminions.black_box");
        }

        @Override
        public void onMinion(ServerPlayer player, ItemStack stack, MinionEntity minion) {
            if (!minion.isOwnedBy(player)) {
                player.displayClientMessage(Component.translatable("message.techminions.tablet.not_yours"), true);
                return;
            }
            long now = minion.level().getGameTime();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("gui.techminions.history").withStyle(ChatFormatting.GRAY));
            List<long[]> log = minion.getStopLog();
            if (log.isEmpty()) {
                lines.add(Component.translatable("message.techminions.blackbox.empty").withStyle(ChatFormatting.DARK_GRAY));
            }
            for (int i = log.size() - 1; i >= 0; i--) {
                long[] record = log.get(i);
                MinionState state = MinionState.byOrdinal((int) record[0]);
                long minutes = Math.max(0, now - record[1]) / 1200L;
                lines.add(Component.translatable("message.techminions.blackbox.line",
                        Component.translatable("state.techminions." + state.getSerializedName()), minutes)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            stack.set(DataComponents.LORE, new ItemLore(lines)); // перезаписывает прошлый снимок: один снимок на ящик
            player.displayClientMessage(Component.translatable("message.techminions.blackbox.saved"), true);
        }
    }

    /** Ключ привязки: запоминает своего миньона; ПКМ по Сигнальной лампе или Табличке привязывает их к нему. */
    public static class BindingKeyItem extends Plain implements MinionInteractable {
        public BindingKeyItem(Properties properties) {
            super(properties, "tooltip.techminions.binding_key");
        }

        public static UUID getMinion(ItemStack stack) {
            CompoundTag tag = data(stack);
            return tag.hasUUID("Minion") ? tag.getUUID("Minion") : null;
        }

        @Override
        public void onMinion(ServerPlayer player, ItemStack stack, MinionEntity minion) {
            if (!minion.isOwnedBy(player)) {
                player.displayClientMessage(Component.translatable("message.techminions.tablet.not_yours"), true);
                return;
            }
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putUUID("Minion", minion.getUUID());
                tag.putUUID("Owner", player.getUUID());
            });
            player.displayClientMessage(Component.translatable("message.techminions.key.saved"), true);
        }

        /** Привязка блока. Возвращает true, если блок привязан. */
        public static boolean bindTo(ServerPlayer player, ItemStack stack, StatusLinkBlockEntity target) {
            CompoundTag tag = data(stack);
            if (!tag.hasUUID("Minion") || !tag.hasUUID("Owner") || !tag.getUUID("Owner").equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.techminions.key.empty"), true);
                return false;
            }
            target.bind(tag.getUUID("Minion"), player.getUUID());
            player.displayClientMessage(Component.translatable("message.techminions.key.bound"), true);
            ModAdvancements.award(player, "signal");
            return true;
        }
    }

    /** Компас мастера: показывает направление и расстояние до миньона. Чанки не загружает, никуда не телепортирует. */
    public static class CompassItem extends Plain implements MinionInteractable {
        public CompassItem(Properties properties) {
            super(properties, "tooltip.techminions.master_compass");
        }

        @Override
        public void onMinion(ServerPlayer player, ItemStack stack, MinionEntity minion) {
            if (!minion.isOwnedBy(player)) {
                player.displayClientMessage(Component.translatable("message.techminions.tablet.not_yours"), true);
                return;
            }
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
                tag.putUUID("Minion", minion.getUUID());
                tag.putUUID("Owner", player.getUUID());
            });
            player.displayClientMessage(Component.translatable("message.techminions.key.saved"), true);
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                point(serverPlayer, stack);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        private static void point(ServerPlayer player, ItemStack stack) {
            CompoundTag tag = data(stack);
            MinecraftServer server = player.getServer();
            if (server == null || !tag.hasUUID("Minion") || !tag.hasUUID("Owner") || !tag.getUUID("Owner").equals(player.getUUID())) {
                player.displayClientMessage(Component.translatable("message.techminions.compass.empty"), true);
                return;
            }
            MinionRegistry.Entry entry = MinionRegistry.get(server).get(tag.getUUID("Minion"));
            if (entry == null) {
                player.displayClientMessage(Component.translatable("message.techminions.compass.lost"), true);
                return;
            }
            if (!entry.dimension.equals(player.level().dimension().location().toString())) {
                player.displayClientMessage(Component.translatable("message.techminions.compass.dimension"), true);
                return;
            }
            BlockPos pos = entry.pos;
            double dx = pos.getX() + 0.5 - player.getX();
            double dz = pos.getZ() + 0.5 - player.getZ();
            int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
            // 0 = юг (+Z), по часовой стрелке: ЮЗ, З, СЗ, С, СВ, В, ЮВ
            double angle = Math.toDegrees(Math.atan2(-dx, dz));
            int index = (int) Math.floorMod(Math.round(angle / 45.0), 8L);
            String[] dirs = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};
            player.displayClientMessage(Component.translatable("message.techminions.compass.point",
                    distance, Component.translatable("message.techminions.dir." + dirs[index]),
                    pos.getX() + " " + pos.getY() + " " + pos.getZ()), true);
        }
    }

    /** Часы смены: ПКМ показывает, сколько прошло с запуска; присед + ПКМ запускает отсчёт заново. */
    public static class ClockItem extends Plain {
        public ClockItem(Properties properties) {
            super(properties, "tooltip.techminions.shift_clock");
        }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide) {
                CompoundTag tag = data(stack);
                long now = level.getGameTime();
                if (player.isShiftKeyDown() || !tag.contains("Start")) {
                    CustomData.update(DataComponents.CUSTOM_DATA, stack, t -> t.putLong("Start", now));
                    player.displayClientMessage(Component.translatable("message.techminions.clock.started"), true);
                } else {
                    long ticks = Math.max(0, now - tag.getLong("Start"));
                    player.displayClientMessage(Component.translatable("message.techminions.clock.elapsed",
                            ticks / 72000, (ticks / 1200) % 60), true);
                }
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }
}
