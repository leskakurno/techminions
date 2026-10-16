package com.techminions.client;

import com.techminions.MinionEntity;
import com.techminions.MinionMenu;
import com.techminions.UpgradeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class MinionScreen extends AbstractContainerScreen<MinionMenu> {
    private Button upgradeButton;

    public MinionScreen(MinionMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 244;
    }

    @Override
    protected void init() {
        super.init();
        upgradeButton = addRenderableWidget(Button.builder(
                        Component.translatable("gui.techminions.upgrade"), b -> press(0))
                .bounds(leftPos + 110, topPos + 64, 58, 16).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.techminions.take_all"), b -> press(1))
                .bounds(leftPos + 8, topPos + 116, 62, 14).build());
        addRenderableWidget(Button.builder(
                        Component.translatable("gui.techminions.pickup"), b -> press(2))
                .bounds(leftPos + 8, topPos + 132, 62, 14).build());
    }

    private void press(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        MinionEntity minion = menu.getMinion();
        if (upgradeButton != null && minion != null) {
            upgradeButton.active = minion.getMinionLevel() < MinionEntity.MAX_LEVEL;
        }
        super.render(g, mouseX, mouseY, partialTick);
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        MinionEntity minion = menu.getMinion();

        // Рамка и фон окна
        g.fill(x - 2, y - 2, x + imageWidth + 2, y + imageHeight + 2, 0xFF0E0E12);
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xFF2A2B35);

        // Окно предпросмотра миньона
        g.fill(x + 7, y + 15, x + 61, y + 81, 0xFF14151B);
        g.fill(x + 8, y + 16, x + 60, y + 80, 0xFF3A3D4D);
        if (minion != null) {
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, x + 8, y + 16, x + 60, y + 80, 30, 0.0625F, (float) mouseX, (float) mouseY, minion);
        }

        // Полоса прогресса цикла
        int progress = minion != null ? minion.getProgress() : 0;
        g.fill(x + 66, y + 47, x + 168, y + 52, 0xFF14151B);
        g.fill(x + 67, y + 48, x + 67 + (100 * progress / 100), y + 51, 0xFF55FF55);

        // Фон слотов
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            boolean upgradeSlot = i >= MinionEntity.STORAGE_SIZE
                    && i < MinionEntity.STORAGE_SIZE + MinionEntity.UPGRADE_SLOTS;
            boolean eggSlot = i == MinionEntity.STORAGE_SIZE + MinionEntity.UPGRADE_SLOTS;
            if (eggSlot && !slot.isActive()) {
                continue; // слот яйца есть только у охотников
            }
            int border = eggSlot ? 0xFF8B3FD0 : upgradeSlot ? 0xFFB08D2E : 0xFF14151B;
            int fill = slot.isActive() ? 0xFF4A4D5E : 0xFF1B1C22;
            g.fill(x + slot.x - 1, y + slot.y - 1, x + slot.x + 17, y + slot.y + 17, border);
            g.fill(x + slot.x, y + slot.y, x + slot.x + 16, y + slot.y + 16, fill);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        MinionEntity minion = menu.getMinion();
        g.drawString(this.font, this.title, 8, 6, 0xFFFFFF, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, 152, 0xBBBBBB, false);
        g.drawString(this.font, Component.translatable("gui.techminions.upgrades"), 8, 84, 0xBBBBBB, false);
        g.drawString(this.font, Component.translatable("gui.techminions.storage"), 80, 84, 0xBBBBBB, false);
        if (minion == null) {
            return;
        }

        int level = minion.getMinionLevel();
        int speed = menu.countUpgrades(UpgradeType.SPEED);
        int cycleTicks = minion.getMinionType().isHunter()
                ? MinionEntity.huntTicks(minion.getMinionType(), level, speed,
                        minion.getState() == com.techminions.MinionState.KILLING)
                : MinionEntity.intervalFor(minion.getMinionType(), level, speed);
        double seconds = Math.round(cycleTicks / 2.0) / 10.0;

        g.drawString(this.font, Component.translatable("gui.techminions.level",
                MinionEntity.roman(level), MinionEntity.roman(MinionEntity.MAX_LEVEL)), 66, 16, 0xFFFFFF, false);
        g.drawString(this.font, Component.translatable("gui.techminions.cycle", String.valueOf(seconds)),
                66, 26, 0xFFFFFF, false);
        if (!minion.getMinionType().isHunter()) {
            g.drawString(this.font, Component.literal("[" + minion.getPlateBlocks() + "/" + minion.getPlateMax() + "]"),
                    124, 26, 0x9BE89B, false);
        }
        g.drawString(this.font, Component.translatable("gui.techminions.status",
                Component.translatable("state.techminions." + minion.getState().getSerializedName())),
                66, 36, 0xFFFFFF, false);
        g.drawString(this.font, Component.translatable("gui.techminions.mined", minion.getMined()),
                66, 55, 0xBBBBBB, false);

        if (level < MinionEntity.MAX_LEVEL) {
            g.renderItem(new ItemStack(minion.getMinionType().getCostItem()), 66, 64);
            g.drawString(this.font, Component.literal("x " + minion.costForNext()), 84, 68, 0xFFFFFF, false);
        } else {
            g.drawString(this.font, Component.translatable("gui.techminions.max_level"), 66, 68, 0xFFD700, false);
        }
    }
}
