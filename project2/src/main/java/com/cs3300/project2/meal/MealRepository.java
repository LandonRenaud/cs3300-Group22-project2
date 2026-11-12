package com.cs3300.project2.meal;

import java.util.List;
import java.util.Optional;

/** Storage abstraction for saved meals; in-memory for now, Firestore-ready. */
public interface MealRepository {

    List<Meal> findAll();

    Optional<Meal> findById(String id);

    Meal save(Meal meal);

    boolean deleteById(String id);
}
