package com.cs3300.project2.food;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FoodService {
    private final FoodNutritionProvider provider;

    public FoodService(FoodNutritionProvider provider) { this.provider = provider; }

    public List<FoodSearchResult> search(String query) {
        if (query == null || query.isBlank()) return List.of();
        String normalized = query.trim();
        if (normalized.length() < 2) return List.of();
        if (normalized.length() > 100) throw new IllegalArgumentException("Search text must be 100 characters or fewer");
        return provider.search(normalized);
    }

    public FoodDetails details(String foodId) { return provider.findDetails(foodId); }
}
