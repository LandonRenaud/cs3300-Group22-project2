package com.cs3300.project2.mealplan;

import java.time.Instant;
import java.util.UUID;

/** A saved snapshot of the recipes, allocations, and shopping list at generation time. */
public record SavedWeeklyMealPlan(String id, Instant savedAt, WeeklyMealPlan plan) {
    public SavedWeeklyMealPlan {
        id = requireId(id);
        if (savedAt == null || plan == null) {
            throw new IllegalArgumentException("A saved plan needs its save time and weekly plan");
        }
    }

    /** Rejects Firebase path separators and noncanonical or partial UUIDs. */
    public static String requireId(String id) {
        if (id == null || !id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
            throw new IllegalArgumentException("Saved plan ID must be a UUID");
        }
        return UUID.fromString(id).toString();
    }
}
