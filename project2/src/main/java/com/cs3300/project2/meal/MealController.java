package com.cs3300.project2.meal;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meals")
public class MealController {

    private final MealService service;

    public MealController(MealService service) {
        this.service = service;
    }

    @GetMapping
    public List<Meal> list(@RequestParam(name = "q", required = false) String query) {
        return service.findAll(query);
    }

    @GetMapping("/{id}")
    public Meal get(@PathVariable String id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/shopping-list")
    public List<ShoppingListItem> shoppingList(@PathVariable String id) {
        return service.shoppingList(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Meal create(@RequestBody MealRequest request) {
        return service.add(request);
    }

    @PutMapping("/{id}")
    public Meal update(@PathVariable String id, @RequestBody MealRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
