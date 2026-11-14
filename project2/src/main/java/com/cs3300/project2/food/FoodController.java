package com.cs3300.project2.food;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/foods")
public class FoodController {
    private final FoodService service;
    public FoodController(FoodService service) { this.service = service; }

    @GetMapping("/search")
    public List<FoodSearchResult> search(@RequestParam(required = false) String q) { return service.search(q); }

    @GetMapping("/{foodId}")
    public FoodDetails details(@PathVariable String foodId) { return service.details(foodId); }
}
