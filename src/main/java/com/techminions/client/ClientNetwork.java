package com.techminions.client;

import com.techminions.net.MinionsPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Клиентская часть сети: открывает/обновляет окна просмотра. Вызывается на главном потоке клиента. */
public final class ClientNetwork {
    private ClientNetwork() {
    }

    public static void handleMinions(MinionsPayload payload, IPayloadContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (payload.mode() == 1) {
            if (mc.screen instanceof TabletScreen tablet) {
                tablet.update(payload);
            } else {
                mc.setScreen(new TabletScreen(payload));
            }
        } else if (mc.screen instanceof MinionsOverviewScreen overview) {
            overview.update(payload);
        }
    }
}
