package com.cs3300.project2.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cs3300.project2.common.NotFoundException;
import com.cs3300.project2.common.UserContext;
import com.cs3300.project2.ingredient.InMemoryIngredientRepository;
import com.cs3300.project2.ingredient.Ingredient;
import com.cs3300.project2.ingredient.IngredientFactory;
import com.cs3300.project2.ingredient.IngredientRequest;
import com.cs3300.project2.ingredient.IngredientService;
import com.cs3300.project2.meal.InMemoryMealRepository;
import com.cs3300.project2.meal.Meal;
import com.cs3300.project2.meal.MealFactory;
import com.cs3300.project2.meal.MealRequest;
import com.cs3300.project2.meal.MealService;

class SavedWeeklyMealPlanServiceTest {
    private static final LocalDate START = LocalDate.of(2026, 11, 28);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T22:00:00Z"), ZoneOffset.UTC);
    private InMemorySavedWeeklyMealPlanRepository repository;
    private IngredientService pantry;
    private MealService meals;
    private WeeklyMealPlanService planner;
    private SavedWeeklyMealPlanService service;

    @BeforeEach
    void setUp() {
        UserContext.set("user-a", "test-token-a");
        repository = new InMemorySavedWeeklyMealPlanRepository();
        pantry = new IngredientService(new InMemoryIngredientRepository(), new IngredientFactory());
        meals = new MealService(new InMemoryMealRepository(), new MealFactory(), pantry, CLOCK);
        planner = new WeeklyMealPlanService(meals, pantry, new MealPlanFactory());
        service = new SavedWeeklyMealPlanService(repository, new SavedWeeklyMealPlanFactory(CLOCK), planner);
    }

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    private Meal addOmelet() {
        return meals.add(new MealRequest("Omelet", "Whisk first", List.of(
                new MealRequest.Item("Eggs", 2.0, "item"))));
    }

    @Test
    void savesACompleteServerGeneratedPlanWithDefaultStrategyWithoutChangingInventory() {
        Meal recipe = addOmelet();
        Ingredient eggs = pantry.add(new IngredientRequest("Eggs", 3.0, "item", null, null));

        SavedWeeklyMealPlan saved = service.save(new SaveWeeklyMealPlanRequest(START, null));

        assertEquals(saved.id(), UUID.fromString(saved.id()).toString());
        assertEquals(CLOCK.instant(), saved.savedAt());
        assertEquals(START, saved.plan().startDate());
        assertEquals(PlanningStrategy.EXPIRY_FIRST, saved.plan().strategy());
        assertEquals(7, saved.plan().days().size());
        assertEquals(recipe, saved.plan().days().getFirst().meal());
        assertEquals(11.0, saved.plan().shoppingList().getFirst().toBuy());
        assertEquals(eggs, pantry.findById(eggs.id()));
        assertEquals(saved, service.findById(saved.id()));
        assertEquals(List.of(saved), service.findAll());
    }

    @Test
    void eachSaveRegeneratesFromCurrentDataAndCreatesAnIndependentSnapshot() {
        addOmelet();
        Ingredient eggs = pantry.add(new IngredientRequest("Eggs", 3.0, "item", null, null));
        SaveWeeklyMealPlanRequest request = new SaveWeeklyMealPlanRequest(START, PlanningStrategy.PANTRY_FIRST);

        SavedWeeklyMealPlan first = service.save(request);
        SavedWeeklyMealPlan duplicate = service.save(request);
        pantry.adjustQuantity(eggs.id(), 1.0);
        SavedWeeklyMealPlan updated = service.save(request);

        assertNotEquals(first.id(), duplicate.id());
        assertNotEquals(first.id(), updated.id());
        assertEquals(first.plan(), duplicate.plan());
        assertEquals(11.0, first.plan().shoppingList().getFirst().toBuy());
        assertEquals(10.0, updated.plan().shoppingList().getFirst().toBuy());
        assertEquals(PlanningStrategy.PANTRY_FIRST, updated.plan().strategy());
        assertEquals(3, service.findAll().size());
        assertEquals(4.0, pantry.findById(eggs.id()).quantity());
        service.delete(duplicate.id());
        assertEquals(first, service.findById(first.id()));
        assertEquals(updated, service.findById(updated.id()));
    }

