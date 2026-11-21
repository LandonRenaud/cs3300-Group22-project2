package com.cs3300.project2.meal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

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

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(MealControllerTest.FixedClockConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MealControllerTest {

    @TestConfiguration
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock shoppingListClock() {
            return Clock.fixed(Instant.parse("2026-11-22T02:00:00Z"), ZoneId.of("America/New_York"));
        }
    }

    private static final String FRIED_RICE = """
            {"name": "Fried Rice", "notes": "day-old rice works best",
             "ingredients": [
               {"name": "Rice", "quantity": 2, "unit": "cup"},
               {"name": "Eggs", "quantity": 3}
             ]}
            """;

    @Autowired
    private MockMvc mvc;

    private String createMeal() throws Exception {
        String body = mvc.perform(post("/api/meals").contentType(MediaType.APPLICATION_JSON).content(FRIED_RICE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Fried Rice"))
                .andExpect(jsonPath("$.ingredients", hasSize(2)))
                .andExpect(jsonPath("$.ingredients[1].unit").value("item"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void createListSearchAndGet() throws Exception {
        String id = createMeal();

        mvc.perform(get("/api/meals")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/meals").param("q", "egg")).andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/meals").param("q", "pasta")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/meals/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("day-old rice works best"));
    }

    @Test
    void shoppingListUsesPantry() throws Exception {
        String id = createMeal();
        mvc.perform(post("/api/ingredients").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"eggs\", \"quantity\": 1}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/meals/" + id + "/shopping-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rice"))
                .andExpect(jsonPath("$[0].toBuy").value(2.0))
                .andExpect(jsonPath("$[1].onHand").value(1.0))
                .andExpect(jsonPath("$[1].toBuy").value(2.0));
    }

    @Test
    void shoppingListUsesTodayOrTheRequestedCookingDateWithoutChangingInventory() throws Exception {
        String id = createMeal();
        String ingredient = mvc.perform(post("/api/ingredients").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Eggs","quantity":2,"expirationDate":"2026-11-21"}
                                """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String ingredientId = JsonPath.read(ingredient, "$.id");
        mvc.perform(post("/api/ingredients").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Eggs","quantity":10,"expirationDate":"2026-11-20"}
                                """))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/ingredients").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Eggs\",\"quantity\":1}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/meals/" + id + "/shopping-list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].onHand").value(3.0))
                .andExpect(jsonPath("$[1].toBuy").value(0.0));
        mvc.perform(get("/api/meals/" + id + "/shopping-list").param("onDate", "2026-11-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].onHand").value(1.0))
                .andExpect(jsonPath("$[1].toBuy").value(2.0));
        mvc.perform(get("/api/ingredients/" + ingredientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(2.0));
    }

    @Test
    void shoppingListRejectsInvalidCookingDatesAndUnknownMeals() throws Exception {
        String id = createMeal();
        for (String date : List.of("not-a-date", "2026-02-30", "11/21/2026", "0000-01-01", "+10000-01-01")) {
            mvc.perform(get("/api/meals/" + id + "/shopping-list").param("onDate", date))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(get("/api/meals/missing/shopping-list").param("onDate", "2026-11-21"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shoppingListRejectsOverflowingPantryTotals() throws Exception {
        String id = createMeal();
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/ingredients").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"name\":\"Eggs\",\"quantity\":1.7976931348623157e308}"))
                    .andExpect(status().isCreated());
        }

        mvc.perform(get("/api/meals/" + id + "/shopping-list"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAndDelete() throws Exception {
        String id = createMeal();

        mvc.perform(put("/api/meals/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Toast\", \"ingredients\": [{\"name\": \"Bread\", \"quantity\": 2}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.notes").value(""));

        mvc.perform(delete("/api/meals/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/meals/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void mealWithoutIngredientsIsRejected() throws Exception {
        mvc.perform(post("/api/meals").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Air\", \"ingredients\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("A meal needs at least one ingredient"));
    }
}
