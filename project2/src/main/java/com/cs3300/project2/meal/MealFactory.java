package com.cs3300.project2.meal;

import static com.cs3300.project2.ingredient.IngredientFactory.normalizeName;
import static com.cs3300.project2.ingredient.IngredientFactory.normalizeUnit;
import static com.cs3300.project2.ingredient.IngredientFactory.validateQuantity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Factory pattern: builds meals with the same normalization rules as pantry ingredients,
 * so a meal's "Eggs" can be matched against the pantry's "eggs". Duplicate lines
 * (same name and unit) are merged by adding their quantities.
 */
@Component
public class MealFactory {

    static final int MAX_NOTES_LENGTH = 2000;

    public Meal create(MealRequest request) {
        return build(UUID.randomUUID().toString(), request);
    }

    /** Builds a replacement for an existing meal, keeping its id. */
    public Meal rebuild(String id, MealRequest request) {
        return build(id, request);
    }

    private Meal build(String id, MealRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Meal details are required");
        }
        return new Meal(id, normalizeName(request.name()), normalizeNotes(request.notes()),
                buildIngredients(request.ingredients()));
    }

    private static String normalizeNotes(String notes) {
        if (notes == null || notes.isBlank()) {
            return "";
        }
        String trimmed = notes.strip();
        if (trimmed.length() > MAX_NOTES_LENGTH) {
            throw new IllegalArgumentException("Notes must be " + MAX_NOTES_LENGTH + " characters or fewer");
        }
        return trimmed;
    }

    private static List<MealIngredient> buildIngredients(List<MealRequest.Item> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("A meal needs at least one ingredient");
        }
        Map<String, MealIngredient> merged = new LinkedHashMap<>();
        for (MealRequest.Item item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Ingredient entries can't be empty");
            }
            MealIngredient line = new MealIngredient(
                    normalizeName(item.name()), validateQuantity(item.quantity()), normalizeUnit(item.unit()));
            merged.merge(key(line), line, (a, b) -> new MealIngredient(a.name(), a.quantity() + b.quantity(), a.unit()));
        }
        return new ArrayList<>(merged.values());
    }

    static String key(MealIngredient line) {
        return line.name().toLowerCase(Locale.ROOT) + "|" + line.unit();
    }
}
