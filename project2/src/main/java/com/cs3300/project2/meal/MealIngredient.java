package com.cs3300.project2.meal;

/** One line of a meal's recipe, e.g. 2 cup rice. Built by {@link MealFactory}. */
public record MealIngredient(String name, double quantity, String unit) {
}
