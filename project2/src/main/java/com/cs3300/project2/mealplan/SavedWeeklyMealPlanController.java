package com.cs3300.project2.mealplan;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meal-plans/saved")
public class SavedWeeklyMealPlanController {
    private final SavedWeeklyMealPlanService service;

    public SavedWeeklyMealPlanController(SavedWeeklyMealPlanService service) {
        this.service = service;
    }

    @GetMapping
    public List<SavedWeeklyMealPlan> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public SavedWeeklyMealPlan get(@PathVariable String id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavedWeeklyMealPlan save(@RequestBody SaveWeeklyMealPlanRequest request) {
        return service.save(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
