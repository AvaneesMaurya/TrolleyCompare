package com.trolleycompare.backend.service;

import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.trolleycompare.backend.dto.BasketComparisonResponse.ItemInput;
import com.trolleycompare.backend.dto.BasketComparisonResponse.StoreBasketComparison;
import com.trolleycompare.backend.dto.ComparisonHistoryResponse;
import com.trolleycompare.backend.model.AppUser;
import com.trolleycompare.backend.model.ComparisonHistory;
import com.trolleycompare.backend.repository.AppUserRepository;
import com.trolleycompare.backend.repository.ComparisonHistoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class ComparisonHistoryService {
    private final AppUserRepository users;
    private final ComparisonHistoryRepository history;
    private final ProductComparisonService comparisonService;
    private final ObjectMapper objectMapper;

    public ComparisonHistoryService(
            AppUserRepository users,
            ComparisonHistoryRepository history,
            ProductComparisonService comparisonService,
            ObjectMapper objectMapper) {
        this.users = users;
        this.history = history;
        this.comparisonService = comparisonService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ComparisonHistoryResponse compareAndSave(Long userId, List<ItemInput> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shopping basket cannot be empty.");
        }
        AppUser user = users.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        List<StoreBasketComparison> results = comparisonService.compareBasket(items);
        ComparisonHistory saved = history.save(new ComparisonHistory(
                user, writeJson(items), writeJson(results)));
        return toResponse(saved, items, results);
    }

    @Transactional(readOnly = true)
    public List<ComparisonHistoryResponse> findForUser(Long userId) {
        return history.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(entry -> toResponse(
                        entry,
                        readJson(entry.getBasketItems(), ITEM_LIST_TYPE),
                        readJson(entry.getStoreResults(), RESULT_LIST_TYPE)))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ItemInput> findRecentProductSearches(Long userId) {
        Map<String, ItemInput> recentUniqueItems = new LinkedHashMap<>();
        for (ComparisonHistory entry : history.findTop100ByUserIdOrderByCreatedAtDesc(userId)) {
            for (ItemInput item : readJson(entry.getBasketItems(), ITEM_LIST_TYPE)) {
                if (item.name() == null || item.name().isBlank()) {
                    continue;
                }
                recentUniqueItems.putIfAbsent(item.name().trim().toLowerCase(Locale.ROOT), item);
                if (recentUniqueItems.size() == 5) {
                    return new ArrayList<>(recentUniqueItems.values());
                }
            }
        }
        return new ArrayList<>(recentUniqueItems.values());
    }

    private ComparisonHistoryResponse toResponse(
            ComparisonHistory entry,
            List<ItemInput> items,
            List<StoreBasketComparison> results) {
        return new ComparisonHistoryResponse(entry.getId(), entry.getCreatedAt(), items, results);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not save comparison history.", exception);
        }
    }

    private <T> T readJson(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not read saved comparison history.", exception);
        }
    }

    private static final TypeReference<List<ItemInput>> ITEM_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<StoreBasketComparison>> RESULT_LIST_TYPE = new TypeReference<>() {};
}
