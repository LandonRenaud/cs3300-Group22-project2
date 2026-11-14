package com.cs3300.project2.ingredient;

/** One additional nutrient value, kept with its display name and source unit. */
public record NutrientValue(String name, double amount, String unit) {
    public NutrientValue {
        if (name == null || name.isBlank() || unit == null || unit.isBlank()
                || amount < 0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            throw new IllegalArgumentException("Nutrient name, unit, and non-negative amount are required");
        }
    }

    NutrientValue scaledBy(double factor) { return new NutrientValue(name, amount * factor, unit); }

    NutrientValue plus(NutrientValue other) {
        if (!unit.equalsIgnoreCase(other.unit)) return this;
        return new NutrientValue(name, amount + other.amount, unit);
    }
}
