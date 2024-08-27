package com.hdbank.taipham.producer.Model;

import java.sql.Timestamp;

public class SaleRecord {
    private String username;
    private String productName;
    private Integer quantity;
    private Double price;
    private Timestamp createdAt;
    private String source;

    // Constructors
    public SaleRecord() {
    }

    public SaleRecord(String username,  String productName, Integer quantity, Double price, Timestamp createdAt, String source) {
        this.username = username;
        this.productName = productName;
        this.quantity = quantity;
        this.price = price;
        this.createdAt = createdAt;
        this.source = source;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public String toString() {
        return "SaleRecord{" +
                "username='" + username + '\'' +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", price=" + price +
                ", createdAt=" + createdAt +
                ", source='" + source + '\'' +
                '}';
    }
}
