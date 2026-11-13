package com.cs3300.project2.ingredient;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class IngredientControllerTest {

    @Autowired
    private MockMvc mvc;

    private String createEggs() throws Exception {
        String body = mvc.perform(post("/api/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " eggs ", "quantity": 12, "unit": "Item",
                                 "nutritionPerUnit": {"calories": 70, "proteinGrams": 6, "carbsGrams": 0.5, "fatGrams": 5},
                                 "expirationDate": "2026-12-01"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("eggs"))
                .andExpect(jsonPath("$.unit").value("item"))
                .andExpect(jsonPath("$.expirationDate").value("2026-12-01"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void createThenListAndGet() throws Exception {
        String id = createEggs();

        mvc.perform(get("/api/ingredients"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(id));

        mvc.perform(get("/api/ingredients/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(12.0));
    }

    @Test
    void adjustQuantityAndTotalNutrition() throws Exception {
        String id = createEggs();

        mvc.perform(patch("/api/ingredients/" + id + "/quantity")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"delta\": -2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(10.0));

        mvc.perform(get("/api/ingredients/nutrition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calories").value(700.0));
    }

    @Test
    void invalidInputReturnsBadRequestWithDetail() throws Exception {
        mvc.perform(post("/api/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"quantity\": 1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Name is required"));

        mvc.perform(post("/api/ingredients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Milk\", \"quantity\": 1, \"nutritionPerUnit\": {\"calories\": -5}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownIdReturnsNotFound() throws Exception {
        mvc.perform(get("/api/ingredients/nope")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/ingredients/nope")).andExpect(status().isNotFound());
    }

    @Test
    void deleteRemovesIngredient() throws Exception {
        String id = createEggs();

        mvc.perform(delete("/api/ingredients/" + id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/ingredients")).andExpect(jsonPath("$", hasSize(0)));
    }
}
