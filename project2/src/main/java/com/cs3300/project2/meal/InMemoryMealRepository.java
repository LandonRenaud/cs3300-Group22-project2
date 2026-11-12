package com.cs3300.project2.meal;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryMealRepository implements MealRepository {

    private final Map<String, Meal> store = new ConcurrentHashMap<>();

    @Override
    public List<Meal> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(Meal::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public Optional<Meal> findById(String id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Meal save(Meal meal) {
        store.put(meal.id(), meal);
        return meal;
    }

    @Override
    public boolean deleteById(String id) {
        return store.remove(id) != null;
    }
}
