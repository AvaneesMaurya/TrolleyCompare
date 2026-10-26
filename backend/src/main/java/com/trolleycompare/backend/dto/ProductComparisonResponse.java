package com.trolleycompare.backend.dto;

public record ProductComparisonResponse(
    String storeName,
    String productName,
    double storePackageQuantity,
    String storePackageUnit,
    double calculatedRegularPrice,
    double calculatedClubPrice
) {}