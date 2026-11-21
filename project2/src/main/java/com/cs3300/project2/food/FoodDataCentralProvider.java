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
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.List;

/** Server-side USDA API adapter. Spring's default singleton scope shares the client per app instance. */
@Component
public class FoodDataCentralProvider implements FoodNutritionProvider {
    private static final String API = "https://api.nal.usda.gov/fdc/v1";
    private final String apiKey;
    private final JsonMapper json;
    private final FoodDataCentralMapper mapper;
    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    private final TimedCache<List<FoodSearchResult>> searchCache = new TimedCache<>(256, Duration.ofHours(1).toMillis());
    private final TimedCache<FoodDetails> detailsCache = new TimedCache<>(512, Duration.ofHours(24).toMillis());

    public FoodDataCentralProvider(@Value("${fdc.api.key:}") String apiKey, JsonMapper json,
                                   FoodDataCentralMapper mapper) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.json = json;
        this.mapper = mapper;
    }

    @Override
    public List<FoodSearchResult> search(String query) {
        String normalized = query.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        List<FoodSearchResult> generic = searchCache.get("generic:" + normalized);
        if (generic == null) {
            generic = searchByTypes(query, List.of("Foundation", "Survey (FNDDS)", "SR Legacy"));
            searchCache.put("generic:" + normalized, generic);
        }
        List<FoodSearchResult> branded = searchCache.get("branded:" + normalized);
        if (branded == null) {
            branded = searchByTypes(query, List.of("Branded"));
            searchCache.put("branded:" + normalized, branded);
        }
        List<FoodSearchResult> results = new ArrayList<>(generic.size() + branded.size());
        results.addAll(generic);
        results.addAll(branded);
        return List.copyOf(results);
    }

    private List<FoodSearchResult> searchByTypes(String query, List<String> dataTypes) {
        String url = API + "/foods/search?api_key=" + encodedKey();
        Map<String, Object> payload = Map.of("query", query, "dataType", dataTypes, "pageSize", 10);
        return mapper.toSearchResults(post(url, payload));
    }

    @Override
    public FoodDetails findDetails(String foodId) {
        if (foodId == null || !foodId.matches("\\d+")) {
            throw new IllegalArgumentException("A valid USDA food ID is required");
        }
        FoodDetails cached = detailsCache.get(foodId);
        if (cached != null) return cached;
        FoodDetails details = mapper.toDetails(get(API + "/food/" + foodId + "?api_key=" + encodedKey()), foodId);
        detailsCache.put(foodId, details);
        return details;
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

    private JsonNode post(String url, Object payload) {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(url))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "MealPrepPlanner/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload)))
                    .build();
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

    private static final class TimedCache<V> {
        private final int maxEntries;
        private final long ttlMillis;
        private final LinkedHashMap<String, Entry<V>> entries = new LinkedHashMap<>(16, 0.75f, true);

        private TimedCache(int maxEntries, long ttlMillis) {
            this.maxEntries = maxEntries;
            this.ttlMillis = ttlMillis;
        }

        private synchronized V get(String key) {
            Entry<V> entry = entries.get(key);
            if (entry == null) return null;
            if (entry.expiresAt <= System.currentTimeMillis()) {
                entries.remove(key);
                return null;
            }
            return entry.value;
        }

        private synchronized void put(String key, V value) {
            long now = System.currentTimeMillis();
            entries.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
            entries.put(key, new Entry<>(value, now + ttlMillis));
            while (entries.size() > maxEntries) entries.remove(entries.keySet().iterator().next());
        }

        private record Entry<V>(V value, long expiresAt) { }
    }
}
