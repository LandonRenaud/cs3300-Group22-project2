package com.cs3300.project2.food;

import com.cs3300.project2.ingredient.NutritionBasis;
import com.cs3300.project2.ingredient.NutritionInfo;
import com.cs3300.project2.ingredient.NutritionSource;

/** Normalized food data suitable for copying into a user's ingredient record. */
public record FoodDetails(String foodId, String description, NutritionInfo nutritionPerUnit,
                          NutritionBasis nutritionBasis, NutritionSource nutritionSource) { }
