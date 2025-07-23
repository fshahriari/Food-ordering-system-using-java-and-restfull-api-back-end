package com.snappfood.model;

/**
 * Defines the lifecycle states of an order, including approval steps.
 */
public enum OrderStatus {
    PENDING_ADMIN_APPROVAL,   // Initial state after customer submits
    REJECTED_BY_ADMIN,        // If the admin rejects the order
    PENDING_VENDOR_APPROVAL,  // After admin approves, waiting for the restaurant
    REJECTED_BY_VENDOR,       // If the restaurant rejects the order
    PREPARING,                // Restaurant is preparing the order
    READY_FOR_PICKUP,         // Order is ready for courier pickup
    ON_THE_WAY,               // Courier has the order and is en route
    COMPLETED,                // Order successfully delivered
    CANCELLED                 // Order was cancelled
}