package com.cs3300.project2.ingredient;

import java.util.List;
import java.util.Optional;

/**
 * Storage abstraction for ingredients. The in-memory implementation is used for now;
 * a Firebase Realtime Database implementation can serve the service and controller.
 */
public interface IngredientRepository {

    List<Ingredient> findAll();

    Optional<Ingredient> findById(String id);

    Ingredient save(Ingredient ingredient);

    boolean deleteById(String id);
}
