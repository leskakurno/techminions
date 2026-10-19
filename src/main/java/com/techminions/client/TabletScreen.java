package com.techminions.client;

import com.techminions.net.MinionInfo;
import com.techminions.net.MinionsPayload;
import com.techminions.net.TakeItemPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Окно Планшета миньонов. Здесь НЕТ ячеек и нет ни одной кнопки «забрать»: предметы нарисованы как картинки,
 * по ним нельзя кликнуть. Планшет показывает снимок добычи миньона и ничего не выдаёт.
 */
public class TabletScreen extends Screen {
    private static final int W = 316;
    private static final int H = 214;
    private static final int LIST_X = 8;
    private static final int LIST_Y = 30;
    private static final int LIST_W = 112;
    private static final int ROW = 24;
    private static final int GRID_X = 130;
    private static final int GRID_Y = 104;
    private static final int COLS = 9;
    private static final int ROWS = 5;

    private record Shown(ItemStack stack, int count, String id) {
    }

    private List<MinionInfo> list;
    private int flags;
    private int capacity;
    private int selected;
    private int itemScroll;
    private List<Shown> shown = new ArrayList<>();
    private Shown hoveredItem;
    private MinionInfo hoveredMinion;

    public TabletScreen(MinionsPayload payload) {
        super(Component.translatable("gui.techminions.tablet.title"));
        update(payload);
    }

