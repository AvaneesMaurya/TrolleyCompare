package com.trolleycompare.backend.service;

import com.trolleycompare.backend.dto.BasketComparisonResponse.*;
import com.trolleycompare.backend.dto.ProductComparisonResponse;
import com.trolleycompare.backend.model.Product;
import com.trolleycompare.backend.repository.ProductRepository;
import com.trolleycompare.backend.util.UnitConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductComparisonService {

    @Autowired
    private ProductRepository productRepository;

    private static final String[] STORES = {"Tesco", "Dunnes Stores", "SuperValu", "Aldi", "Lidl"};

    public List<ProductComparisonResponse> findAndScaleProducts(String name, double requestedQty, String requestedUnit) {
        if (requestedQty <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        LocalDate today = LocalDate.now();
        List<Product> storeProducts = productRepository.findByNameContainingIgnoreCaseAndDate(name, today);

        if (storeProducts.isEmpty()) {
            storeProducts = productRepository.findByNameContainingIgnoreCaseOrderByDateDesc(name);
        }

        double requestedBaseQty = UnitConverter.normalizeToBasePath(requestedQty, requestedUnit);
        String requestedCategory = UnitConverter.getUnitCategory(requestedUnit);

        Map<String, Product> bestProductPerStore = storeProducts.stream()
            .filter(p -> UnitConverter.getUnitCategory(p.getUnit()).equals(requestedCategory))
            .collect(Collectors.toMap(
                Product::getStoreName,
                p -> p,
                (existing, replacement) -> {
                    double diffExisting = Math.abs(UnitConverter.normalizeToBasePath(existing.getQuantity(), existing.getUnit()) - requestedBaseQty);
                    double diffReplacement = Math.abs(UnitConverter.normalizeToBasePath(replacement.getQuantity(), replacement.getUnit()) - requestedBaseQty);
                    return diffReplacement < diffExisting ? replacement : existing;
                }
            ));

        return bestProductPerStore.values().stream().map(storeProduct -> {
            double storeBaseQty = UnitConverter.normalizeToBasePath(storeProduct.getQuantity(), storeProduct.getUnit());
            double scalingFactor = storeBaseQty > 0 ? (requestedBaseQty / storeBaseQty) : 1.0;

            double scaledRegularPrice = Math.round((storeProduct.getRegularPrice() * scalingFactor) * 100.0) / 100.0;
            double scaledClubPrice = Math.round((storeProduct.getClubPrice() * scalingFactor) * 100.0) / 100.0;

            return new ProductComparisonResponse(
                storeProduct.getStoreName(),
                storeProduct.getName(),
                storeProduct.getQuantity(),
                storeProduct.getUnit(),
                scaledRegularPrice,
                scaledClubPrice
            );
        }).collect(Collectors.toList());
    }

    public List<StoreBasketComparison> compareBasket(List<ItemInput> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException("Shopping basket cannot be empty.");
        }

        LocalDate today = LocalDate.now();
        List<Product> allProducts = productRepository.findByDate(today);
        if (allProducts.isEmpty()) {
            allProducts = productRepository.findAll();
        }

        List<StoreBasketComparison> basketComparisons = new ArrayList<>();

        for (String store : STORES) {
            List<Product> storeProducts = allProducts.stream()
                .filter(p -> p.getStoreName().equalsIgnoreCase(store))
                .collect(Collectors.toList());

            List<BasketItemResult> itemResults = new ArrayList<>();
            double totalReg = 0.0;
            double totalClub = 0.0;

            for (ItemInput req : requestedItems) {
                double requestedBaseQty = UnitConverter.normalizeToBasePath(req.quantity(), req.unit());
                String requestedCategory = UnitConverter.getUnitCategory(req.unit());

                Product bestMatch = storeProducts.stream()
                    .filter(p -> p.getName().toLowerCase().contains(req.name().toLowerCase()))
                    .filter(p -> UnitConverter.getUnitCategory(p.getUnit()).equals(requestedCategory))
                    .min(Comparator.comparingDouble(p -> 
                        Math.abs(UnitConverter.normalizeToBasePath(p.getQuantity(), p.getUnit()) - requestedBaseQty)
                    ))
                    .orElse(null);

                if (bestMatch != null) {
                    double storeBaseQty = UnitConverter.normalizeToBasePath(bestMatch.getQuantity(), bestMatch.getUnit());
                    double scalingFactor = storeBaseQty > 0 ? (requestedBaseQty / storeBaseQty) : 1.0;

                    double scaledReg = Math.round((bestMatch.getRegularPrice() * scalingFactor) * 100.0) / 100.0;
                    double scaledClub = Math.round((bestMatch.getClubPrice() * scalingFactor) * 100.0) / 100.0;

                    totalReg += scaledReg;
                    totalClub += scaledClub;

                    itemResults.add(new BasketItemResult(
                        bestMatch.getName(),
                        bestMatch.getQuantity(),
                        bestMatch.getUnit(),
                        scaledReg,
                        scaledClub
                    ));
                } else {
                    itemResults.add(new BasketItemResult(
                        "NA",
                        null,
                        "NA",
                        null,
                        null
                    ));
                }
            }

            basketComparisons.add(new StoreBasketComparison(
                store,
                Math.round(totalReg * 100.0) / 100.0,
                Math.round(totalClub * 100.0) / 100.0,
                itemResults
            ));
        }

        basketComparisons.sort(Comparator.comparingDouble(StoreBasketComparison::totalRegularPrice));
        return basketComparisons;
    }
}