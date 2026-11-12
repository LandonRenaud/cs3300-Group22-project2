package com.cs3300.project2.meal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class MealFactoryTest {

    private final MealFactory factory = new MealFactory();

    private static MealRequest request(String name, MealRequest.Item... items) {
        return new MealRequest(name, null, Arrays.asList(items));
    }

    @Test
    void normalizesNameNotesAndIngredients() {
        Meal meal = factory.create(new MealRequest("  Fried   Rice ", "  use day-old rice  ",
                List.of(new MealRequest.Item(" Rice ", 2.0, "CUP"), new MealRequest.Item("Eggs", 2.0, null))));

        assertEquals("Fried Rice", meal.name());
        assertEquals("use day-old rice", meal.notes());
        assertEquals(List.of(new MealIngredient("Rice", 2.0, "cup"), new MealIngredient("Eggs", 2.0, "item")),
                meal.ingredients());
    }

    @Test
    void blankNotesBecomeEmpty() {
        Meal meal = factory.create(request("Toast", new MealRequest.Item("Bread", 2.0, "slice")));

        assertEquals("", meal.notes());
    }

    @Test
    void mergesDuplicateIngredientLines() {
        Meal meal = factory.create(request("Omelette",
                new MealRequest.Item("Eggs", 2.0, null),
                new MealRequest.Item("eggs", 1.0, "item"),
                new MealRequest.Item("Milk", 0.25, "cup")));

        assertEquals(List.of(new MealIngredient("Eggs", 3.0, "item"), new MealIngredient("Milk", 0.25, "cup")),
                meal.ingredients());
    }

    @Test
    void sameNameDifferentUnitIsNotMerged() {
        Meal meal = factory.create(request("Cake",
                new MealRequest.Item("Sugar", 1.0, "cup"),
                new MealRequest.Item("Sugar", 2.0, "tbsp")));

        assertEquals(2, meal.ingredients().size());
    }

    @Test
    void rebuildKeepsId() {
        assertEquals("abc", factory.rebuild("abc", request("Toast", new MealRequest.Item("Bread", 1.0, null))).id());
    }

    @Test
    void rejectsInvalidMeals() {
        assertThrows(IllegalArgumentException.class, () -> factory.create(null));
        assertThrows(IllegalArgumentException.class, () -> factory.create(request(" ", new MealRequest.Item("Bread", 1.0, null))));
        assertThrows(IllegalArgumentException.class, () -> factory.create(new MealRequest("Toast", null, null)));
        assertThrows(IllegalArgumentException.class, () -> factory.create(new MealRequest("Toast", null, List.of())));
        assertThrows(IllegalArgumentException.class, () -> factory.create(request("Toast", (MealRequest.Item) null)));
        assertThrows(IllegalArgumentException.class, () -> factory.create(request("Toast", new MealRequest.Item("Bread", -1.0, null))));
        assertThrows(IllegalArgumentException.class, () -> factory.create(new MealRequest("Toast",
                "x".repeat(MealFactory.MAX_NOTES_LENGTH + 1), List.of(new MealRequest.Item("Bread", 1.0, null)))));
    }

    @Test
    void ingredientListIsImmutable() {
        Meal meal = factory.create(request("Toast", new MealRequest.Item("Bread", 1.0, null)));

        assertThrows(UnsupportedOperationException.class, () -> meal.ingredients().clear());
    }
}
