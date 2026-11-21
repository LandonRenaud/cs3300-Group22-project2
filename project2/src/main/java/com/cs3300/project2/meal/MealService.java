package com.cs3300.project2.meal;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cs3300.project2.common.NotFoundException;
import com.cs3300.project2.ingredient.Ingredient;
import com.cs3300.project2.ingredient.IngredientService;

@Service
public class MealService {

    private final MealRepository repository;
    private final MealFactory factory;
    private final IngredientService ingredients;
    private final Clock clock;

    public MealService(MealRepository repository, MealFactory factory, IngredientService ingredients, Clock clock) {
        this.repository = repository;
        this.factory = factory;
        this.ingredients = ingredients;
        this.clock = clock;
    }

    /**
     * All saved meals, or only those whose name or any ingredient contains {@code query}
     * (case-insensitive) when a query is given.
     */
    public List<Meal> findAll(String query) {
        List<Meal> all = repository.findAll();
        if (query == null || query.isBlank()) {
            return all;
        }
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return all.stream()
                .filter(meal -> contains(meal.name(), needle)
                        || meal.ingredients().stream().anyMatch(i -> contains(i.name(), needle)))
                .toList();
    }

    public Meal findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Meal " + id + " not found"));
    }

    public Meal add(MealRequest request) {
        return repository.save(factory.create(request));
    }

    public Meal update(String id, MealRequest request) {
        findById(id);
        return repository.save(factory.rebuild(id, request));
    }

    public void delete(String id) {
        if (!repository.deleteById(id)) {
            throw new NotFoundException("Meal " + id + " not found");
        }
    }

    /** Compares a meal with stock that is usable today, without changing saved quantities. */
    public List<ShoppingListItem> shoppingList(String id) {
        return shoppingList(id, null);
    }

    /** Stock remains usable on its expiry date; undated stock is included. */
    public List<ShoppingListItem> shoppingList(String id, LocalDate onDate) {
        LocalDate date = onDate == null ? LocalDate.now(clock) : onDate;
        if (date.getYear() < 1 || date.getYear() > 9999) {
            throw new IllegalArgumentException("Cooking date must be within years 0001 to 9999");
        }
        Meal meal = findById(id);
        Map<String, BigDecimal> pantry = new HashMap<>();
        for (Ingredient ingredient : ingredients.findAll()) {
            if (ingredient.expirationDate() != null && ingredient.expirationDate().isBefore(date)) continue;
            pantry.merge(MealFactory.ingredientNameKey(ingredient.name()),
                    BigDecimal.valueOf(ingredient.quantity()), BigDecimal::add);
        }

        List<ShoppingListItem> list = new ArrayList<>();
        for (MealIngredient line : meal.ingredients()) {
            String key = MealFactory.ingredientNameKey(line.name());
            BigDecimal onHand = pantry.getOrDefault(key, BigDecimal.ZERO);
            if (!Double.isFinite(onHand.doubleValue())) {
                throw new IllegalArgumentException("Available pantry quantity is too large for a shopping list");
            }
            BigDecimal needed = BigDecimal.valueOf(line.quantity());
            BigDecimal used = needed.min(onHand);
            pantry.put(key, onHand.subtract(used));
            list.add(new ShoppingListItem(line.name(), line.unit(), line.quantity(), onHand.doubleValue(),
                    needed.subtract(used).doubleValue()));
        }
        return list;
    }
    private static boolean contains(String text, String lowerNeedle) {
        return text.toLowerCase(Locale.ROOT).contains(lowerNeedle);
    }
}