    public void update(MinionsPayload payload) {
        this.list = new ArrayList<>(payload.minions());
        this.flags = payload.tabletFlags();
        this.capacity = payload.capacity();
        this.selected = Math.max(0, Math.min(selected, list.size() - 1));
        rebuildItems();
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private MinionInfo current() {
        return list.isEmpty() ? null : list.get(Math.max(0, Math.min(selected, list.size() - 1)));
    }

    private void rebuildItems() {
        shown = new ArrayList<>();
        itemScroll = 0;
        MinionInfo info = current();
        if (info == null) {
            return;
        }
        for (MinionInfo.ItemCount entry : info.items()) {
            ResourceLocation id = ResourceLocation.tryParse(entry.itemId());
            Item item = id != null ? BuiltInRegistries.ITEM.get(id) : Items.AIR;
            if (item != Items.AIR) {
                shown.add(new Shown(new ItemStack(item), entry.count(), entry.itemId()));
            }
        }
        shown.sort(Comparator.comparingInt((Shown s) -> s.count()).reversed());
    }

    private int totalRows() {
        return (shown.size() + COLS - 1) / COLS;
    }

    private int maxItemScroll() {
        return Math.max(0, totalRows() - ROWS);
    }

    private boolean remote() {
        return (flags & MinionsPayload.FLAG_REMOTE) != 0;
    }

    /** Удалённый доступ (только элитный планшет с улучшением): клик по предмету просит сервер выдать его. */
    private boolean tryTake(double mouseX, double mouseY, int button) {
        MinionInfo info = current();
        if (!remote() || info == null || info.locked() || shown.isEmpty()) {
            return false;
        }
        int dx = left() + GRID_X;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = (itemScroll + row) * COLS + col;
                int ix = dx + col * 18;
                int iy = top() + GRID_Y + row * 18;
                if (idx < shown.size() && mouseX >= ix && mouseX < ix + 17 && mouseY >= iy && mouseY < iy + 17) {
                    Shown item = shown.get(idx);
                    int stackSize = Math.max(1, item.stack().getMaxStackSize());
                    int amount;
                    if (button == 1) {
                        amount = 1;
                    } else if (hasShiftDown()) {
                        amount = stackSize * 9;
                    } else {
                        amount = stackSize;
                    }
                    PacketDistributor.sendToServer(new TakeItemPayload(info.id(), item.id(), Math.min(amount, item.count())));
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (list != null && (button == 0 || button == 1) && tryTake(mouseX, mouseY, button)) {
            return true;
        }
        if (list != null && button == 0) {
            int x = left() + LIST_X;
            int y = top() + LIST_Y;
            for (int i = 0; i < list.size(); i++) {
                int ry = y + i * ROW;
                if (mouseX >= x && mouseX < x + LIST_W && mouseY >= ry && mouseY < ry + ROW - 2) {
                    if (selected != i) {
                        selected = i;
                        rebuildItems();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        itemScroll = Math.max(0, Math.min(maxItemScroll(), itemScroll - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        hoveredItem = null;
        hoveredMinion = null;
        int x = left();
        int y = top();
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0xFF0E0E12);
        g.fill(x, y, x + W, y + H, 0xFF2A2B35);
        g.drawString(font, title, x + 8, y + 8, 0xFFFFFF, false);
        Component modeLabel = Component.translatable(remote() ? "gui.techminions.tablet.remote" : "gui.techminions.tablet.view_only");
        g.drawString(font, modeLabel, x + W - 8 - font.width(modeLabel), y + 8, remote() ? 0xFF8855 : 0x8888AA, false);

        if (list == null) {
            return;
        }
        g.drawString(font, Component.translatable("gui.techminions.tablet.links", list.size(), capacity),
                x + 8, y + 19, 0xBBBBBB, false);
        if (list.isEmpty()) {
            g.drawString(font, Component.translatable("gui.techminions.tablet.empty1"), x + 8, y + 40, 0xBBBBBB, false);
            g.drawString(font, Component.translatable("gui.techminions.tablet.empty2", capacity), x + 8, y + 52, 0xBBBBBB, false);
            return;
        }

        // Список привязанных миньонов
        for (int i = 0; i < list.size(); i++) {
            MinionInfo info = list.get(i);
            int rx = x + LIST_X;
            int ry = y + LIST_Y + i * ROW;
            g.fill(rx, ry, rx + LIST_W, ry + ROW - 2, i == selected ? 0xFF4A4D5E : 0xFF33343F);
            g.renderItem(StatusView.icon(info), rx + 3, ry + 3);
            g.drawString(font, StatusView.name(info), rx + 22, ry + 3, 0xFFFFFF, false);
            g.fill(rx + 22, ry + 14, rx + 30, ry + 20, 0xFF000000);
            g.fill(rx + 23, ry + 15, rx + 29, ry + 19, info.locked() ? 0xFF8855AA : StatusView.kind(info).color);
            if (mouseX >= rx && mouseX < rx + LIST_W && mouseY >= ry && mouseY < ry + ROW - 2) {
                hoveredMinion = info;
            }
        }

        // Подробности выбранного
        MinionInfo info = current();
        int dx = x + GRID_X;
        g.drawString(font, StatusView.name(info), dx, y + 30, 0xFFFFFF, false);
        if (info.locked()) {
            g.drawString(font, Component.translatable("gui.techminions.tablet.locked1"), dx, y + 44, 0xFFAA55, false);
            g.drawString(font, Component.translatable("gui.techminions.tablet.locked2"), dx, y + 56, 0xFFAA55, false);
            return;
        }
        g.drawString(font, StatusView.text(info), dx, y + 42, StatusView.kind(info).color, false);
        g.drawString(font, Component.literal(StatusView.place(info)), dx, y + 54, 0xBBBBBB, false);
        if ((flags & MinionsPayload.FLAG_STATS) != 0) {
            g.drawString(font, Component.translatable("gui.techminions.mined", info.mined()), dx, y + 66, 0xBBBBBB, false);
            int total = Math.max(1, info.slotsTotal());
            int percent = Math.min(100, info.slotsUsed() * 100 / total);
            g.drawString(font, Component.translatable("gui.techminions.tablet.fill", info.slotsUsed(), info.slotsTotal(), percent),
                    dx, y + 78, 0xBBBBBB, false);
            int barW = 160;
            g.fill(dx, y + 90, dx + barW + 2, y + 98, 0xFF14151B);
            int color = percent >= 90 ? 0xFFFF5555 : percent >= 60 ? 0xFFFFAA00 : 0xFF55FF55;
            g.fill(dx + 1, y + 91, dx + 1 + barW * percent / 100, y + 97, color);
        }

        // Содержимое: просто картинки, без ячеек и без возможности что-то взять
        if (shown.isEmpty()) {
            g.drawString(font, Component.translatable("gui.techminions.tablet.nothing"), dx, y + GRID_Y + 4, 0x888888, false);
            return;
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int idx = (itemScroll + row) * COLS + col;
                int ix = dx + col * 18;
                int iy = y + GRID_Y + row * 18;
                boolean over = mouseX >= ix && mouseX < ix + 17 && mouseY >= iy && mouseY < iy + 17;
                g.fill(ix, iy, ix + 17, iy + 17, (remote() && over && idx < shown.size()) ? 0xFF4A4D5E : 0xFF1B1C22);
                if (idx >= shown.size()) {
                    continue;
                }
                Shown item = shown.get(idx);
                g.renderItem(item.stack(), ix + 1, iy + 1);
                g.renderItemDecorations(font, item.stack(), ix + 1, iy + 1, StatusView.abbreviate(item.count()));
                if (over) {
                    hoveredItem = item;
                }
            }
        }
        if (remote()) {
            g.drawString(font, Component.translatable("gui.techminions.tablet.remote_hint"), dx, y + H - 14, 0xBBBBBB, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (hoveredItem != null) {
            g.renderComponentTooltip(font, List.of(
                    hoveredItem.stack().getHoverName(),
                    Component.translatable("gui.techminions.tablet.count", hoveredItem.count())), mouseX, mouseY);
        } else if (hoveredMinion != null) {
            g.renderComponentTooltip(font, List.of(
                    StatusView.name(hoveredMinion),
                    StatusView.text(hoveredMinion),
                    Component.literal(StatusView.place(hoveredMinion))), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
