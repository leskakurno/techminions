package com.techminions;

import java.util.Locale;

/** Размеры Сундука миньонов. Разные размеры - это три отдельных блока, а не улучшение одного. */
public enum MinionChestTier {
    SMALL(9, 1),   //  9 ячеек
    MEDIUM(9, 2),  // 18 ячеек
    LARGE(9, 4),   // 36 ячеек
    WAREHOUSE(9, 6), // Центральный склад: 54 ячейки, привязывается к миньонам ключом
    UPGRADE_CRATE(9, 1); // Ящик улучшений: 9 ячеек, принимает только улучшения

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
