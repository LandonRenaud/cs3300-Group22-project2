package com.cs3300.project2.mealplan;

import java.time.LocalDate;
import java.util.List;

import com.cs3300.project2.meal.Meal;

/** Strategy pattern: chooses a meal from candidates evaluated against the remaining pantry. */
public interface MealSelectionStrategy {

    Candidate select(List<Candidate> candidates);

    /** Coverage is the mean fulfilled fraction of the recipe's positive-quantity lines. */
    record Candidate(Meal meal, LocalDate earliestExpiry, double coverage, int previousUses) {
    }
}
