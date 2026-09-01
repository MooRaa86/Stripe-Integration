package com.Stripe.exception;

import java.util.UUID;

/**
 * Thrown when a payment cannot be found for a given identifier.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(UUID paymentId) {
        super("Payment not found: " + paymentId);
    }

    public PaymentNotFoundException(String message) {
        super(message);
    }
}
