package com.cs3300.project2.ingredient;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

/** Test-only in-memory implementation. This class is not included in the application artifact. */
@Repository
@Profile("test")
public class InMemoryIngredientRepository implements IngredientRepository {

    private final Map<String, Ingredient> store = new ConcurrentHashMap<>();

    @Override
    public List<Ingredient> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Ingredient::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public Optional<Ingredient> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Ingredient save(Ingredient ingredient) {
        store.put(ingredient.id(), ingredient);
        return ingredient;
    }

    @Override
    public boolean deleteById(String id) {
        return store.remove(id) != null;
    }
}
