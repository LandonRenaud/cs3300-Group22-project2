package com.cs3300.project2.mealplan;

import java.time.Clock;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class SavedWeeklyMealPlanFactory {
    private final Clock clock;

    public SavedWeeklyMealPlanFactory(Clock clock) {
        this.clock = clock;
    }

    public SavedWeeklyMealPlan create(WeeklyMealPlan plan) {
        if (plan == null || plan.days().isEmpty()) {
            throw new IllegalArgumentException("Add a meal with a positive ingredient quantity before saving a weekly plan");
        }
        return new SavedWeeklyMealPlan(UUID.randomUUID().toString(), clock.instant(), plan);
    }
}
