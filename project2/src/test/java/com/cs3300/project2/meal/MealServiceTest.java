package com.cs3300.project2.meal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cs3300.project2.common.NotFoundException;
import com.cs3300.project2.ingredient.InMemoryIngredientRepository;
import com.cs3300.project2.ingredient.IngredientFactory;
import com.cs3300.project2.ingredient.IngredientRequest;
import com.cs3300.project2.ingredient.IngredientService;

class MealServiceTest {

    private IngredientService pantry;
    private MealService service;

    @BeforeEach
    void setUp() {
        pantry = new IngredientService(new InMemoryIngredientRepository(), new IngredientFactory());
        service = new MealService(new InMemoryMealRepository(), new MealFactory(), pantry);
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
        pantry.add(new IngredientRequest("Soy Sauce", 1.0, "cup", null, null)); // wrong unit, doesn't count

        List<ShoppingListItem> list = service.shoppingList(meal.id());

        assertEquals(List.of(
                new ShoppingListItem("Rice", "cup", 2.0, 5.0, 0.0),
                new ShoppingListItem("Eggs", "item", 3.0, 2.0, 1.0),
                new ShoppingListItem("Soy Sauce", "tbsp", 2.0, 0.0, 2.0)), list);
    }

    @Test
    void shoppingListForUnknownMealThrows() {
        assertThrows(NotFoundException.class, () -> service.shoppingList("missing"));
    }
}
