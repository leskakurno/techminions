package com.techminions.net;

import com.techminions.TechMinions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Клиент -> сервер: прислать список миньонов. mode 0 = вкладка в инвентаре, mode 1 = Планшет в руке. */
public record RequestMinionsPayload(int mode) implements CustomPacketPayload {
    public static final Type<RequestMinionsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TechMinions.MOD_ID, "request_minions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestMinionsPayload> CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeVarInt(payload.mode()),
            buf -> new RequestMinionsPayload(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
