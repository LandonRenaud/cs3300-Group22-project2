package com.cs3300.project2.meal;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class MealControllerTest {

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
