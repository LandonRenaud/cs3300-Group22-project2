package com.cs3300.project2.mealplan;

import java.time.LocalDate;
import java.util.List;

import com.cs3300.project2.meal.Meal;

/** A read-only preview: one saved recipe per day, with no persisted inventory reservations. */
public record WeeklyMealPlan(LocalDate startDate, PlanningStrategy strategy,
                             List<Day> days, List<IngredientNeed> shoppingList) {
    public WeeklyMealPlan {
        days = List.copyOf(days);
        shoppingList = List.copyOf(shoppingList);
    }

    public record Day(LocalDate date, Meal meal, List<IngredientNeed> ingredients) {
        public Day {
            ingredients = List.copyOf(ingredients);
        }
    }

    /** fromPantry is allocated once across the week; toBuy is the remaining shortfall. */
    public record IngredientNeed(String name, String unit, double needed, double fromPantry, double toBuy) {
    }
}
