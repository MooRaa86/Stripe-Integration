package com.Stripe.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCheckoutRequest(

        @NotNull
        UUID orderId
) {
}