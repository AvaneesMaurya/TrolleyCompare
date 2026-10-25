package com.trolleycompare.backend.repository;

import com.trolleycompare.backend.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByNameContainingIgnoreCaseAndDate(String name, LocalDate date);
    List<Product> findByNameContainingIgnoreCaseOrderByDateDesc(String name);
}