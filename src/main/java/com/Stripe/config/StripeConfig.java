package com.Stripe.config;

import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StripeConfig {
    @Value("${stripe.secret-key}")
    private String secretKey;

    public StripeConfig(
            @Value("${stripe.secret-key}") String secretKey
    ) {
        Stripe.apiKey = secretKey;
    }
}
