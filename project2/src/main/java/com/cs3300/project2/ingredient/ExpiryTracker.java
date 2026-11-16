package com.cs3300.project2.ingredient;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ExpiryTracker {

    static final int SOON_DAYS = 3;

    private final IngredientService ingredients;
    private final Clock clock;

    public ExpiryTracker(IngredientService ingredients, Clock clock) {
        this.ingredients = ingredients;
        this.clock = clock;
    }

    public IngredientExpiry check(Ingredient ingredient) {
        LocalDate expires = ingredient.expirationDate();
        if (expires == null) {
            return new IngredientExpiry(ingredient.id(), ingredient.name(), null, null, ExpiryStatus.NO_DATE);
        }
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(clock), expires);
        return new IngredientExpiry(ingredient.id(), ingredient.name(), expires, daysLeft, statusFor(daysLeft));
    }

    /** Every ingredient, soonest to expire first; ingredients without a date come last. */
    public List<IngredientExpiry> report() {
        return ingredients.findAll().stream()
                .map(this::check)
                .sorted(Comparator.comparing(IngredientExpiry::daysLeft, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    static ExpiryStatus statusFor(long daysLeft) {
        if (daysLeft < 0) {
            return ExpiryStatus.EXPIRED;
        }
        return daysLeft <= SOON_DAYS ? ExpiryStatus.EXPIRING_SOON : ExpiryStatus.FRESH;
    }
}
