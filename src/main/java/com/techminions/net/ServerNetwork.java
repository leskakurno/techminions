package com.techminions.net;

import com.techminions.MinionEntity;
import com.techminions.MinionRegistry;
import com.techminions.ModAdvancements;
import com.techminions.TabletItem;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Серверная часть сети: отдаёт игроку только СНИМКИ данных. Никаких предметов по сети не передаётся и не забирается. */
public final class ServerNetwork {
    private ServerNetwork() {
    }

    public static void handleRequest(RequestMinionsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (payload.mode() == 1) {
            ItemStack tablet = findTablet(player);
            if (!tablet.isEmpty()) {
                sendTablet(player, tablet);
            }
        } else {
            sendOwned(player);
        }
    }

    /**
     * Забор предметов планшетом. Работает ТОЛЬКО если в руке элитный планшет с улучшением удалённого доступа,
     * миньон привязан к этому планшету и принадлежит игроку, а его чанк загружен.
     */
    public static void handleTake(TakeItemPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        MinecraftServer server = player.getServer();
        ItemStack tablet = findTablet(player);
        if (server == null || tablet.isEmpty() || !TabletItem.hasRemoteUpgrade(tablet)
                || !TabletItem.getLinks(tablet).contains(payload.minion())) {
            return;
        }
        ResourceLocation id = ResourceLocation.tryParse(payload.itemId());
        Item item = id != null ? BuiltInRegistries.ITEM.get(id) : Items.AIR;
        int amount = Math.max(1, Math.min(payload.amount(), 2304));
        if (item == Items.AIR) {
            return;
        }
        MinionEntity minion = null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(payload.minion());
            if (entity instanceof MinionEntity found && found.isAlive()) {
                minion = found;
                break;
            }
        }
        if (minion == null) {
            player.displayClientMessage(Component.translatable("message.techminions.tablet.not_loaded"), true);
            return;
        }
        if (!minion.isOwnedBy(player)) {
            return;
        }
        int taken = minion.takeRemote(player, item, amount);
        if (taken > 0) {
            ModAdvancements.award(player, "remote");
            minion.reportToRegistry();
        }
        sendTablet(player, tablet); // обновляем окно
    }

    private static ItemStack findTablet(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof TabletItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Вкладка в инвентаре: все миньоны, которых поставил этот игрок (без содержимого - только статус). */
    public static void sendOwned(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        long now = server.overworld().getGameTime();
        List<MinionInfo> list = new ArrayList<>();
        for (MinionRegistry.Entry entry : MinionRegistry.get(server).ownedBy(player.getUUID())) {
            list.add(toInfo(entry, now, false, false));
        }
        PacketDistributor.sendToPlayer(player, new MinionsPayload(0, 0, 0, list));
    }

    /** Планшет: только привязанные миньоны, с содержимым хранилища (только для просмотра). */
    public static void sendTablet(ServerPlayer player, ItemStack tablet) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        long now = server.overworld().getGameTime();
        boolean crossDimension = TabletItem.hasDimensionUpgrade(tablet);
        boolean stats = TabletItem.hasStatsUpgrade(tablet);
        String currentDim = player.level().dimension().location().toString();
        MinionRegistry registry = MinionRegistry.get(server);
        List<MinionInfo> list = new ArrayList<>();
        for (UUID id : TabletItem.getLinks(tablet)) {
            MinionRegistry.Entry entry = registry.get(id);
            if (entry == null || !player.getUUID().equals(entry.owner)) {
                continue; // миньона больше нет или он принадлежит другому игроку
            }
            boolean locked = !crossDimension && !entry.dimension.equals(currentDim);
            list.add(toInfo(entry, now, !locked, locked));
        }
        int flags = (crossDimension ? MinionsPayload.FLAG_DIMENSION : 0) | (stats ? MinionsPayload.FLAG_STATS : 0)
                | (TabletItem.hasRemoteUpgrade(tablet) ? MinionsPayload.FLAG_REMOTE : 0);
        PacketDistributor.sendToPlayer(player, new MinionsPayload(1, flags, TabletItem.linkCapacity(tablet), list));
    }

    private static MinionInfo toInfo(MinionRegistry.Entry e, long now, boolean withItems, boolean locked) {
        int age = (int) Math.max(0, Math.min(Integer.MAX_VALUE, now - e.lastSeen));
        List<MinionInfo.ItemCount> items = new ArrayList<>();
        if (withItems) {
            for (Map.Entry<String, Integer> item : e.items.entrySet()) {
                items.add(new MinionInfo.ItemCount(item.getKey(), item.getValue()));
            }
        }
        return new MinionInfo(e.id, e.dimension, e.pos, e.type, e.level, e.state, age, e.mined,
                e.slotsUsed, e.slotsTotal, locked, items);
    }
}
