package com.techminions.client;

import com.techminions.net.MinionInfo;
import com.techminions.net.MinionsPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Вкладка «Миньоны» из инвентаря: все миньоны игрока и работают они или нет. Только просмотр. */
public class MinionsOverviewScreen extends Screen {
    private static final int W = 290;
    private static final int H = 196;
    private static final int ROW = 24;
    private static final int LIST_TOP = 36;
    private static final int LIST_HEIGHT = 120;

    private List<MinionInfo> list; // null - данные ещё не пришли
    private MinionInfo hovered;
    private int scroll;

    public MinionsOverviewScreen() {
        super(Component.translatable("gui.techminions.overview.title"));
    }

    public void update(MinionsPayload payload) {
        this.list = new ArrayList<>(payload.minions());
        this.scroll = Math.max(0, Math.min(scroll, maxScroll()));
    }

    private int left() {
        return (width - W) / 2;
    }

    private int top() {
        return (height - H) / 2;
    }

    private int visibleRows() {
        return LIST_HEIGHT / ROW;
    }

    private int maxScroll() {
        return list == null ? 0 : Math.max(0, list.size() - visibleRows());
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.techminions.back"), b -> onClose())
                .bounds(left() + W / 2 - 45, top() + H - 28, 90, 20).build());
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.setScreen(new InventoryScreen(minecraft.player));
        } else {
            super.onClose();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(scrollY)));
        return true;
    }

    /** Фон и содержимое рисуются здесь (в 1.21 Screen.render сам вызывает renderBackground, а затем рисует кнопки поверх). */
    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        hovered = null;
        int x = left();
        int y = top();
        g.fill(x - 2, y - 2, x + W + 2, y + H + 2, 0xFF0E0E12);
        g.fill(x, y, x + W, y + H, 0xFF2A2B35);
        g.drawString(font, title, x + 8, y + 8, 0xFFFFFF, false);

        if (list == null) {
            g.drawCenteredString(font, Component.translatable("gui.techminions.loading"), x + W / 2, y + 80, 0xBBBBBB);
            return;
        }
        if (list.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.techminions.overview.empty"), x + W / 2, y + 80, 0xBBBBBB);
            return;
        }
        int working = 0;
        for (MinionInfo info : list) {
            if (StatusView.kind(info) == StatusView.Kind.WORKING) {
                working++;
            }
        }
        g.drawString(font, Component.translatable("gui.techminions.overview.summary", working, list.size()),
                x + 8, y + 22, 0xBBBBBB, false);
        int shown = Math.min(visibleRows(), list.size() - scroll);
        for (int i = 0; i < shown; i++) {
            MinionInfo info = list.get(scroll + i);
            int ry = y + LIST_TOP + i * ROW;
            g.fill(x + 6, ry, x + W - 6, ry + ROW - 2, (i % 2 == 0) ? 0xFF33343F : 0xFF2F303A);
            g.renderItem(StatusView.icon(info), x + 10, ry + 3);
            g.drawString(font, StatusView.name(info), x + 32, ry + 3, 0xFFFFFF, false);
            g.drawString(font, StatusView.text(info), x + 32, ry + 13, StatusView.kind(info).color, false);
            g.fill(x + W - 20, ry + 7, x + W - 12, ry + 15, 0xFF000000);
            g.fill(x + W - 19, ry + 8, x + W - 13, ry + 14, StatusView.kind(info).color);
            if (mouseX >= x + 6 && mouseX < x + W - 6 && mouseY >= ry && mouseY < ry + ROW - 2) {
                hovered = info;
            }
        }
        if (list.size() > visibleRows()) {
            g.drawString(font, Component.translatable("gui.techminions.scroll"), x + 8, y + H - 24, 0x777777, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (hovered != null) {
            g.renderComponentTooltip(font, List.of(
                    StatusView.name(hovered),
                    StatusView.text(hovered),
                    Component.literal(StatusView.place(hovered)),
                    Component.translatable("gui.techminions.mined", hovered.mined())), mouseX, mouseY);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
