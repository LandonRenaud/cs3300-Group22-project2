package com.cs3300.project2.mealplan;

import java.time.LocalDate;

/** The server regenerates the snapshot from current recipes and pantry data. */
public record SaveWeeklyMealPlanRequest(LocalDate startDate, PlanningStrategy strategy) {
}
