package com.snappfood.model;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * Represents a customer's order in the system.
 */
public class Order {
    private int id;
    private int customerId;
    private int restaurantId;
    private Integer courierId;
    private OrderStatus status;
    private String deliveryAddress;

    private int rawPrice;
    private int taxFee;
    private int additionalFee;
    private int courierFee;
    private int payPrice;
    private Integer couponId;

    private Map<Integer, Integer> items; //key: food id, value: quantity

    private Timestamp createdAt;
    private Timestamp updatedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    public int getRestaurantId() { return restaurantId; }
    public void setRestaurantId(int restaurantId) { this.restaurantId = restaurantId; }
    public Integer getCourierId() { return courierId; }
    public void setCourierId(Integer courierId) { this.courierId = courierId; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public int getRawPrice() { return rawPrice; }
    public void setRawPrice(int rawPrice) { this.rawPrice = rawPrice; }
    public int getTaxFee() { return taxFee; }
    public void setTaxFee(int taxFee) { this.taxFee = taxFee; }
    public int getAdditionalFee() { return additionalFee; }
    public void setAdditionalFee(int additionalFee) { this.additionalFee = additionalFee; }
    public int getCourierFee() { return courierFee; }
    public void setCourierFee(int courierFee) { this.courierFee = courierFee; }
    public int getPayPrice() { return payPrice; }
    public void setPayPrice(int payPrice) { this.payPrice = payPrice; }
    public Integer getCouponId() { return couponId; }
    public void setCouponId(Integer couponId) { this.couponId = couponId; }
    public Map <Integer, Integer> getItems() { return items; }
    public void setItems(Map<Integer, Integer> items) { this.items = items; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
