package com.trolleycompare.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "comparison_history")
public class ComparisonHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String basketItems;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String storeResults;

    protected ComparisonHistory() {}

    public ComparisonHistory(AppUser user, String basketItems, String storeResults) {
        this.user = user;
        this.basketItems = basketItems;
        this.storeResults = storeResults;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public AppUser getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }
    public String getBasketItems() { return basketItems; }
    public String getStoreResults() { return storeResults; }
}
