package com.trolleycompare.backend.dto;

import com.trolleycompare.backend.dto.BasketComparisonResponse.ItemInput;
import com.trolleycompare.backend.dto.BasketComparisonResponse.StoreBasketComparison;

import java.time.Instant;
import java.util.List;

public record ComparisonHistoryResponse(
        Long id,
        Instant createdAt,
        List<ItemInput> items,
        List<StoreBasketComparison> results) {}
