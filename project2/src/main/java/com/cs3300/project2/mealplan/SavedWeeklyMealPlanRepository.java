package com.cs3300.project2.mealplan;

import java.util.List;
import java.util.Optional;

/** Saved plans belong to the authenticated user, just like recipes and pantry entries. */
public interface SavedWeeklyMealPlanRepository {
    List<SavedWeeklyMealPlan> findAll();

    Optional<SavedWeeklyMealPlan> findById(String id);

    SavedWeeklyMealPlan save(SavedWeeklyMealPlan plan);

    boolean deleteById(String id);
}
