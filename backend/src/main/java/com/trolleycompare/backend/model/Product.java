package com.trolleycompare.backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String brand;
    private String category;
    private String unitSize;     // e.g., "1L", "800g", "1kg", "250g", "4 pcs"
    private String storeName;    // e.g., "Tesco", "Lidl", "Eurospar"
    private BigDecimal price;      // Regular Store Price
    private BigDecimal clubPrice;  // Store Offer / Loyalty Price
    private BigDecimal unitPrice;  // Price per normalized unit (e.g., per 1L or 1kg)
    private LocalDate date;        // Date the price snapshot was recorded

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUnitSize() { return unitSize; }
    public void setUnitSize(String unitSize) { this.unitSize = unitSize; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getClubPrice() { return clubPrice; }
    public void setClubPrice(BigDecimal clubPrice) { this.clubPrice = clubPrice; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}