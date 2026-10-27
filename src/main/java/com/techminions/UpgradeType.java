package com.techminions;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum UpgradeType implements StringRepresentable {
    SPEED,
    STORAGE,
    FORTUNE,
    SMELTER,
    MACHINE_FEED,
    MACHINE_COLLECT,
    REDSTONE,
    FILTER,
    RECYCLER,
    LIMITER,
    ECO_DRIVE,
    RARITY_LIMITER,
    RESERVE,
    FILL_SENSOR,
    SEED_COLLECTOR,
    LUCK_STABILIZER,
    CONDENSER,
    REGENERATOR,
    RESONATOR,
    CALIBRATOR,
    IMPULSE,
    XP_EXTRACTOR,
    CALENDAR;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