    @Test
    void snapshotsSurviveRecipeAndPantryChangesAndDeletion() {
        Meal recipe = addOmelet();
        Ingredient eggs = pantry.add(new IngredientRequest("Eggs", 3.0, "item", null, null));
        SavedWeeklyMealPlan saved = service.save(new SaveWeeklyMealPlanRequest(START, PlanningStrategy.EXPIRY_FIRST));

        meals.update(recipe.id(), new MealRequest("Large omelet", "Different notes", List.of(
                new MealRequest.Item("Eggs", 5.0, "item"))));
        pantry.update(eggs.id(), new IngredientRequest("Eggs", 50.0, "item", null, START.minusDays(1)));
        assertEquals(saved, service.findById(saved.id()));
        meals.delete(recipe.id());
        pantry.delete(eggs.id());

        assertEquals(saved, service.findById(saved.id()));
        assertEquals(recipe, service.findById(saved.id()).plan().days().getFirst().meal());
        assertEquals(11.0, service.findById(saved.id()).plan().shoppingList().getFirst().toBuy());
        assertTrue(planner.generate(START, PlanningStrategy.EXPIRY_FIRST).days().isEmpty());
    }

    @Test
    void listsNewestSavedTimeFirstWithDeterministicTies() {
        addOmelet();
        SaveWeeklyMealPlanRequest request = new SaveWeeklyMealPlanRequest(START, null);
        SavedWeeklyMealPlan old = service.save(request);
        SavedWeeklyMealPlanService later = new SavedWeeklyMealPlanService(repository,
                new SavedWeeklyMealPlanFactory(Clock.offset(CLOCK, Duration.ofHours(1))), planner);
        SavedWeeklyMealPlan newest = later.save(request);
        SavedWeeklyMealPlan tied = service.save(request);
        List<String> expectedTies = List.of(old.id(), tied.id()).stream().sorted().toList();

        List<SavedWeeklyMealPlan> saved = service.findAll();

        assertEquals(newest, saved.getFirst());
        assertEquals(expectedTies, saved.subList(1, 3).stream().map(SavedWeeklyMealPlan::id).toList());
    }

    @Test
    void rejectsEmptyPlansAndInvalidRequestsWithoutSavingAnything() {
        assertThrows(IllegalArgumentException.class, () -> service.save(null));
        assertThrows(IllegalArgumentException.class, () -> service.save(new SaveWeeklyMealPlanRequest(null, null)));
        for (LocalDate date : List.of(LocalDate.of(0, 1, 1), LocalDate.of(9999, 12, 26), LocalDate.MAX)) {
            assertThrows(IllegalArgumentException.class, () -> service.save(new SaveWeeklyMealPlanRequest(date, null)));
        }
        assertThrows(IllegalArgumentException.class, () -> service.save(new SaveWeeklyMealPlanRequest(START, null)));
        meals.add(new MealRequest("Empty meal", "", List.of(new MealRequest.Item("Eggs", 0.0, "item"))));
        assertThrows(IllegalArgumentException.class, () -> service.save(new SaveWeeklyMealPlanRequest(START, null)));
        assertTrue(service.findAll().isEmpty());
    }

    @Test
    void deletingAPlanLeavesRecipesAndPantryUntouchedAndMissingIdsReturnNotFound() {
        Meal recipe = addOmelet();
        Ingredient eggs = pantry.add(new IngredientRequest("Eggs", 3.0, "item", null, null));
        SavedWeeklyMealPlan saved = service.save(new SaveWeeklyMealPlanRequest(START, null));

        service.delete(saved.id());

        assertTrue(service.findAll().isEmpty());
        assertEquals(recipe, meals.findById(recipe.id()));
        assertEquals(eggs, pantry.findById(eggs.id()));
        assertThrows(NotFoundException.class, () -> service.findById(saved.id()));
        assertThrows(NotFoundException.class, () -> service.delete(saved.id()));
    }

    @Test
    void savedPlansAreIsolatedByAuthenticatedUser() {
        addOmelet();
        SaveWeeklyMealPlanRequest request = new SaveWeeklyMealPlanRequest(START, null);
        SavedWeeklyMealPlan alice = service.save(request);
        UserContext.set("user-b", "test-token-b");

        assertTrue(service.findAll().isEmpty());
        assertThrows(NotFoundException.class, () -> service.findById(alice.id()));
        assertThrows(NotFoundException.class, () -> service.delete(alice.id()));
        SavedWeeklyMealPlan bob = service.save(request);
        assertEquals(List.of(bob), service.findAll());

        UserContext.set("user-a", "test-token-a");
        assertEquals(List.of(alice), service.findAll());
        assertThrows(NotFoundException.class, () -> service.findById(bob.id()));
    }

    @Test
    void rejectsMalformedIdsInsteadOfUsingThemAsFirebasePaths() {
        for (String id : List.of("", "missing", "..", "a/b", "a.json", "1-1-1-1-1", "12345678-1234-1234-1234-123456789xyz")) {
            assertThrows(IllegalArgumentException.class, () -> service.findById(id));
            assertThrows(IllegalArgumentException.class, () -> service.delete(id));
        }
        assertThrows(IllegalArgumentException.class, () -> service.findById(null));
        assertThrows(IllegalArgumentException.class, () -> service.delete(null));
    }
}
