package com.trolleycompare.backend.service;

import com.trolleycompare.backend.dto.BasketComparisonResponse.ItemInput;
import com.trolleycompare.backend.model.Product;
import com.trolleycompare.backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductComparisonServiceTest {

    private ProductRepository products;
    private ProductComparisonService service;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        service = new ProductComparisonService();
        ReflectionTestUtils.setField(service, "productRepository", products);
    }

    @Test
    void rejectsNonPositiveProductQuantitiesBeforeQueryingRepository() {
        assertThrows(IllegalArgumentException.class, () -> service.findAndScaleProducts("Milk", 0, "L"));
        assertThrows(IllegalArgumentException.class, () -> service.findAndScaleProducts("Milk", -1, "L"));
        verifyNoInteractions(products);
    }

    @Test
    void scalesNearestSameCategoryProductForEachStoreAndUsesRecentFallback() {
        when(products.findByNameContainingIgnoreCaseAndDate(eq("Milk"), any(LocalDate.class)))
                .thenReturn(List.of());
        when(products.findByNameContainingIgnoreCaseOrderByDateDesc("Milk")).thenReturn(List.of(
                product("Tesco", "Milk 2L", 2, "L", 4, 3.6),
                product("Tesco", "Milk 1L", 1, "L", 2.25, 2),
                product("Aldi", "Milk 500g", 500, "g", 1, 0.9),
                product("Lidl", "Milk 500ml", 500, "ml", 1.2, 1.1)));

        var result = service.findAndScaleProducts("Milk", 1, "L");

        assertEquals(2, result.size());
        Map<String, com.trolleycompare.backend.dto.ProductComparisonResponse> byStore =
                result.stream().collect(Collectors.toMap(
                        com.trolleycompare.backend.dto.ProductComparisonResponse::storeName,
                        Function.identity()));
        assertEquals("Milk 1L", byStore.get("Tesco").productName());
        assertEquals(2.25, byStore.get("Tesco").calculatedRegularPrice());
        assertEquals(2.0, byStore.get("Tesco").calculatedClubPrice());
        assertEquals(2.4, byStore.get("Lidl").calculatedRegularPrice());
        verify(products).findByNameContainingIgnoreCaseOrderByDateDesc("Milk");
    }

    @Test
    void usesTodayResultsAndHandlesZeroSizedPackages() {
        when(products.findByNameContainingIgnoreCaseAndDate(eq("Milk"), any(LocalDate.class)))
                .thenReturn(List.of(product("Tesco", "Milk", 0, "L", 2, 1.5)));

        var result = service.findAndScaleProducts("Milk", 1, "L");

        assertEquals(1, result.size());
        assertEquals(2.0, result.get(0).calculatedRegularPrice());
        assertEquals(1.5, result.get(0).calculatedClubPrice());
        verify(products, never()).findByNameContainingIgnoreCaseOrderByDateDesc(anyString());
    }

    @Test
    void rejectsAnEmptyBasket() {
        assertThrows(IllegalArgumentException.class, () -> service.compareBasket(null));
        assertThrows(IllegalArgumentException.class, () -> service.compareBasket(List.of()));
        verifyNoInteractions(products);
    }

    @Test
    void comparesBasketWithTodayProductsAndMarksMissingStoreItems() {
        when(products.findByDate(any(LocalDate.class))).thenReturn(List.of(
                product("Tesco", "Whole Milk", 2, "L", 4, 3.5),
                product("Aldi", "whole milk", 1000, "ml", 1.8, 1.8),
                product("Tesco", "Bread", 800, "g", 2.5, 2.5)));

        var result = service.compareBasket(List.of(
                new ItemInput("milk", 1, "L"),
                new ItemInput("Bread", 400, "g")));

        assertEquals(5, result.size());
        assertEquals(List.of("Dunnes Stores", "SuperValu", "Lidl", "Aldi", "Tesco"),
                result.stream().map(store -> store.storeName()).toList());
        var aldi = result.stream().filter(store -> store.storeName().equals("Aldi")).findFirst().orElseThrow();
        assertEquals(1.8, aldi.totalRegularPrice());
        assertEquals(1.8, aldi.totalClubPrice());
        assertEquals("whole milk", aldi.items().get(0).productName());
        assertEquals("NA", aldi.items().get(1).productName());
        assertNull(aldi.items().get(1).calculatedRegularPrice());

        var tesco = result.stream().filter(store -> store.storeName().equals("Tesco")).findFirst().orElseThrow();
        assertEquals(3.25, tesco.totalRegularPrice());
        assertEquals(3.0, tesco.totalClubPrice());
        assertEquals(1.25, tesco.items().get(1).calculatedRegularPrice());
        verify(products).findByDate(any(LocalDate.class));
        verify(products, never()).findAll();
    }

    @Test
    void fallsBackToAllProductsWhenNoPricesExistToday() {
        when(products.findByDate(any(LocalDate.class))).thenReturn(List.of());
        when(products.findAll()).thenReturn(List.of(product("Lidl", "Milk", 1, "L", 1, 0.9)));

        var result = service.compareBasket(List.of(new ItemInput("Milk", 1, "L")));

        assertEquals(5, result.size());
        var lidl = result.stream().filter(store -> store.storeName().equals("Lidl")).findFirst().orElseThrow();
        assertEquals(0.9, lidl.totalClubPrice());
        assertEquals("NA", result.get(0).items().get(0).productName());
        verify(products).findAll();
    }

    @Test
    void usesAUnitScalingFactorOfOneForZeroSizedBasketPackages() {
        when(products.findByDate(any(LocalDate.class))).thenReturn(List.of(
                product("Aldi", "Milk", 0, "L", 2, 1.5)));

        var result = service.compareBasket(List.of(new ItemInput("Milk", 1, "L")));

        var aldi = result.stream().filter(store -> store.storeName().equals("Aldi")).findFirst().orElseThrow();
        assertEquals(2.0, aldi.totalRegularPrice());
        assertEquals(1.5, aldi.totalClubPrice());
    }

    private Product product(String store, String name, double quantity, String unit, double regular, double club) {
        Product product = new Product();
        product.setStoreName(store);
        product.setName(name);
        product.setQuantity(quantity);
        product.setUnit(unit);
        product.setRegularPrice(regular);
        product.setClubPrice(club);
        return product;
    }
}
