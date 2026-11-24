package com.trolleycompare.backend.controller;

import com.trolleycompare.backend.dto.BasketComparisonResponse.ItemInput;
import com.trolleycompare.backend.dto.SaveComparisonRequest;
import com.trolleycompare.backend.model.AppUser;
import com.trolleycompare.backend.security.UserPrincipal;
import com.trolleycompare.backend.service.ComparisonHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ComparisonHistoryControllerTest {

    private ComparisonHistoryService history;
    private ComparisonHistoryController controller;
    private final UserPrincipal principal = new UserPrincipal(
            7L, "Person", "person@example.com", "hash", AppUser.Role.USER, true);

    @BeforeEach
    void setUp() {
        history = mock(ComparisonHistoryService.class);
        controller = new ComparisonHistoryController(history);
    }

    @Test
    void loadsHistoryAndRecentSearchesForTheSignedInUser() {
        when(history.findForUser(7L)).thenReturn(List.of());
        when(history.findRecentProductSearches(7L)).thenReturn(List.of());

        assertEquals(List.of(), controller.getHistory(principal));
        assertEquals(List.of(), controller.getRecentSearches(principal));
        verify(history).findForUser(7L);
        verify(history).findRecentProductSearches(7L);
    }

    @Test
    void rejectsUnauthenticatedHistoryRequestsAndNullSaveBodies() {
        assertStatus(HttpStatus.UNAUTHORIZED, () -> controller.getHistory(null));
        assertStatus(HttpStatus.UNAUTHORIZED, () -> controller.getRecentSearches(null));
        assertStatus(HttpStatus.BAD_REQUEST, () -> controller.compareAndSave(null, principal));
        verifyNoInteractions(history);
    }

    @Test
    void savesTheItemsFromTheRequestForTheSignedInUser() {
        List<ItemInput> items = List.of(new ItemInput("Milk", 1, "L"));
        var expected = new com.trolleycompare.backend.dto.ComparisonHistoryResponse(null, null, items, List.of());
        when(history.compareAndSave(7L, items)).thenReturn(expected);

        assertSame(expected, controller.compareAndSave(new SaveComparisonRequest(items), principal));
        verify(history).compareAndSave(7L, items);
    }

    private void assertStatus(HttpStatus expected, org.junit.jupiter.api.function.Executable action) {
        ResponseStatusException error = assertThrows(ResponseStatusException.class, action);
        assertEquals(expected, error.getStatusCode());
    }
}
