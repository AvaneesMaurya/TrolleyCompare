package com.trolleycompare.backend.service;

import com.trolleycompare.backend.dto.BasketComparisonResponse.ItemInput;
import com.trolleycompare.backend.dto.BasketComparisonResponse.StoreBasketComparison;
import com.trolleycompare.backend.model.AppUser;
import com.trolleycompare.backend.model.ComparisonHistory;
import com.trolleycompare.backend.repository.AppUserRepository;
import com.trolleycompare.backend.repository.ComparisonHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
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
    void rejectsEmptyBasketsAndUnknownUsersBeforeComparing() {
        ComparisonHistoryService service = new ComparisonHistoryService(
                users, history, comparisonService, new ObjectMapper());

        assertStatus(HttpStatus.BAD_REQUEST, () -> service.compareAndSave(7L, null));
        assertStatus(HttpStatus.BAD_REQUEST, () -> service.compareAndSave(7L, List.of()));
        when(users.findById(7L)).thenReturn(Optional.empty());
        assertStatus(HttpStatus.NOT_FOUND,
                () -> service.compareAndSave(7L, List.of(new ItemInput("Milk", 1, "L"))));
        verifyNoInteractions(comparisonService);
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
    void deserializesSavedHistoryIntoResponseRecords() {
        AppUser user = new AppUser("Person", "person@example.com", "encoded");
        ComparisonHistory entry = new ComparisonHistory(user,
                "[{\"name\":\"Milk\",\"quantity\":1,\"unit\":\"L\"}]",
                "[{\"storeName\":\"Tesco\",\"totalRegularPrice\":1.2,\"totalClubPrice\":1.1,\"items\":[]}]");
        ReflectionTestUtils.setField(entry, "id", 18L);
        Instant createdAt = Instant.parse("2026-01-02T03:04:05Z");
        ReflectionTestUtils.setField(entry, "createdAt", createdAt);
        when(history.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(entry));
        ComparisonHistoryService service = new ComparisonHistoryService(
                users, history, comparisonService, new ObjectMapper());

        var result = service.findForUser(7L);

        assertEquals(1, result.size());
        assertEquals(18L, result.get(0).id());
        assertEquals(createdAt, result.get(0).createdAt());
        assertEquals("Milk", result.get(0).items().get(0).name());
        assertEquals(1.2, result.get(0).results().get(0).totalRegularPrice());
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
    void ignoresBlankSearchNamesAndReturnsFewerThanFiveRecentItems() {
        when(history.findTop100ByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(
                new ComparisonHistory(null, """
                        [{"name":null,"quantity":1,"unit":"L"},
                         {"name":"  ","quantity":1,"unit":"L"},
                         {"name":"  Bread  ","quantity":1,"unit":"pcs"}]
                        """, "[]")));
        ComparisonHistoryService service = new ComparisonHistoryService(
                users, history, comparisonService, new ObjectMapper());

        var searches = service.findRecentProductSearches(7L);

        assertEquals(1, searches.size());
        assertEquals("  Bread  ", searches.get(0).name());
    }

    @Test
    void reportsCorruptedSavedJsonClearly() {
        when(history.findByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(
                new ComparisonHistory(null, "not json", "[]")));
        ComparisonHistoryService service = new ComparisonHistoryService(
                users, history, comparisonService, new ObjectMapper());

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> service.findForUser(7L));
        assertEquals("Could not read saved comparison history.", error.getMessage());
    }

    @Test
    void noSearchHistoryReturnsAnEmptyList() {
        when(history.findTop100ByUserIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());
        ComparisonHistoryService service = new ComparisonHistoryService(
                users, history, comparisonService, new ObjectMapper());

        assertEquals(List.of(), service.findRecentProductSearches(7L));
    }

    private void assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable action) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, action);
        assertEquals(expected, error.getStatusCode());
    }
}
