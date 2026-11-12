package com.cs3300.project2.ingredient;

import java.time.LocalDate;

/** JSON body accepted when creating or updating an ingredient. Unit, nutrition and expiration are optional. */
public record IngredientRequest(
        String name,
        Double quantity,
        String unit,
        NutritionInfo nutritionPerUnit,
        LocalDate expirationDate) {
}
