package com.cs3300.project2.ingredient;

import java.util.LinkedHashMap;
import java.util.Map;

/** Nutrition amounts for one declared basis. Extra nutrients retain their source label and unit. */
public record NutritionInfo(
        double calories,
        double proteinGrams,
        double carbsGrams,
        double fatGrams,
        Map<String, NutrientValue> additionalNutrients) {

    public static final NutritionInfo EMPTY = new NutritionInfo(0, 0, 0, 0, Map.of());

    public NutritionInfo(double calories, double proteinGrams, double carbsGrams, double fatGrams) {
        this(calories, proteinGrams, carbsGrams, fatGrams, Map.of());
    }

    public NutritionInfo {
        requireNonNegative(calories, "calories");
        requireNonNegative(proteinGrams, "proteinGrams");
        requireNonNegative(carbsGrams, "carbsGrams");
        requireNonNegative(fatGrams, "fatGrams");
        additionalNutrients = additionalNutrients == null ? Map.of() : Map.copyOf(additionalNutrients);
    }

    public NutritionInfo scaledBy(double factor) {
        Map<String, NutrientValue> scaled = new LinkedHashMap<>();
        additionalNutrients.forEach((key, value) -> scaled.put(key, value.scaledBy(factor)));
        return new NutritionInfo(calories * factor, proteinGrams * factor, carbsGrams * factor, fatGrams * factor, scaled);
    }

    public NutritionInfo plus(NutritionInfo other) {
        Map<String, NutrientValue> sum = new LinkedHashMap<>(additionalNutrients);
        other.additionalNutrients.forEach((key, value) -> sum.merge(key, value, NutrientValue::plus));
        return new NutritionInfo(calories + other.calories, proteinGrams + other.proteinGrams,
                carbsGrams + other.carbsGrams, fatGrams + other.fatGrams, sum);
    }

    private static void requireNonNegative(double value, String field) {
        if (value < 0 || Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException(field + " must be zero or greater");
        }
    }
}
