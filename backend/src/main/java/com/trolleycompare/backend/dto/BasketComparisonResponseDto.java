package com.trolleycompare.backend.dto;

import java.math.BigDecimal;
import java.util.List;

public class BasketComparisonResponseDto {
    private List<StoreComparisonResultDto> storeSummaries;
    private String cheapestStoreRegular;
    private BigDecimal lowestRegularTotal;
    private String cheapestStoreClub;
    private BigDecimal lowestClubTotal;

    public static class StoreComparisonResultDto {
        private String storeName;
        private List<ItemDetailDto> items;
        private BigDecimal totalRegularPrice;
        private BigDecimal totalClubPrice;

        public String getStoreName() { return storeName; }
        public void setStoreName(String storeName) { this.storeName = storeName; }
        public List<ItemDetailDto> getItems() { return items; }
        public void setItems(List<ItemDetailDto> items) { this.items = items; }
        public BigDecimal getTotalRegularPrice() { return totalRegularPrice; }
        public void setTotalRegularPrice(BigDecimal totalRegularPrice) { this.totalRegularPrice = totalRegularPrice; }
        public BigDecimal getTotalClubPrice() { return totalClubPrice; }
        public void setTotalClubPrice(BigDecimal totalClubPrice) { this.totalClubPrice = totalClubPrice; }
    }

    public static class ItemDetailDto {
        private String productName;
        private int quantity;
        private BigDecimal regularPrice;
        private BigDecimal clubPrice;
        private BigDecimal regularSubtotal;
        private BigDecimal clubSubtotal;

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public BigDecimal getRegularPrice() { return regularPrice; }
        public void setRegularPrice(BigDecimal regularPrice) { this.regularPrice = regularPrice; }
        public BigDecimal getClubPrice() { return clubPrice; }
        public void setClubPrice(BigDecimal clubPrice) { this.clubPrice = clubPrice; }
        public BigDecimal getRegularSubtotal() { return regularSubtotal; }
        public void setRegularSubtotal(BigDecimal regularSubtotal) { this.regularSubtotal = regularSubtotal; }
        public BigDecimal getClubSubtotal() { return clubSubtotal; }
        public void setClubSubtotal(BigDecimal clubSubtotal) { this.clubSubtotal = clubSubtotal; }
    }

    public List<StoreComparisonResultDto> getStoreSummaries() { return storeSummaries; }
    public void setStoreSummaries(List<StoreComparisonResultDto> storeSummaries) { this.storeSummaries = storeSummaries; }
    public String getCheapestStoreRegular() { return cheapestStoreRegular; }
    public void setCheapestStoreRegular(String cheapestStoreRegular) { this.cheapestStoreRegular = cheapestStoreRegular; }
    public BigDecimal getLowestRegularTotal() { return lowestRegularTotal; }
    public void setLowestRegularTotal(BigDecimal lowestRegularTotal) { this.lowestRegularTotal = lowestRegularTotal; }
    public String getCheapestStoreClub() { return cheapestStoreClub; }
    public void setCheapestStoreClub(String cheapestStoreClub) { this.cheapestStoreClub = cheapestStoreClub; }
    public BigDecimal getLowestClubTotal() { return lowestClubTotal; }
    public void setLowestClubTotal(BigDecimal lowestClubTotal) { this.lowestClubTotal = lowestClubTotal; }
}