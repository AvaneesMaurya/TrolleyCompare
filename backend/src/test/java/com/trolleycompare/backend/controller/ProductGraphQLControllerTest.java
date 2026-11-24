package com.trolleycompare.backend.controller;

import com.trolleycompare.backend.dto.BasketComparisonResponse.ItemInput;
import com.trolleycompare.backend.dto.BasketComparisonResponse.StoreBasketComparison;
import com.trolleycompare.backend.dto.ProductComparisonResponse;
import com.trolleycompare.backend.service.ProductComparisonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class ProductGraphQLControllerTest {

    private ProductComparisonService comparison;
    private ProductGraphQLController controller;

    @BeforeEach
    void setUp() {
        comparison = mock(ProductComparisonService.class);
        controller = new ProductGraphQLController();
        ReflectionTestUtils.setField(controller, "productComparisonService", comparison);
    }

    @Test
    void delegatesSingleProductComparisons() {
        List<ProductComparisonResponse> expected = List.of(
                new ProductComparisonResponse("Tesco", "Milk", 1, "L", 1.2, 1.1));
        when(comparison.findAndScaleProducts("Milk", 1, "L")).thenReturn(expected);

        assertSame(expected, controller.compareProduct("Milk", 1, "L"));
        verify(comparison).findAndScaleProducts("Milk", 1, "L");
    }

    @Test
    void delegatesBasketComparisons() {
        List<ItemInput> items = List.of(new ItemInput("Milk", 1, "L"));
        List<StoreBasketComparison> expected = List.of(new StoreBasketComparison("Tesco", 1.2, 1.1, List.of()));
        when(comparison.compareBasket(items)).thenReturn(expected);

        assertSame(expected, controller.compareBasket(items));
        verify(comparison).compareBasket(items);
    }
}
