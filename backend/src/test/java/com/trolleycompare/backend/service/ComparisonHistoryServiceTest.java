package com.trolleycompare.backend.service;

import com.trolleycompare.backend.dto.BasketComparisonResponse.ItemInput;
import com.trolleycompare.backend.dto.BasketComparisonResponse.StoreBasketComparison;
import com.trolleycompare.backend.model.AppUser;
import com.trolleycompare.backend.model.ComparisonHistory;
import com.trolleycompare.backend.model.ComparisonHistory;
import com.trolleycompare.backend.repository.AppUserRepository;
import com.trolleycompare.backend.repository.ComparisonHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComparisonHistoryServiceTest {

    @Mock
    private AppUserRepository users;

    @Mock
    private ComparisonHistoryRepository history;

    @Mock
    private ProductComparisonService comparisonService;

    @Test
    void comparesAndSavesHistoryForTheSignedInUser() {
        AppUser user = new AppUser("Person", "person@example.com", "encoded");
        List<ItemInput> items = List.of(new ItemInput("Milk", 1, "L"));
        List<StoreBasketComparison> results = List.of(
                new StoreBasketComparison("Tesco", 1.25, 1.10, List.of()));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(comparisonService.compareBasket(items)).thenReturn(results);
        when(history.save(any(ComparisonHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ComparisonHistoryService service = new ComparisonHistoryService(
                users, history, comparisonService, new ObjectMapper());

        var saved = service.compareAndSave(7L, items);

        assertEquals(items, saved.items());
        assertEquals(results, saved.results());
        verify(comparisonService).compareBasket(items);
        verify(history).save(argThat(entry -> entry.getUser() == user));
    }

    @Test
    void onlyLoadsHistoryForRequestedUser() {
        when(history.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        ComparisonHistoryService service = new ComparisonHistoryService(
                users, history, comparisonService, new ObjectMapper());

        assertEquals(List.of(), service.findForUser(7L));
        verify(history).findByUserIdOrderByCreatedAtDesc(7L);
    }

    @Test
    void recentSearchesAreUniqueAndLimitedToFive() {
        when(history.findTop100ByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(
                new ComparisonHistory(null, """
                        [{"name":"Milk","quantity":2,"unit":"L"},
                         {"name":"Bread","quantity":1,"unit":"pcs"},
                         {"name":"Eggs","quantity":6,"unit":"pcs"}]
                        """, "[]"),
                new ComparisonHistory(null, """
                        [{"name":"milk","quantity":1,"unit":"L"},
                         {"name":"Apples","quantity":4,"unit":"pcs"},
                         {"name":"Cheese","quantity":1,"unit":"kg"},
                         {"name":"Rice","quantity":1,"unit":"kg"}]
                        """, "[]")));
        ComparisonHistoryService service = new ComparisonHistoryService(
                users, history, comparisonService, new ObjectMapper());

        var searches = service.findRecentProductSearches(7L);

        assertEquals(5, searches.size());
        assertEquals(List.of("Milk", "Bread", "Eggs", "Apples", "Cheese"),
                searches.stream().map(ItemInput::name).toList());
    }

    @Test
    void noSearchHistoryReturnsAnEmptyList() {
        when(history.findTop100ByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        ComparisonHistoryService service = new ComparisonHistoryService(
                users, history, comparisonService, new ObjectMapper());

        assertEquals(List.of(), service.findRecentProductSearches(7L));
    }
}
