package com.cs3300.project2.food;

import com.cs3300.project2.ingredient.NutrientValue;
import com.cs3300.project2.ingredient.NutritionBasis;
import com.cs3300.project2.ingredient.NutritionInfo;
import com.cs3300.project2.ingredient.NutritionSource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Converts USDA response JSON into stable application DTOs and ingredient nutrition values. */
@Component
public class FoodDataCentralMapper {
    public List<FoodSearchResult> toSearchResults(JsonNode root) {
        List<FoodSearchResult> results = new ArrayList<>();
        JsonNode foods = root.path("foods");
        if (foods.isArray()) {
            for (JsonNode food : foods) {
                String id = text(food, "fdcId");
                if (id != null) results.add(new FoodSearchResult(id, text(food, "description"),
                        text(food, "dataType"), text(food, "brandOwner"), calorieAmount(food.path("foodNutrients"))));
            }
        }
        return List.copyOf(results);
    }

    public FoodDetails toDetails(JsonNode food, String foodId) {
        Map<String, NutrientValue> extra = new LinkedHashMap<>();
        double calories = 0, protein = 0, carbs = 0, fat = 0;
        JsonNode nutrients = food.path("foodNutrients");
        if (nutrients.isArray()) {
            for (JsonNode entry : nutrients) {
                JsonNode nutrient = entry.path("nutrient");
                String name = text(nutrient, "name");
                if (name == null) name = text(entry, "nutrientName");
                if (name == null) continue;
                double amount = number(entry, "amount", number(entry, "value", Double.NaN));
                if (!Double.isFinite(amount) || amount < 0) continue;
                String unit = text(nutrient, "unitName");
                if (unit == null) unit = text(entry, "unitName");
                if (unit == null) unit = "g";
                String id = text(nutrient, "id");
                if (id == null) id = text(entry, "nutrientId");
                String key = id == null ? slug(name) : "usda_" + id;
                String normalized = name.toLowerCase(Locale.ROOT);
                boolean standard = false;
                if (normalized.equals("protein")) { protein = amount; standard = true; }
                else if (normalized.contains("carbohydrate")) { carbs = amount; standard = true; }
                else if (normalized.equals("total lipid (fat)") || normalized.equals("total lipid")) { fat = amount; standard = true; }
                else if (normalized.equals("energy") && unit.toLowerCase(Locale.ROOT).contains("kcal")) { calories = amount; standard = true; }
                else if (normalized.equals("energy") && unit.toLowerCase(Locale.ROOT).contains("kj")) { calories = amount / 4.184; standard = true; }
                if (!standard) extra.put(key, new NutrientValue(name, amount, unit));
            }
        }
        String description = text(food, "description");
        return new FoodDetails(foodId, description, new NutritionInfo(calories, protein, carbs, fat, extra),
                new NutritionBasis(100, "g", "per 100 g"),
                new NutritionSource("USDA_FDC", foodId, description, Instant.now().toString()));
    }

    private static Double calorieAmount(JsonNode nutrients) {
        if (!nutrients.isArray()) return null;
        for (JsonNode node : nutrients) {
            String name = text(node, "nutrientName");
            if (name == null) name = text(node.path("nutrient"), "name");
            if (name != null && name.toLowerCase(Locale.ROOT).equals("energy")) {
                double value = number(node, "value", number(node, "amount", Double.NaN));
                String unit = text(node, "unitName");
                return unit != null && unit.equalsIgnoreCase("kJ") ? value / 4.184 : value;
            }
        }
        return null;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }

    private static double number(JsonNode node, String field, double fallback) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asDouble() : fallback;
    }

    private static String slug(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }
}
