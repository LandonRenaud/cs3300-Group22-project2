package com.cs3300.project2.ingredient;

/** Amount and unit to which the associated nutrition facts apply. */
public record NutritionBasis(double amount, String unit, String label) {
    public NutritionBasis {
        if (amount <= 0 || Double.isNaN(amount) || Double.isInfinite(amount)
                || unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("Nutrition basis must have a positive amount and unit");
        }
        label = label == null || label.isBlank() ? "per " + amount + " " + unit : label.trim();
    }
}
