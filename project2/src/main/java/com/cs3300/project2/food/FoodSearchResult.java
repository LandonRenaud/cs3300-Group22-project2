package com.cs3300.project2.food;

/** Compact autocomplete result; full facts are fetched only after selection. */
public record FoodSearchResult(String foodId, String description, String dataType,
                               String brandOwner, Double caloriesPer100g) { }
