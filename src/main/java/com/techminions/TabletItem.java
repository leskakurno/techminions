package com.techminions;

import com.techminions.net.ServerNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Планшет миньонов. ПКМ по своему миньону - привязать/отвязать. ПКМ в воздухе - окно просмотра: что миньон
 * наработал (хранилище + Сундуки миньонов рядом). Работает издалека, но ТОЛЬКО ДЛЯ ПРОСМОТРА: ни одно улучшение
 * планшета не позволяет забирать предметы. Улучшения вставляются так: взять улучшение курсором и нажать ПКМ по планшету
 * в инвентаре.
 */
public class TabletItem extends Item {
    public static final int BASE_LINKS = 1;
    public static final int ELITE_BASE_LINKS = 4;
    public static final int LINKS_PER_UPGRADE = 2;
    public static final int MAX_LINK_UPGRADES = 3;

    private final boolean elite;

    public TabletItem(Properties properties, boolean elite) {
        super(properties);
        this.elite = elite;
    }

    /** Элитный планшет: больше связей, межмерная связь и статистика уже встроены, принимает улучшение удалённого доступа. */
    public static boolean isElite(ItemStack stack) {
        return stack.getItem() instanceof TabletItem tablet && tablet.elite;
    }

    // ---------- Данные в предмете ----------

    private static CompoundTag data(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom != null ? custom.copyTag() : new CompoundTag();
    }

    public static List<UUID> getLinks(ItemStack stack) {
        List<UUID> result = new ArrayList<>();
        ListTag list = data(stack).getList("Links", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            try {
                result.add(UUID.fromString(list.getString(i)));
            } catch (IllegalArgumentException ignored) {
                // повреждённая запись - пропускаем
            }
        }
        return result;
    }

    private static void setLinks(ItemStack stack, List<UUID> links) {
        ListTag list = new ListTag();
        for (UUID id : links) {
            list.add(StringTag.valueOf(id.toString()));
        }
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.put("Links", list));
    }

    public static int linkUpgrades(ItemStack stack) {
        return data(stack).getInt("LinkUp");
    }

    public static boolean hasDimensionUpgrade(ItemStack stack) {
        return isElite(stack) || data(stack).getBoolean("Dim");
    }

    public static boolean hasStatsUpgrade(ItemStack stack) {
        return isElite(stack) || data(stack).getBoolean("Stats");
    }

    /** Удалённый доступ: можно забирать предметы из хранилища миньона. Только у элитного планшета с улучшением. */
    public static boolean hasRemoteUpgrade(ItemStack stack) {
        return isElite(stack) && data(stack).getBoolean("Remote");
    }

    public static int linkCapacity(ItemStack stack) {
        return (isElite(stack) ? ELITE_BASE_LINKS : BASE_LINKS) + LINKS_PER_UPGRADE * linkUpgrades(stack);
    }

    static void setLinkUpgrades(ItemStack stack, int value) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt("LinkUp", value));
    }

    static void setFlag(ItemStack stack, String key) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(key, true));
    }

    // ---------- Привязка миньона (вызывается из MinionEntity.mobInteract) ----------

    public static void linkMinion(ServerPlayer player, ItemStack tablet, MinionEntity minion) {
        if (!minion.isOwnedBy(player)) {
            player.displayClientMessage(Component.translatable("message.techminions.tablet.not_yours"), true);
            return;
        }
        List<UUID> links = getLinks(tablet);
        UUID id = minion.getUUID();
        if (links.remove(id)) {
            setLinks(tablet, links);
            player.displayClientMessage(Component.translatable("message.techminions.tablet.unlinked"), true);
        } else if (links.size() >= linkCapacity(tablet)) {
            player.displayClientMessage(Component.translatable("message.techminions.tablet.full", linkCapacity(tablet)), true);
        } else {
            links.add(id);
            setLinks(tablet, links);
            minion.reportToRegistry();
            ModAdvancements.award(player, "tablet_linked");
            player.displayClientMessage(Component.translatable("message.techminions.tablet.linked",
                    links.size(), linkCapacity(tablet)), true);
        }
    }

    // ---------- Использование ----------

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            ServerNetwork.sendTablet(serverPlayer, stack); // окно откроется на клиенте, когда придут данные
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /** Улучшение планшета вставляется ПКМ курсором с улучшением по планшету в инвентаре. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action,
                                            Player player, SlotAccess access) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)
                || !(other.getItem() instanceof TabletUpgradeItem upgrade)) {
            return false;
        }
        if (upgrade.getKind().applyTo(stack)) {
            other.shrink(1);
        } else if (!player.level().isClientSide) {
            player.displayClientMessage(Component.translatable("message.techminions.tablet.cannot_install"), true);
        }
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.techminions.tablet").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.techminions.tablet.links",
                getLinks(stack).size(), linkCapacity(stack)).withStyle(ChatFormatting.AQUA));
        if (!isElite(stack) && hasDimensionUpgrade(stack)) {
            tooltip.add(Component.translatable("tooltip.techminions.tablet.has_dimension").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (!isElite(stack) && hasStatsUpgrade(stack)) {
            tooltip.add(Component.translatable("tooltip.techminions.tablet.has_stats").withStyle(ChatFormatting.GREEN));
        }
        if (isElite(stack)) {
            tooltip.add(Component.translatable("tooltip.techminions.tablet.elite").withStyle(ChatFormatting.GOLD));
        }
        if (hasRemoteUpgrade(stack)) {
            tooltip.add(Component.translatable("tooltip.techminions.tablet.has_remote").withStyle(ChatFormatting.RED));
        } else {
            tooltip.add(Component.translatable("tooltip.techminions.tablet.view_only").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
