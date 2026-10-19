package com.techminions.net;

import com.techminions.TechMinions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервер -> клиент: список миньонов. mode 0 = вкладка в инвентаре, mode 1 = Планшет.
 * tabletFlags: бит 0 - межмерная связь, бит 1 - статистика; capacity - сколько миньонов можно привязать.
 */
public record MinionsPayload(int mode, int tabletFlags, int capacity, List<MinionInfo> minions) implements CustomPacketPayload {
    public static final Type<MinionsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TechMinions.MOD_ID, "minions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MinionsPayload> CODEC = StreamCodec.of(
            (buf, payload) -> {
                buf.writeVarInt(payload.mode());
                buf.writeVarInt(payload.tabletFlags());
                buf.writeVarInt(payload.capacity());
                buf.writeVarInt(payload.minions().size());
                for (MinionInfo info : payload.minions()) {
                    info.write(buf);
                }
            },
            buf -> {
                int mode = buf.readVarInt();
                int flags = buf.readVarInt();
                int capacity = buf.readVarInt();
                int n = buf.readVarInt();
                List<MinionInfo> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    list.add(MinionInfo.read(buf));
                }
                return new MinionsPayload(mode, flags, capacity, list);
            });

    public static final int FLAG_DIMENSION = 1;
    public static final int FLAG_STATS = 2;
    public static final int FLAG_REMOTE = 4;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
