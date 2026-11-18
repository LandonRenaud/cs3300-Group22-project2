package com.cs3300.project2.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.cs3300.project2.meal.Meal;
import com.cs3300.project2.meal.MealIngredient;
import com.cs3300.project2.mealplan.WeeklyMealPlan.Day;
import com.cs3300.project2.mealplan.WeeklyMealPlan.IngredientNeed;

class MealPlanFactoryTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 30);
    private final MealPlanFactory factory = new MealPlanFactory();
    private final Meal meal = new Meal("m1", "Omelette", "", List.of(new MealIngredient("Eggs", 2, "count")));

    @Test
    void emptyPlanHasNoDaysOrShopping() {
        WeeklyMealPlan plan = factory.empty(START, PlanningStrategy.PANTRY_FIRST);
        assertEquals(START, plan.startDate());
        assertEquals(PlanningStrategy.PANTRY_FIRST, plan.strategy());
        assertTrue(plan.days().isEmpty());
        assertTrue(plan.shoppingList().isEmpty());
    }

    @Test
    void shoppingListAggregatesShortfallsIgnoringNameCase() {
        Day first = factory.day(START, meal, List.of(
                new IngredientNeed("Eggs", "count", 2, 1, 1),
                new IngredientNeed("Salt", "g", 1, 1, 0)));
        Day second = factory.day(START.plusDays(1), meal, List.of(new IngredientNeed("eggs", "count", 2, 0, 2)));

        WeeklyMealPlan plan = factory.create(START, PlanningStrategy.EXPIRY_FIRST, List.of(first, second));

        assertEquals(List.of(new IngredientNeed("Eggs", "count", 4, 1, 3)), plan.shoppingList());
        assertEquals(2, plan.days().size());
    }

    @Test
    void sameNameWithDifferentUnitsStaysSeparate() {
        Day day = factory.day(START, meal, List.of(
                new IngredientNeed("Milk", "cup", 1, 0, 1),
                new IngredientNeed("Milk", "ml", 100, 0, 100)));
        assertEquals(2, factory.create(START, PlanningStrategy.EXPIRY_FIRST, List.of(day)).shoppingList().size());
    }

    @Test
    void rejectsDaysThatAreNotConsecutiveFromStart() {
        Day skipped = factory.day(START.plusDays(1), meal, List.of());
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(START, PlanningStrategy.EXPIRY_FIRST, List.of(skipped)));
    }

    @Test
    void rejectsMissingInputs() {
        assertThrows(IllegalArgumentException.class, () -> factory.empty(null, PlanningStrategy.EXPIRY_FIRST));
        assertThrows(IllegalArgumentException.class, () -> factory.empty(START, null));
        assertThrows(IllegalArgumentException.class, () -> factory.day(START, null, List.of()));
    }

    @Test
    void rejectsTotalsThatOverflow() {
        Day a = factory.day(START, meal, List.of(new IngredientNeed("Eggs", "count", Double.MAX_VALUE, 0, Double.MAX_VALUE)));
        Day b = factory.day(START.plusDays(1), meal, List.of(new IngredientNeed("Eggs", "count", Double.MAX_VALUE, 0, Double.MAX_VALUE)));
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(START, PlanningStrategy.EXPIRY_FIRST, List.of(a, b)));
    }
}
