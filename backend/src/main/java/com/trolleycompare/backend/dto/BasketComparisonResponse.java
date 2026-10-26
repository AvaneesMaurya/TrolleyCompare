package com.trolleycompare.backend.dto;

import java.util.List;

public class BasketComparisonResponse {
    
    public record ItemInput(String name, double quantity, String unit) {}

    public record BasketItemResult(
        String productName,
        Double storePackageQuantity,
        String storePackageUnit,
        Double calculatedRegularPrice,
        Double calculatedClubPrice
    ) {}

    public record StoreBasketComparison(
        String storeName,
        Double totalRegularPrice,
        Double totalClubPrice,
        List<BasketItemResult> items
    ) {}
}