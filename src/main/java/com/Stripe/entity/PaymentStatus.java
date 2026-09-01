package com.Stripe.entity;

/**
 * Lifecycle of a Payment. Only Stripe webhook confirmations advance the
 * status; client requests never change it optimistically.
 */
public enum PaymentStatus {
    PENDING,
    SUCCEEDED,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED
}