package com.techminions.client;

import com.techminions.ModRegistry;
import com.techminions.TechMinions;
import com.techminions.net.RequestMinionsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** Добавляет в инвентарь игрока вкладку «Миньоны» (справа от окна): какие миньоны игрока работают, а какие нет. */
@EventBusSubscriber(modid = TechMinions.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class InventoryTabHandler {
    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen screen)) {
            return;
        }
        int x = screen.getGuiLeft() + screen.getXSize() + 2;
        int y = screen.getGuiTop() + 6;
        event.addListener(new TabButton(x, y));
    }

    private static class TabButton extends Button {
        private final ItemStack icon = new ItemStack(ModRegistry.TABLET.get());

        TabButton(int x, int y) {
            super(x, y, 24, 24, Component.translatable("gui.techminions.tab"), b -> open(), DEFAULT_NARRATION);
            setTooltip(Tooltip.create(Component.translatable("gui.techminions.tab.tooltip")));
        }

        private static void open() {
            Minecraft mc = Minecraft.getInstance();
            mc.setScreen(new MinionsOverviewScreen());
            PacketDistributor.sendToServer(new RequestMinionsPayload(0));
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            int x = getX();
            int y = getY();
            int bg = isHoveredOrFocused() ? 0xFF5A5E72 : 0xFF3A3D4D;
            g.fill(x, y, x + width, y + height, 0xFF0E0E12);
            g.fill(x + 1, y + 1, x + width - 1, y + height - 1, bg);
            g.renderItem(icon, x + 4, y + 4);
        }
    }
}
