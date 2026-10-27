package com.techminions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;
import java.util.UUID;

/**
 * Команды /techminions. Только чтение данных реестра и сброс новых временных значений: чанки не загружаются,
 * уровни, ядра и UUID миньонов не меняются. Команды администратора требуют права оператора.
 */
public final class TechCommands {
    private TechCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("techminions")
                .executes(ctx -> help(ctx.getSource()))
                .then(Commands.literal("help").executes(ctx -> help(ctx.getSource())))
                .then(Commands.literal("list").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> list(ctx, EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("diag").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("uuid", UuidArgument.uuid())
                                .executes(ctx -> diag(ctx, UuidArgument.getUuid(ctx, "uuid")))))
                .then(Commands.literal("goal").requires(s -> s.hasPermission(2))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> resetGoal(ctx, EntityArgument.getPlayer(ctx, "player"))))))
                .then(Commands.literal("clearbonuses").requires(s -> s.hasPermission(2))
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> clearBonuses(ctx, EntityArgument.getPlayer(ctx, "player"))))));
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("command.techminions.help.title"), false);
        source.sendSuccess(() -> Component.translatable("command.techminions.help.help"), false);
        if (source.hasPermission(2)) {
            source.sendSuccess(() -> Component.translatable("command.techminions.help.list"), false);
            source.sendSuccess(() -> Component.translatable("command.techminions.help.diag"), false);
            source.sendSuccess(() -> Component.translatable("command.techminions.help.goal"), false);
            source.sendSuccess(() -> Component.translatable("command.techminions.help.clear"), false);
        }
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx, ServerPlayer target) throws CommandSyntaxException {
        MinecraftServer server = ctx.getSource().getServer();
        long now = server.overworld().getGameTime();
        List<MinionRegistry.Entry> entries = MinionRegistry.get(server).ownedBy(target.getUUID());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.techminions.list.header",
                target.getName(), entries.size()), false);
        int shown = 0;
        for (MinionRegistry.Entry e : entries) {
            if (shown++ >= 30) {
                ctx.getSource().sendSuccess(() -> Component.translatable("command.techminions.list.more"), false);
                break;
            }
            boolean stale = now - e.lastSeen > 600;
            Component line = Component.translatable("command.techminions.list.line",
                    Component.translatable("type.techminions." + MinionType.byOrdinal(e.type).getSerializedName()),
                    e.level,
                    stale ? Component.translatable("gui.techminions.status.unloaded")
                            : Component.translatable("state.techminions." + MinionState.byOrdinal(e.state).getSerializedName()),
                    e.pos.getX() + " " + e.pos.getY() + " " + e.pos.getZ());
            ctx.getSource().sendSuccess(() -> line, false);
        }
        return entries.size();
    }

    private static int diag(CommandContext<CommandSourceStack> ctx, UUID id) {
        MinecraftServer server = ctx.getSource().getServer();
        MinionRegistry.Entry e = MinionRegistry.get(server).get(id);
        if (e == null) {
            ctx.getSource().sendFailure(Component.translatable("command.techminions.diag.none"));
            return 0;
        }
        long ageMin = Math.max(0, server.overworld().getGameTime() - e.lastSeen) / 1200L;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.techminions.diag.line1",
                Component.translatable("type.techminions." + MinionType.byOrdinal(e.type).getSerializedName()), e.level,
                Component.translatable("state.techminions." + MinionState.byOrdinal(e.state).getSerializedName())), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.techminions.diag.line2",
                e.ownerName.isEmpty() ? "?" : e.ownerName, e.dimension,
                e.pos.getX() + " " + e.pos.getY() + " " + e.pos.getZ()), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.techminions.diag.line3",
                e.slotsUsed, e.slotsTotal, e.mined, e.hourly, ageMin), false);
        return 1;
    }

    private static int resetGoal(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        DailyGoals.reset(ctx.getSource().getServer(), target.getUUID());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.techminions.goal.reset", target.getName()), true);
        return 1;
    }

    private static int clearBonuses(CommandContext<CommandSourceStack> ctx, ServerPlayer target) {
        MinecraftServer server = ctx.getSource().getServer();
        int cleared = 0;
        for (MinionRegistry.Entry e : MinionRegistry.get(server).ownedBy(target.getUUID())) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(e.id);
                if (entity instanceof MinionEntity minion) {
                    minion.clearBonuses(); // только временные бонусы v4, уровень и ядро не трогаются
                    cleared++;
                    break;
                }
            }
        }
        int result = cleared;
        ctx.getSource().sendSuccess(() -> Component.translatable("command.techminions.clear.done", result, target.getName()), true);
        return result;
    }
}
