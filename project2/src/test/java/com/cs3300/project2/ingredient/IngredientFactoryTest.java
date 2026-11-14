package com.cs3300.project2.ingredient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

class IngredientFactoryTest {

    private final IngredientFactory factory = new IngredientFactory();

    @Test
    void normalizesNameAndUnit() {
        Ingredient ingredient = factory.create(new IngredientRequest("  Brown   Rice ", 2.0, " KG ", null, null));

        assertEquals("Brown Rice", ingredient.name());
        assertEquals("kg", ingredient.unit());
        assertEquals(2.0, ingredient.quantity());
    }

    @Test
    void appliesDefaultsForOptionalFields() {
        Ingredient ingredient = factory.create(new IngredientRequest("Eggs", 12.0, null, null, null));

        assertEquals(IngredientFactory.DEFAULT_UNIT, ingredient.unit());
        assertEquals(NutritionInfo.EMPTY, ingredient.nutritionPerUnit());
        assertNull(ingredient.expirationDate());
    }

    @Test
    void keepsNutritionAndExpiration() {
        NutritionInfo nutrition = new NutritionInfo(70, 6, 0.5, 5);
        LocalDate expires = LocalDate.of(2026, 12, 1);

        Ingredient ingredient = factory.create(new IngredientRequest("Eggs", 12.0, "item", nutrition, expires));

        assertEquals(nutrition, ingredient.nutritionPerUnit());
        assertEquals(expires, ingredient.expirationDate());
    }

    @Test
    void preservesImportedFactsBasisAndSource() {
        NutritionInfo facts = new NutritionInfo(379, 13.2, 67.7, 6.5,
                Map.of("usda_1079", new NutrientValue("Fiber, total dietary", 10.1, "g")));
        NutritionBasis basis = new NutritionBasis(100, "g", "per 100 g");
        NutritionSource source = new NutritionSource("USDA_FDC", "123", "Oats", "2026-11-14T12:00:00Z");

        Ingredient ingredient = factory.create(new IngredientRequest("Oats", 500.0, "g", facts, null, basis, source));

        assertEquals(facts, ingredient.nutritionPerUnit());
        assertEquals(basis, ingredient.nutritionBasis());
        assertEquals(source, ingredient.nutritionSource());
        assertEquals(50.5, ingredient.totalNutrition().additionalNutrients().get("usda_1079").amount(), 1e-9);
    }

    @Test
    void assignsUniqueIds() {
        IngredientRequest request = new IngredientRequest("Milk", 1.0, "gallon", null, null);

        Ingredient first = factory.create(request);
        Ingredient second = factory.create(request);

        assertNotNull(first.id());
        assertNotEquals(first.id(), second.id());
    }

    @Test
    void rebuildKeepsId() {
        Ingredient rebuilt = factory.rebuild("abc", new IngredientRequest("Milk", 1.0, null, null, null));

        assertEquals("abc", rebuilt.id());
    }

    @Test
    void zeroQuantityIsAllowed() {
        Ingredient ingredient = factory.create(new IngredientRequest("Salt", 0.0, null, null, null));

        assertEquals(0.0, ingredient.quantity());
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(new IngredientRequest("   ", 1.0, null, null, null)));
    }

    @Test
    void rejectsOverlyLongName() {
        String name = "a".repeat(IngredientFactory.MAX_NAME_LENGTH + 1);

        assertThrows(IllegalArgumentException.class,
                () -> factory.create(new IngredientRequest(name, 1.0, null, null, null)));
    }

    @Test
    void rejectsMissingOrInvalidQuantity() {
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(new IngredientRequest("Milk", null, null, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(new IngredientRequest("Milk", -1.0, null, null, null)));
        assertThrows(IllegalArgumentException.class,
                () -> factory.create(new IngredientRequest("Milk", Double.NaN, null, null, null)));
    }

    @Test
    void rejectsNullRequest() {
        assertThrows(IllegalArgumentException.class, () -> factory.create(null));
    }

    @Test
    void nutritionRejectsNegativeValues() {
        assertThrows(IllegalArgumentException.class, () -> new NutritionInfo(-1, 0, 0, 0));
    }
}
