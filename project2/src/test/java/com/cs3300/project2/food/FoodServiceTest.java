package com.cs3300.project2.food;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;

class FoodServiceTest {
    private final FoodNutritionProvider provider = mock(FoodNutritionProvider.class);
    private final FoodService service = new FoodService(provider);

    @Test
    void trimsQueriesAndReturnsProviderMatches() {
        List<FoodSearchResult> matches = List.of(new FoodSearchResult("123", "Oats", "Foundation", null, 379.0));
        when(provider.search("oats")).thenReturn(matches);

        assertEquals(matches, service.search("  oats  "));
        verify(provider).search("oats");
    }

    @Test
    void shortQueriesDoNotCallExternalProvider() {
        assertTrue(service.search(" a ").isEmpty());
        verifyNoInteractions(provider);
    }

    @Test
    void rejectsOverlongQueries() {
        assertThrows(IllegalArgumentException.class, () -> service.search("x".repeat(101)));
        verifyNoInteractions(provider);
    }

    @Test
    void delegatesFoodDetailsLookup() {
        FoodDetails details = new FoodDetails("123", "Oats", null, null, null);
        when(provider.findDetails("123")).thenReturn(details);
        assertSame(details, service.details("123"));
    }
}
