package com.cs3300.project2.ingredient;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingredients")
public class IngredientController {

    private final IngredientService service;

    public IngredientController(IngredientService service) {
        this.service = service;
    }

    @GetMapping
    public List<Ingredient> list() {
        return service.findAll();
    }

    @GetMapping("/nutrition")
    public NutritionInfo totalNutrition() {
        return service.totalNutrition();
    }

    @GetMapping("/{id}")
    public Ingredient get(@PathVariable String id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Ingredient create(@RequestBody IngredientRequest request) {
        return service.add(request);
    }

    @PutMapping("/{id}")
    public Ingredient update(@PathVariable String id, @RequestBody IngredientRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/quantity")
    public Ingredient adjustQuantity(@PathVariable String id, @RequestBody QuantityChange change) {
        if (change == null || change.delta() == null) {
            throw new IllegalArgumentException("delta is required");
        }
        return service.adjustQuantity(id, change.delta());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }

    /** Body for {@code PATCH /api/ingredients/{id}/quantity}, e.g. {@code {"delta": -1}}. */
    public record QuantityChange(Double delta) {
    }
}
