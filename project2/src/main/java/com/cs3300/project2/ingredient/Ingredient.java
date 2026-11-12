package com.cs3300.project2.ingredient;

import java.time.LocalDate;

/**
 * An ingredient the user currently has on hand.
 * Instances are created through {@link IngredientFactory} so attributes are always normalized.
 *
 * @param expirationDate optional; stored now so the Expiry Date Tracker feature can use it
 */
public record Ingredient(
        String id,
        String name,
        double quantity,
        String unit,
        NutritionInfo nutritionPerUnit,
        LocalDate expirationDate) {

    /** Total nutrition for the full quantity the user has. */
    public NutritionInfo totalNutrition() {
        return nutritionPerUnit.scaledBy(quantity);
    }

    public Ingredient withQuantity(double newQuantity) {
        return new Ingredient(id, name, newQuantity, unit, nutritionPerUnit, expirationDate);
    }
}
