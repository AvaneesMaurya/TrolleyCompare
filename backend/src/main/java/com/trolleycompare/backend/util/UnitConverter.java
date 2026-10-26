package com.trolleycompare.backend.util;

public class UnitConverter {

    public static double normalizeToBasePath(double quantity, String unit) {
        if (unit == null) return quantity;
        String u = unit.trim().toLowerCase();

        switch (u) {
            case "kg":
                return quantity * 1000.0;
            case "g":
                return quantity;
            case "l":
                return quantity * 1000.0;
            case "ml":
                return quantity;
            default:
                return quantity;
        }
    }

    public static String getUnitCategory(String unit) {
        if (unit == null) return "count";
        String u = unit.trim().toLowerCase();
        if (u.equals("kg") || u.equals("g")) return "weight";
        if (u.equals("l") || u.equals("ml")) return "volume";
        return "count";
    }
}