package com.cs3300.project2.meal;

import java.util.List;

/** JSON body for creating or updating a meal. Notes and ingredient units are optional. */
public record MealRequest(String name, String notes, List<Item> ingredients) {

    public record Item(String name, Double quantity, String unit) {
    }
}
