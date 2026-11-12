package com.cs3300.project2.ingredient;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cs3300.project2.common.NotFoundException;

@Service
public class IngredientService {

    private final IngredientRepository repository;
    private final IngredientFactory factory;

    public IngredientService(IngredientRepository repository, IngredientFactory factory) {
        this.repository = repository;
        this.factory = factory;
    }

    public List<Ingredient> findAll() {
        return repository.findAll();
    }

    public Ingredient findById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ingredient " + id + " not found"));
    }

    public Ingredient add(IngredientRequest request) {
        return repository.save(factory.create(request));
    }

    public Ingredient update(String id, IngredientRequest request) {
        findById(id);
        return repository.save(factory.rebuild(id, request));
    }

    /**
     * Adjusts the quantity on hand, e.g. +2 after buying more or -1 after using one.
     * The result is never allowed to drop below zero.
     */
    public Ingredient adjustQuantity(String id, double delta) {
        if (Double.isNaN(delta) || Double.isInfinite(delta)) {
            throw new IllegalArgumentException("Quantity change must be a number");
        }
        Ingredient existing = findById(id);
        double newQuantity = existing.quantity() + delta;
        if (newQuantity < 0) {
            throw new IllegalArgumentException(
                    "Only " + existing.quantity() + " " + existing.unit() + " of " + existing.name() + " on hand");
        }
        return repository.save(existing.withQuantity(newQuantity));
    }

    public void delete(String id) {
        if (!repository.deleteById(id)) {
            throw new NotFoundException("Ingredient " + id + " not found");
        }
    }

    /** Combined nutrition of everything currently on hand. */
    public NutritionInfo totalNutrition() {
        return repository.findAll().stream()
                .map(Ingredient::totalNutrition)
                .reduce(NutritionInfo.EMPTY, NutritionInfo::plus);
    }
}
