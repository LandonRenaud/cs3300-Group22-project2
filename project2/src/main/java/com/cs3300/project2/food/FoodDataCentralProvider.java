package com.cs3300.project2.food;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Server-side USDA API adapter. Spring's default singleton scope shares the client per app instance. */
@Component
public class FoodDataCentralProvider implements FoodNutritionProvider {
    private static final String API = "https://api.nal.usda.gov/fdc/v1";
    private final String apiKey;
    private final JsonMapper json;
    private final FoodDataCentralMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();

    public FoodDataCentralProvider(@Value("${fdc.api.key:}") String apiKey, JsonMapper json,
                                   FoodDataCentralMapper mapper) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.json = json;
        this.mapper = mapper;
    }

    @Override
    public List<FoodSearchResult> search(String query) {
        String url = API + "/foods/search?api_key=" + encodedKey() + "&query="
                + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&pageSize=10";
        return mapper.toSearchResults(get(url));
    }

    @Override
    public FoodDetails findDetails(String foodId) {
        if (foodId == null || !foodId.matches("\\d+")) {
            throw new IllegalArgumentException("A valid USDA food ID is required");
        }
        return mapper.toDetails(get(API + "/food/" + foodId + "?api_key=" + encodedKey()), foodId);
    }

    private JsonNode get(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("Accept", "application/json")
                .header("User-Agent", "MealPrepPlanner/1.0").GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) throw new FoodDataException("USDA search limit reached. Please wait and try again.");
            if (response.statusCode() >= 400) {
                throw new FoodDataException("USDA food data is temporarily unavailable (HTTP " + response.statusCode() + ")");
            }
            return json.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FoodDataException("USDA food lookup was interrupted", ex);
        } catch (IOException ex) {
            throw new FoodDataException("Could not reach USDA FoodData Central", ex);
        }
    }

    private String encodedKey() {
        if (apiKey.isBlank()) throw new FoodDataException("Food search is not configured. Set FDC_API_KEY on the server.");
        return URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
    }
}
