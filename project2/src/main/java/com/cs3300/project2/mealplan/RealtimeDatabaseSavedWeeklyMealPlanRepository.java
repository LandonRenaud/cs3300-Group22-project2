package com.cs3300.project2.mealplan;

import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import com.cs3300.project2.common.RealtimeDatabaseDocuments;

@Repository
@Profile("!test")
public class RealtimeDatabaseSavedWeeklyMealPlanRepository implements SavedWeeklyMealPlanRepository {
    private static final String COLLECTION = "savedMealPlans";
    private final RealtimeDatabaseDocuments documents;

    public RealtimeDatabaseSavedWeeklyMealPlanRepository(RealtimeDatabaseDocuments documents) {
        this.documents = documents;
    }

    @Override
    public List<SavedWeeklyMealPlan> findAll() {
        return documents.list(COLLECTION, SavedWeeklyMealPlan.class);
    }

    @Override
    public Optional<SavedWeeklyMealPlan> findById(String id) {
        return documents.find(COLLECTION, SavedWeeklyMealPlan.requireId(id), SavedWeeklyMealPlan.class);
    }

    @Override
    public SavedWeeklyMealPlan save(SavedWeeklyMealPlan plan) {
        return documents.save(COLLECTION, SavedWeeklyMealPlan.requireId(plan.id()), plan, SavedWeeklyMealPlan.class);
    }

    @Override
    public boolean deleteById(String id) {
        return documents.delete(COLLECTION, SavedWeeklyMealPlan.requireId(id));
    }
}
