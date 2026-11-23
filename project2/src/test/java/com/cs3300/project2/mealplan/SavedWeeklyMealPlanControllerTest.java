package com.cs3300.project2.mealplan;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.cs3300.project2.common.UserContext;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(SavedWeeklyMealPlanControllerTest.FixedClockConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SavedWeeklyMealPlanControllerTest {
    private static final String SAVED = "/api/meal-plans/saved";
    private static final String SAVE_REQUEST = "{\"startDate\":\"2026-11-28\"}";

    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock savedPlanClock() {
            return Clock.fixed(Instant.parse("2026-11-21T22:00:00Z"), ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void setUser() {
        UserContext.set("user-a", "test-token-a");
    }

    @AfterEach
    void clearUser() {
        UserContext.clear();
    }

    private String addRecipe() throws Exception {
        String body = mvc.perform(post("/api/meals").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Omelet","notes":"Whisk first","ingredients":[{"name":"Eggs","quantity":2}]}
                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String addPantry() throws Exception {
        String body = mvc.perform(post("/api/ingredients").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Eggs\",\"quantity\":3}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String save() throws Exception {
        return mvc.perform(post(SAVED).contentType(MediaType.APPLICATION_JSON).content(SAVE_REQUEST))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void createListGetAndDeleteSavedPlan() throws Exception {
        addRecipe();
        String ingredientId = addPantry();
        mvc.perform(get(SAVED)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        String saved = mvc.perform(post(SAVED).contentType(MediaType.APPLICATION_JSON).content(SAVE_REQUEST))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.savedAt").value("2026-11-21T22:00:00Z"))
                .andExpect(jsonPath("$.plan.startDate").value("2026-11-28"))
                .andExpect(jsonPath("$.plan.strategy").value("EXPIRY_FIRST"))
                .andExpect(jsonPath("$.plan.days", hasSize(7)))
                .andExpect(jsonPath("$.plan.days[0].meal.name").value("Omelet"))
                .andExpect(jsonPath("$.plan.shoppingList[0].toBuy").value(11.0))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(saved, "$.id");
        assertEquals(id, UUID.fromString(id).toString());

        mvc.perform(get(SAVED)).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id))
                .andExpect(jsonPath("$[0].plan.days", hasSize(7)));
        String reopened = mvc.perform(get(SAVED + "/" + id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(saved, reopened);
        mvc.perform(get("/api/ingredients/" + ingredientId)).andExpect(jsonPath("$.quantity").value(3.0));
        mvc.perform(delete(SAVED + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(SAVED)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(SAVED + "/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete(SAVED + "/" + id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/meals")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/ingredients/" + ingredientId)).andExpect(jsonPath("$.quantity").value(3.0));
    }

    @Test
    void savedSnapshotSurvivesPantryAndRecipeUpdatesAndDeletion() throws Exception {
        String recipeId = addRecipe();
        String ingredientId = addPantry();
        String saved = save();
        String id = JsonPath.read(saved, "$.id");

        mvc.perform(put("/api/meals/" + recipeId).contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Updated recipe","ingredients":[{"name":"Eggs","quantity":5}]}
                """))
                .andExpect(status().isOk());
        mvc.perform(patch("/api/ingredients/" + ingredientId + "/quantity").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":5}"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/meals/" + recipeId)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/ingredients/" + ingredientId)).andExpect(status().isNoContent());

        String reopened = mvc.perform(get(SAVED + "/" + id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertEquals(saved, reopened);
        mvc.perform(post(SAVED).contentType(MediaType.APPLICATION_JSON).content(SAVE_REQUEST))
                .andExpect(status().isBadRequest());
        mvc.perform(get(SAVED)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void repeatedSavesAreIndependentAndUseCurrentDataAndRequestedStrategy() throws Exception {
        addRecipe();
        String ingredientId = addPantry();
        String firstId = JsonPath.read(save(), "$.id");
        String duplicateId = JsonPath.read(save(), "$.id");
        assertNotEquals(firstId, duplicateId);
        mvc.perform(patch("/api/ingredients/" + ingredientId + "/quantity").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\":1}"))
                .andExpect(status().isOk());

        mvc.perform(post(SAVED).contentType(MediaType.APPLICATION_JSON).content("""
                {"startDate":"2026-11-28","strategy":"PANTRY_FIRST"}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plan.strategy").value("PANTRY_FIRST"))
                .andExpect(jsonPath("$.plan.shoppingList[0].toBuy").value(10.0));
        mvc.perform(get(SAVED)).andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(delete(SAVED + "/" + duplicateId)).andExpect(status().isNoContent());
        mvc.perform(get(SAVED + "/" + firstId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.plan.shoppingList[0].toBuy").value(11.0));
    }

    @Test
    void rejectsEmptyPlansAndInvalidRequestsWithoutCreatingSnapshots() throws Exception {
        mvc.perform(post(SAVED).contentType(MediaType.APPLICATION_JSON).content(SAVE_REQUEST))
                .andExpect(status().isBadRequest());
        addRecipe();
        for (String request : List.of("null", "{}", "{\"startDate\":\"tomorrow\"}",
                "{\"startDate\":\"2026-02-30\"}", "{\"startDate\":\"0000-01-01\"}",
                "{\"startDate\":\"9999-12-26\"}",
                "{\"startDate\":\"2026-11-28\",\"strategy\":\"UNKNOWN\"}")) {
            mvc.perform(post(SAVED).contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post(SAVED).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        mvc.perform(get(SAVED)).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void anotherUserCannotListOpenOrDeleteSavedPlans() throws Exception {
        addRecipe();
        String aliceId = JsonPath.read(save(), "$.id");
        UserContext.set("user-b", "test-token-b");

        mvc.perform(get(SAVED)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get(SAVED + "/" + aliceId)).andExpect(status().isNotFound());
        mvc.perform(delete(SAVED + "/" + aliceId)).andExpect(status().isNotFound());
        String bobId = JsonPath.read(save(), "$.id");
        mvc.perform(get(SAVED)).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(bobId));

        UserContext.set("user-a", "test-token-a");
        mvc.perform(get(SAVED)).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].id").value(aliceId));
        mvc.perform(get(SAVED + "/" + bobId)).andExpect(status().isNotFound());
    }

    @Test
    void malformedIdsAreBadRequestsAndUnknownUuidIsNotFound() throws Exception {
        for (String id : List.of("invalid", "1-1-1-1-1", "a.json")) {
            mvc.perform(get(SAVED + "/" + id)).andExpect(status().isBadRequest());
            mvc.perform(delete(SAVED + "/" + id)).andExpect(status().isBadRequest());
        }
        String unknown = UUID.randomUUID().toString();
        mvc.perform(get(SAVED + "/" + unknown)).andExpect(status().isNotFound());
        mvc.perform(delete(SAVED + "/" + unknown)).andExpect(status().isNotFound());
    }
}
