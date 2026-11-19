package com.trolleycompare.backend.controller;

import com.trolleycompare.backend.dto.BasketComparisonResponse.ItemInput;
import com.trolleycompare.backend.dto.ComparisonHistoryResponse;
import com.trolleycompare.backend.dto.SaveComparisonRequest;
import com.trolleycompare.backend.security.UserPrincipal;
import com.trolleycompare.backend.service.ComparisonHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/history")
public class ComparisonHistoryController {
    private final ComparisonHistoryService historyService;

    public ComparisonHistoryController(ComparisonHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping
    public List<ComparisonHistoryResponse> getHistory(@AuthenticationPrincipal UserPrincipal principal) {
        return historyService.findForUser(requireUserId(principal));
    }

    @GetMapping("/searches")
    public List<ItemInput> getRecentSearches(@AuthenticationPrincipal UserPrincipal principal) {
        return historyService.findRecentProductSearches(requireUserId(principal));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComparisonHistoryResponse compareAndSave(
            @RequestBody SaveComparisonRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shopping basket cannot be empty.");
        }
        List<ItemInput> items = request.items();
        return historyService.compareAndSave(requireUserId(principal), items);
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please sign in.");
        }
        return principal.id();
    }
}
