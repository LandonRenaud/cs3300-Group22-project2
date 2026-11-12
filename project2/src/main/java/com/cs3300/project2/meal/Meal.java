package com.cs3300.project2.meal;

import java.util.List;

/** A meal the user has made before, kept as a quick reference of what goes into it. */
public record Meal(String id, String name, String notes, List<MealIngredient> ingredients) {

    public Meal {
        ingredients = List.copyOf(ingredients);
    }
}
