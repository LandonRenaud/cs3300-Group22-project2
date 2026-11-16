package com.cs3300.project2.ingredient;

import java.time.LocalDate;

/** How close an ingredient is to expiring. {@code daysLeft} is negative once expired and null when no date is set. */
public record IngredientExpiry(
        String id,
        String name,
        LocalDate expirationDate,
        Long daysLeft,
        ExpiryStatus status) {
}
