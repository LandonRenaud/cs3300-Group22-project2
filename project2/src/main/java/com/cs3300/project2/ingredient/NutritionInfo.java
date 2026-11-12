package com.cs3300.project2.ingredient;

/**
 * Nutrition facts for one unit of an ingredient (e.g. per egg, per 100 g).
 * All values are non-negative; {@link #EMPTY} is used when the user has not supplied any.
 */
public record NutritionInfo(double calories, double proteinGrams, double carbsGrams, double fatGrams) {

    public static final NutritionInfo EMPTY = new NutritionInfo(0, 0, 0, 0);

    public NutritionInfo {
        requireNonNegative(calories, "calories");
        requireNonNegative(proteinGrams, "proteinGrams");
        requireNonNegative(carbsGrams, "carbsGrams");
        requireNonNegative(fatGrams, "fatGrams");
    }

    /** Nutrition for {@code amount} units of this ingredient. */
    public NutritionInfo scaledBy(double amount) {
        return new NutritionInfo(calories * amount, proteinGrams * amount, carbsGrams * amount, fatGrams * amount);
    }

    public NutritionInfo plus(NutritionInfo other) {
        return new NutritionInfo(
                calories + other.calories,
                proteinGrams + other.proteinGrams,
                carbsGrams + other.carbsGrams,
                fatGrams + other.fatGrams);
    }

    private static void requireNonNegative(double value, String field) {
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(field + " must be zero or greater");
        }
    }
}
