package com.cs3300.project2.food;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

class FoodDataCentralMapperTest {
    private final FoodDataCentralMapper mapper = new FoodDataCentralMapper();
    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void mapsSearchResultsIntoCompactSuggestions() throws Exception {
        var root = json.readTree("""
                {"foods":[{"fdcId":123,"description":"Oats, rolled","dataType":"Foundation",
                  "foodNutrients":[{"nutrientName":"Energy","value":379,"unitName":"kcal"}]}]}
                """);

        var results = mapper.toSearchResults(root);

        assertEquals(1, results.size());
        assertEquals("123", results.getFirst().foodId());
        assertEquals(379.0, results.getFirst().caloriesPer100g(), 1e-9);
    }

    @Test
    void mapsAllReturnedNutrientsAndRecordsPerHundredGramBasis() throws Exception {
        var root = json.readTree("""
                {"description":"Oats, rolled","foodNutrients":[
                  {"nutrient":{"id":1008,"name":"Energy","unitName":"kcal"},"amount":379},
                  {"nutrient":{"id":1003,"name":"Protein","unitName":"g"},"amount":13.2},
                  {"nutrient":{"id":1079,"name":"Fiber, total dietary","unitName":"g"},"amount":10.1}]}
                """);

        FoodDetails details = mapper.toDetails(root, "123");

        assertEquals(379, details.nutritionPerUnit().calories(), 1e-9);
        assertEquals(13.2, details.nutritionPerUnit().proteinGrams(), 1e-9);
        assertEquals(10.1, details.nutritionPerUnit().additionalNutrients().get("usda_1079").amount());
        assertEquals("g", details.nutritionPerUnit().additionalNutrients().get("usda_1079").unit());
        assertEquals(new com.cs3300.project2.ingredient.NutritionBasis(100, "g", "per 100 g"), details.nutritionBasis());
        assertEquals("USDA_FDC", details.nutritionSource().provider());
    }
}
