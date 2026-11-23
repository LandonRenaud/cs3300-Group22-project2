package com.cs3300.project2.mealplan;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.cs3300.project2.common.NotFoundException;

@Service
public class SavedWeeklyMealPlanService {
    private final SavedWeeklyMealPlanRepository repository;
    private final SavedWeeklyMealPlanFactory factory;
    private final WeeklyMealPlanService planner;

    public SavedWeeklyMealPlanService(SavedWeeklyMealPlanRepository repository,
            SavedWeeklyMealPlanFactory factory, WeeklyMealPlanService planner) {
        this.repository = repository;
        this.factory = factory;
        this.planner = planner;
    }

    public List<SavedWeeklyMealPlan> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(SavedWeeklyMealPlan::savedAt).reversed()
                        .thenComparing(SavedWeeklyMealPlan::id))
                .toList();
    }

    public SavedWeeklyMealPlan findById(String id) {
        return repository.findById(SavedWeeklyMealPlan.requireId(id))
                .orElseThrow(() -> new NotFoundException("Saved weekly plan " + id + " not found"));
    }

    public SavedWeeklyMealPlan save(SaveWeeklyMealPlanRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("A start date and planning options are required");
        }
        PlanningStrategy strategy = request.strategy() == null ? PlanningStrategy.EXPIRY_FIRST : request.strategy();
        WeeklyMealPlan plan = planner.generate(request.startDate(), strategy);
        return repository.save(factory.create(plan));
    }

    public void delete(String id) {
        if (!repository.deleteById(SavedWeeklyMealPlan.requireId(id))) {
            throw new NotFoundException("Saved weekly plan " + id + " not found");
        }
    }
}
