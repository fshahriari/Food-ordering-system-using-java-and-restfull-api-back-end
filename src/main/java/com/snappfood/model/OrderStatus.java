package com.snappfood.model;

/**
 * Defines the lifecycle states of an order, including approval and payment steps.
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    UNPAID_AND_CANCELLED,
    PENDING_ADMIN_APPROVAL,
    APPROVED_BY_ADMIN, //TODO
    REJECTED_BY_ADMIN,
    PENDING_VENDOR_APPROVAL,
    REJECTED_BY_VENDOR,
    PREPARING,
    READY_FOR_PICKUP,
    ON_THE_WAY,
    COMPLETED,
    CANCELLED
}