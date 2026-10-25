package com.trolleycompare.backend.repository;

import com.trolleycompare.backend.model.StorePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorePriceRepository extends JpaRepository<StorePrice, Long> {
    List<StorePrice> findByProductId(Long productId);
    
    // Find the single lowest price entry for a product
    Optional<StorePrice> findTopByProductIdOrderByPriceAsc(Long productId);
}