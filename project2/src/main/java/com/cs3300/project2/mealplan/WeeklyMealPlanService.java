package com.cs3300.project2.mealplan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cs3300.project2.ingredient.Ingredient;
import com.cs3300.project2.ingredient.IngredientService;
import com.cs3300.project2.meal.Meal;
import com.cs3300.project2.meal.MealIngredient;
import com.cs3300.project2.meal.MealService;
import com.cs3300.project2.mealplan.MealSelectionStrategy.Candidate;
import com.cs3300.project2.mealplan.WeeklyMealPlan.IngredientNeed;

@Service
public class WeeklyMealPlanService {
    private final MealService meals;
    private final IngredientService ingredients;

    public WeeklyMealPlanService(MealService meals, IngredientService ingredients) {
        this.meals = meals;
        this.ingredients = ingredients;
    }

    public WeeklyMealPlan generate(LocalDate startDate, PlanningStrategy strategy) {
        if (startDate == null || startDate.getYear() < 1
                || startDate.isAfter(LocalDate.of(9999, 12, 25))) {
            throw new IllegalArgumentException("Start date must allow seven days within years 0001 to 9999");
        }
        if (strategy == null) {
            throw new IllegalArgumentException("A planning strategy is required");
        }
        List<Meal> recipes = meals.findAll(null).stream()
                .filter(m -> m.ingredients().stream().anyMatch(i -> i.quantity() > 0)).toList();
        if (recipes.isEmpty()) {
            return new WeeklyMealPlan(startDate, strategy, List.of(), List.of());
        }
        List<Stock> pantry = ingredients.findAll().stream()
                .sorted(Comparator.comparing(Ingredient::expirationDate,
                        Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Ingredient::id))
                .map(Stock::new).toList();
        Map<String, Integer> uses = new HashMap<>();
        Map<Key, IngredientNeed> totals = new LinkedHashMap<>();
        List<WeeklyMealPlan.Day> days = new ArrayList<>();
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = startDate.plusDays(offset);
            List<Candidate> candidates = new ArrayList<>();
            for (Meal meal : recipes) {
                // Evaluate on copies so unselected recipes cannot consume the week's stock.
                Allocation allocation = allocate(meal, pantry.stream().map(Stock::copy).toList(), date);
                candidates.add(new Candidate(meal, allocation.earliestExpiry(), allocation.coverage(),
                        uses.getOrDefault(meal.id(), 0)));
            }
            Meal selected = strategy.select(candidates).meal();
            Allocation allocation = allocate(selected, pantry, date);
            days.add(new WeeklyMealPlan.Day(date, selected, allocation.needs()));
            uses.merge(selected.id(), 1, Integer::sum);
            for (IngredientNeed need : allocation.needs()) {
                totals.merge(new Key(need.name(), need.unit()), need, (a, b) -> new IngredientNeed(
                        a.name(), a.unit(), sum(a.needed(), b.needed()), sum(a.fromPantry(), b.fromPantry()),
                        sum(a.toBuy(), b.toBuy())));
            }
        }
        List<IngredientNeed> shopping = totals.values().stream().filter(n -> n.toBuy() > 0).toList();
        return new WeeklyMealPlan(startDate, strategy, days, shopping);
    }

    private static Allocation allocate(Meal meal, List<Stock> pantry, LocalDate date) {
        List<IngredientNeed> needs = new ArrayList<>();
        LocalDate earliest = null;
        double coverage = 0;
        for (MealIngredient line : meal.ingredients()) {
            if (line.quantity() <= 0) continue;
            BigDecimal required = BigDecimal.valueOf(line.quantity());
            BigDecimal missing = required;
            Key key = new Key(line.name(), line.unit());
            for (Stock stock : pantry) {
                if (missing.signum() == 0) break;
                LocalDate expiry = stock.ingredient.expirationDate();
                if (!stock.key.equals(key) || stock.remaining.signum() <= 0
                        || (expiry != null && expiry.isBefore(date))) continue;
                BigDecimal used = missing.min(stock.remaining);
                stock.remaining = stock.remaining.subtract(used);
                missing = missing.subtract(used);
                if (expiry != null && (earliest == null || expiry.isBefore(earliest))) earliest = expiry;
            }
            double allocated = required.subtract(missing).doubleValue();
            coverage += allocated / line.quantity();
            needs.add(new IngredientNeed(line.name(), line.unit(), line.quantity(), allocated, missing.doubleValue()));
        }
        return new Allocation(needs, earliest, needs.isEmpty() ? 0 : coverage / needs.size());
    }

    private static double sum(double a, double b) {
        double total = BigDecimal.valueOf(a).add(BigDecimal.valueOf(b)).doubleValue();
        if (!Double.isFinite(total)) {
            throw new IllegalArgumentException("Weekly ingredient quantities exceed the supported range");
        }
        return total;
    }

    private record Allocation(List<IngredientNeed> needs, LocalDate earliestExpiry, double coverage) {
    }

    private record Key(String name, String unit) {
        private Key {
            name = name.toLowerCase(Locale.ROOT);
        }
    }

    private static final class Stock {
        private final Ingredient ingredient;
        private final Key key;
        private BigDecimal remaining;

        private Stock(Ingredient ingredient) {
            this.ingredient = ingredient;
            key = new Key(ingredient.name(), ingredient.unit());
            remaining = BigDecimal.valueOf(ingredient.quantity());
        }

        private Stock copy() {
            Stock copy = new Stock(ingredient);
            copy.remaining = remaining;
            return copy;
        }
    }
}
