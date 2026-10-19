package com.techminions;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/** Порядок менять нельзя: новые состояния только в конец. */
public enum MinionState implements StringRepresentable {
    PREPARING,
    WORKING,
    WAITING,
    FULL,
    PLACING,
    BREAKING,
    UNAVAILABLE,
    SPAWNING,
    KILLING,
    NO_EGG,
    PAUSED,
    NEEDS_WATER,
    NEEDS_CROPS,
    NEEDS_TREES;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MinionState byOrdinal(int ordinal) {
        MinionState[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}
