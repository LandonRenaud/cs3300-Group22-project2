package com.cs3300.project2.ingredient;

import com.cs3300.project2.common.RealtimeDatabaseDocuments;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!test")
public class RealtimeDatabaseIngredientRepository implements IngredientRepository {
    private final RealtimeDatabaseDocuments documents;
    public RealtimeDatabaseIngredientRepository(RealtimeDatabaseDocuments documents) { this.documents = documents; }

    @Override
    public List<Ingredient> findAll() {
        return documents.list("ingredients", Ingredient.class).stream()
                .sorted(Comparator.comparing(Ingredient::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    @Override
    public Optional<Ingredient> findById(String id) {
        return documents.find("ingredients", id, Ingredient.class);
    }

    @Override
    public Ingredient save(Ingredient ingredient) {
        return documents.save("ingredients", ingredient.id(), ingredient, Ingredient.class);
    }

    @Override
    public boolean deleteById(String id) {
        return documents.delete("ingredients", id);
    }
}
