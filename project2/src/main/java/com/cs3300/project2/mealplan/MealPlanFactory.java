package com.cs3300.project2.mealplan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.cs3300.project2.meal.Meal;
import com.cs3300.project2.mealplan.WeeklyMealPlan.Day;
import com.cs3300.project2.mealplan.WeeklyMealPlan.IngredientNeed;

/**
 * Factory pattern: assembles weekly meal plans and their days. The plan's shopping list is
 * derived here from the days, so it always matches the meals that were scheduled.
 */
@Component
public class MealPlanFactory {

    public Day day(LocalDate date, Meal meal, List<IngredientNeed> ingredients) {
        if (date == null || meal == null || ingredients == null) {
            throw new IllegalArgumentException("A plan day needs a date, a meal, and its ingredients");
        }
        return new Day(date, meal, ingredients);
    }

    /** A plan with no days, used when there are no usable saved recipes. */
    public WeeklyMealPlan empty(LocalDate startDate, PlanningStrategy strategy) {
        return create(startDate, strategy, List.of());
    }

    /**
     * Builds a plan from consecutive days beginning at {@code startDate}. The shopping list
     * aggregates every day's shortfall by ingredient name (ignoring case) and unit.
     */
    public WeeklyMealPlan create(LocalDate startDate, PlanningStrategy strategy, List<Day> days) {
        if (startDate == null || strategy == null || days == null) {
            throw new IllegalArgumentException("A plan needs a start date, a strategy, and its days");
        }
        for (int i = 0; i < days.size(); i++) {
            if (!days.get(i).date().equals(startDate.plusDays(i))) {
                throw new IllegalArgumentException("Plan days must be consecutive from the start date");
            }
        }
        return new WeeklyMealPlan(startDate, strategy, days, shoppingList(days));
    }

    private static List<IngredientNeed> shoppingList(List<Day> days) {
        Map<String, IngredientNeed> totals = new LinkedHashMap<>();
        for (Day day : days) {
            for (IngredientNeed need : day.ingredients()) {
                String key = need.name().toLowerCase(Locale.ROOT) + "|" + need.unit();
                totals.merge(key, need, (a, b) -> new IngredientNeed(a.name(), a.unit(),
                        sum(a.needed(), b.needed()), sum(a.fromPantry(), b.fromPantry()),
                        sum(a.toBuy(), b.toBuy())));
            }
        }
        return totals.values().stream().filter(n -> n.toBuy() > 0).toList();
    }

    private static double sum(double a, double b) {
        double total = BigDecimal.valueOf(a).add(BigDecimal.valueOf(b)).doubleValue();
        if (!Double.isFinite(total)) {
            throw new IllegalArgumentException("Weekly ingredient quantities exceed the supported range");
        }
        return total;
    }
}
