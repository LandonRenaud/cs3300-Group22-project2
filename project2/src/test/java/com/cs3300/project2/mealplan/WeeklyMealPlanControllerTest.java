package com.cs3300.project2.mealplan;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class WeeklyMealPlanControllerTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void returnsAnEmptyPlanUntilRecipesExist() throws Exception {
        mvc.perform(get("/api/meal-plans/weekly").param("startDate", "2026-11-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDate").value("2026-11-15"))
                .andExpect(jsonPath("$.strategy").value("EXPIRY_FIRST"))
                .andExpect(jsonPath("$.days", hasSize(0)))
                .andExpect(jsonPath("$.shoppingList", hasSize(0)));
    }

    @Test
    void returnsSevenDaysAndACombinedShortfallWithoutChangingInventory() throws Exception {
        mvc.perform(post("/api/ingredients").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Eggs\",\"quantity\":3}"))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/meals").contentType(MediaType.APPLICATION_JSON).content("""
                {"name":"Omelet","ingredients":[{"name":"Eggs","quantity":2}]}
                """)).andExpect(status().isCreated());

        mvc.perform(get("/api/meal-plans/weekly").param("startDate", "2026-11-15")
                        .param("strategy", "PANTRY_FIRST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("PANTRY_FIRST"))
                .andExpect(jsonPath("$.days", hasSize(7)))
                .andExpect(jsonPath("$.days[0].meal.name").value("Omelet"))
                .andExpect(jsonPath("$.days[0].ingredients[0].fromPantry").value(2.0))
                .andExpect(jsonPath("$.days[6].date").value("2026-11-21"))
                .andExpect(jsonPath("$.shoppingList[0].toBuy").value(11.0));
        mvc.perform(get("/api/ingredients"))
                .andExpect(jsonPath("$[0].quantity").value(3.0));
    }

    @Test
    void rejectsMissingMalformedOrOutOfRangeStartDates() throws Exception {
        mvc.perform(get("/api/meal-plans/weekly")).andExpect(status().isBadRequest());
        for (String date : new String[]{"tomorrow", "2026-02-30", "9999-12-31", "0000-01-01"}) {
            mvc.perform(get("/api/meal-plans/weekly").param("startDate", date))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void rejectsUnknownStrategy() throws Exception {
        mvc.perform(get("/api/meal-plans/weekly").param("startDate", "2026-11-15")
                        .param("strategy", "UNKNOWN"))
                .andExpect(status().isBadRequest());
    }
}
