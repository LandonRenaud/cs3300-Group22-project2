package com.cs3300.project2.mealplan;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.cs3300.project2.common.UserContext;

/** Test-only storage with the same user boundary as the Firebase implementation. */
@Repository
@Profile("test")
public class InMemorySavedWeeklyMealPlanRepository implements SavedWeeklyMealPlanRepository {
    private final Map<String, Map<String, SavedWeeklyMealPlan>> users = new ConcurrentHashMap<>();

    private Map<String, SavedWeeklyMealPlan> currentUserPlans() {
        return users.computeIfAbsent(UserContext.requireUid(), ignored -> new ConcurrentHashMap<>());
    }

    @Override
    public List<SavedWeeklyMealPlan> findAll() {
        return List.copyOf(currentUserPlans().values());
    }

    @Override
    public Optional<SavedWeeklyMealPlan> findById(String id) {
        return Optional.ofNullable(currentUserPlans().get(SavedWeeklyMealPlan.requireId(id)));
    }

    @Override
    public SavedWeeklyMealPlan save(SavedWeeklyMealPlan plan) {
        currentUserPlans().put(SavedWeeklyMealPlan.requireId(plan.id()), plan);
        return plan;
    }

    @Override
    public boolean deleteById(String id) {
        return currentUserPlans().remove(SavedWeeklyMealPlan.requireId(id)) != null;
    }
}
