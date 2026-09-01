package com.Stripe.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StripeAmountConverterTest {

    @Test
    void convertsMajorUnitsToSmallestUnits() {
        assertEquals(1999L,
                StripeAmountConverter.toSmallestUnits(
                        new BigDecimal("19.99")));
        assertEquals(100L,
                StripeAmountConverter.toSmallestUnits(
                        new BigDecimal("1.00")));
    }

    @Test
    void convertsSmallestUnitsToMajorUnits() {
        assertEquals(new BigDecimal("19.99"),
                StripeAmountConverter.fromSmallestUnits(1999L));
        assertEquals(new BigDecimal("1.00"),
                StripeAmountConverter.fromSmallestUnits(100L));
    }

    @Test
    void conversionsAreLossless() {
        BigDecimal major = new BigDecimal("12.34");
        long smallest = StripeAmountConverter.toSmallestUnits(major);
        assertEquals(major, StripeAmountConverter.fromSmallestUnits(smallest));
    }
}
