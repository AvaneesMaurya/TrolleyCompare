package com.trolleycompare.backend.controller;

import com.trolleycompare.backend.dto.BasketComparisonResponseDto;
import com.trolleycompare.backend.dto.GraphqlBasketRequest;
import com.trolleycompare.backend.exception.ResourceNotFoundException;
import com.trolleycompare.backend.model.Product;
import com.trolleycompare.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ProductGraphController {

    @Autowired
    private ProductRepository repository;

    @QueryMapping
    public List<Product> getAllProducts() {
        return repository.findAll();
    }

    @QueryMapping
    public List<Product> getProductPriceHistory(@Argument String productName) {
        return repository.findByNameContainingIgnoreCaseOrderByDateDesc(productName.trim());
    }

    @MutationMapping
    public String clearAllData() {
        repository.deleteAll();
        return "All previous daily data wiped successfully!";
    }

    @MutationMapping
    public Product addProduct(
            @Argument String name, 
            @Argument String brand, 
            @Argument String category, 
            @Argument String unitSize, 
            @Argument String storeName, 
            @Argument BigDecimal price, 
            @Argument BigDecimal clubPrice,
            @Argument BigDecimal unitPrice,
            @Argument String date) {
        
        Product product = new Product();
        product.setName(name);
        product.setBrand(brand);
        product.setCategory(category);
        product.setUnitSize(unitSize);
        product.setStoreName(storeName);
        product.setPrice(price);
        product.setClubPrice(clubPrice);
        product.setUnitPrice(unitPrice);
        
        if (date != null && !date.isBlank()) {
            product.setDate(LocalDate.parse(date.trim()));
        } else {
            product.setDate(LocalDate.now());
        }
        
        return repository.save(product);
    }

    @MutationMapping
    public BasketComparisonResponseDto compareBasket(
            @Argument("input") GraphqlBasketRequest input,
            @Argument String targetDate) {
        
        if (input == null || input.getItems() == null) {
            throw new ResourceNotFoundException("Basket items cannot be null");
        }

        LocalDate queryDate = (targetDate != null && !targetDate.isBlank()) 
            ? LocalDate.parse(targetDate.trim()) 
            : null;

        // 1. Resolve each requested basket item to its matching catalog items
        Map<GraphqlBasketRequest.BasketItemInput, List<Product>> resolvedItemsMap = new LinkedHashMap<>();

        for (GraphqlBasketRequest.BasketItemInput itemReq : input.getItems()) {
            String searchKeyword = itemReq.getProductName().trim();
            List<Product> matchingEntries;

            if (queryDate != null) {
                matchingEntries = repository.findByNameContainingIgnoreCaseAndDate(searchKeyword, queryDate);
            } else {
                List<Product> allMatches = repository.findByNameContainingIgnoreCaseOrderByDateDesc(searchKeyword);
                if (!allMatches.isEmpty()) {
                    LocalDate latestDate = allMatches.get(0).getDate();
                    matchingEntries = allMatches.stream()
                        .filter(p -> p.getDate().equals(latestDate))
                        .collect(Collectors.toList());
                } else {
                    matchingEntries = Collections.emptyList();
                }
            }

            if (matchingEntries.isEmpty()) {
                throw new ResourceNotFoundException("Product not found in catalog: " + searchKeyword);
            }

            resolvedItemsMap.put(itemReq, matchingEntries);
        }

        // 2. Identify all unique stores available in these matching products
        Set<String> allStores = resolvedItemsMap.values().stream()
            .flatMap(List::stream)
            .map(Product::getStoreName)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        if (allStores.isEmpty()) {
            throw new ResourceNotFoundException("No store prices found for the requested basket items.");
        }

        List<BasketComparisonResponseDto.StoreComparisonResultDto> storeSummaries = new ArrayList<>();
        String bestRegStore = null;
        BigDecimal lowestRegTotal = null;
        String bestClubStore = null;
        BigDecimal lowestClubTotal = null;

        // 3. Evaluate each store against the full basket (putting N/A for missing items)
        for (String storeName : allStores) {
            List<BasketComparisonResponseDto.ItemDetailDto> storeItemDetails = new ArrayList<>();
            BigDecimal storeRegTotal = BigDecimal.ZERO;
            BigDecimal storeClubTotal = BigDecimal.ZERO;

            for (Map.Entry<GraphqlBasketRequest.BasketItemInput, List<Product>> entry : resolvedItemsMap.entrySet()) {
                GraphqlBasketRequest.BasketItemInput itemReq = entry.getKey();
                List<Product> possibleProducts = entry.getValue();

                // Find if this store carries this item
                Optional<Product> storeProductMatch = possibleProducts.stream()
                    .filter(p -> storeName.equalsIgnoreCase(p.getStoreName()))
                    .min(Comparator.comparing(p -> p.getPrice() != null ? p.getPrice() : BigDecimal.valueOf(999999)));

                BasketComparisonResponseDto.ItemDetailDto detail = new BasketComparisonResponseDto.ItemDetailDto();
                detail.setProductName(itemReq.getProductName());
                detail.setQuantity((int) itemReq.getQuantity());

                if (storeProductMatch.isPresent()) {
                    Product found = storeProductMatch.get();
                    BigDecimal regPrice = found.getPrice() != null ? found.getPrice() : BigDecimal.ZERO;
                    BigDecimal clubPrice = found.getClubPrice();

                    BigDecimal regSubtotal = regPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
                    BigDecimal clubSubtotal = clubPrice != null 
                        ? clubPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity())) 
                        : regSubtotal;

                    detail.setRegularPrice(regPrice);
                    detail.setClubPrice(clubPrice);
                    detail.setRegularSubtotal(regSubtotal);
                    detail.setClubSubtotal(clubSubtotal);

                    storeRegTotal = storeRegTotal.add(regSubtotal);
                    storeClubTotal = storeClubTotal.add(clubSubtotal);
                } else {
                    // Item not present in this store -> N/A (null values)
                    detail.setRegularPrice(null);
                    detail.setClubPrice(null);
                    detail.setRegularSubtotal(null);
                    detail.setClubSubtotal(null);
                }

                storeItemDetails.add(detail);
            }

            BasketComparisonResponseDto.StoreComparisonResultDto summary = new BasketComparisonResponseDto.StoreComparisonResultDto();
            summary.setStoreName(storeName);
            summary.setItems(storeItemDetails);
            summary.setTotalRegularPrice(storeRegTotal);
            summary.setTotalClubPrice(storeClubTotal);
            storeSummaries.add(summary);

            if (lowestRegTotal == null || storeRegTotal.compareTo(lowestRegTotal) < 0) {
                lowestRegTotal = storeRegTotal;
                bestRegStore = storeName;
            }
            if (lowestClubTotal == null || storeClubTotal.compareTo(lowestClubTotal) < 0) {
                lowestClubTotal = storeClubTotal;
                bestClubStore = storeName;
            }
        }

        BasketComparisonResponseDto response = new BasketComparisonResponseDto();
        response.setStoreSummaries(storeSummaries);
        response.setCheapestStoreRegular(bestRegStore);
        response.setLowestRegularTotal(lowestRegTotal);
        response.setCheapestStoreClub(bestClubStore);
        response.setLowestClubTotal(lowestClubTotal);

        return response;
    }
}