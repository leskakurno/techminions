package com.techminions;

import java.time.LocalDate;

/** Проверка периодов по реальному календарю: «MM-DD..MM-DD» (период может переходить через Новый год). */
public final class Seasonal {
    private Seasonal() {
    }

    public static boolean active(String range) {
        if (range == null || range.isBlank()) {
            return false;
        }
        try {
            String[] parts = range.trim().split("\\.\\.");
            if (parts.length != 2) {
                return false;
            }
            int from = code(parts[0]);
            int to = code(parts[1]);
            LocalDate now = LocalDate.now(java.time.ZoneOffset.UTC);
            int today = now.getMonthValue() * 100 + now.getDayOfMonth();
            return from <= to ? today >= from && today <= to : today >= from || today <= to;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static int code(String mmdd) {
        String[] p = mmdd.trim().split("-");
        return Integer.parseInt(p[0].trim()) * 100 + Integer.parseInt(p[1].trim());
    }
}
