package com.techminions;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum UpgradeType implements StringRepresentable {
    SPEED,
    STORAGE,
    FORTUNE,
    OUTPUT,
    SMELTER;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
