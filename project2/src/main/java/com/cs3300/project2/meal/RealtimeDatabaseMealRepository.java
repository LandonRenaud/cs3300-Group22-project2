package com.cs3300.project2.meal;

import com.cs3300.project2.common.RealtimeDatabaseDocuments;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!test")
public class RealtimeDatabaseMealRepository implements MealRepository {
    private final RealtimeDatabaseDocuments documents;

    public RealtimeDatabaseMealRepository(RealtimeDatabaseDocuments documents) {
        this.documents = documents;
    }

    @Override
    public List<Meal> findAll() {
        return documents.list("meals", Meal.class).stream()
                .sorted(Comparator.comparing(Meal::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Override
    public Optional<Meal> findById(String id) {
        return documents.find("meals", id, Meal.class);
    }

    @Override
    public Meal save(Meal meal) {
        return documents.save("meals", meal.id(), meal, Meal.class);
    }

    @Override
    public boolean deleteById(String id) {
        return documents.delete("meals", id);
    }
}
