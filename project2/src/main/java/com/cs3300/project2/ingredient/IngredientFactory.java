package com.cs3300.project2.ingredient;

import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Factory pattern: the single place ingredients are built, so every ingredient has
 * standardized attributes (trimmed name, lower-case unit, default unit and nutrition, validated quantity).
 */
@Component
public class IngredientFactory {

    static final String DEFAULT_UNIT = "item";
    static final int MAX_NAME_LENGTH = 100;

    public Ingredient create(IngredientRequest request) {
        return build(UUID.randomUUID().toString(), request);
    }

    /** Builds a replacement for an existing ingredient, keeping its id. */
    public Ingredient rebuild(String id, IngredientRequest request) {
        return build(id, request);
    }

    private Ingredient build(String id, IngredientRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Ingredient details are required");
        }
        return new Ingredient(
                id,
                normalizeName(request.name()),
                validateQuantity(request.quantity()),
                normalizeUnit(request.unit()),
                request.nutritionPerUnit() == null ? NutritionInfo.EMPTY : request.nutritionPerUnit(),
                request.expirationDate());
    }

    public static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        String collapsed = name.trim().replaceAll("\\s+", " ");
        if (collapsed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("Name must be " + MAX_NAME_LENGTH + " characters or fewer");
        }
        return collapsed;
    }

    public static String normalizeUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return DEFAULT_UNIT;
        }
        return unit.trim().toLowerCase(Locale.ROOT);
    }

    public static double validateQuantity(Double quantity) {
        if (quantity == null) {
            throw new IllegalArgumentException("Quantity is required");
        }
        if (quantity < 0 || quantity.isNaN() || quantity.isInfinite()) {
            throw new IllegalArgumentException("Quantity must be zero or greater");
        }
        return quantity;
    }
}
