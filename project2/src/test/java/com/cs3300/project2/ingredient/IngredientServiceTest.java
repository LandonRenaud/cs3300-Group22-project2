package com.cs3300.project2.ingredient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cs3300.project2.common.NotFoundException;

class IngredientServiceTest {

    private IngredientService service;

    @BeforeEach
    void setUp() {
        service = new IngredientService(new InMemoryIngredientRepository(), new IngredientFactory());
    }

    @Test
    void addedIngredientsAreListedAlphabetically() {
        service.add(new IngredientRequest("tomatoes", 4.0, null, null, null));
        service.add(new IngredientRequest("Apples", 3.0, null, null, null));

        List<String> names = service.findAll().stream().map(Ingredient::name).toList();

        assertEquals(List.of("Apples", "tomatoes"), names);
    }

    @Test
    void updateReplacesFieldsButKeepsId() {
        Ingredient original = service.add(new IngredientRequest("Milk", 1.0, "gallon", null, null));

        Ingredient updated = service.update(original.id(), new IngredientRequest("Oat Milk", 2.0, "carton", null, null));

        assertEquals(original.id(), updated.id());
        assertEquals("Oat Milk", service.findById(original.id()).name());
        assertEquals(1, service.findAll().size());
    }

    @Test
    void updateUnknownIdThrowsNotFound() {
        assertThrows(NotFoundException.class,
                () -> service.update("missing", new IngredientRequest("Milk", 1.0, null, null, null)));
    }

    @Test
    void adjustQuantityAddsAndSubtracts() {
        Ingredient eggs = service.add(new IngredientRequest("Eggs", 12.0, null, null, null));

        service.adjustQuantity(eggs.id(), 6);
        Ingredient result = service.adjustQuantity(eggs.id(), -18);

        assertEquals(0.0, result.quantity());
    }

    @Test
    void adjustQuantityCannotGoNegative() {
        Ingredient eggs = service.add(new IngredientRequest("Eggs", 2.0, null, null, null));

        assertThrows(IllegalArgumentException.class, () -> service.adjustQuantity(eggs.id(), -3));
        assertEquals(2.0, service.findById(eggs.id()).quantity());
    }

    @Test
    void deleteRemovesIngredient() {
        Ingredient eggs = service.add(new IngredientRequest("Eggs", 2.0, null, null, null));

        service.delete(eggs.id());

        assertTrue(service.findAll().isEmpty());
        assertThrows(NotFoundException.class, () -> service.delete(eggs.id()));
    }

    @Test
    void totalNutritionSumsQuantityTimesPerUnit() {
        service.add(new IngredientRequest("Eggs", 2.0, null, new NutritionInfo(70, 6, 0.5, 5), null));
        service.add(new IngredientRequest("Bread", 3.0, "slice", new NutritionInfo(80, 3, 15, 1), null));

        NutritionInfo total = service.totalNutrition();

        assertEquals(380, total.calories(), 1e-9);
        assertEquals(21, total.proteinGrams(), 1e-9);
        assertEquals(46, total.carbsGrams(), 1e-9);
        assertEquals(13, total.fatGrams(), 1e-9);
    }

    @Test
    void totalNutritionOfEmptyPantryIsZero() {
        assertEquals(NutritionInfo.EMPTY, service.totalNutrition());
    }
}
