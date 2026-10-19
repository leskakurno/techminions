package com.techminions.net;

import com.techminions.TechMinions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/** Клиент -> сервер: забрать предметы у привязанного миньона. Сервер сам проверяет планшет, улучшение и владельца. */
public record TakeItemPayload(UUID minion, String itemId, int amount) implements CustomPacketPayload {
    public static final Type<TakeItemPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TechMinions.MOD_ID, "take_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TakeItemPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUUID(p.minion());
                buf.writeUtf(p.itemId());
                buf.writeVarInt(p.amount());
            },
            buf -> new TakeItemPayload(buf.readUUID(), buf.readUtf(), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
