package com.techminions;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/**
 * Улучшения для Планшета миньонов. Ни одно из них не даёт забирать предметы - планшет только показывает.
 */
public class TabletUpgradeItem extends Item {
    public enum Kind implements StringRepresentable {
        /** +2 привязанных миньона (до 3 раз). */
        LINKS,
        /** Работает из любого измерения. */
        DIMENSION,
        /** Показывает заполненность хранилищ и общую добычу. */
        STATS,
        /** Только для элитного планшета: можно забирать предметы из хранилища миньона. */
        REMOTE;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Применяет улучшение к планшету. false - уже установлено / достигнут максимум. */
        public boolean applyTo(ItemStack tablet) {
            switch (this) {
                case LINKS -> {
                    int n = TabletItem.linkUpgrades(tablet);
                    if (n >= TabletItem.MAX_LINK_UPGRADES) {
                        return false;
                    }
                    TabletItem.setLinkUpgrades(tablet, n + 1);
                    return true;
                }
                case DIMENSION -> {
                    if (TabletItem.hasDimensionUpgrade(tablet)) {
                        return false;
                    }
                    TabletItem.setFlag(tablet, "Dim");
                    return true;
                }
                case REMOTE -> {
                    if (!TabletItem.isElite(tablet) || TabletItem.hasRemoteUpgrade(tablet)) {
                        return false;
                    }
                    TabletItem.setFlag(tablet, "Remote");
                    return true;
                }
                default -> {
                    if (TabletItem.hasStatsUpgrade(tablet)) {
                        return false;
                    }
                    TabletItem.setFlag(tablet, "Stats");
                    return true;
                }
            }
        }
    }

    private final Kind kind;

    public TabletUpgradeItem(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    public Kind getKind() {
        return kind;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.techminions.tablet_upgrade." + kind.getSerializedName())
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.techminions.tablet_upgrade.how").withStyle(ChatFormatting.DARK_GRAY));
    }
}
