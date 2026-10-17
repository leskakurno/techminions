package com.techminions;

import java.util.Locale;

/** Размеры Сундука миньонов. Разные размеры - это три отдельных блока, а не улучшение одного. */
public enum MinionChestTier {
    SMALL(9, 3),
    MEDIUM(9, 6),
    LARGE(18, 6);

    private final int columns;
    private final int rows;

    MinionChestTier(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public int slots() {
        return columns * rows;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MinionChestTier byOrdinal(int ordinal) {
        MinionChestTier[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, ordinal))];
    }
}
