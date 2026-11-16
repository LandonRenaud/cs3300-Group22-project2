package com.cs3300.project2.mealplan;

import java.util.Comparator;
import java.util.List;

/** Both policies have stable tie-breakers and prefer less repetition when pantry scores tie. */
public enum PlanningStrategy implements MealSelectionStrategy {
    EXPIRY_FIRST,
    PANTRY_FIRST;

    @Override
    public Candidate select(List<Candidate> candidates) {
        Comparator<Candidate> expiry = Comparator.comparing(Candidate::earliestExpiry,
                Comparator.nullsLast(Comparator.naturalOrder()));
        Comparator<Candidate> coverage = Comparator.comparingDouble(Candidate::coverage).reversed();
        Comparator<Candidate> ordering = this == EXPIRY_FIRST
                ? expiry.thenComparing(coverage) : coverage.thenComparing(expiry);
        return candidates.stream().min(ordering
                .thenComparingInt(Candidate::previousUses)
                .thenComparing(c -> c.meal().name(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(c -> c.meal().id()))
                .orElseThrow(() -> new IllegalArgumentException("No meals are available to plan"));
    }
}
