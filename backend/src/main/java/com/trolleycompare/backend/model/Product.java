package com.trolleycompare.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String storeName;
    private String name;
    private Double quantity;
    private String unit;
    private Double regularPrice;
    private Double clubPrice;
    private LocalDate date;

    public Product() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public Double getRegularPrice() { return regularPrice; }
    public void setRegularPrice(Double regularPrice) { this.regularPrice = regularPrice; }

    public Double getClubPrice() { return clubPrice; }
    public void setClubPrice(Double clubPrice) { this.clubPrice = clubPrice; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
}