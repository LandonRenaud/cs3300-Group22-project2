package com.cs3300.project2.ingredient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExpiryTrackerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    private IngredientService pantry;
    private ExpiryTracker tracker;

    @BeforeEach
    void setUp() {
        pantry = new IngredientService(new InMemoryIngredientRepository(), new IngredientFactory());
        Clock clock = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        tracker = new ExpiryTracker(pantry, clock);
    }

    private Ingredient add(String name, LocalDate expires) {
        return pantry.add(new IngredientRequest(name, 1.0, null, null, expires));
    }

    @Test
    void pastDateIsExpired() {
        IngredientExpiry result = tracker.check(add("Milk", TODAY.minusDays(2)));

        assertEquals(-2L, result.daysLeft());
        assertEquals(ExpiryStatus.EXPIRED, result.status());
    }

    @Test
    void todayAndUpToThreeDaysAreExpiringSoon() {
        assertEquals(ExpiryStatus.EXPIRING_SOON, tracker.check(add("Bread", TODAY)).status());
        assertEquals(ExpiryStatus.EXPIRING_SOON,
                tracker.check(add("Yogurt", TODAY.plusDays(ExpiryTracker.SOON_DAYS))).status());
    }

    @Test
    void laterDateIsFresh() {
        IngredientExpiry result = tracker.check(add("Rice", TODAY.plusDays(ExpiryTracker.SOON_DAYS + 1)));

        assertEquals(4L, result.daysLeft());
        assertEquals(ExpiryStatus.FRESH, result.status());
    }

    @Test
    void missingDateHasNoStatus() {
        IngredientExpiry result = tracker.check(add("Salt", null));

        assertNull(result.daysLeft());
        assertEquals(ExpiryStatus.NO_DATE, result.status());
    }

    @Test
    void reportIsSortedSoonestFirstWithUndatedLast() {
        add("Salt", null);
        add("Rice", TODAY.plusDays(30));
        add("Milk", TODAY.minusDays(1));
        add("Eggs", TODAY.plusDays(2));

        List<String> names = tracker.report().stream().map(IngredientExpiry::name).toList();

        assertEquals(List.of("Milk", "Eggs", "Rice", "Salt"), names);
    }

    @Test
    void emptyPantryGivesEmptyReport() {
        assertTrue(tracker.report().isEmpty());
    }
}
