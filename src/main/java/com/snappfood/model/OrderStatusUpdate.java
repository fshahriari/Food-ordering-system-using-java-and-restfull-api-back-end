package com.snappfood.model;

/**
 * Represents the JSON object for updating an order's status via the admin endpoint.
 */
public class OrderStatusUpdate {
    private int order_id;
    private String status;

    public int getOrderId() { return order_id; }
    public void setOrderId(int orderId) { this.order_id = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}