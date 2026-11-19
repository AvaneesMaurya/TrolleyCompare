package com.trolleycompare.backend.repository;

import com.trolleycompare.backend.model.ComparisonHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComparisonHistoryRepository extends JpaRepository<ComparisonHistory, Long> {
    List<ComparisonHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ComparisonHistory> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
}
