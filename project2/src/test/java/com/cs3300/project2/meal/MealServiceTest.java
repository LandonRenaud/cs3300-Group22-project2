package com.cs3300.project2.meal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cs3300.project2.common.NotFoundException;
import com.cs3300.project2.ingredient.InMemoryIngredientRepository;
import com.cs3300.project2.ingredient.Ingredient;
import com.cs3300.project2.ingredient.IngredientFactory;
import com.cs3300.project2.ingredient.IngredientRequest;
import com.cs3300.project2.ingredient.IngredientService;

class MealServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 21);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-22T02:00:00Z"), ZoneId.of("America/New_York"));
    private IngredientService pantry;
    private MealService service;

    @BeforeEach
    void setUp() {
        pantry = new IngredientService(new InMemoryIngredientRepository(), new IngredientFactory());
        service = new MealService(new InMemoryMealRepository(), new MealFactory(), pantry, CLOCK);
    }

    private Meal friedRice() {
        return service.add(new MealRequest("Fried Rice", null, List.of(
                new MealRequest.Item("Rice", 2.0, "cup"),
                new MealRequest.Item("Eggs", 3.0, null),
                new MealRequest.Item("Soy Sauce", 2.0, "tbsp"))));
    }

    @Test
    void searchMatchesMealNameOrIngredient() {
        friedRice();
        service.add(new MealRequest("Toast", null, List.of(new MealRequest.Item("Bread", 2.0, "slice"))));

        assertEquals(2, service.findAll(null).size());
        assertEquals(2, service.findAll("  ").size());
        assertEquals(List.of("Fried Rice"), service.findAll("rice").stream().map(Meal::name).toList());
        assertEquals(List.of("Fried Rice"), service.findAll("SOY").stream().map(Meal::name).toList());
        assertEquals(List.of("Toast"), service.findAll("bread").stream().map(Meal::name).toList());
        assertTrue(service.findAll("pizza").isEmpty());
    }

    @Test
    void updateAndDelete() {
        Meal meal = friedRice();

        Meal updated = service.update(meal.id(),
                new MealRequest("Egg Fried Rice", "add scallions", List.of(new MealRequest.Item("Rice", 1.0, "cup"))));

        assertEquals(meal.id(), updated.id());
        assertEquals("Egg Fried Rice", service.findById(meal.id()).name());

        service.delete(meal.id());
        assertThrows(NotFoundException.class, () -> service.findById(meal.id()));
        assertThrows(NotFoundException.class, () -> service.delete(meal.id()));
        assertThrows(NotFoundException.class, () -> service.update(meal.id(),
                new MealRequest("x", null, List.of(new MealRequest.Item("y", 1.0, null)))));
    }

    @Test
    void shoppingListComparesAgainstPantry() {
        Meal meal = friedRice();
        pantry.add(new IngredientRequest("rice", 5.0, "Cup", null, null));      // enough, case-insensitive
        pantry.add(new IngredientRequest("Eggs", 1.0, null, null, null));       // partial
        pantry.add(new IngredientRequest("Eggs", 1.0, "item", null, null));     // separate batch, summed
        pantry.add(new IngredientRequest("Soy Sauce", 1.0, "cup", null, null)); // same ingredient counts despite unit

        List<ShoppingListItem> list = service.shoppingList(meal.id());

        assertEquals(List.of(
                new ShoppingListItem("Rice", "cup", 2.0, 5.0, 0.0),
                new ShoppingListItem("Eggs", "item", 3.0, 2.0, 1.0),
                new ShoppingListItem("Soy Sauce", "tbsp", 2.0, 1.0, 1.0)), list);
    }

    @Test
    void defaultDateUsesClockAndIncludesExpiryTodayAndUndatedStock() {
        Meal meal = friedRice();
        pantry.add(new IngredientRequest("Eggs", 10.0, "item", null, TODAY.minusDays(1)));
        pantry.add(new IngredientRequest("Eggs", 1.0, "item", null, TODAY));
        pantry.add(new IngredientRequest("EGGS", 1.0, "item", null, TODAY.plusDays(1)));
        pantry.add(new IngredientRequest("eggs", 1.0, "item", null, null));
        pantry.add(new IngredientRequest("Eggs", 0.0, "item", null, TODAY));

        ShoppingListItem eggs = service.shoppingList(meal.id()).get(1);

        assertEquals(new ShoppingListItem("Eggs", "item", 3.0, 3.0, 0.0), eggs);
        assertEquals(service.shoppingList(meal.id()), service.shoppingList(meal.id(), null));
        assertEquals(service.shoppingList(meal.id()), service.shoppingList(meal.id(), TODAY));
    }

    @Test
    void plannedDateExcludesStockThatExpiresBeforeCookingAndDoesNotChangeInventory() {
        Meal meal = friedRice();
        pantry.add(new IngredientRequest("Eggs", 1.0, "item", null, TODAY));
        pantry.add(new IngredientRequest("Eggs", 1.0, "item", null, TODAY.plusDays(1)));
        pantry.add(new IngredientRequest("Eggs", 1.0, "item", null, null));
        List<Ingredient> before = pantry.findAll();

        List<ShoppingListItem> tomorrow = service.shoppingList(meal.id(), TODAY.plusDays(1));
        List<ShoppingListItem> later = service.shoppingList(meal.id(), TODAY.plusDays(2));

        assertEquals(new ShoppingListItem("Eggs", "item", 3.0, 2.0, 1.0), tomorrow.get(1));
        assertEquals(new ShoppingListItem("Eggs", "item", 3.0, 1.0, 2.0), later.get(1));
        assertEquals(later, service.shoppingList(meal.id(), TODAY.plusDays(2)));
        assertEquals(before, pantry.findAll());
        assertEquals(meal, service.findById(meal.id()));
    }

    @Test
    void duplicateNamesAcrossUnitsShareStockWithoutDecimalRoundingShortfalls() {
        Meal meal = service.add(new MealRequest("Rice dish", null, List.of(
                new MealRequest.Item("Rice", 0.1, "cup"),
                new MealRequest.Item("rice", 0.2, "g"),
                new MealRequest.Item("RICE", 0.1, "tbsp"))));
        Ingredient first = pantry.add(new IngredientRequest("rice", 0.1, "cup", null, null));
        Ingredient second = pantry.add(new IngredientRequest("RICE", 0.2, "g", null, null));

        assertEquals(List.of(
                new ShoppingListItem("Rice", "cup", 0.1, 0.3, 0.0),
                new ShoppingListItem("rice", "g", 0.2, 0.2, 0.0),
                new ShoppingListItem("RICE", "tbsp", 0.1, 0.0, 0.1)), service.shoppingList(meal.id()));
        assertEquals(0.1, pantry.findById(first.id()).quantity());
        assertEquals(0.2, pantry.findById(second.id()).quantity());
    }

    @Test
    void decimalShortfallIsExact() {
        Meal meal = service.add(new MealRequest("Salad", null,
                List.of(new MealRequest.Item("Oil", 0.3, "cup"))));
        pantry.add(new IngredientRequest("Oil", 0.1, "cup", null, null));

        assertEquals(new ShoppingListItem("Oil", "cup", 0.3, 0.1, 0.2),
                service.shoppingList(meal.id()).getFirst());
    }

    @Test
    void rejectsDatesOutsideTheSupportedCalendarRange() {
        Meal meal = friedRice();
        for (LocalDate date : List.of(LocalDate.of(0, 1, 1), LocalDate.of(10000, 1, 1), LocalDate.MAX)) {
            assertThrows(IllegalArgumentException.class, () -> service.shoppingList(meal.id(), date));
        }
        assertEquals(3, service.shoppingList(meal.id(), LocalDate.of(1, 1, 1)).size());
        assertEquals(3, service.shoppingList(meal.id(), LocalDate.of(9999, 12, 31)).size());
    }

    @Test
    void rejectsUnrepresentablePantryTotalsButIgnoresExpiredBatches() {
        Meal meal = friedRice();
        pantry.add(new IngredientRequest("Eggs", Double.MAX_VALUE, "item", null, null));
        pantry.add(new IngredientRequest("Eggs", Double.MAX_VALUE, "item", null, TODAY));

        assertThrows(IllegalArgumentException.class, () -> service.shoppingList(meal.id()));
        assertEquals(Double.MAX_VALUE, service.shoppingList(meal.id(), TODAY.plusDays(1)).get(1).onHand());
    }

    @Test
    void shoppingListForUnknownMealThrows() {
        assertThrows(NotFoundException.class, () -> service.shoppingList("missing"));
    }
}
