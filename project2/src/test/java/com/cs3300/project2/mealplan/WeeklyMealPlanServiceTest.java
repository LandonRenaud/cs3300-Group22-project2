package com.cs3300.project2.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cs3300.project2.ingredient.InMemoryIngredientRepository;
import com.cs3300.project2.ingredient.Ingredient;
import com.cs3300.project2.ingredient.IngredientFactory;
import com.cs3300.project2.ingredient.IngredientRequest;
import com.cs3300.project2.ingredient.IngredientService;
import com.cs3300.project2.meal.InMemoryMealRepository;
import com.cs3300.project2.meal.Meal;
import com.cs3300.project2.meal.MealFactory;
import com.cs3300.project2.meal.MealRequest;
import com.cs3300.project2.meal.MealService;

class WeeklyMealPlanServiceTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 28);
    private IngredientService pantry;
    private MealService meals;
    private WeeklyMealPlanService planner;

    @BeforeEach
    void setUp() {
        pantry = new IngredientService(new InMemoryIngredientRepository(), new IngredientFactory());
        meals = new MealService(new InMemoryMealRepository(), new MealFactory(), pantry);
        planner = new WeeklyMealPlanService(meals, pantry, new MealPlanFactory());
    }

    private Ingredient stock(String name, double quantity, String unit, LocalDate expiry) {
        return pantry.add(new IngredientRequest(name, quantity, unit, null, expiry));
    }

    private Meal meal(String name, String ingredient, double quantity, String unit) {
        return meals.add(new MealRequest(name, null,
                List.of(new MealRequest.Item(ingredient, quantity, unit))));
    }

    @Test
    void emptyRecipesProduceAnEmptyPlan() {
        WeeklyMealPlan plan = planner.generate(START, PlanningStrategy.EXPIRY_FIRST);
        assertTrue(plan.days().isEmpty());
        assertTrue(plan.shoppingList().isEmpty());
    }

    @Test
    void producesSevenConsecutiveDaysAcrossMonthAndYearBoundaries() {
        meal("Rice bowl", "Rice", 1, "cup");
        for (LocalDate start : List.of(START, LocalDate.of(2026, 12, 29))) {
            WeeklyMealPlan plan = planner.generate(start, PlanningStrategy.EXPIRY_FIRST);
            assertEquals(7, plan.days().size());
            for (int i = 0; i < 7; i++) assertEquals(start.plusDays(i), plan.days().get(i).date());
        }
    }

    @Test
    void acceptsLatestStartDateThatFitsSevenDays() {
        LocalDate latestStart = LocalDate.of(9999, 12, 25);
        meal("Rice bowl", "Rice", 1, "cup");

        WeeklyMealPlan plan = planner.generate(latestStart, PlanningStrategy.EXPIRY_FIRST);

        assertEquals(7, plan.days().size());
        assertEquals(latestStart, plan.days().getFirst().date());
        assertEquals(LocalDate.of(9999, 12, 31), plan.days().getLast().date());
        assertThrows(IllegalArgumentException.class,
                () -> planner.generate(latestStart.plusDays(1), PlanningStrategy.EXPIRY_FIRST));
    }

    @Test
    void allocatesInventoryOnlyOnceAndLeavesSavedDataUnchanged() {
        Ingredient eggs = stock("EGGS", 3, "item", null);
        meal("Omelet", "Eggs", 2, "item");
        WeeklyMealPlan plan = planner.generate(START, PlanningStrategy.EXPIRY_FIRST);
        assertEquals(0, plan.days().get(0).ingredients().getFirst().toBuy());
        assertEquals(1, plan.days().get(1).ingredients().getFirst().toBuy());
        assertEquals(14, plan.shoppingList().getFirst().needed());
        assertEquals(3, plan.shoppingList().getFirst().fromPantry());
        assertEquals(11, plan.shoppingList().getFirst().toBuy());
        assertEquals(3, pantry.findById(eggs.id()).quantity());
        assertEquals(1, meals.findAll(null).size());
        assertEquals(plan, planner.generate(START, PlanningStrategy.EXPIRY_FIRST));
    }

    @Test
    void checksExpiryOnEachPlannedDayIncludingTheExpiryDate() {
        stock("Milk", 100, "cup", START);
        meal("Porridge", "Milk", 1, "cup");
        WeeklyMealPlan plan = planner.generate(START, PlanningStrategy.EXPIRY_FIRST);
        assertEquals(1, plan.days().getFirst().ingredients().getFirst().fromPantry());
        assertEquals(0, plan.days().get(1).ingredients().getFirst().fromPantry());
        assertEquals(6, plan.shoppingList().getFirst().toBuy());
    }

    @Test
    void consumesEarliestValidBatchBeforeLaterAndUndatedBatches() {
        stock("Milk", 20, "cup", START.minusDays(1));
        stock("Milk", 1, "cup", null);
        stock("Milk", 1, "cup", START.plusDays(2));
        stock("Milk", 1, "cup", START);
        meal("Porridge", "Milk", 1, "cup");
        WeeklyMealPlan plan = planner.generate(START, PlanningStrategy.EXPIRY_FIRST);
        assertEquals(List.of(1.0, 1.0, 1.0, 0.0, 0.0, 0.0, 0.0), plan.days().stream()
                .map(d -> d.ingredients().getFirst().fromPantry()).toList());
        assertEquals(4, plan.shoppingList().getFirst().toBuy());
    }

    @Test
    void matchesByIngredientNameRegardlessOfUnitAndIgnoresZeroStock() {
        stock("Rice", 100, "g", null);
        stock("Rice", 0, "cup", START);
        meal("Rice bowl", "rice", 1, "cup");
        WeeklyMealPlan plan = planner.generate(START, PlanningStrategy.PANTRY_FIRST);
        assertEquals(List.of(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0), plan.days().stream()
                .map(d -> d.ingredients().getFirst().fromPantry()).toList());
        assertTrue(plan.shoppingList().isEmpty());
    }

    @Test
    void expiryAndPantryStrategiesChooseDifferentMeals() {
        stock("Spinach", 1, "cup", START);
        stock("Rice", 7, "cup", null);
        meal("Spinach soup", "Spinach", 2, "cup");
        meal("Rice bowl", "Rice", 1, "cup");
        assertEquals("Spinach soup", planner.generate(START, PlanningStrategy.EXPIRY_FIRST)
                .days().getFirst().meal().name());
        assertEquals("Rice bowl", planner.generate(START, PlanningStrategy.PANTRY_FIRST)
                .days().getFirst().meal().name());
    }

    @Test
    void candidateEvaluationDoesNotReserveStockForUnselectedMeals() {
        stock("Rice", 1, "cup", null);
        meal("A bowl", "Rice", 1, "cup");
        meal("B bowl", "Rice", 1, "cup");
        WeeklyMealPlan plan = planner.generate(START, PlanningStrategy.PANTRY_FIRST);
        assertEquals("A bowl", plan.days().getFirst().meal().name());
        assertEquals(1, plan.days().getFirst().ingredients().getFirst().fromPantry());
        assertEquals(6, plan.shoppingList().getFirst().toBuy());
    }

    @Test
    void tiesRotateRecipesDeterministicallyWhenPantryIsEmpty() {
        meal("B bowl", "Rice", 1, "cup");
        meal("A bowl", "Rice", 1, "cup");
        WeeklyMealPlan plan = planner.generate(START, PlanningStrategy.PANTRY_FIRST);
        assertEquals(List.of("A bowl", "B bowl", "A bowl", "B bowl", "A bowl", "B bowl", "A bowl"),
                plan.days().stream().map(d -> d.meal().name()).toList());
        assertEquals(7, plan.shoppingList().getFirst().toBuy());
    }

    @Test
    void decimalQuantitiesDoNotCreatePhantomShoppingItems() {
        stock("Oil", 0.7, "cup", null);
        meal("Salad", "Oil", 0.1, "cup");
        WeeklyMealPlan plan = planner.generate(START, PlanningStrategy.PANTRY_FIRST);
        assertTrue(plan.shoppingList().isEmpty());
        assertTrue(plan.days().stream().allMatch(d -> d.ingredients().getFirst().fromPantry() == 0.1));
    }

    @Test
    void aggregatesDuplicateRecipeLinesUsingExistingFactoryRules() {
        meals.add(new MealRequest("Rice bowl", null, List.of(
                new MealRequest.Item("Rice", 1.0, "cup"), new MealRequest.Item("rice", 2.0, "CUP"))));
        stock("Rice", 3, "cup", null);
        WeeklyMealPlan plan = planner.generate(START, PlanningStrategy.EXPIRY_FIRST);
        assertEquals(1, plan.shoppingList().size());
        assertEquals(21, plan.shoppingList().getFirst().needed());
        assertEquals(18, plan.shoppingList().getFirst().toBuy());
    }

    @Test
    void duplicateDecimalLinesDoNotCreatePhantomShoppingItems() {
        meals.add(new MealRequest("Rice bowl", null, List.of(
                new MealRequest.Item("Rice", 0.1, "cup"), new MealRequest.Item("rice", 0.2, "cup"))));
        stock("Rice", 2.1, "cup", null);
        WeeklyMealPlan plan = planner.generate(START, PlanningStrategy.EXPIRY_FIRST);
        assertTrue(plan.shoppingList().isEmpty());
        assertEquals(0.3, plan.days().getFirst().ingredients().getFirst().needed());
    }

    @Test
    void rejectsWeeklyTotalsThatCannotBeRepresentedAsFiniteQuantities() {
        meal("Oversized recipe", "Rice", 3e307, "cup");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> planner.generate(START, PlanningStrategy.EXPIRY_FIRST));
        assertEquals("Weekly ingredient quantities exceed the supported range", error.getMessage());
    }

    @Test
    void ignoresRecipesWithNoPositiveIngredientQuantities() {
        meal("Empty", "Rice", 0, "cup");
        assertTrue(planner.generate(START, PlanningStrategy.EXPIRY_FIRST).days().isEmpty());
    }

    @Test
    void rejectsMissingOrOverflowingDatesAndMissingStrategy() {
        assertThrows(IllegalArgumentException.class, () -> planner.generate(null, PlanningStrategy.EXPIRY_FIRST));
        assertThrows(IllegalArgumentException.class, () -> planner.generate(LocalDate.MAX, PlanningStrategy.EXPIRY_FIRST));
        assertThrows(IllegalArgumentException.class, () -> planner.generate(START, null));
    }
}
