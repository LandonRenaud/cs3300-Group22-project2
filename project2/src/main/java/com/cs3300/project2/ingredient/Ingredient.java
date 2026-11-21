package com.cs3300.project2.ingredient;

import java.time.LocalDate;
import java.util.Locale;

/** A user's inventory entry, including a saved snapshot of its nutrition facts. */
public record Ingredient(
        String id,
        String name,
        double quantity,
        String unit,
        NutritionInfo nutritionPerUnit,
        LocalDate expirationDate,
        NutritionBasis nutritionBasis,
        NutritionSource nutritionSource) {

    /** Compatibility constructor for existing callers and old Firebase records. */
    public Ingredient(String id, String name, double quantity, String unit,
                      NutritionInfo nutritionPerUnit, LocalDate expirationDate) {
        this(id, name, quantity, unit, nutritionPerUnit, expirationDate, null, null);
    }

    public Ingredient {
        unit = unit == null || unit.isBlank() ? "item" : unit;
        nutritionPerUnit = nutritionPerUnit == null ? NutritionInfo.EMPTY : nutritionPerUnit;
        // Old Firebase records stored nutrition per inventory unit; default them to that basis.
        nutritionBasis = nutritionBasis == null ? new NutritionBasis(1, unit, "per " + unit) : nutritionBasis;
    }

    /** Returns total nutrition only when inventory and nutrition units can be converted safely. */
    public NutritionInfo totalNutrition() {
        if (normalize(unit).equals(normalize(nutritionBasis.unit()))) {
            return nutritionPerUnit.scaledBy(quantity / nutritionBasis.amount());
        }
        Double inventoryAmount = toBase(quantity, unit);
        Double basisAmount = toBase(nutritionBasis.amount(), nutritionBasis.unit());
        if (inventoryAmount == null || basisAmount == null || !sameFamily(unit, nutritionBasis.unit())) {
            return NutritionInfo.EMPTY;
        }
        return nutritionPerUnit.scaledBy(inventoryAmount / basisAmount);
    }

    public Ingredient withQuantity(double newQuantity) {
        return new Ingredient(id, name, newQuantity, unit, nutritionPerUnit, expirationDate,
                nutritionBasis, nutritionSource);
    }

    private static boolean sameFamily(String left, String right) {
        String a = family(left), b = family(right);
        return a != null && a.equals(b);
    }

    private static String family(String unit) {
        return switch (normalize(unit)) {
            case "g", "gram", "grams", "kg", "kilogram", "kilograms", "mg", "milligram", "milligrams",
                    "oz", "ounce", "ounces", "lb", "lbs", "pound", "pounds" -> "mass";
            case "ml", "milliliter", "milliliters", "l", "liter", "liters" -> "volume";
            case "item", "items", "each", "piece", "pieces" -> "count";
            case "serving", "servings" -> "serving";
            default -> null;
        };
    }

    private static Double toBase(double amount, String rawUnit) {
        return switch (normalize(rawUnit)) {
            case "g", "gram", "grams", "ml", "milliliter", "milliliters", "item", "items", "each", "piece", "pieces", "serving", "servings" -> amount;
            case "kg", "kilogram", "kilograms", "l", "liter", "liters" -> amount * 1000;
            case "mg", "milligram", "milligrams" -> amount / 1000;
            case "oz", "ounce", "ounces" -> amount * 28.349523125;
            case "lb", "lbs", "pound", "pounds" -> amount * 453.59237;
            default -> null;
        };
    }

    private static String normalize(String value) { return value.trim().toLowerCase(Locale.ROOT); }
}
