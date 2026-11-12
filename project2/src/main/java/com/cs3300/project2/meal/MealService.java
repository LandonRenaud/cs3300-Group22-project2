package com.cs3300.project2.meal;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.cs3300.project2.common.NotFoundException;
import com.cs3300.project2.ingredient.Ingredient;
import com.cs3300.project2.ingredient.IngredientService;

@Service
public class MealService {

    private final MealRepository repository;
    private final MealFactory factory;
    private final IngredientService ingredients;

    public MealService(MealRepository repository, MealFactory factory, IngredientService ingredients) {
        this.repository = repository;
        this.factory = factory;
        this.ingredients = ingredients;
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

    /** Compares a meal's ingredients with the pantry so the user knows what to pick up at the store. */
    public List<ShoppingListItem> shoppingList(String id) {
        Meal meal = findById(id);
        Map<String, Double> pantry = ingredients.findAll().stream()
                .collect(Collectors.groupingBy(MealService::pantryKey, Collectors.summingDouble(Ingredient::quantity)));

        return meal.ingredients().stream()
                .map(line -> {
                    double onHand = pantry.getOrDefault(MealFactory.key(line), 0.0);
                    return new ShoppingListItem(line.name(), line.unit(), line.quantity(), onHand,
                            Math.max(0, line.quantity() - onHand));
                })
                .toList();
    }

    private static String pantryKey(Ingredient ingredient) {
        return MealFactory.key(new MealIngredient(ingredient.name(), ingredient.quantity(), ingredient.unit()));
    }

    private static boolean contains(String text, String lowerNeedle) {
        return text.toLowerCase(Locale.ROOT).contains(lowerNeedle);
    }
}
