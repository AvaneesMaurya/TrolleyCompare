package com.trolleycompare.backend.service;

import com.trolleycompare.backend.model.Product;
import com.trolleycompare.backend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DailyPriceRefreshSchedulerTest {

    private ProductRepository products;
    private DailyPriceRefreshScheduler scheduler;

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        scheduler = new DailyPriceRefreshScheduler();
        ReflectionTestUtils.setField(scheduler, "productRepository", products);
    }

    @Test
    void refreshesAllStorePricesAndKeepsClubPricesAtOrBelowRegularPrices() {
        AtomicReference<List<Product>> refreshedProducts = new AtomicReference<>();
        when(products.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Product> saved = invocation.getArgument(0);
            List<Product> list = StreamSupport.stream(saved.spliterator(), false).toList();
            refreshedProducts.set(list);
            return saved;
        });

        scheduler.refreshDatabasePrices();

        verify(products).deleteAllInBatch();

        List<Product> refreshed = refreshedProducts.get();
        assertEquals(40, refreshed.size());
        assertEquals(LocalDate.now(), refreshed.get(0).getDate());
        assertEquals(5, refreshed.stream().map(Product::getStoreName).distinct().count());
        assertEquals(6, refreshed.stream().map(Product::getName).distinct().count());
        assertTrue(refreshed.stream().allMatch(product ->
                product.getRegularPrice() >= 0.0
                        && product.getClubPrice() >= 0.0
                        && product.getClubPrice() <= product.getRegularPrice()));
        assertTrue(refreshed.stream().anyMatch(product -> product.getUnit().equals("kg")));
        assertTrue(refreshed.stream().anyMatch(product -> product.getUnit().equals("pcs")));
    }

    @Test
    void startupTriggersInitialPriceRefresh() {
        scheduler.onStartup();

        verify(products).deleteAllInBatch();
        verify(products).saveAll(argThat(items -> StreamSupport.stream(items.spliterator(), false).count() == 40));
    }
}
