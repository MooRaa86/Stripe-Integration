package com.Stripe.config;

import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the Stripe Java SDK with the application's secret key.
 * The key is read from the STRIPE_SECRET_KEY environment variable and
 * is never stored in the repository.
 */
@Configuration
public class StripeConfig {

    public StripeConfig(
            @Value("${stripe.secret-key}") String secretKey
    ) {
        Stripe.apiKey = secretKey;
    }
}
