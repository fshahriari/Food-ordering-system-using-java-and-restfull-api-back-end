package com.snappfood.model;

/**
 * Defines the lifecycle states of an order, including approval steps.
 */
public enum OrderStatus {
    PENDING_ADMIN_APPROVAL,
    REJECTED_BY_ADMIN,
    PENDING_VENDOR_APPROVAL,
    REJECTED_BY_VENDOR,
    PREPARING,
    READY_FOR_PICKUP,
    ON_THE_WAY,
    COMPLETED,
    CANCELLED
}