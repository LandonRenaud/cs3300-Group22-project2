package com.cs3300.project2.food;

import java.util.List;

/** Provider boundary between the application and an external food database. */
public interface FoodNutritionProvider {
    List<FoodSearchResult> search(String query);
    FoodDetails findDetails(String foodId);
}
