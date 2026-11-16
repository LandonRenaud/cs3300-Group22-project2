package com.cs3300.project2.mealplan;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Uses the existing /api authentication filter and user-scoped ingredient/meal services. */
@RestController
@RequestMapping("/api/meal-plans")
public class WeeklyMealPlanController {
    private final WeeklyMealPlanService planner;

    public WeeklyMealPlanController(WeeklyMealPlanService planner) {
        this.planner = planner;
    }

    @GetMapping("/weekly")
    public WeeklyMealPlan weekly(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "EXPIRY_FIRST") PlanningStrategy strategy) {
        return planner.generate(startDate, strategy);
    }
}
