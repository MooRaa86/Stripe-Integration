package com.Stripe.exception;

import java.util.UUID;

/**
 * Thrown when an order cannot be found for a given identifier.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(UUID orderId) {
        super("Order not found: " + orderId);
    }
}
