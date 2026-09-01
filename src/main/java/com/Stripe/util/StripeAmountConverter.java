package com.Stripe.util;

import java.math.BigDecimal;

/**
 * Converts monetary amounts between BigDecimal major currency units and
 * Stripe's integer smallest currency units. Every amount sent to or received
 * from Stripe is expressed in the smallest unit (for example cents), while the
 * application stores amounts in major units.
 */
public final class StripeAmountConverter {

    private StripeAmountConverter() {
    }

    /**
     * Converts a major currency amount (for example 19.99) to Stripe smallest
     * units (1999).
     */
    public static long toSmallestUnits(BigDecimal amount) {
        return amount.movePointRight(2).longValue();
    }

    /**
     * Converts Stripe smallest units (1999) to a major currency amount
     * (19.99).
     */
    public static BigDecimal fromSmallestUnits(long smallestUnits) {
        return BigDecimal.valueOf(smallestUnits).movePointLeft(2);
    }
}
