package com.cs3300.project2.ingredient;

/** Provenance for imported nutrition facts; null for user-entered facts. */
public record NutritionSource(String provider, String foodId, String description, String retrievedAt) { }
