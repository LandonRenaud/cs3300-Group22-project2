package com.cs3300.project2.mealplan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.cs3300.project2.common.FirebaseSettings;
import com.cs3300.project2.common.RealtimeDatabaseDocuments;
import com.cs3300.project2.common.UserContext;
import com.cs3300.project2.meal.Meal;
import com.cs3300.project2.meal.MealIngredient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@ActiveProfiles("test")
class RealtimeDatabaseSavedWeeklyMealPlanRepositoryTest {
    private static final String PLAN_ID = "11111111-1111-4111-8111-111111111111";
    private static final String SHARED_PLAN_ID = "22222222-2222-4222-8222-222222222222";

    @Autowired
    private JsonMapper json;

    private HttpClient http;
    private final Map<String, String> database = new LinkedHashMap<>();
    private final List<HttpRequest> requests = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        UserContext.clear();
        http = mock(HttpClient.class);
        when(http.send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any()))
                .thenAnswer(call -> respond(call.getArgument(0)));
    }

    @AfterEach
    void clearIdentity() {
        UserContext.clear();
    }

    @Test
    void roundTripsTheFullSnapshotThroughJsonAndANewRepositoryInstance() {
        UserContext.set("user-a", "token-a+value");
        SavedWeeklyMealPlan saved = snapshot(PLAN_ID, "Breakfast \"special\"");

        assertEquals(saved, repository().save(saved));

        HttpRequest put = requests.getFirst();
        assertEquals("PUT", put.method());
        assertEquals("https", put.uri().getScheme());
        assertEquals("meal-plans.example", put.uri().getHost());
        assertEquals("/users/user-a/savedMealPlans/" + PLAN_ID + ".json", put.uri().getRawPath());
        assertEquals("auth=token-a%2Bvalue", put.uri().getRawQuery());
        assertEquals("application/json", put.headers().firstValue("Content-Type").orElseThrow());
        JsonNode payload = json.readTree(database.get(put.uri().getRawPath()));
        assertEquals(PLAN_ID, payload.get("id").asText());
        assertEquals("2026-11-21T23:00:00Z", payload.get("savedAt").asText());
        assertEquals("2026-11-23", payload.at("/plan/startDate").asText());
        assertEquals("EXPIRY_FIRST", payload.at("/plan/strategy").asText());
        assertEquals(7, payload.at("/plan/days").size());
        assertEquals("2026-11-29", payload.at("/plan/days/6/date").asText());
        assertEquals("Breakfast \"special\"", payload.at("/plan/days/0/meal/name").asText());
        assertEquals("Eggs", payload.at("/plan/days/0/meal/ingredients/0/name").asText());
        assertEquals(1.0, payload.at("/plan/days/0/ingredients/0/fromPantry").asDouble());
        assertEquals(13.0, payload.at("/plan/shoppingList/0/toBuy").asDouble());

        RealtimeDatabaseSavedWeeklyMealPlanRepository reopened = repository();
        assertEquals(saved, reopened.findById(saved.id()).orElseThrow());
        assertEquals(List.of(saved), reopened.findAll());
    }

    @Test
    void isolatesReadsWritesAndDeletesForUsersWithTheSamePlanId() {
        SavedWeeklyMealPlan first = snapshot(SHARED_PLAN_ID, "User A breakfast");
        SavedWeeklyMealPlan second = snapshot(SHARED_PLAN_ID, "User B breakfast");
        RealtimeDatabaseSavedWeeklyMealPlanRepository repository = repository();
        UserContext.set("user-a", "token-a+value");
        repository.save(first);

        UserContext.set("user-b", "token-b/value");
        int firstUserBRequest = requests.size();
        assertTrue(repository.findById(first.id()).isEmpty());
        assertTrue(repository.findAll().isEmpty());
        assertFalse(repository.deleteById(first.id()));
        repository.save(second);
        assertEquals(List.of(second), repository.findAll());
        for (HttpRequest request : requests.subList(firstUserBRequest, requests.size())) {
            assertTrue(request.uri().getRawPath().startsWith("/users/user-b/savedMealPlans"));
            assertEquals("auth=token-b%2Fvalue", request.uri().getRawQuery());
        }

        UserContext.set("user-a", "token-a+value");
        assertEquals(first, repository.findById(first.id()).orElseThrow());
        assertEquals(List.of(first), repository.findAll());
        assertTrue(repository.deleteById(first.id()));
        assertTrue(repository.findById(first.id()).isEmpty());
        assertTrue(repository.findAll().isEmpty());
        List<HttpRequest> deletes = requests.stream().filter(request -> request.method().equals("DELETE")).toList();
        assertEquals(1, deletes.size());
        assertEquals("/users/user-a/savedMealPlans/" + SHARED_PLAN_ID + ".json", deletes.getFirst().uri().getRawPath());
        assertEquals("auth=token-a%2Bvalue", deletes.getFirst().uri().getRawQuery());

        UserContext.set("user-b", "token-b/value");
        assertEquals(second, repository().findById(second.id()).orElseThrow());
        assertEquals(List.of(second), repository().findAll());
    }

    @Test
    void requiresAnAuthenticatedIdentityBeforeAccessingStorage() {
        RealtimeDatabaseSavedWeeklyMealPlanRepository repository = repository();
        assertThrows(IllegalStateException.class, repository::findAll);
        assertThrows(IllegalStateException.class, () -> repository.findById(PLAN_ID));
        assertThrows(IllegalStateException.class, () -> repository.save(snapshot(PLAN_ID, "Breakfast")));
        assertThrows(IllegalStateException.class, () -> repository.deleteById(PLAN_ID));
        verifyNoInteractions(http);
    }

    private RealtimeDatabaseSavedWeeklyMealPlanRepository repository() {
        RealtimeDatabaseDocuments documents = new RealtimeDatabaseDocuments(
                new FirebaseSettings("unused-test-key", "https://meal-plans.example/"), json, http);
        return new RealtimeDatabaseSavedWeeklyMealPlanRepository(documents);
    }

    private SavedWeeklyMealPlan snapshot(String id, String mealName) {
        LocalDate start = LocalDate.of(2026, 11, 23);
        Meal meal = new Meal("meal-1", mealName, "Use fresh eggs", List.of(new MealIngredient("Eggs", 2, "item")));
        List<WeeklyMealPlan.Day> days = IntStream.range(0, 7).mapToObj(offset -> {
            double fromPantry = offset == 0 ? 1 : 0;
            return new WeeklyMealPlan.Day(start.plusDays(offset), meal,
                    List.of(new WeeklyMealPlan.IngredientNeed("Eggs", "item", 2, fromPantry, 2 - fromPantry)));
        }).toList();
        WeeklyMealPlan plan = new MealPlanFactory().create(start, PlanningStrategy.EXPIRY_FIRST, days);
        return new SavedWeeklyMealPlan(id, Instant.parse("2026-11-21T23:00:00Z"), plan);
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> respond(HttpRequest request) {
        requests.add(request);
        String path = request.uri().getRawPath();
        String body;
        if (request.method().equals("PUT")) {
            HttpResponse.BodySubscriber<String> subscriber = HttpResponse.BodySubscribers.ofString(StandardCharsets.UTF_8);
            request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    subscriber.onSubscribe(subscription);
                }

                @Override
                public void onNext(ByteBuffer buffer) {
                    subscriber.onNext(List.of(buffer));
                }

                @Override
                public void onError(Throwable error) {
                    subscriber.onError(error);
                }

                @Override
                public void onComplete() {
                    subscriber.onComplete();
                }
            });
            body = subscriber.getBody().toCompletableFuture().join();
            database.put(path, body);
        } else if (request.method().equals("DELETE")) {
            database.remove(path);
            body = "null";
        } else if (path.endsWith("/savedMealPlans.json")) {
            String prefix = path.substring(0, path.length() - ".json".length()) + "/";
            Map<String, JsonNode> collection = new LinkedHashMap<>();
            database.forEach((key, value) -> {
                if (key.startsWith(prefix)) {
                    collection.put(key.substring(prefix.length(), key.length() - ".json".length()), json.readTree(value));
                }
            });
            body = json.writeValueAsString(collection);
        } else {
            body = database.getOrDefault(path, "null");
        }
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        return response;
    }
}
