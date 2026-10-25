package com.trolleycompare.backend.dto;

import java.util.List;

public class GraphqlBasketRequest {
    private List<BasketItemInput> items;

    public static class BasketItemInput {
        private String productName;
        private double quantity;
        private String unit;

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public double getQuantity() { return quantity; }
        public void setQuantity(double quantity) { this.quantity = quantity; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }
    }

    public List<BasketItemInput> getItems() { return items; }
    public void setItems(List<BasketItemInput> items) { this.items = items; }
}