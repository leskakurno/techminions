package com.techminions.client;

import com.techminions.MinionEntity;
import com.techminions.MinionState;
import com.techminions.MinionType;
import com.techminions.ModRegistry;
import com.techminions.net.MinionInfo;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Общие правила показа миньона в окнах «Миньоны» и Планшета. */
final class StatusView {
    enum Kind {
        WORKING(0xFF55FF55),
        STOPPED(0xFFFF5555),
        UNLOADED(0xFFAAAAAA);

        final int color;

        Kind(int color) {
            this.color = color;
        }
    }

    private StatusView() {
    }

    static Kind kind(MinionInfo info) {
        if (info.ageTicks() > MinionInfo.STALE_TICKS) {
            return Kind.UNLOADED;
        }
        MinionState state = MinionState.byOrdinal(info.state());
        return switch (state) {
            case PLACING, BREAKING, WORKING, SPAWNING, KILLING, PREPARING -> Kind.WORKING;
            default -> Kind.STOPPED;
        };
    }

    /** Текст статуса: «Работает: Добыча площадки» / «Не работает: Хранилище полно» / «Не работает: чанк не загружен». */
    static Component text(MinionInfo info) {
        Kind kind = kind(info);
        if (kind == Kind.UNLOADED) {
            return Component.translatable("gui.techminions.status.unloaded");
        }
        Component state = Component.translatable("state.techminions." + MinionState.byOrdinal(info.state()).getSerializedName());
        return Component.translatable(kind == Kind.WORKING ? "gui.techminions.status.working" : "gui.techminions.status.stopped", state);
    }

    static Component name(MinionInfo info) {
        MinionType type = MinionType.byOrdinal(info.type());
        return Component.translatable("name.techminions.minion",
                Component.translatable("type.techminions." + type.getSerializedName()),
                MinionEntity.roman(info.level()));
    }

    static ItemStack icon(MinionInfo info) {
        var holder = ModRegistry.MINION_ITEMS.get(MinionType.byOrdinal(info.type()));
        return holder != null ? new ItemStack(holder.get()) : new ItemStack(Items.BARRIER);
    }

    static String place(MinionInfo info) {
        String dim = info.dimension();
        int colon = dim.indexOf(':');
        String shortDim = colon >= 0 ? dim.substring(colon + 1) : dim;
        return info.pos().getX() + " " + info.pos().getY() + " " + info.pos().getZ() + "  (" + shortDim + ")";
    }

    /** Подсказка «что сделать», если миньон не работает. null - подсказки нет. */
    static Component hint(MinionInfo info) {
        Kind kind = kind(info);
        if (kind == Kind.WORKING) {
            return null;
        }
        String key = kind == Kind.UNLOADED ? "hint.techminions.unloaded"
                : "hint.techminions." + MinionState.byOrdinal(info.state()).getSerializedName();
        return I18n.exists(key) ? Component.translatable(key) : null;
    }

    static Component ago(int ticks) {
        int seconds = ticks / 20;
        if (seconds < 60) {
            return Component.translatable("gui.techminions.ago.sec", seconds);
        }
        if (seconds < 3600) {
            return Component.translatable("gui.techminions.ago.min", seconds / 60);
        }
        return Component.translatable("gui.techminions.ago.hour", seconds / 3600);
    }

    /** Полная подсказка по миньону: статус, место, что делать и последние остановки. */
    static List<Component> tooltip(MinionInfo info, boolean withMined) {
        List<Component> lines = new ArrayList<>();
        lines.add(name(info));
        lines.add(text(info));
        lines.add(Component.literal(place(info)));
        if (withMined) {
            lines.add(Component.translatable("gui.techminions.mined", info.mined()));
        }
        if (info.slotsTotal() > 0 && !info.locked()) {
            int percent = Math.min(100, info.slotsUsed() * 100 / info.slotsTotal());
            lines.add(Component.translatable("gui.techminions.fill", percent, info.slotsUsed(), info.slotsTotal())
                    .withStyle(percent >= 90 ? ChatFormatting.RED : ChatFormatting.GRAY));
        }
        Component hint = hint(info);
        if (hint != null) {
            lines.add(hint.copy().withStyle(ChatFormatting.YELLOW));
        }
        if (!info.history().isEmpty()) {
            lines.add(Component.translatable("gui.techminions.history").withStyle(ChatFormatting.GRAY));
            int shown = Math.min(5, info.history().size());
            for (int i = 0; i < shown; i++) {
                MinionInfo.StopEvent e = info.history().get(i);
                lines.add(Component.translatable("gui.techminions.history.line",
                        Component.translatable("state.techminions." + MinionState.byOrdinal(e.state()).getSerializedName()),
                        ago(e.agoTicks())).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        return lines;
    }

    static String abbreviate(int count) {
        if (count >= 1_000_000) {
            return (count / 100_000) / 10.0 + "M";
        }
        if (count >= 10_000) {
            return (count / 1000) + "k";
        }
        if (count >= 1000) {
            return (count / 100) / 10.0 + "k";
        }
        return Integer.toString(count);
    }
}
